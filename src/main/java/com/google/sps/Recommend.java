// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;
import org.apache.spark.ml.recommendation.ALS;
import org.apache.spark.ml.recommendation.ALSModel;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

public class Recommend {

  private static final Logger LOGGER = Logger.getLogger(Recommend.class.getName());

  // keep and use up to this many Interaction entities
  private static final int UPPER_LIMIT = 5_000_000;

  private static SparkSession spark;
  private static DatastoreService datastore;

  // keep track of ids and hashcodes -- spark requires numeric entries
  private static Map<Integer, String> userIdHash = new HashMap<>();
  private static Map<Integer, Long> eventIdHash = new HashMap<>();

  // keep track of metrics and location for each user and event
  private static Map<String, Map<String, Float>> userPrefs = new HashMap<>();
  private static Map<Long, Map<String, Integer>> eventInfo = new HashMap<>();
  private static Map<String, String> userLocations = new HashMap<>();
  private static Map<Long, String> eventLocations = new HashMap<>();

  // multipliers for score calculation
  public static final float ZERO = 0.1f;
  public static final double NO_INTERACTION = 1.5;
  public static final double ALREADY_SAVED = 0.3;
  // for distance score calculation
  public static final double DISTANCE_BASE = 1.04;
  public static final int INVALID_DISTANCE = 1000;

  // comparator to sort doubles in descending order
  public static final Comparator<Double> SCORE_DESCENDING =
      new Comparator<Double>() {
        @Override
        public int compare(Double a, Double b) {
          return Double.compare(b, a);
        }
      };

  /** Initializes datastore and other necessary items. */
  private static void init() {
    if (datastore == null) {
      LOGGER.info("initializing datastore");
      datastore = DatastoreServiceFactory.getDatastoreService();
    } else {
      LOGGER.info("datastore has already been set up");
    }

    userIdHash = new HashMap<>();
    eventIdHash = new HashMap<>();
    userPrefs = new HashMap<>();
    eventInfo = new HashMap<>();
    userLocations = new HashMap<>();
    eventLocations = new HashMap<>();
  }

  /** Initializes the sparkSession. */
  private static void sparkInit() {
    if (spark == null) {
      LOGGER.info("initializing sparksession");
      spark =
          SparkSession.builder()
              .appName("UniteByStep Recommendations")
              .config("spark.master", "local[*]")
              .getOrCreate();
    } else {
      LOGGER.info("sparksession has already been set up");
    }
  }
  
  /**
   * Rebuilds recommendation model and calculates recommendations for users.
   *
   * @param skipSpark If true, will skip the spark stage
   */
  public static void calculateRecommend(boolean skipSpark) throws IOException {
    performCalculation(skipSpark);
  }

  /**
   * Rebuilds recommendation model and calculates recommendations for users. Does not skip spark.
   */
  public static void calculateRecommend() throws IOException {
    performCalculation(false);
  }

  private static void performCalculation(boolean skipSpark) throws IOException {
    init();
    getInfoFromDatastore();
    List<Key> toDelete = new ArrayList<>();

    try {
      if (skipSpark) {
        LOGGER.info("skipping spark");
        throw new NullPointerException("skipping spark");
      }
      sparkInit();

      Dataset<Row> ratings =
          makeDataframeAndPreprocess(userIdHash.keySet(), eventIdHash.keySet(), toDelete);
      LOGGER.info("completed preprocessing dataframe");

      ALSModel model = trainModel(ratings);
      LOGGER.info("trained ALSModel for interactions");
      Dataset<Row> userRecs = model.recommendForAllUsers(150);
      List<Row> userRecsList = userRecs.collectAsList();
      // build rankings for each user
      for (Row recRow : userRecsList) {
        String userId = userIdHash.get(recRow.getInt(0));
        List<Row> predScores = recRow.getList(1);

        Map<Double, Long> userTopRecs = new TreeMap<>(SCORE_DESCENDING);
        for (Row itemRow : predScores) {
          long eventId = eventIdHash.get(itemRow.getInt(0));
          float predScore = itemRow.getFloat(1);
          if (predScore < ZERO) {
            predScore = ZERO;
          }
          double totalScore = computeScore(userId, eventId, predScore);
          addToRanking(eventId, totalScore, userTopRecs);
        }

        saveRecsToDatastore(userId, userTopRecs);
        LOGGER.info("saved recommendations for " + userId);
        userPrefs.remove(userId);
      }
      LOGGER.info("completed spark step of calculations");
    } catch (NullPointerException nullPointer) {
      // do nothing and skip the rest of spark steps
    }

    // ALSModel will ignore users that have insufficient interaction data
    for (String userId : userPrefs.keySet()) {
      Map<Double, Long> userTopRecs = new TreeMap<>(SCORE_DESCENDING);
      for (Long eventId : eventInfo.keySet()) {
        double totalScore = computeScore(userId, eventId, 1.0f);
        addToRanking(eventId, totalScore, userTopRecs);
      }
      saveRecsToDatastore(userId, userTopRecs);
      LOGGER.info("saved recommendations for " + userId);
    }

    datastore.delete(toDelete);
  }

  /**
   * Trains an ALSModel on the provided training dataset.
   *
   * @param path If specified, will save the model parameters at this path
   * @param training Will fit the model to this training dataset
   */
  public static ALSModel trainModel(Dataset<Row> training) throws IOException {
    ALS als =
        new ALS().setMaxIter(5).setUserCol("userId").setItemCol("eventId").setRatingCol("rating");
    ALSModel model = als.fit(training);
    model.setColdStartStrategy("drop");
    return model;
  }

  /** Queries datastore and populates data maps. */
  private static void getInfoFromDatastore() {
    // get user entities with their preferences
    LOGGER.info("querying user and event information from datastore");
    Iterable<Entity> queriedUsers = datastore.prepare(new Query("User")).asIterable();
    for (Entity e : queriedUsers) {
      String id = e.getKey().getName();
      userPrefs.put(id, Interactions.buildVectorForUser(e));
      userIdHash.put(id.hashCode(), id);
      if (e.hasProperty("location")) {
        String location = e.getProperty("location").toString();
        if (location.length() > 0) {
          userLocations.put(id, location);
        }
      }
    }
    LOGGER.info("completed query of users from datastore");

    // get event entities
    Iterable<Entity> queriedEvents = datastore.prepare(new Query("Event")).asIterable();
    for (Entity e : queriedEvents) {
      long id = e.getKey().getId();
      eventInfo.put(id, Interactions.buildVectorForEvent(e));
      eventIdHash.put((Long.toString(id)).hashCode(), id);
      eventLocations.put(id, e.getProperty("address").toString());
    }
    LOGGER.info("completed query of events from datastore");
  }

  /**
   * Constructs ratings dataframe. Also flushes outdated Interaction entities from datastore.
   *
   * @param users List of users that datastore is keeping track of.
   * @param events List of existing events that datastore is keeping track of.
   * @param toDelete Save list of keys to delete, if interaction entities need to be flushed.
   */
  private static Dataset<Row> makeDataframeAndPreprocess(
      Set<Integer> users, Set<Integer> events, List<Key> toDelete) {
    LOGGER.info("querying interactions from datastore and initializng dataframe.");
    Iterable<Entity> queriedInteractions =
        datastore
            .prepare(new Query("Interaction").addSort("timestamp", Query.SortDirection.DESCENDING))
            .asIterable();
    LOGGER.info("queried interactions from datastore");
    Iterator<Entity> itr = queriedInteractions.iterator();
    int count = 0;
    List<EventRating> ratings = new ArrayList<>();
    while (itr.hasNext() && count < UPPER_LIMIT) {
      Entity entity = itr.next();
      // convert Entities to spark-friendly format
      EventRating rate = EventRating.parseEntity(entity);
      if (rate != null) {
        if (!users.contains(rate.getUserId()) || !events.contains(rate.getEventId())) {
          // delete interaction of user or event id is invalid
          toDelete.add(entity.getKey());
        } else {
          count++;
          ratings.add(rate);
        }
      } else {
        // something wrong with this entry, delete it
        toDelete.add(entity.getKey());
      }
    }
    // delete the oldest Interaction entries from datastore
    while (itr.hasNext()) {
      toDelete.add(itr.next().getKey());
    }
    if (ratings.size() == 0) {
      return null;
    }
    return spark.createDataFrame(ratings, EventRating.class);
  }

  /**
   * Computes cumulative total score for given user and event.
   *
   * @param baseScore Base score to apply multipliers to.
   */
  public static double computeScore(String userId, long eventId, float baseScore) {
    double dotProduct = Interactions.dotProduct(userPrefs.get(userId), eventInfo.get(eventId));
    if (Math.abs(dotProduct) < ZERO) {
      dotProduct = ZERO;
    }
    double totalScore = dotProduct * baseScore;
    // adjust scaling based on user's past interaction with event
    Entity interactionEntity = Interactions.hasInteraction(userId, eventId);
    if (interactionEntity == null) {
      totalScore *= NO_INTERACTION;
    } else {
      float interactionScore = Float.parseFloat(interactionEntity.getProperty("rating").toString());
      if (interactionScore >= Interactions.SAVE_SCORE) {
        totalScore *= ALREADY_SAVED;
      }
    }

    String userLocation = userLocations.get(userId);
    String eventLocation = eventLocations.get(eventId);
    if (userLocation != null && eventLocations != null) {
      int distance = Utils.getDistance(userLocation, eventLocation);
      if (distance < 0) {
        distance = INVALID_DISTANCE;
      }
      totalScore /= Math.pow(DISTANCE_BASE, distance);
    }

    totalScore = Math.round(totalScore * 1000.0) / 1000.0;
    return totalScore;
  }

  /** Adds event and its calculated score to rankings map. */
  public static void addToRanking(long eventId, double score, Map<Double, Long> ranking) {
    while (ranking.containsKey(score)) {
      Long otherWithScore = ranking.get(score);
      ranking.put(score, eventId);
      eventId = otherWithScore;
      score -= 0.001;
    }
    ranking.put(score, eventId);
  }

  /** Stores a recommendation datastore entry for the given user ID. */
  private static void saveRecsToDatastore(String userId, Map<Double, Long> recs) {
    List<Long> recsList = new ArrayList<>();
    for (Double score : recs.keySet()) {
      recsList.add(recs.get(score));
    }
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Key recKey = KeyFactory.createKey("Recommendation", userId);
    Entity recEntity = null;
    try {
      recEntity = datastore.get(recKey);
    } catch (EntityNotFoundException e) {
      recEntity = new Entity(recKey);
    }
    recEntity.setProperty("recs", recsList);
    datastore.put(recEntity);
  }
}
