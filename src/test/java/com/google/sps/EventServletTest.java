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
import static org.mockito.Mockito.*;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.sps.servlets.EventServlet;
import com.google.sps.servlets.AuthServlet;
import com.google.gson.Gson;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URL;
import java.net.MalformedURLException;

/** */
@RunWith(PowerMockRunner.class)
@PrepareForTest({URL.class, UserServiceFactory.class})
public final class EventServletTest {
  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
  private final Gson gson = new Gson();
  private EventServlet testEventServlet;
  private AuthServlet testAuthServlet;
  private MockedUserService mockService;

  private String activeUrl;

  /**
   * Use the current url to login/logout
   *
   * @param email If logging in, will log into this user's account.
   */
  private void toggleLogin(String email) throws MalformedURLException, IOException {
    URL mockurl = PowerMockito.mock(URL.class);
    when(mockurl.openConnection()).thenReturn(mockService.evaluateURL(AuthServletTest.makeLoginURL(activeUrl, email)));
    mockurl.openConnection();

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    StringWriter out = new StringWriter();
    PrintWriter writer = new PrintWriter(out);
    when(response.getWriter()).thenReturn(writer);

    testAuthServlet.doGet(request, response);
    out.flush();
    LoginObject result = gson.fromJson(out.toString(), LoginObject.class);
    activeUrl = result.url;
  }

  @Before
  public void setUp() throws IOException {
    helper.setUp();
    PowerMockito.mockStatic(UserServiceFactory.class);
    mockService = new MockedUserService();
    when(UserServiceFactory.getUserService()).thenReturn(mockService);
    testEventServlet = new EventServlet();
    testAuthServlet = new AuthServlet();

    // get the initial login url
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    StringWriter out = new StringWriter();
    PrintWriter writer = new PrintWriter(out);
    when(response.getWriter()).thenReturn(writer);
    testAuthServlet.doGet(request, response);
    out.flush();

    LoginObject result = gson.fromJson(out.toString(), LoginObject.class);
    activeUrl = result.url;
  }

  @After
  public void tearDown() {
    helper.tearDown();
    activeUrl = "";
  }

  @Test
  public void postOneEventToDatastore() throws IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    toggleLogin("test@example.com");

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
    when(request.getParameter("all-tags")).thenReturn("[environment]");

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
    toggleLogin("test@example.com");

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
    toggleLogin(creatorEmail);

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
    toggleLogin("test@example.com");

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
    when(request.getParameter("date")).thenReturn("2020-17-05");
    when(request.getParameter("start-time")).thenReturn("14:00");
    when(request.getParameter("all-tags")).thenReturn("['environment']");

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

  /* the LoginObject structure used by AuthServlet */
  private static class LoginObject {
    private boolean loggedIn;
    private String url;
  }
}
