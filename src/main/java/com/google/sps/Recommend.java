package com.google.sps;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.spark.ml.evaluation.RegressionEvaluator;
import org.apache.spark.ml.recommendation.ALS;
import org.apache.spark.ml.recommendation.ALSModel;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

public class Recommend {

  private static final int UPPER_LIMIT = 5_000_000;

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
    Iterable<Entity> queriedUsers = datastore.prepare(new Query("User")).asIterable();
    // get user prefs
    Map<Integer, String> userIdHash = new HashMap<>();
    Map<String, Map<String, Integer>> userPrefs = new HashMap<>();
    for (Entity e : queriedUsers) {
      String id = e.getKey().getName();
      userPrefs.put(id, Interactions.buildVectorForEntity(e));
      userIdHash.put(id.hashCode(), id);
    }
    Map<Integer, Long> eventIdHash = new HashMap<>();
    Dataset<Row> ratings = makeRatingsDataframe(eventIdHash);
    ALSModel model = trainModel(null, ratings);

    Dataset<Row> userRecs = model.recommendForAllUsers(150);
    List<Row> userRecsList = userRecs.collectAsList();
    for (Row recRow : userRecsList) {
      int hash = recRow.getInt(0); // hashcode of user id
      List<Row> predScores = recRow.getList(1); // id-score pairs for items

      for (Row itemRow : predScores) {
        int itemHash = itemRow.getInt(0); // hashcode of event id
        float predScore = itemRow.getFloat(1); // pred score
      }
    }

    Iterable<Entity> currentRecs = datastore.prepare(new Query("UserRecs")).asIterable();
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
    } catch (IOException e) {
      // do nothing
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
   * Constructs ratings dataframe. Also keeps track of event ids with their hashcodes and deletes
   * oldest Interaction entries from datastore.
   */
  private static Dataset<Row> makeRatingsDataframe(Map<Integer, Long> eventIdHash) {
    Iterable<Entity> queriedInteractions =
        datastore
            .prepare(new Query("Interaction").addSort("timestamp", Query.SortDirection.DESCENDING))
            .asIterable();
    Iterator<Entity> itr = queriedInteractions.iterator();
    int count = 0;
    // convert Entities to spark-friendly format
    List<EventRating> ratings = new ArrayList<>();
    // keep track of eventId hashcodes and eventIds
    while (itr.hasNext() && count < UPPER_LIMIT) {
      Entity entity = itr.next();
      EventRating rate = EventRating.parseEntity(entity);
      if (rate != null) {
        count++;
        ratings.add(rate);
        long eventId = Long.parseLong(entity.getProperty("event").toString());
        eventIdHash.put(rate.getEventId(), eventId);
      }
    }
    // delete the oldest Interaction entries from datastore
    List<Key> toDelete = new ArrayList<>();
    while (itr.hasNext()) {
      toDelete.add(itr.next().getKey());
    }
    datastore.delete(toDelete);
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
      // spark implicitly casts long to int
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
