package com.google.sps;

import java.io.IOException;
import org.apache.spark.ml.evaluation.RegressionEvaluator;
import org.apache.spark.ml.recommendation.ALS;
import org.apache.spark.ml.recommendation.ALSModel;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

public class Recommend {

  private static SparkSession spark;

  /** Initializes the SparkSession. */
  private static void init() {
    spark =
        SparkSession.builder()
            .appName("Java Spark SQL basic example")
            .config("spark.master", "local[*]")
            .getOrCreate();
    spark.sparkContext().setLogLevel("ERROR");
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
