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
import static org.junit.Assert.assertEquals;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
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

  private static final Map<Long, Event> EVENT_INFO = new HashMap<>();
  private static final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

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
    Recommend.calculateRecommend();
    PreparedQuery completedRecs = ds.prepare(new Query("Recommendation"));
    int recsCount = completedRecs.countEntities(withLimit(250));
    int userCount = ds.prepare(new Query("User")).countEntities(withLimit(250));
    assertEquals(userCount, recsCount);
    int eventsCount = ds.prepare(new Query("Event")).countEntities(withLimit(250));
    for (Entity entity : completedRecs.asIterable()) {
      System.out.println(entity.getKey().getName());
      List<Long> userRecs = (List<Long>) entity.getProperty("recs");
      assertEquals(eventsCount, userRecs.size());
      for (int i = 0; i < userRecs.size() && i < 10; i++) {
        System.out.println("  " + EVENT_INFO.get(userRecs.get(i)));
      }
      System.out.println();
    }
  }

  /** Adds all info from ratings CSV and events CSV file to datastore. */
  private void addInfoToDatastore(String ratingsFile, String eventsFile)
      throws FileNotFoundException {
    final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    final Map<String, Entity> users = new HashMap<>();
    // scan events and event data
    Scanner scan = new Scanner(new File(eventsFile));
    while (scan.hasNext()) {
      Entity event = parseEventEntity(scan.nextLine());
      if (event != null) {
        datastore.put(event);
      }
    }
    scan.close();
    // scan ratings and save users
    scan = new Scanner(new File(ratingsFile));
    while (scan.hasNext()) {
      Interaction interaction = Interaction.parseInteraction(scan.nextLine());
      if (interaction != null) {
        List<String> tags = EVENT_INFO.get(interaction.eventId).getTags();
        if (tags != null) {
          float delta =
              Interactions.recordInteraction(
                  interaction.userId, interaction.eventId, interaction.rating, true);

          Entity userEntity = users.get(interaction.userId);
          if (userEntity == null) {
            Key userKey = KeyFactory.createKey("User", interaction.userId);
            userEntity = new Entity(userKey);
            users.put(interaction.userId, userEntity);
          }
          Interactions.updatePrefs(userEntity, tags, delta);
        } else {
          throw new IllegalStateException("unidentifiable event: " + interaction.eventId);
        }
      }
    }
    scan.close();
    for (String id : users.keySet()) {
      datastore.put(users.get(id));
    }
  }

  /** Parses one line from events.csv and returns as an entity. */
  private Entity parseEventEntity(String input) {
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
      Event event = new Event(eventId, eventName, eventDesc, tagsList);
      EVENT_INFO.put(eventId, event);

      // Create an event entity (other unneeded fields are omitted)
      Key eventKey = KeyFactory.createKey("Event", eventId);
      eventEntity = new Entity(eventKey);
      eventEntity.setProperty("eventName", eventName);
      eventEntity.setProperty("eventDescription", eventDesc);
      eventEntity.setProperty("address", "location"); // TODO
      eventEntity.setIndexedProperty("tags", tagsList);
      // save tag info for easier access later
    } catch (NumberFormatException e) {
      eventEntity = null;
    }
    return eventEntity;
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

    /** Parses one line from ratings.csv and returns it as an Interaction object. */
    private static Interaction parseInteraction(String line) throws FileNotFoundException {
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
  }

  /** Utility class used for CSV parsing. */
  public static class Event {
    private long eventId;
    private String eventName;
    private String eventDesc;
    private String[] tags;

    /** Creates an Event object. */
    public Event(long eventId, String eventName, String eventDesc, List<String> tagsList) {
      this.eventId = eventId;
      this.eventName = eventName;
      this.eventDesc = eventDesc;
      tags = new String[tagsList.size()];
      for (int i = 0; i < tagsList.size(); i++) {
        tags[i] = tagsList.get(i);
      }
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

    /** Returns event tags as a List. */
    public List<String> getTags() {
      List<String> result = new ArrayList<>();
      for (String t : tags) {
        result.add(t);
      }
      return result;
    }
  }
}
