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

import static com.google.appengine.api.datastore.FetchOptions.Builder.withLimit;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.sps.Recommend.EventRating;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.ml.recommendation.ALSModel;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** A class to make sure that Spark works as intended. */
@RunWith(JUnit4.class)
public final class RecommendTest {
  // file paths
  private static final String RATINGS = "src/test/data/ratings.csv";
  private static final String EVENTS = "src/test/data/events.csv";
  private static final String MODEL_PATH = "src/test/data/eventsmodel";

  // dataframes only take numeric info, so event items, user ids are mapped to their hashcodes for
  // printing info
  private static final Map<Integer, Event> EVENT_INFO = new HashMap<>();
  private static final Map<Integer, String> HASH_NAMES = new HashMap<>();

  private static final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  // active datasets
  private static Dataset<Row> ratings;
  private static Dataset<Row> training;
  private static Dataset<Row> test;

  /** Initializes the spark session and reads in data from CSV files. */
  @Before
  public void setUp() throws IOException {
    helper.setUp();
  }

  @After
  public void tearDown() {
    helper.tearDown();
  }

  @Test
  public void doTests() throws IOException {

    addInfoToDatastore(RATINGS, EVENTS);
    DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
    System.out.println(ds.prepare(new Query("Interaction")).countEntities(withLimit(250)));
    System.out.println(ds.prepare(new Query("Event")).countEntities(withLimit(250)));
    System.out.println(ds.prepare(new Query("User")).countEntities(withLimit(250)));
    Recommend.calculateRecommend();
  }

  /** Adds all info from ratings CSV and events CSV file to datastore */
  private void addInfoToDatastore(String ratingsFile, String eventsFile)
      throws FileNotFoundException {
    final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    final Map<Long, List<String>> eventInfo = new HashMap<>();
    final Map<String, Entity> users = new HashMap<>();
    final Map<Long, Long> fakeAndRealIds = new HashMap<>();
    // scan events and event data
    Scanner scan = new Scanner(new File(eventsFile));
    while (scan.hasNext()) {
      String line = scan.nextLine();
      Entity event = parseEventEntity(line, eventInfo);
      if (event != null) {
        datastore.put(event);
      }
    }
    scan.close();
    for (Entity entity : datastore.prepare(new Query("Event")).asIterable()) {
      long id = entity.getKey().getId();
      long otherId = Long.parseLong(entity.getProperty("eventId").toString());
      fakeAndRealIds.put(otherId, id);
    }
    // scan ratings and save users
    scan = new Scanner(new File(ratingsFile));
    while (scan.hasNext()) {
      Interaction interaction = parseInteraction(scan.nextLine());
      if (interaction != null) {
        List<String> tags = eventInfo.get(interaction.eventId);
        if (tags != null) {
          float delta =
              Interactions.recordInteraction(
                  interaction.userId,
                  fakeAndRealIds.get(interaction.eventId),
                  interaction.rating,
                  true);

          Entity userEntity = users.get(interaction.userId);
          if (userEntity == null) {
            Key userKey = KeyFactory.createKey("User", interaction.userId);
            userEntity = new Entity(userKey);
            users.put(interaction.userId, userEntity);
          }
          Interactions.updatePrefs(userEntity, tags, delta);
        }
      }
    }
    for (String id : users.keySet()) {
      datastore.put(users.get(id));
    }
  }

  /** Parses one line from ratings.csv and returns it as an Interaction object. */
  private Interaction parseInteraction(String line) throws FileNotFoundException {
    String[] fields = line.split(",");
    if (fields.length < 3) {
      return null;
    }
    try {
      String userId = fields[0];
      float rating = Float.parseFloat(fields[1]);
      long eventId = Long.parseLong(fields[2]);
      return new Interaction(userId, eventId, rating);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  /**
   * Parses one line from events.csv and returns as an entity.
   *
   * @param input A single input line.
   * @param eventInfo A place to keep track of event Ids and corresponding tags.
   */
  private Entity parseEventEntity(String input, Map<Long, List<String>> eventInfo) {
    String[] fields = input.split(",");
    if (fields.length < 4) {
      return null;
    }
    Entity eventEntity = null;
    try {
      long eventId = Long.parseLong(fields[0]);
      String eventName = fields[1];
      String eventDesc = fields[2];
      List<String> tagsList = new ArrayList<>();
      String[] tags = fields[3].split("-");
      for (String t : tags) {
        tagsList.add(t);
      }
      if (fields.length > 4) {
        String[] tags2 = fields[4].split("-");
        for (String t : tags2) {
          tagsList.add(t);
        }
      }
      // Create an event entity (other unneeded fields are omitted)
      eventEntity = new Entity("Event");
      eventEntity.setProperty("eventName", eventName);
      eventEntity.setProperty("eventDescription", eventDesc);
      eventEntity.setProperty("address", ""); // TODO
      eventEntity.setIndexedProperty("tags", tagsList);
      eventEntity.setProperty("eventId", eventId);
      // save tag info for easier access later
      eventInfo.put(eventId, tagsList);
    } catch (NumberFormatException e) {
      eventEntity = null;
    }
    return eventEntity;
  }

  /** Builds and evaluates model once, printing all relevant results. */
  public double test() throws IOException {
    SparkSession spark =
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

    Dataset<Row>[] splits = ratings.randomSplit(new double[] {0.8, 0.2});
    training = splits[0];
    test = splits[1];
    // ALSModel model = Recommend.trainModel(MODEL_PATH, training);
    ALSModel model = ALSModel.load(MODEL_PATH);

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

  private static class Interaction {
    private String userId;
    private long eventId;
    private float rating;

    private Interaction(String userId, long eventId, float rating) {
      this.userId = userId;
      this.eventId = eventId;
      this.rating = rating;
    }
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
      if (fields.length < 4) {
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
