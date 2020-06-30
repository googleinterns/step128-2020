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
import com.google.sps.servlets.AuthServlet;
import com.google.sps.servlets.EventServlet;
import com.google.sps.servlets.UserServlet;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.StringReader;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

@RunWith(PowerMockRunner.class)
@PrepareForTest({URL.class, UserServiceFactory.class})
public final class UserServletTest {
  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
  private final Gson gson = new Gson();
  private AuthServlet testAuthServlet;
  private EventServlet testEventServlet;
  private UserServlet testUserServlet;
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

  /**
   * Checks for equality and order between two entity lists without checking the keys
   */
  private void assertListsEqual(List<Entity> goalEntityList, List<Entity> resultingEntities) {
    assertEquals(goalEntityList.size(), resultingEntities.size());
    for(int i = 0; i < goalEntityList.size(); i++) {
      Entity goal = goalEntityList.get(i);
      Entity resultEntity = resultingEntities.get(i);
      Set<String> goalProperties = goal.getProperties().keySet();
      Set<String> resultProperties = goal.getProperties().keySet();
      // checks along each of entity properties
      assertEquals(goalProperties.size(), resultProperties.size());
      for(String s: goalProperties) {
        assertEquals(goal.getProperty(s), resultEntity.getProperty(s));
      }
    }
  }

  /**
   * Logs in and out a few times, posting events to datastore
   */
  private void postEventsSetup() throws IOException {

    // posted by test@example.com
    toggleLogin("test@example.com");
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
    testEventServlet.doPost(request, response);

    request = mock(HttpServletRequest.class);
    response = mock(HttpServletResponse.class);
    when(request.getParameter("event-name")).thenReturn("BLM Protest");
    when(request.getParameter("event-description")).thenReturn("Fight for racial justice!");
    when(request.getParameter("street-address")).thenReturn("Main Street");
    when(request.getParameter("city")).thenReturn("Los Angeles");
    when(request.getParameter("state")).thenReturn("California");
    when(request.getParameter("date")).thenReturn("2020-17-05");
    when(request.getParameter("start-time")).thenReturn("13:00");
    when(request.getParameter("all-tags")).thenReturn("['blm']");
    testEventServlet.doPost(request, response);
    toggleLogin("test@example.com");

    // posted by another@example.com
    toggleLogin("another@example.com");
    when(request.getParameter("event-name")).thenReturn("Book Drive");
    when(request.getParameter("event-description")).thenReturn("Let's donate books for kids");
    when(request.getParameter("street-address")).thenReturn("School Drive");
    when(request.getParameter("city")).thenReturn("Los Angeles");
    when(request.getParameter("state")).thenReturn("California");
    when(request.getParameter("date")).thenReturn("2020-17-05");
    when(request.getParameter("start-time")).thenReturn("10:00");
    when(request.getParameter("all-tags")).thenReturn("['education']");
    testEventServlet.doPost(request, response);

    // logout
    toggleLogin("another@example.com");
  }

  @Before
  public void setUp() throws IOException {
    helper.setUp();
    PowerMockito.mockStatic(UserServiceFactory.class);
    mockService = new MockedUserService();
    when(UserServiceFactory.getUserService()).thenReturn(mockService);
    testEventServlet = new EventServlet();
    testAuthServlet = new AuthServlet();
    testUserServlet = new UserServlet();

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
  public void notLoggedIn() throws IOException {
    // post the events to datastore
    postEventsSetup();

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    StringWriter out = new StringWriter();
    PrintWriter writer = new PrintWriter(out);
    when(response.getWriter()).thenReturn(writer);

    testUserServlet.doGet(request, response);
    out.flush();
    List<Entity> resultingEntities = gson.fromJson(out.toString(), new TypeToken<ArrayList<Entity>>(){}.getType());

    // create the expected resulting search results
    Entity goalEntity = new Entity("Event");
    goalEntity.setProperty("eventName", "Lake Clean Up");
    goalEntity.setProperty("eventDescription", "We're cleaning up the lake");
    goalEntity.setProperty("streetAddress", "678 Lakeview Way");
    goalEntity.setProperty("city", "Lakeside");
    goalEntity.setProperty("state", "Michigan");
    goalEntity.setProperty("date", "2020-17-05");
    goalEntity.setProperty("startTime", "14:00");
    goalEntity.setProperty("endTime", "");
    goalEntity.setProperty("coverPhoto", "");
    goalEntity.setProperty("tags", "['environment']");
    goalEntity.setProperty("creator", "test@example.com");

    Entity goalEntity2 = new Entity("Event");
    goalEntity2.setProperty("eventName", "BLM Protest");
    goalEntity2.setProperty("eventDescription", "Fight for racial justice!");
    goalEntity2.setProperty("streetAddress", "Main Street");
    goalEntity2.setProperty("city", "Los Angeles");
    goalEntity2.setProperty("state", "California");
    goalEntity2.setProperty("date", "2020-17-05");
    goalEntity2.setProperty("startTime", "13:00");
    goalEntity2.setProperty("endTime", "");
    goalEntity2.setProperty("coverPhoto", "");
    goalEntity2.setProperty("tags", "['blm']");
    goalEntity2.setProperty("creator", "test@example.com");

    Entity goalEntity3 = new Entity("Event");
    goalEntity3.setProperty("eventName", "Book Drive");
    goalEntity3.setProperty("eventDescription", "Let's donate books for kids");
    goalEntity3.setProperty("streetAddress", "School Drive");
    goalEntity3.setProperty("city", "Los Angeles");
    goalEntity3.setProperty("state", "California");
    goalEntity3.setProperty("date", "2020-17-05");
    goalEntity3.setProperty("startTime", "10:00");
    goalEntity3.setProperty("endTime", "");
    goalEntity3.setProperty("coverPhoto", "");
    goalEntity3.setProperty("tags", "['education']");
    goalEntity3.setProperty("creator", "another@example.com");

    // goal list, should be sorted in ascending order by name
    List<Entity> goalEntityList = new ArrayList<>();
    goalEntityList.add(goalEntity2);
    goalEntityList.add(goalEntity3);
    goalEntityList.add(goalEntity);

    assertListsEqual(goalEntityList, resultingEntities);
  }

  @Test
  public void getCreatedEvents() throws IOException {
    // login as test@example.com and make sure method returns correct events

    postEventsSetup();
    toggleLogin("test@example.com");

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    when(request.getParameter("get")).thenReturn("created");

    StringWriter out = new StringWriter();
    PrintWriter writer = new PrintWriter(out);
    when(response.getWriter()).thenReturn(writer);

    testUserServlet.doGet(request, response);
    out.flush();
    List<Entity> resultingEntities = gson.fromJson(out.toString(), new TypeToken<ArrayList<Entity>>(){}.getType());
    
    Entity goalEntity = new Entity("Event");
    goalEntity.setProperty("eventName", "Lake Clean Up");
    goalEntity.setProperty("eventDescription", "We're cleaning up the lake");
    goalEntity.setProperty("streetAddress", "678 Lakeview Way");
    goalEntity.setProperty("city", "Lakeside");
    goalEntity.setProperty("state", "Michigan");
    goalEntity.setProperty("date", "2020-17-05");
    goalEntity.setProperty("startTime", "14:00");
    goalEntity.setProperty("endTime", "");
    goalEntity.setProperty("coverPhoto", "");
    goalEntity.setProperty("tags", "['environment']");
    goalEntity.setProperty("creator", "test@example.com");

    Entity goalEntity2 = new Entity("Event");
    goalEntity2.setProperty("eventName", "BLM Protest");
    goalEntity2.setProperty("eventDescription", "Fight for racial justice!");
    goalEntity2.setProperty("streetAddress", "Main Street");
    goalEntity2.setProperty("city", "Los Angeles");
    goalEntity2.setProperty("state", "California");
    goalEntity2.setProperty("date", "2020-17-05");
    goalEntity2.setProperty("startTime", "13:00");
    goalEntity2.setProperty("endTime", "");
    goalEntity2.setProperty("coverPhoto", "");
    goalEntity2.setProperty("tags", "['blm']");
    goalEntity2.setProperty("creator", "test@example.com");

    List<Entity> goalEntityList = new ArrayList<>();
    goalEntityList.add(goalEntity2);
    goalEntityList.add(goalEntity);

    assertListsEqual(goalEntityList, resultingEntities);
  }

  // TODO: no tests yet for saved events (no means of saving events yet)


  /* the LoginObject structure used by AuthServlet */
  private static class LoginObject {
    private boolean loggedIn;
    private String url;
  }
}
