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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.sps.servlets.AuthServlet;
import com.google.sps.servlets.EventServlet;
import com.google.sps.servlets.UserServlet;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

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
    when(mockurl.openConnection())
        .thenReturn(mockService.evaluateURL(AuthServletTest.makeLoginURL(activeUrl, email)));
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

  /** Checks for equivalent content between two entity lists */
  private void assertListsEqual(List<Entity> goalEntityList, List<Entity> resultingEntities) {
    if (goalEntityList == null || resultingEntities == null) {
      fail();
    }
    assertEquals(goalEntityList.size(), resultingEntities.size());
    Collections.sort(goalEntityList, UserServlet.ORDER_BY_NAME);
    Collections.sort(resultingEntities, UserServlet.ORDER_BY_NAME);

    for (int i = 0; i < goalEntityList.size(); i++) {
      Entity goal = goalEntityList.get(i);
      Entity resultEntity = resultingEntities.get(i);
      Set<String> goalProperties = goal.getProperties().keySet();
      Set<String> resultProperties = goal.getProperties().keySet();
      // checks along each of entity properties
      assertEquals(goalProperties.size(), resultProperties.size());
      for (String s : goalProperties) {
        assertEquals(goal.getProperty(s), resultEntity.getProperty(s));
      }
    }
  }

  /** Checks for equality between two numeric lists (event ids) */
  private void assertIdListsEqual(List<Long> goalIds, List<Long> resultIds) {
    if (goalIds == null || resultIds == null) {
      fail();
    }
    assertEquals(goalIds.size(), resultIds.size());
    Collections.sort(goalIds);
    Collections.sort(resultIds);
    // add to sorted lists, then use assertListsEqual
    for (int i = 0; i < goalIds.size(); i++) {
      if (goalIds.get(i) != resultIds.get(i)) {
        fail();
      }
    }
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

    List<Entity> resultingEntities = callGet("");

    // create the expected resulting search results
    Entity goalEntity = createLakeCleanupEvent();
    Entity goalEntity2 = createBlmProtestEvent();
    Entity goalEntity3 = createBookDriveEvent();

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
    List<Entity> resultingEntities = callGet("created");

    Entity goalEntity = createLakeCleanupEvent();
    Entity goalEntity2 = createBlmProtestEvent();

    List<Entity> goalEntityList = new ArrayList<>();
    goalEntityList.add(goalEntity2);
    goalEntityList.add(goalEntity);
    assertListsEqual(goalEntityList, resultingEntities);
  }

  @Test
  public void saveAnEvent() throws IOException {
    postEventsSetup();
    List<Entity> allEntities = callGet("");

    Entity entityToSave = allEntities.get(0);
    long id = entityToSave.getKey().getId();
    List<Long> goalList = new ArrayList<>();
    goalList.add(id);

    // login and add BLM event to user's saved events
    toggleLogin("test@example.com");
    callPost(id, "save");

    DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
    Key userKey = KeyFactory.createKey("User", "test@example.com");
    try {
      Entity userEntity = ds.get(userKey);
      assertIdListsEqual(goalList, (ArrayList<Long>) userEntity.getProperty("saved"));
    } catch (EntityNotFoundException exception) {
      // user should have been created during previous get/post calls
      fail();
    }
  }

  @Test
  public void saveTwoEvents() throws IOException {
    // save two events, this time checking that doGet behaves correctly
    postEventsSetup();
    List<Entity> allEntities = callGet("");

    long id0 = allEntities.get(0).getKey().getId();
    long id1 = allEntities.get(1).getKey().getId();
    List<Entity> goalList = new ArrayList<>();
    goalList.add(allEntities.get(0));
    goalList.add(allEntities.get(1));

    // login and save these two events
    toggleLogin("test@example.com");
    callPost(id0, "save");
    callPost(id1, "save");

    assertListsEqual(goalList, callGet("saved"));
  }

  @Test
  public void saveAndUnsave() throws IOException {
    postEventsSetup();

    // for reference, get list of existing entities with their auto-assigned keys
    List<Entity> allEntities = callGet("");
    long id = allEntities.get(0).getKey().getId();

    // save an event, then unsave it
    toggleLogin("test@example.com");
    callPost(id, "save");
    callPost(id, "unsave");

    assertListsEqual(new ArrayList<Entity>(), callGet("saved"));
  }

  @Test
  public void saveDuplicateEvent() throws IOException {
    postEventsSetup();

    List<Entity> allEntities = callGet("");
    long id = allEntities.get(0).getKey().getId();
    List<Entity> goal = new ArrayList<>();
    goal.add(allEntities.get(0));

    // save an event, then save it again
    toggleLogin("test@example.com");
    callPost(id, "save");
    callPost(id, "save");

    assertListsEqual(goal, callGet("saved"));
  }

  @Test
  public void saveNonexistent() throws IOException {
    postEventsSetup();

    List<Entity> allEntities = callGet("");
    // create an id that does not currently exist in datastore
    long id = 0;
    for (int i = 0; i < allEntities.size(); i++) {
      id += allEntities.get(i).getKey().getId();
    }
    List<Entity> goal = new ArrayList<>();

    // save an event, then save it again
    toggleLogin("test@example.com");
    callPost(id, "save");

    assertListsEqual(goal, callGet("saved"));
  }

  @Test
  public void unsaveNotSaved() throws IOException {
    postEventsSetup();

    List<Entity> allEntities = callGet("");
    long id0 = allEntities.get(0).getKey().getId();
    long id1 = allEntities.get(1).getKey().getId();
    List<Entity> goalList = new ArrayList<>();
    goalList.add(allEntities.get(0));

    // save an event, but unsave a different event
    toggleLogin("test@example.com");
    callPost(id0, "save");
    callPost(id1, "unsave");

    assertListsEqual(goalList, callGet("saved"));
  }

  @Test
  public void saveWhileNotLoggedIn() throws IOException {
    postEventsSetup();

    List<Entity> allEntities = callGet("");
    long id = allEntities.get(0).getKey().getId();

    try {
      callPost(id, "save");
      fail();
    } catch (IOException e) {
      // ensure that no users have saved anything
      DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
      for (Entity user : ds.prepare(new Query("User")).asIterable()) {
        String email = user.getKey().getName();
        toggleLogin(email);
        assertListsEqual(new ArrayList<Entity>(), callGet("saved"));
        toggleLogin(email);
      }
    }
  }

  @Test
  public void unsaveWhileNotLoggedIn() throws IOException {
    postEventsSetup();
    DatastoreService ds = DatastoreServiceFactory.getDatastoreService();

    List<Entity> allEntities = callGet("");
    long id = allEntities.get(0).getKey().getId();
    List<Entity> goalList = new ArrayList<>();
    goalList.add(allEntities.get(0));

    // save the same event for all users
    for (Entity user : ds.prepare(new Query("User")).asIterable()) {
      String email = user.getKey().getName();
      toggleLogin(email);
      callPost(id, "save");
      toggleLogin(email);
    }

    try {
      callPost(id, "unsave");
      fail();
    } catch (IOException e) {
      // ensure that no users have saved anything
      for (Entity user : ds.prepare(new Query("User")).asIterable()) {
        String email = user.getKey().getName();
        toggleLogin(email);
        assertListsEqual(goalList, callGet("saved"));
        toggleLogin(email);
      }
    }
  }

  /** Logs in and out a few times, posting events to datastore */
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

  // performs the GET request to return a list of events
  private List<Entity> callGet(String get) throws IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    when(request.getParameter("get")).thenReturn(get);

    StringWriter out = new StringWriter();
    PrintWriter writer = new PrintWriter(out);
    when(response.getWriter()).thenReturn(writer);

    testUserServlet.doGet(request, response);
    out.flush();

    return gson.fromJson(out.toString(), new TypeToken<ArrayList<Entity>>() {}.getType());
  }

  // performs the POST request to save or unsave an event with a given id
  private void callPost(long id, String action) throws IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    when(request.getParameter("event")).thenReturn(id + "");
    when(request.getParameter("action")).thenReturn(action);
    testUserServlet.doPost(request, response);
  }

  // entities to compare against postSetup() method
  private static Entity createLakeCleanupEvent() {
    Entity entity = new Entity("Event");
    entity.setProperty("eventName", "Lake Clean Up");
    entity.setProperty("eventDescription", "We're cleaning up the lake");
    entity.setProperty("streetAddress", "678 Lakeview Way");
    entity.setProperty("city", "Lakeside");
    entity.setProperty("state", "Michigan");
    entity.setProperty("date", "2020-17-05");
    entity.setProperty("startTime", "14:00");
    entity.setProperty("endTime", "");
    entity.setProperty("coverPhoto", "");
    entity.setProperty("tags", "['environment']");
    entity.setProperty("creator", "test@example.com");

    return entity;
  }

  private static Entity createBlmProtestEvent() {
    Entity entity = new Entity("Event");
    entity.setProperty("eventName", "BLM Protest");
    entity.setProperty("eventDescription", "Fight for racial justice!");
    entity.setProperty("streetAddress", "Main Street");
    entity.setProperty("city", "Los Angeles");
    entity.setProperty("state", "California");
    entity.setProperty("date", "2020-17-05");
    entity.setProperty("startTime", "13:00");
    entity.setProperty("endTime", "");
    entity.setProperty("coverPhoto", "");
    entity.setProperty("tags", "['blm']");
    entity.setProperty("creator", "test@example.com");

    return entity;
  }

  private static Entity createBookDriveEvent() {
    Entity entity = new Entity("Event");
    entity.setProperty("eventName", "Book Drive");
    entity.setProperty("eventDescription", "Let's donate books for kids");
    entity.setProperty("streetAddress", "School Drive");
    entity.setProperty("city", "Los Angeles");
    entity.setProperty("state", "California");
    entity.setProperty("date", "2020-17-05");
    entity.setProperty("startTime", "10:00");
    entity.setProperty("endTime", "");
    entity.setProperty("coverPhoto", "");
    entity.setProperty("tags", "['education']");
    entity.setProperty("creator", "another@example.com");

    return entity;
  }

  /* the LoginObject structure used by AuthServlet */
  private static class LoginObject {
    private boolean loggedIn;
    private String url;
  }
}
