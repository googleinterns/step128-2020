// Copyright 2019 Google LLC
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.ml.recommendation.ALSModel;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** A class to make sure that Spark works as intended. */
@RunWith(JUnit4.class)
public final class RecommendTest {
  // file paths
  private static final String RATINGS = "src/test/data/userdatav2.csv";
  private static final String EVENTS = "src/test/data/events.csv";
  private static final String MODEL_PATH = "src/test/data/eventsmodel";

  // dataframes only take numeric info, so event items, user ids are mapped to their hashcodes for
  // printing info
  private static final Map<Integer, Event> EVENT_INFO = new HashMap<>();
  private static final Map<Integer, String> HASH_NAMES = new HashMap<>();

  private static SparkSession spark;

  // active datasets
  private static Dataset<Row> ratings;
  private static Dataset<Row> training;
  private static Dataset<Row> test;

  /** Initializes the spark session and reads in data from CSV files. */
  @Before
  public void setUp() throws IOException {
    // sets up sparksession and reads in CSV data
    spark =
        SparkSession.builder()
            .appName("Java Spark SQL basic example")
            .config("spark.master", "local[*]")
            .getOrCreate();
    spark.sparkContext().setLogLevel("ERROR");

    JavaRDD<EventRating> ratingsRdd =
        spark
            .read()
            .textFile(RATINGS)
            .javaRDD()
            .map(EventRating::parseRating)
            .filter(rating -> rating != null);
    ratings = spark.createDataFrame(ratingsRdd, EventRating.class);

    getEvents(EVENTS);
  }

  @Test
  public void doTests() throws IOException {
    testMultiple(1);
  }

  /** Builds and evaluates model multiple times, printing out the average RMSE. */
  public final void testMultiple(int count) throws IOException {
    double rmse = 0;
    for (int i = 0; i < count; i++) {
      System.out.println("Running test " + (i + 1) + " out of " + count);
      rmse += test();
    }
    System.out.println("Average RMSE: " + rmse / count);
  }

  /** Builds and evaluates model once, printing all relevant results. */
  public double test() throws IOException {
    Dataset<Row>[] splits = ratings.randomSplit(new double[] {0.8, 0.2});
    training = splits[0];
    test = splits[1];
    ALSModel model = Recommend.trainModel(MODEL_PATH, training);

    Dataset<Row> predictions = model.transform(test);
    double rmse = Recommend.evaluatePredictions(predictions);

    Dataset<Row> userRecs = model.recommendForAllUsers(10);

    List<Row> predList = predictions.collectAsList();
    List<Row> recsList = userRecs.collectAsList();
    printRecs(recsList);
    printPreds(predList);
    System.out.println("Root-mean-square error = " + rmse);
    return rmse;
  }

  /** Prints and parses relevant information from predicitions dataframe. */
  private void printPreds(List<Row> preds) {
    for (Row r : preds) {
      int eventHash = r.getInt(0);
      float actual = r.getFloat(1);
      int userHash = r.getInt(2);
      float predicted = r.getFloat(3);
      System.out.println(
          EVENT_INFO.get(eventHash).getEventName()
              + "  "
              + HASH_NAMES.get(userHash)
              + " predicted: "
              + predicted
              + " actual: "
              + actual);
    }
  }

  /** Prints and parses relevant information from recommendations dataframe. */
  private void printRecs(List<Row> recs) {
    for (Row r : recs) {
      List<Row> o = r.getList(1);
      int hash = r.getInt(0);
      System.out.println(HASH_NAMES.get(hash));
      for (Row row : o) {
        System.out.println(
            "  " + EVENT_INFO.get(row.getInt(0)).getEventName() + "   " + row.getFloat(1));
      }
    }
  }

  /** Builds event list from CSV file. */
  private void getEvents(String path) throws FileNotFoundException {
    Scanner scan = new Scanner(new File(path));
    while (scan.hasNext()) {
      String line = scan.nextLine();
      Event event = Event.parseEvent(line);
      if (event != null) {
        EVENT_INFO.put((Long.toString(event.getId())).hashCode(), event);
      }
    }
    scan.close();
  }

  /** Utility class for parsing CSV and converting it to dataframe-friendly format. */
  public static class EventRating {
    private int userId;
    private int eventId;
    private float rating;

    public EventRating() {}

    /** Creates an EventRating object and saves it to the hashcode - userId mapping. */
    public EventRating(String userId, long eventId, float rating) {
      this.userId = userId.hashCode(); // all fields must be numeric
      this.eventId = (Long.toString(eventId)).hashCode();
      this.rating = rating;

      HASH_NAMES.put(this.userId, userId);
    }

    public int getUserId() {
      return userId;
    }

    public int getEventId() {
      return eventId;
    }

    public float getRating() {
      return rating;
    }

    /** Creates and returns EventRating object from a CSV input line. */
    public static EventRating parseRating(String str) {
      String[] fields = str.split(",");
      if (fields.length < 3) {
        return null;
      }
      try {
        String userId = fields[0];
        float rating = Float.parseFloat(fields[1]);
        long eventId = Long.parseLong(fields[2]);
        return new EventRating(userId, eventId, rating);
      } catch (NumberFormatException e) {
        return null;
      }
    }
  }

  /** Utility class used for CSV parsing. */
  public static class Event {
    private long eventId;
    private String eventName;
    private String eventDesc;
    private String[] tags;

    /** Creates an Event object. */
    public Event(long eventId, String eventName, String eventDesc, String[] tags) {
      this.eventId = eventId;
      this.eventName = eventName;
      this.eventDesc = eventDesc;
      this.tags = tags;
    }

    public String toString() {
      return eventName + "    " + eventDesc + "   " + String.join(",", tags);
    }

    public long getId() {
      return eventId;
    }

    public String getEventName() {
      return eventName;
    }

    /** Creates and returns Event object from a CSV input line. */
    public static Event parseEvent(String str) {
      String[] fields = str.split(",");
      if (fields.length != 4) {
        return null;
      }
      try {
        long eventId = Long.parseLong(fields[0]);
        String eventName = fields[1];
        String eventDesc = fields[2];
        String[] tags = fields[3].split("-");
        return new Event(eventId, eventName, eventDesc, tags);
      } catch (NumberFormatException e) {
        return null;
      }
    }
  }
}
