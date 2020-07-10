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
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.sps.servlets.EventServlet;
import com.google.sps.servlets.Utils;
import java.io.IOException;
import java.util.Arrays;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@PowerMockIgnore("okhttp3.*")
@RunWith(PowerMockRunner.class)
@PrepareForTest(UserServiceFactory.class)
public final class EventServletTest {
  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
  private EventServlet testEventServlet;

  @Before
  public void setUp() throws IOException {
    helper.setUp();
    testEventServlet = new EventServlet();
    TestingUtil.setUp();
  }

  @After
  public void tearDown() {
    helper.tearDown();
    TestingUtil.tearDown();
  }

  @Test
  public void postOneEventToDatastore() throws IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    TestingUtil.toggleLogin("test@example.com");

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
  }

  @Test
  public void postMultipleEventsToDatastore() throws IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    TestingUtil.toggleLogin("test@example.com");

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
  }

  @Test
  public void postEventWithAllFields() throws IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    String creatorEmail = "test@example.com";
    TestingUtil.toggleLogin(creatorEmail);

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

    // Create what the event Entity should look like, but do not post to
    // it to Datastore.
    Entity goalEntity = new Entity("Event");
    goalEntity.setProperty("eventName", "Lake Clean Up");
    goalEntity.setProperty("eventDescription", "We're cleaning up the lake");
    goalEntity.setProperty("address", "678 Lakeview Way, Lakeside, Michigan");
    goalEntity.setProperty("date", "Sunday, May 17, 2020");
    goalEntity.setProperty("startTime", "2:00 PM");
    goalEntity.setProperty("endTime", "3:00 PM");
    goalEntity.setProperty("coverPhoto", "/img-2030121");
    goalEntity.setIndexedProperty("tags", Arrays.asList(tags));
    goalEntity.setProperty("creator", creatorEmail);

    // Retrieve the Entity posted to Datastore.
    DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
    Entity postedEntity = ds.prepare(new Query("Event")).asSingleEntity();

    // Assert the Entity posted to Datastore has the same properties as the
    // the goalEntity.
    assertEquals(goalEntity.getProperties(), postedEntity.getProperties());
  }

  @Test
  public void postEventWithEmptyOptionalFields() throws IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    TestingUtil.toggleLogin("test@example.com");

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
}
