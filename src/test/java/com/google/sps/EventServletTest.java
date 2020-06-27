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
import static org.mockito.Mockito.*;

import com.google.sps.servlets.EventServlet;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import java.io.IOException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.Mock;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.FetchOptions;

/** */
@RunWith(JUnit4.class)
public final class EventServletTest {
  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
  private HttpServletRequest mockedRequest;
  private HttpServletResponse mockedResponse;
  private EventServlet testEventServlet;

  @Before
  public void setUp() {
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

    when(request.getParameter("event-name")).thenReturn("Lake Clean Up");
    when(request.getParameter("event-description")).thenReturn("We're cleaning up the lake");
    when(request.getParameter("street-address")).thenReturn("678 Lakeview Way");
    when(request.getParameter("city")).thenReturn("Lakeside");
    when(request.getParameter("state")).thenReturn("Michigan");
    when(request.getParameter("date")).thenReturn("2020-17-05");
    when(request.getParameter("start-time")).thenReturn("14:00");
    when(request.getParameter("end-time")).thenReturn("15:00");
    when(request.getParameter("cover-photo")).thenReturn("/img-2030121");
    when(request.getParameter("all-tags")).thenReturn("[environment]");

    testEventServlet.doPost(request, response);

    DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
    assertEquals(1, ds.prepare(new Query("Event")).countEntities(withLimit(10)));
  }

  @Test
  public void postMultipleEventsToDatastore() throws IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);       
    HttpServletResponse response = mock(HttpServletResponse.class);    

    when(request.getParameter("event-name")).thenReturn("Lake Clean Up");
    when(request.getParameter("event-description")).thenReturn("We're cleaning up the lake");
    when(request.getParameter("street-address")).thenReturn("678 Lakeview Way");
    when(request.getParameter("city")).thenReturn("Lakeside");
    when(request.getParameter("state")).thenReturn("Michigan");
    when(request.getParameter("date")).thenReturn("2020-17-05");
    when(request.getParameter("start-time")).thenReturn("14:00");
    when(request.getParameter("end-time")).thenReturn("15:00");
    when(request.getParameter("cover-photo")).thenReturn("/img-2030121");
    when(request.getParameter("all-tags")).thenReturn("['environment']");

    testEventServlet.doPost(request, response);
    testEventServlet.doPost(request, response);
    testEventServlet.doPost(request, response);

    DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
    assertEquals(3, ds.prepare(new Query("Event")).countEntities(withLimit(10)));
  }

  @Test
  public void postEventWithAllFields() throws IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);       
    HttpServletResponse response = mock(HttpServletResponse.class);    

    // Add a mock request to pass as a parameter to doPost.
    when(request.getParameter("event-name")).thenReturn("Lake Clean Up");
    when(request.getParameter("event-description")).thenReturn("We're cleaning up the lake");
    when(request.getParameter("street-address")).thenReturn("678 Lakeview Way");
    when(request.getParameter("city")).thenReturn("Lakeside");
    when(request.getParameter("state")).thenReturn("Michigan");
    when(request.getParameter("date")).thenReturn("2020-17-05");
    when(request.getParameter("start-time")).thenReturn("14:00");
    when(request.getParameter("end-time")).thenReturn("15:00");
    when(request.getParameter("cover-photo")).thenReturn("/img-2030121");
    when(request.getParameter("all-tags")).thenReturn("['environment']");

    // Post event to Datastore.
    testEventServlet.doPost(request, response);

    // Create what the event Entity should look like, but do not post to 
    // it to Datastore.
    Entity goalEntity = new Entity("Event");
    goalEntity.setProperty("eventName", "Lake Clean Up");
    goalEntity.setProperty("eventDescription", "We're cleaning up the lake");
    goalEntity.setProperty("streetAddress", "678 Lakeview Way");
    goalEntity.setProperty("city", "Lakeside");
    goalEntity.setProperty("state", "Michigan");
    goalEntity.setProperty("date", "2020-17-05");
    goalEntity.setProperty("startTime", "14:00");
    goalEntity.setProperty("endTime", "15:00");
    goalEntity.setProperty("coverPhoto", "/img-2030121");
    goalEntity.setProperty("tags", "['environment']");

    // Retrieve the Entity posted to Datastore.
    DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
    Entity postedEntity = ds.prepare(new Query("Event")).asSingleEntity();
    
    // Assert the Entity posted to data store has the same properties as the
    // the goalEntity
    assertEquals(goalEntity.getProperties(), postedEntity.getProperties());
  }

  @Test
  public void postEventWithEmptyFields() throws IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);       
    HttpServletResponse response = mock(HttpServletResponse.class);    

    // This mock request does not include optional fields end-time and cover-photo.
    when(request.getParameter("event-name")).thenReturn("Lake Clean Up");
    when(request.getParameter("event-description")).thenReturn("We're cleaning up the lake");
    when(request.getParameter("street-address")).thenReturn("678 Lakeview Way");
    when(request.getParameter("city")).thenReturn("Lakeside");
    when(request.getParameter("state")).thenReturn("Michigan");
    when(request.getParameter("date")).thenReturn("2020-17-05");
    when(request.getParameter("start-time")).thenReturn("14:00");
    when(request.getParameter("all-tags")).thenReturn("['environment']");

    // Post event to Datastore.
    testEventServlet.doPost(request, response);

    // Retrieve the Entity posted to Datastore.
    DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
    Entity postedEntity = ds.prepare(new Query("Event")).asSingleEntity();
    
    // Assert the Entity posted to data store has empty properties for the
    // parameters that were not in the request.
    assertEquals("", postedEntity.getProperty("endTime"));
    assertEquals("", postedEntity.getProperty("coverPhoto"));
  }
}