package com.google.sps;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.apache.spark.ml.evaluation.RegressionEvaluator;
import org.apache.spark.ml.recommendation.ALS;
import org.apache.spark.ml.recommendation.ALSModel;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

public class Recommend {

  // keep and use up to this many Interaction entities
  private static final int UPPER_LIMIT = 5_000_000;
  // multipliers for score calculation
  private static final double ALREADY_VIEWED = 1.2;
  private static final double ALREADY_SAVED = 0.6;

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

  /** Initializes the SparkSession. */
  private static void init() {
    spark =
        SparkSession.builder()
            .appName("Java Spark SQL basic example")
            .config("spark.master", "local[*]")
            .getOrCreate();
    spark.sparkContext().setLogLevel("ERROR");

    datastore = DatastoreServiceFactory.getDatastoreService();
  }

  /** Rebuilds recommendation model and calculates recommendations for users. */
  public static void calculateRecommend() throws IOException {
    if (spark == null || datastore == null) {
      init();
    }
    // keep track of ids and hashcodes -- spark requires numeric entries
    final Map<Integer, String> userIdHash = new HashMap<>();
    final Map<Integer, Long> eventIdHash = new HashMap<>();

    // keep track of metrics and location for each user and event
    final Map<String, Map<String, Integer>> userPrefs = new HashMap<>();
    final Map<Long, Map<String, Integer>> eventInfo = new HashMap<>();
    final Map<String, String> userLocations = new HashMap<>();
    final Map<Long, String> eventLocations = new HashMap<>();

    // get user entities with their preferences
    Iterable<Entity> queriedUsers = datastore.prepare(new Query("User")).asIterable();
    for (Entity e : queriedUsers) {
      String id = e.getKey().getName();
      userPrefs.put(id, Interactions.buildVectorForUser(e));
      userIdHash.put(id.hashCode(), id);
      String location = e.getProperty("location").toString();
      if (location != null && location.length() > 0) {
        userLocations.put(id, location);
      }
    }

    // get event entities
    Iterable<Entity> queriedEvents = datastore.prepare(new Query("Event")).asIterable();
    for (Entity e : queriedEvents) {
      long id = e.getKey().getId();
      eventInfo.put(id, Interactions.buildVectorForEvent(e));
      eventIdHash.put((Long.toString(id)).hashCode(), id);
      userLocations.put(id, e.getProperty("address").toString());
    }

    List<Key> toDelete = new ArrayList<>();
    Dataset<Row> ratings =
        makeDataframeAndPreprocess(userPrefs.keySet(), eventInfo.keySet(), toDelete);

    // compute recommendations from matrix factorization
    ALSModel model = trainModel(null, ratings);
    Dataset<Row> userRecs = model.recommendForAllUsers(150);
    List<Row> userRecsList = userRecs.collectAsList();
    for (Row recRow : userRecsList) {
      String userId = userIdHash.get(recRow.getInt(0));
      List<Row> predScores = recRow.getList(1);
      Map<String, Integer> userVector = userPrefs.get(userId);
      String userLocation = userLocations.get(userId);

      Map<Double, List<Long>> userTopRecs = new TreeMap<>(SCORE_DESCENDING);
      for (Row itemRow : predScores) {
        long eventId = eventIdHash.get(itemRow.getInt(0));
        float predScore = itemRow.getFloat(1);

        // calculate score for this item
        int dotProduct = Interactions.dotProduct(userVector, eventInfo.get(eventId));
        double totalScore = dotProduct * predScore; // todo: calculate

        // adjust scaling based on user's past interaction with event
        Entity interactionEntity = Interactions.hasInteraction(userId, eventId);
        if (interactionEntity == null) {
        } else {
          int interactionScore =
              Integer.parseInt(interactionEntity.getProperty("rating").toString());
          if (interactionScore >= Interactions.SAVE_SCORE) {
            totalScore *= ALREADY_SAVED;
          } else if (interactionScore == Interactions.VIEW_SCORE) {
            totalScore *= ALREADY_VIEWED;
          }
        }

        // TODO: adjust scaling based on user's location, if location is saved

        // add item to ranking
        List<Long> eventsWithScore = userTopRecs.get(totalScore);
        if (eventsWithScore == null) {
          eventsWithScore = new ArrayList<>();
          userTopRecs.put(totalScore, eventsWithScore);
        }
        eventsWithScore.add(eventId);
      }

      // TODO: save user recs
    }
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
    try {
      model.write().overwrite().save(path);
      LOGGER.info("model saved at " + path);
    } catch (IOException e) {
      // do nothing
      LOGGER.info("failed to save model");
    }
    return model;
  }

  /** Uses RMSE to evaluate a set of predicted data. */
  public static double evaluatePredictions(Dataset<Row> predictions) {
    RegressionEvaluator evaluator =
        new RegressionEvaluator()
            .setMetricName("rmse")
            .setLabelCol("rating")
            .setPredictionCol("prediction");
    return evaluator.evaluate(predictions);
  }

  /**
   * Constructs ratings dataframe. Also flushes outdated Interaction entities from datastore.
   *
   * @param users List of users that datastore is keeping track of.
   * @param events List of existing events that datastore is keeping track of.
   * @param toDelete Save list of keys to delete, if interaction entities need to be flushed.
   */
  private static Dataset<Row> makeDataframeAndPreprocess(
      Set<String> users, Set<Long> events, List<Key> toDelete) {
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
        if (!users.contains(rate.getUserId())
            || !events.contains(Long.parseLong(entity.getProperty("event").toString()))) {
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

  public static class EventRating implements Serializable {
    private int userId;
    private int eventId;
    private float rating;
    private long timestamp;

    public EventRating() {}

    public EventRating(String userId, long eventId, float rating, long timestamp) {
      // all fields must be numeric
      this.userId = userId.hashCode();
      // spark implicitly casts long to int otherwise
      this.eventId = (Long.toString(eventId)).hashCode();
      this.rating = rating;
      this.timestamp = timestamp;
    }

    // need getters to work with spark.createDataFrame()
    public int getUserId() {
      return userId;
    }

    public int getEventId() {
      return eventId;
    }

    public float getRating() {
      return rating;
    }

    public long getTimestamp() {
      return timestamp;
    }

    public static EventRating parseEntity(Entity entity) {
      if (!entity.getKind().equals("Interaction")) {
        return null;
      }
      Object userField = entity.getProperty("user");
      Object eventField = entity.getProperty("event");
      Object ratingField = entity.getProperty("rating");
      Object timeField = entity.getProperty("timestamp");
      if (userField == null || eventField == null || ratingField == null || timeField == null) {
        return null;
      }
      try {
        String userId = userField.toString();
        long eventId = Long.parseLong(eventField.toString());
        float rating = Float.parseFloat(ratingField.toString());
        long timestamp = Long.parseLong(timeField.toString());
        return new EventRating(userId, eventId, rating, timestamp);
      } catch (NumberFormatException e) {
        return null;
      }
    }

    public String toString() {
      return userId + " " + eventId + " " + rating + " " + timestamp;
    }
  }
}
