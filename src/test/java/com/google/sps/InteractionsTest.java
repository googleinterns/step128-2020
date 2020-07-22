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
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.PreparedQuery.TooManyResultsException;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.sps.servlets.LoadEventServlet;
import com.google.sps.servlets.SurveyServlet;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
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

/** Tests for the utility methods in Interactions.java */
@PowerMockIgnore("okhttp3.*")
@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor({"com.google.sps.Firebase"})
@PrepareForTest({Firebase.class})
public final class InteractionsTest {
  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
  private static final SurveyServlet testSurveyServlet = new SurveyServlet();

  private void takeSurvey(String email) throws IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    TestingUtil.mockFirebase(request, email);

    when(request.getParameter("environment")).thenReturn("3");
    when(request.getParameter("blm")).thenReturn("4");
    when(request.getParameter("volunteer")).thenReturn("3");
    when(request.getParameter("education")).thenReturn("2");
    when(request.getParameter("LGBTQ+")).thenReturn("4");

    HttpServletResponse response = mock(HttpServletResponse.class);
    testSurveyServlet.doPost(request, response);
  }

  /** Sets up the datastore helper and authentication utility for each test. */
  @Before
  public void setUp() throws IOException {
    helper.setUp();
  }

  @After
  public void tearDown() {
    helper.tearDown();
  }

  @Test
  public void checkInterestMetrics() throws IOException {
    // a basic test to make sure Interactions.buildVectorForUser() works as intended
    String email = "test@example.com";
    takeSurvey(email);

    Map<String, Integer> expectedSurvey = new HashMap<>();
    expectedSurvey.put("environment", 3);
    expectedSurvey.put("blm", 4);
    expectedSurvey.put("volunteer", 3);
    expectedSurvey.put("education", 2);
    expectedSurvey.put("LGBTQ+", 4);

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Entity userEntity = datastore.prepare(new Query("User")).asSingleEntity();

    assertEquals(expectedSurvey, Interactions.buildVectorForUser(userEntity));
  }

  @Test
  public void buildVector() throws IOException {
    Map<String, Integer> expectedVector = new HashMap<>();
    expectedVector.put("environment", 0);
    expectedVector.put("blm", 1);
    expectedVector.put("volunteer", 0);
    expectedVector.put("education", 0);
    expectedVector.put("LGBTQ+", 0);

    Entity entity = UserServletTest.createBlmProtestEvent();
    assertEquals(expectedVector, Interactions.buildVectorForEvent(entity));
  }

  @Test
  public void checkDotProduct() throws IOException {
    Map<String, Integer> v1 = new HashMap<>();
    v1.put("1", 1);
    v1.put("2", 2);
    v1.put("3", 3);

    Map<String, Integer> v2 = new HashMap<>();
    v2.put("2", 2);

    assertEquals(4, Interactions.dotProduct(v1, v2));
  }

  @Test
  public void recordMiscInteractions() throws IOException, ServletException {
    PowerMockito.mockStatic(Firebase.class);
    final LoadEventServlet loadServlet = new LoadEventServlet();
    final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

    // blm protest (test@example), book drive (another@example), lake clean up(test@example)
    UserServletTest.postEventsSetup();
    List<Entity> allEntities = UserServletTest.callGet("", "");
    final long id0 = allEntities.get(0).getKey().getId();
    final long id1 = allEntities.get(1).getKey().getId();
    final long id2 = allEntities.get(2).getKey().getId();

    final String key0 = allEntities.get(0).getProperty("eventKey").toString();
    final String key1 = allEntities.get(1).getProperty("eventKey").toString();
    final String key2 = allEntities.get(2).getProperty("eventKey").toString();

    HttpServletRequest request = mock(HttpServletRequest.class);
    RequestDispatcher dispatcher = mock(RequestDispatcher.class);
    when(request.getRequestDispatcher("/WEB-INF/jsp/display-event.jsp")).thenReturn(dispatcher);

    // test@example: created blm protest, created lake clean up, saved book drive, viewed all
    String dummyToken = "test@example.com";

    UserServletTest.callPost(id1, "save", dummyToken);
    UserServletTest.callPost(id1, "save", dummyToken);

    when(request.getParameter("userToken")).thenReturn(dummyToken);
    PowerMockito.when(Firebase.isUserLoggedIn(anyString())).thenCallRealMethod();
    PowerMockito.when(Firebase.authenticateUser(anyString())).thenReturn(dummyToken);

    HttpServletResponse response = mock(HttpServletResponse.class);
    when(request.getParameter("Event")).thenReturn(key0);
    loadServlet.doGet(request, response);

    when(request.getParameter("Event")).thenReturn(key1);
    loadServlet.doGet(request, response);

    when(request.getParameter("Event")).thenReturn(key2);
    loadServlet.doGet(request, response);
    loadServlet.doGet(request, response);

    assertExpectedInteraction(dummyToken, id0, Interactions.CREATE_SCORE);
    assertExpectedInteraction(dummyToken, id1, Interactions.SAVE_SCORE);
    assertExpectedInteraction(dummyToken, id2, Interactions.CREATE_SCORE);

    // another@example: created book drive, saved book drive, viewed lake clean up
    dummyToken = "another@example.com";

    UserServletTest.callPost(id1, "save", "another@example.com");

    when(request.getParameter("userToken")).thenReturn(dummyToken);
    PowerMockito.when(Firebase.isUserLoggedIn(anyString())).thenCallRealMethod();
    PowerMockito.when(Firebase.authenticateUser(anyString())).thenReturn(dummyToken);

    when(request.getParameter("Event")).thenReturn(key2);
    loadServlet.doGet(request, response);

    assertExpectedInteraction(dummyToken, id1, Interactions.CREATE_SCORE);
    assertExpectedInteraction(dummyToken, id2, Interactions.VIEW_SCORE);

    // person@example: saved blm protest, viewed blm protest, unsaved blm protest, viewed lake
    // cleanup
    dummyToken = "person@example.com";

    UserServletTest.callPost(id0, "save", "person@example.com");

    when(request.getParameter("userToken")).thenReturn(dummyToken);
    PowerMockito.when(Firebase.isUserLoggedIn(anyString())).thenCallRealMethod();
    PowerMockito.when(Firebase.authenticateUser(anyString())).thenReturn(dummyToken);

    when(request.getParameter("Event")).thenReturn(key0);
    loadServlet.doGet(request, response);

    UserServletTest.callPost(id0, "unsave", "person@example.com");

    when(request.getParameter("Event")).thenReturn(key2);
    loadServlet.doGet(request, response);

    assertExpectedInteraction(dummyToken, id0, Interactions.SAVE_SCORE + Interactions.UNSAVE_DELTA);
    assertExpectedInteraction(dummyToken, id2, Interactions.VIEW_SCORE);

    // check that user interests have been saved correctly
    try {
      Key userKey = KeyFactory.createKey("User", "test@example.com");
      Entity userEntity = datastore.get(userKey);
      assertEquals(
          Interactions.CREATE_SCORE,
          Integer.parseInt(userEntity.getProperty("environment").toString()));
      assertEquals(
          Interactions.CREATE_SCORE, Integer.parseInt(userEntity.getProperty("blm").toString()));
      assertEquals(
          Interactions.SAVE_SCORE,
          Integer.parseInt(userEntity.getProperty("education").toString()));

      userKey = KeyFactory.createKey("User", "another@example.com");
      userEntity = datastore.get(userKey);
      assertEquals(
          Interactions.VIEW_SCORE,
          Integer.parseInt(userEntity.getProperty("environment").toString()));
      assertEquals(
          Interactions.CREATE_SCORE,
          Integer.parseInt(userEntity.getProperty("education").toString()));

      userKey = KeyFactory.createKey("User", "person@example.com");
      userEntity = datastore.get(userKey);
      assertEquals(
          Interactions.VIEW_SCORE,
          Integer.parseInt(userEntity.getProperty("environment").toString()));
      assertEquals(
          Interactions.SAVE_SCORE + Interactions.UNSAVE_DELTA,
          Integer.parseInt(userEntity.getProperty("blm").toString()));
    } catch (EntityNotFoundException e) {
      fail();
    }
  }

  /** Makes sure interactions have been recorded correctly in datastore. */
  public static void assertExpectedInteraction(String userId, long eventId, int value) {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Query q =
        new Query("Interaction")
            .setFilter(
                CompositeFilterOperator.and(
                    new FilterPredicate("user", FilterOperator.EQUAL, userId),
                    new FilterPredicate("event", FilterOperator.EQUAL, eventId)));
    PreparedQuery pq = datastore.prepare(q);
    try {
      Entity interactionEntity = pq.asSingleEntity();
      int score = Integer.parseInt(interactionEntity.getProperty("rating").toString());
      assertEquals(value, score);
    } catch (TooManyResultsException e) {
      fail();
    } catch (NullPointerException e) {
      fail();
    }
  }
}
