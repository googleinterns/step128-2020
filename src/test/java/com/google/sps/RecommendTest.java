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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.sps.servlets.RecommendServlet;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

/** Tests to make sure that the Recommendation class works as intended. */
@PowerMockIgnore({"okhttp3.*", "org.apache.hadoop.*", "javax.*", "org.apache.xerces.*"})
@SuppressStaticInitializationFor({"com.google.sps.Firebase"})
@RunWith(PowerMockRunner.class)
@PrepareForTest({Utils.class, Firebase.class})
public final class RecommendTest {
  private static final Map<Long, Event> EVENT_INFO = new HashMap<>();
  private static final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
  private static final Gson gson = new Gson();

  /** Sets up the datastore helper. */
  @Before
  public void setUp() throws IOException {
    helper.setUp();
  }

  @After
  public void tearDown() {
    helper.tearDown();
  }

  @Test
  public void checkOutput() throws IOException {
    PowerMockito.mockStatic(Utils.class);
    PowerMockito.when(Utils.getDistance("90001", "90001")).thenReturn(0);

    // a test to make sure everything is in an expected format and runs without hiccups
    String users = "src/test/data/users-1.csv";
    String ratings = "src/test/data/ratings-1.csv";
    String events = "src/test/data/events-1.csv";
    addInfoToDatastore(events, users, ratings);
    Recommend.calculateRecommend();

    DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
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

  @Test
  public void rankFromDistance() throws IOException, Exception {
    // set up distance mocking
    PowerMockito.mockStatic(Utils.class);
    PowerMockito.when(Utils.getDistance("90045", "90045")).thenReturn(0);
    PowerMockito.when(Utils.getDistance("90045", "90301")).thenReturn(10);
    PowerMockito.when(Utils.getDistance("90045", "90305")).thenReturn(20);
    PowerMockito.when(Utils.getDistance("90045", "90047")).thenReturn(30);
    PowerMockito.when(Utils.getDistance("90045", "90003")).thenReturn(40);
    PowerMockito.when(Utils.getDistance("90003", "90003")).thenReturn(0);
    PowerMockito.when(Utils.getDistance("90003", "90047")).thenReturn(10);
    PowerMockito.when(Utils.getDistance("90003", "90305")).thenReturn(20);
    PowerMockito.when(Utils.getDistance("90003", "90301")).thenReturn(30);
    PowerMockito.when(Utils.getDistance("90003", "90045")).thenReturn(40);

    // check that recommendation ranks distances correctly
    String users = "src/test/data/users-2.csv";
    String ratings = "src/test/data/ratings-none.csv";
    String events = "src/test/data/events-2.csv";
    addInfoToDatastore(events, users, ratings);
    Recommend.calculateRecommend();

    DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery completedRecs = ds.prepare(new Query("Recommendation"));
    int recsCount = completedRecs.countEntities(withLimit(250));
    int userCount = ds.prepare(new Query("User")).countEntities(withLimit(250));
    assertEquals(userCount, recsCount);

    int eventsCount = ds.prepare(new Query("Event")).countEntities(withLimit(250));
    try {
      Key key1 = KeyFactory.createKey("Recommendation", "test@example.com");
      Entity user1 = ds.get(key1);
      List<Long> userRecs1 = (List<Long>) user1.getProperty("recs");
      assertEquals(eventsCount, userRecs1.size());
      for (int i = 0; i < userRecs1.size() - 1; i++) {
        assertTrue(userRecs1.get(i) < userRecs1.get(i + 1));
      }

      Key key2 = KeyFactory.createKey("Recommendation", "another@example.com");
      Entity user2 = ds.get(key2);
      List<Long> userRecs2 = (List<Long>) user2.getProperty("recs");
      assertEquals(eventsCount, userRecs2.size());
      for (int i = 0; i < userRecs2.size() - 1; i++) {
        assertTrue(userRecs2.get(i) > userRecs2.get(i + 1));
      }
    } catch (EntityNotFoundException e) {
      fail();
    }
  }

  @Test
  public void rankWithoutInteractions() throws IOException {
    PowerMockito.mockStatic(Utils.class);
    PowerMockito.when(Utils.getDistance("90045", "90045")).thenReturn(0);

    // test that ranks are calculated correctly when interactions and locations are held constant
    String users = "src/test/data/users-3.csv";
    String ratings = "src/test/data/ratings-none.csv";
    String events = "src/test/data/events-3.csv";
    addInfoToDatastore(events, users, ratings);
    Recommend.calculateRecommend();

    DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
    try {
      Key key1 = KeyFactory.createKey("Recommendation", "test@example.com");
      Entity user1 = ds.get(key1);
      List<Long> userRecs1 = (List<Long>) user1.getProperty("recs");
      for (int i = 0; i < userRecs1.size() - 1; i++) {
        assertTrue(userRecs1.get(i) < userRecs1.get(i + 1));
      }

      Key key2 = KeyFactory.createKey("Recommendation", "another@example.com");
      Entity user2 = ds.get(key2);
      List<Long> userRecs2 = (List<Long>) user2.getProperty("recs");
      for (int i = 0; i < userRecs2.size() - 1; i++) {
        assertTrue(userRecs2.get(i) > userRecs2.get(i + 1));
      }
    } catch (EntityNotFoundException e) {
      fail();
    }
  }

  @Test
  public void noRecommendationsServlet() throws IOException {
    // similar to rankFromDistance(), but performed through the servlet instead
    PowerMockito.mockStatic(Utils.class);
    PowerMockito.when(Utils.getDistance("90045", "90045")).thenReturn(0);
    PowerMockito.when(Utils.getDistance("90045", "90301")).thenReturn(10);
    PowerMockito.when(Utils.getDistance("90045", "90305")).thenReturn(20);
    PowerMockito.when(Utils.getDistance("90045", "90047")).thenReturn(30);
    PowerMockito.when(Utils.getDistance("90045", "90003")).thenReturn(40);

    String users = "src/test/data/users-2.csv";
    String ratings = "src/test/data/ratings-none.csv";
    String events = "src/test/data/events-2.csv";
    addInfoToDatastore(events, users, ratings);
    Key recKey = KeyFactory.createKey("Recommendation", "test@example.com");
    DatastoreService ds = DatastoreServiceFactory.getDatastoreService();

    try {
      Entity user = ds.get(recKey);
      fail();
    } catch (EntityNotFoundException e) {
      // recommendations should not exist yet
    }

    List<Entity> results = callGet("test@example.com");
    assertEquals(5, results.size());
    for (int i = 0; i < results.size() - 1; i++) {
      assertTrue(results.get(i).getKey().getId() < results.get(i + 1).getKey().getId());
    }

    // check that new recommendation item has been created correctly
    try {
      Entity user = ds.get(recKey);
      List<Long> userRecs = (List<Long>) user.getProperty("recs");
      for (int i = 0; i < userRecs.size() - 1; i++) {
        assertTrue(userRecs.get(i) < userRecs.get(i + 1));
      }
    } catch (EntityNotFoundException e) {
      fail();
    }
  }

  @Test
  public void returnRecommendations() throws IOException {
    // similar to rankFromDistance(), but performed through the servlet instead
    PowerMockito.mockStatic(Utils.class);
    PowerMockito.when(Utils.getDistance("90045", "90045")).thenReturn(0);
    PowerMockito.when(Utils.getDistance("90045", "90301")).thenReturn(10);
    PowerMockito.when(Utils.getDistance("90045", "90305")).thenReturn(20);
    PowerMockito.when(Utils.getDistance("90045", "90047")).thenReturn(30);
    PowerMockito.when(Utils.getDistance("90045", "90003")).thenReturn(40);
    PowerMockito.when(Utils.getDistance("90003", "90003")).thenReturn(0);
    PowerMockito.when(Utils.getDistance("90003", "90047")).thenReturn(10);
    PowerMockito.when(Utils.getDistance("90003", "90305")).thenReturn(20);
    PowerMockito.when(Utils.getDistance("90003", "90301")).thenReturn(30);
    PowerMockito.when(Utils.getDistance("90003", "90045")).thenReturn(40);

    // check that recommendation ranks distances correctly
    String users = "src/test/data/users-2.csv";
    String ratings = "src/test/data/ratings-none.csv";
    String events = "src/test/data/events-2.csv";
    addInfoToDatastore(events, users, ratings);
    Recommend.calculateRecommend();
    List<Entity> results = callGet("test@example.com");
    assertEquals(5, results.size());
    for (int i = 0; i < results.size() - 1; i++) {
      assertTrue(results.get(i).getKey().getId() < results.get(i + 1).getKey().getId());
    }
  }

  @Test
  public void problematicEntity() throws IOException {
    PowerMockito.mockStatic(Utils.class);
    PowerMockito.when(Utils.getDistance("90045", "90045")).thenReturn(0);
    PowerMockito.when(Utils.getDistance("90045", "90301")).thenReturn(10);
    PowerMockito.when(Utils.getDistance("90045", "90305")).thenReturn(20);
    PowerMockito.when(Utils.getDistance("90045", "90047")).thenReturn(30);
    PowerMockito.when(Utils.getDistance("90045", "90003")).thenReturn(40);

    String users = "src/test/data/users-2.csv";
    String ratings = "src/test/data/ratings-none.csv";
    String events = "src/test/data/events-2.csv";
    addInfoToDatastore(events, users, ratings);

    // make sure servlet handles the problematic Recommendation entity correctly
    DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
    Entity recEntity = new Entity("Recommendation", "test@example.com");
    ds.put(recEntity);

    List<Entity> results = callGet("test@example.com");
    assertEquals(5, results.size());
    for (int i = 0; i < results.size() - 1; i++) {
      assertTrue(results.get(i).getKey().getId() < results.get(i + 1).getKey().getId());
    }
  }

  @Test
  public void noResults() throws IOException {
    String users = "src/test/data/users-none.csv";
    String ratings = "src/test/data/ratings-none.csv";
    String events = "src/test/data/events-2.csv";
    addInfoToDatastore(events, users, ratings);

    List<Entity> results = callGet("");
    assertEquals(0, results.size());

    DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
    assertEquals(0, ds.prepare(new Query("User")).countEntities(withLimit(250)));
    results = callGet("test@example.com");
    assertEquals(0, results.size());

    assertEquals(1, ds.prepare(new Query("User")).countEntities(withLimit(250)));
    results = callGet("test@example.com");
    assertEquals(0, results.size());
  }

  /** Performs the GET request to retrieve event recommendations. */
  private static List<Entity> callGet(String dummyToken) throws IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    TestingUtil.mockFirebase(request, dummyToken);

    HttpServletResponse response = mock(HttpServletResponse.class);
    StringWriter out = new StringWriter();
    PrintWriter writer = new PrintWriter(out);
    when(response.getWriter()).thenReturn(writer);

    RecommendServlet recServlet = new RecommendServlet();
    recServlet.doGet(request, response);
    out.flush();
    return gson.fromJson(out.toString(), new TypeToken<ArrayList<Entity>>() {}.getType());
  }

  /** Adds all info from ratings CSV, users CSV, events CSV to datastore. */
  private void addInfoToDatastore(String eventsFile, String usersFile, String ratingsFile)
      throws FileNotFoundException, IOException {
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
    // scan users and user data
    scan = new Scanner(new File(usersFile));
    String[] fieldNames = scan.nextLine().split(",");
    if (fieldNames.length < 2) {
      throw new IOException("Please check format of input file.");
    }
    while (scan.hasNext()) {
      Entity userEntity = parseUserEntity(scan.nextLine(), fieldNames);
      if (userEntity != null) {
        String userId = userEntity.getKey().getName();
        users.put(userId, userEntity);
      }
    }
    scan.close();
    // scan ratings
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
            throw new IOException("user entity not found");
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

  /** Parses one line from event CSV and returns as an entity. */
  private Entity parseEventEntity(String input) {
    String[] fields = input.split(",");
    if (fields.length < 5) {
      return null;
    }
    Entity eventEntity = null;
    try {
      long eventId = Long.parseLong(fields[0]);
      String eventName = fields[1];
      String eventDesc = fields[2];
      List<String> tagsList = new ArrayList<>();
      String[] tags = fields[4].split("-");
      for (String t : tags) {
        tagsList.add(t);
      }
      if (fields.length > 5) {
        String[] tags2 = fields[5].split("-");
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
      eventEntity.setProperty("address", fields[3]);
      eventEntity.setIndexedProperty("tags", tagsList);
      // save tag info for easier access later
    } catch (NumberFormatException e) {
      eventEntity = null;
    }
    return eventEntity;
  }

  /** Parses one line from user CSV and returns as an entity. */
  private Entity parseUserEntity(String input, String[] fieldNames) {
    String[] fields = input.split(",");
    if (fields.length < 2) {
      return null;
    }
    Entity userEntity = null;
    try {
      String userId = fields[0];
      String userLocation = fields[1];
      Key userKey = KeyFactory.createKey("User", userId);
      userEntity = new Entity(userKey);
      userEntity.setProperty("location", userLocation);
      for (int i = 2; i < fields.length && i < fieldNames.length; i++) {
        userEntity.setProperty(fieldNames[i], Float.parseFloat(fields[i]));
      }
    } catch (NumberFormatException e) {
      userEntity = null;
    }
    return userEntity;
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

  /** Utility class used for CSV parsing and data storage. */
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
