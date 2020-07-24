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

  // keep and use up to this many Interaction entities
  private static final int UPPER_LIMIT = 5_000_000;
  // multipliers for score calculation
  private static final double NO_INTERACTION = 1.5;
  private static final double ALREADY_VIEWED = 1.2;
  private static final double ALREADY_SAVED = 0.6;
  private static final float ZERO = 0.1f;

  // comparator to sort doubles in descending order
  private static final Comparator<Double> SCORE_DESCENDING =
      new Comparator<Double>() {
        @Override
        public int compare(Double a, Double b) {
          return Double.compare(b, a);
        }
      };

  private static final Logger LOGGER = Logger.getLogger(Recommend.class.getName());

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

  /** Initializes the SparkSession, datastore, and other necessary items. */
  private static void init() {
    if (spark == null) {
      spark =
          SparkSession.builder()
              .appName("Java Spark SQL basic example")
              .config("spark.master", "local[*]")
              .getOrCreate();
      spark.sparkContext().setLogLevel("ERROR");
    }

    if (datastore == null) {
      datastore = DatastoreServiceFactory.getDatastoreService();
    }

    userIdHash = new HashMap<>();
    eventIdHash = new HashMap<>();
    userPrefs = new HashMap<>();
    eventInfo = new HashMap<>();
    userLocations = new HashMap<>();
    eventLocations = new HashMap<>();
  }

  /** Rebuilds recommendation model and calculates recommendations for users. */
  public static void calculateRecommend() throws IOException {
    init();
    getInfoFromDatastore();

    List<Key> toDelete = new ArrayList<>();
    Dataset<Row> ratings =
        makeDataframeAndPreprocess(userIdHash.keySet(), eventIdHash.keySet(), toDelete);

    // compute recommendations from matrix factorization
    ALSModel model = trainModel(null, ratings);
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

        // add item to ranking
        while (userTopRecs.containsKey(totalScore)) {
          Long otherScore = userTopRecs.get(totalScore);
          userTopRecs.put(totalScore, eventId);
          eventId = otherScore;
          totalScore -= 0.01;
        }
        userTopRecs.put(totalScore, eventId);
      }

      saveRecsToDatastore(userId, userTopRecs);
      userPrefs.remove(userId);
    }

    // handle users not accounted for by ALSModel
    for (String userId : userPrefs.keySet()) {}

    datastore.delete(toDelete);
  }

  /**
   * Trains an ALSModel on the provided training dataset.
   *
   * @param path If specified, will save the model parameters at this path
   * @param training Will fit the model to this training dataset
   */
  public static ALSModel trainModel(String path, Dataset<Row> training) throws IOException {
    ALS als =
        new ALS().setMaxIter(5).setUserCol("userId").setItemCol("eventId").setRatingCol("rating");
    ALSModel model = als.fit(training);
    model.setColdStartStrategy("drop");
    if (path != null) {
      model.write().overwrite().save(path);
      LOGGER.info("model saved at " + path);
    }
    return model;
  }

  /** Queries datastore and populates data maps. */
  private static void getInfoFromDatastore() {
    // get user entities with their preferences
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

    // get event entities
    Iterable<Entity> queriedEvents = datastore.prepare(new Query("Event")).asIterable();
    for (Entity e : queriedEvents) {
      long id = e.getKey().getId();
      eventInfo.put(id, Interactions.buildVectorForEvent(e));
      eventIdHash.put((Long.toString(id)).hashCode(), id);
      eventLocations.put(id, e.getProperty("address").toString());
    }
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
    Iterable<Entity> queriedInteractions =
        datastore
            .prepare(new Query("Interaction").addSort("timestamp", Query.SortDirection.DESCENDING))
            .asIterable();
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
    return spark.createDataFrame(ratings, EventRating.class);
  }

  /**
   * Computes cumulative total score for given user and event.
   *
   * @param predScore Base score to apply multipliers to.
   */
  private static double computeScore(String userId, long eventId, float predScore) {
    float dotProduct = Interactions.dotProduct(userPrefs.get(userId), eventInfo.get(eventId));
    double totalScore = dotProduct * predScore;

    // adjust scaling based on user's past interaction with event
    Entity interactionEntity = Interactions.hasInteraction(userId, eventId);
    if (interactionEntity == null) {
      totalScore *= NO_INTERACTION;
    } else {
      float interactionScore = Float.parseFloat(interactionEntity.getProperty("rating").toString());
      if (interactionScore >= Interactions.SAVE_SCORE) {
        totalScore *= ALREADY_SAVED;
      } else if (interactionScore == Interactions.VIEW_SCORE) {
        totalScore *= ALREADY_VIEWED;
      }
    }

    String userLocation = userLocations.get(userId);
    if (userLocation != null) {
      // TODO: scale based on location
    }

    totalScore = Math.round(totalScore * 100.0) / 100.0;
    return totalScore;
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
