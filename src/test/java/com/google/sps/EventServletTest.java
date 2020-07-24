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
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.sps.servlets.EventServlet;
import com.google.sps.servlets.KeywordSearchServlet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

@PowerMockIgnore("okhttp3.*")
@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor({"com.google.sps.Firebase"})
@PrepareForTest({Firebase.class})
public final class EventServletTest {
  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
  private EventServlet testEventServlet;

  @Before
  public void setUp() throws IOException {
    helper.setUp();
    testEventServlet = new EventServlet();
  }

  @After
  public void tearDown() {
    helper.tearDown();
  }

  @Test
  public void postOneEventToDatastore() throws IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    TestingUtil.mockFirebase(request, "test@example.com");

    // Add a mock request to pass as a parameter to doPost.
    when(request.getParameter("event-name")).thenReturn("Lake Clean Up");
    when(request.getParameter("event-description")).thenReturn("We're cleaning up the lake");
    when(request.getParameter("street-address")).thenReturn("678 Lakeview Way");
    when(request.getParameter("city")).thenReturn("Lakeside");
    when(request.getParameter("state")).thenReturn("Michigan");
    when(request.getParameter("date")).thenReturn("2020-05-17");
    when(request.getParameter("start-time")).thenReturn("14:00");
    when(request.getParameter("end-time")).thenReturn("15:00");
    when(request.getParameter("cover-photo")).thenReturn("/img-2030121");
    String[] tags = {"environment"};
    when(request.getParameter("all-tags")).thenReturn(Utils.convertToJson(tags));

    // Post event to Datastore.
    testEventServlet.doPost(request, response);

    // Assert only one Entity was posted to Datastore.
    DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
    assertEquals(1, ds.prepare(new Query("Event")).countEntities(withLimit(10)));
    assertEquals(1, ds.prepare(new Query("Interaction")).countEntities(withLimit(10)));
  }

  @Test
  public void postMultipleEventsToDatastore() throws IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    TestingUtil.mockFirebase(request, "test@example.com");

    // Add a mock request to pass as a parameter to doPost.
    when(request.getParameter("event-name")).thenReturn("Lake Clean Up");
    when(request.getParameter("event-description")).thenReturn("We're cleaning up the lake");
    when(request.getParameter("street-address")).thenReturn("678 Lakeview Way");
    when(request.getParameter("city")).thenReturn("Lakeside");
    when(request.getParameter("state")).thenReturn("Michigan");
    when(request.getParameter("date")).thenReturn("2020-05-17");
    when(request.getParameter("start-time")).thenReturn("14:00");
    when(request.getParameter("end-time")).thenReturn("15:00");
    when(request.getParameter("cover-photo")).thenReturn("/img-2030121");
    String[] tags = {"environment"};
    when(request.getParameter("all-tags")).thenReturn(Utils.convertToJson(tags));

    // Post three events to Datastore.
    testEventServlet.doPost(request, response);
    testEventServlet.doPost(request, response);
    testEventServlet.doPost(request, response);

    // Assert all three Entities were posted to Datastore.
    DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
    assertEquals(3, ds.prepare(new Query("Event")).countEntities(withLimit(10)));
    Iterable<Entity> interactions = ds.prepare(new Query("Interaction")).asIterable();
    int size = 0;
    for (Entity e : interactions) {
      size++;
      assertEquals(Interactions.CREATE_SCORE, Integer.parseInt(e.getProperty("rating").toString()));
      assertEquals("test@example.com", e.getProperty("user").toString());
    }
    assertEquals(3, size);
  }

  @Test
  public void postEventWithAllFields() throws IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    String creatorEmail = "test@example.com";
    TestingUtil.mockFirebase(request, creatorEmail);

    // Add a mock request to pass as a parameter to doPost.
    when(request.getParameter("event-name")).thenReturn("Lake Clean Up");
    when(request.getParameter("event-description")).thenReturn("We're cleaning up the lake");
    when(request.getParameter("street-address")).thenReturn("678 Lakeview Way");
    when(request.getParameter("city")).thenReturn("Lakeside");
    when(request.getParameter("state")).thenReturn("Michigan");
    when(request.getParameter("date")).thenReturn("2020-05-17");
    when(request.getParameter("start-time")).thenReturn("14:00");
    when(request.getParameter("end-time")).thenReturn("15:00");
    when(request.getParameter("cover-photo")).thenReturn("/img-2030121");
    String[] tagsArr = {"environment"};
    String tagsStr = Utils.convertToJson(tagsArr);
    when(request.getParameter("all-tags")).thenReturn(tagsStr);

    // Post event to Datastore.
    testEventServlet.doPost(request, response);

    // Retrieve the Entity posted to Datastore.
    DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
    Entity postedEntity = ds.prepare(new Query("Event")).asSingleEntity();

    Entity goalEntity = createEntity();

    // Assert the Entity posted to Datastore has the same properties as the
    // the goalEntity.
    assertEntitiesEqual(goalEntity, postedEntity);
  }

  @Test
  public void postEventWithEmptyOptionalFields() throws IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    TestingUtil.mockFirebase(request, "test@example.com");

    // This mock request does not include optional fields end-time and cover-photo.
    when(request.getParameter("event-name")).thenReturn("Lake Clean Up");
    when(request.getParameter("event-description")).thenReturn("We're cleaning up the lake");
    when(request.getParameter("street-address")).thenReturn("678 Lakeview Way");
    when(request.getParameter("city")).thenReturn("Lakeside");
    when(request.getParameter("state")).thenReturn("Michigan");
    when(request.getParameter("date")).thenReturn("2020-05-17");
    when(request.getParameter("start-time")).thenReturn("14:00");
    String[] tags = {"environment"};
    when(request.getParameter("all-tags")).thenReturn(Utils.convertToJson(tags));

    // Post event to Datastore.
    testEventServlet.doPost(request, response);

    // Retrieve the Entity posted to Datastore.
    DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
    Entity postedEntity = ds.prepare(new Query("Event")).asSingleEntity();

    // Assert the Entity posted to Datastore has empty properties for the
    // parameters that were not in the request.
    assertEquals("", postedEntity.getProperty("endTime"));
    assertEquals("", postedEntity.getProperty("coverPhoto"));
  }

  @Test
  public void postEventWithoutLoggingIn() throws IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    TestingUtil.mockFirebase(request, "");

    when(request.getParameter("event-name")).thenReturn("Lake Clean Up");
    when(request.getParameter("event-description")).thenReturn("We're cleaning up the lake");
    when(request.getParameter("street-address")).thenReturn("678 Lakeview Way");
    when(request.getParameter("city")).thenReturn("Lakeside");
    when(request.getParameter("state")).thenReturn("Michigan");
    when(request.getParameter("date")).thenReturn("2020-05-17");
    when(request.getParameter("start-time")).thenReturn("14:00");
    String[] tags = {"environment"};
    when(request.getParameter("all-tags")).thenReturn(Utils.convertToJson(tags));

    try {
      testEventServlet.doPost(request, response);
      // doPost should throw an error because it is not logged in
      fail();
    } catch (IOException e) {
      // no entities should have been posted

      DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
      assertEquals(0, ds.prepare(new Query("Event")).countEntities());
    }
  }

  private Entity createEntity() {
    // Create what the event Entity should look like, but do not post to
    // it to Datastore.
    Entity entity = new Entity("Event");
    entity.setProperty("eventName", "Lake Clean Up");
    entity.setProperty("eventDescription", "We're cleaning up the lake");
    entity.setProperty("address", "678 Lakeview Way, Lakeside, Michigan");
    entity.setProperty("date", "Sunday, May 17, 2020");
    entity.setProperty("startTime", "2:00 PM");
    entity.setProperty("endTime", "3:00 PM");
    entity.setProperty("coverPhoto", "/img-2030121");
    entity.setProperty("creator", "test@example.com");
    entity.setProperty("attendeeCount", 0L);
    entity.setProperty("eventKey", "agR0ZXN0cgsLEgVFdmVudBgBDA");
    entity.setProperty("unformattedStart", "14:00");
    entity.setProperty("unformattedEnd", "15:00");
    entity.setProperty("unformattedDate", "2020-05-17");

    // Convert tags and set tag property.
    String[] tagsArr = {"environment"};
    String tagsStr = Utils.convertToJson(tagsArr);
    Gson gson = new Gson();
    List<String> tags = gson.fromJson(tagsStr, new TypeToken<ArrayList<String>>() {}.getType());
    entity.setIndexedProperty("tags", tags);

    // Retrieve keywords and set keyword properties.
    Map<String, Integer> keywords =
        KeywordSearchServlet.getKeywords("Lake Clean Up", "We're cleaning up the lake");
    entity.setProperty("keywords", KeywordSearchServlet.getKeywordMapKeys(keywords));
    entity.setProperty("keywordsValues", KeywordSearchServlet.getKeywordMapValues(keywords));

    return entity;
  }

  private void assertEntitiesEqual(Entity goal, Entity resultEntity) {
    Set<String> goalProperties = goal.getProperties().keySet();
    Set<String> resultProperties = resultEntity.getProperties().keySet();
    assertEquals(goalProperties.size(), resultProperties.size());
    for (String s : goalProperties) {
      if (s.equals("keywordsValues")) {
        assertTrue(
            ((goal.getProperty(s)).toString()).equals((resultEntity.getProperty(s)).toString()));
      } else {
        assertEquals(goal.getProperty(s), resultEntity.getProperty(s));
      }
    }
  }
}
