package com.google.sps;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.spark.ml.evaluation.RegressionEvaluator;
import org.apache.spark.ml.recommendation.ALS;
import org.apache.spark.ml.recommendation.ALSModel;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

public class Recommend {

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
  public static void calculateRecommend() {
    if (spark == null || datastore == null) {
      init();
    }
    Iterable<Entity> queriedUsers = datastore.prepare(new Query("User")).asIterable();
    // get user prefs
    Map<String, Map<String, Integer>> userPrefs = new HashMap<>();
    for (Entity e : queriedUsers) {
      //   userPrefs.put(e.getKey().getName(), Interactions.buildVectorForEvent(e));
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
    if (spark == null) {
      init();
    }
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

  /** Returns an existing ALSModel. */
  public static ALSModel getModel(String path) {
    return ALSModel.load(path);
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
}
