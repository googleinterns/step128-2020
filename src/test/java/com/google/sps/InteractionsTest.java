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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.GeoPt;
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
import com.google.maps.model.LatLng;
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
@PrepareForTest({Utils.class, Firebase.class})
public final class InteractionsTest {
  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  private static final SurveyServlet testSurveyServlet = new SurveyServlet();
  private static final double FLOAT_THRESHOLD = 0.0000001;

  /** Logs in as a user and submits a survey. */
  public static void takeSurvey(String email) throws IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    TestingUtil.mockFirebase(request, email);

    when(request.getParameter("environment")).thenReturn("3");
    when(request.getParameter("blm")).thenReturn("4");
    when(request.getParameter("volunteer")).thenReturn("3");
    when(request.getParameter("education")).thenReturn("2");
    when(request.getParameter("LGBTQ+")).thenReturn("4");
    when(request.getParameter("healthcare")).thenReturn("4");
    when(request.getParameter("civics")).thenReturn("4");
    when(request.getParameter("fundraiser")).thenReturn("4");
    when(request.getParameter("activism")).thenReturn("4");
    when(request.getParameter("item donation")).thenReturn("4");

    HttpServletResponse response = mock(HttpServletResponse.class);
    testSurveyServlet.doPost(request, response);
  }

  /** Sets up the datastore helper for each test. */
  @Before
  public void setUp() throws IOException {
    helper.setUp();

    TestingUtil.mockUtilsForLocation();
    PowerMockito.when(Utils.getGeopt("13364 Lakeview Way, Lakeside, California"))
        .thenReturn(new GeoPt(32.858758f, -116.904991f));
    PowerMockito.when(Utils.getGeopt("11852 S Main Street, Los Angeles, California"))
        .thenReturn(new GeoPt(33.925076f, -118.27369f));
    PowerMockito.when(Utils.getGeopt("9800 Regent Street, Los Angeles, California"))
        .thenReturn(new GeoPt(34.025995f, -118.399908f));

    PowerMockito.when(Utils.getLatLng("13364 Lakeview Way, Lakeside, California"))
        .thenReturn(new LatLng(32.858758, -116.904991));
    PowerMockito.when(Utils.getLatLng("11852 S Main Street, Los Angeles, California"))
        .thenReturn(new LatLng(33.925076, -118.27369));
    PowerMockito.when(Utils.getLatLng("9800 Regent Street, Los Angeles, California"))
        .thenReturn(new LatLng(34.025995, -118.399908));
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

    Map<String, Float> expectedSurvey = new HashMap<>();
    expectedSurvey.put("environment", 3.0f);
    expectedSurvey.put("blm", 4.0f);
    expectedSurvey.put("volunteer", 3.0f);
    expectedSurvey.put("education", 2.0f);
    expectedSurvey.put("LGBTQ+", 4.0f);
    expectedSurvey.put("healthcare", 4.0f);
    expectedSurvey.put("civics", 4.0f);
    expectedSurvey.put("fundraiser", 4.0f);
    expectedSurvey.put("activism", 4.0f);
    expectedSurvey.put("item donation", 4.0f);

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Entity userEntity = datastore.prepare(new Query("User")).asSingleEntity();

    assertEquals(expectedSurvey, Interactions.buildVectorForUser(userEntity));
  }

  @Test
  public void buildVector() throws IOException {
    Map<String, Integer> expectedVector = new HashMap<>();
    expectedVector.put("blm", 1);

    Entity entity = UserServletTest.createBlmProtestEvent();
    assertEquals(expectedVector, Interactions.buildVectorForEvent(entity));
  }

  @Test
  public void checkDotProduct() throws IOException {
    Map<String, Float> v1 = new HashMap<>();
    v1.put("1", 1.0f);
    v1.put("2", 2.0f);
    v1.put("3", 1.0f);

    Map<String, Integer> v2 = new HashMap<>();
    v2.put("1", 1);
    v2.put("2", 2);
    v2.put("3", 1);

    assertEquals(1.0, Interactions.dotProduct(v1, v2), FLOAT_THRESHOLD);
  }

  @Test
  public void checkZeroDotProduct() throws IOException {
    Map<String, Float> v1 = new HashMap<>();
    v1.put("1", 0.0f);
    v1.put("2", 0.0f);
    v1.put("3", 0.0f);

    Map<String, Integer> v2 = new HashMap<>();
    v2.put("1", 2);
    v2.put("2", 1);
    v2.put("3", 1);

    assertEquals(0.0, Interactions.dotProduct(v1, v2), FLOAT_THRESHOLD);
  }

  @Test
  public void recordMiscInteractions() throws IOException, ServletException {
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
    TestingUtil.mockFirebase(request, dummyToken);

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
    TestingUtil.mockFirebase(request, dummyToken);

    when(request.getParameter("Event")).thenReturn(key2);
    loadServlet.doGet(request, response);

    assertExpectedInteraction(dummyToken, id1, Interactions.CREATE_SCORE);
    assertExpectedInteraction(dummyToken, id2, Interactions.VIEW_SCORE);

    // person@example: saved blm protest, viewed blm protest, unsaved blm protest, viewed lake
    // cleanup
    dummyToken = "person@example.com";

    UserServletTest.callPost(id0, "save", "person@example.com");
    TestingUtil.mockFirebase(request, dummyToken);

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
          Float.parseFloat(userEntity.getProperty("environment").toString()),
          FLOAT_THRESHOLD);
      assertEquals(
          Interactions.CREATE_SCORE,
          Float.parseFloat(userEntity.getProperty("blm").toString()),
          FLOAT_THRESHOLD);
      assertEquals(
          Interactions.SAVE_SCORE,
          Float.parseFloat(userEntity.getProperty("education").toString()),
          FLOAT_THRESHOLD);

      userKey = KeyFactory.createKey("User", "another@example.com");
      userEntity = datastore.get(userKey);
      assertEquals(
          Interactions.VIEW_SCORE,
          Float.parseFloat(userEntity.getProperty("environment").toString()),
          FLOAT_THRESHOLD);
      assertEquals(
          Interactions.CREATE_SCORE,
          Float.parseFloat(userEntity.getProperty("education").toString()),
          FLOAT_THRESHOLD);

      userKey = KeyFactory.createKey("User", "person@example.com");
      userEntity = datastore.get(userKey);
      assertEquals(
          Interactions.VIEW_SCORE,
          Float.parseFloat(userEntity.getProperty("environment").toString()),
          FLOAT_THRESHOLD);
      assertEquals(
          Interactions.SAVE_SCORE + Interactions.UNSAVE_DELTA,
          Float.parseFloat(userEntity.getProperty("blm").toString()),
          FLOAT_THRESHOLD);
    } catch (EntityNotFoundException e) {
      fail();
    }
  }

  @Test
  public void tooManyInteractions() throws IOException, ServletException {
    UserServletTest.postEventsSetup();
    List<Entity> allEntities = UserServletTest.callGet("", "");
    long id = allEntities.get(0).getKey().getId();
    String user = "dummy@example.com";

    // make two interaction entities for the same user-event pair and put into datastore
    Entity firstEntity = new Entity("Interaction");
    firstEntity.setProperty("user", user);
    firstEntity.setProperty("event", id);
    firstEntity.setProperty("rating", Interactions.CREATE_SCORE);
    Entity secondEntity = new Entity("Interaction");
    secondEntity.setProperty("user", user);
    secondEntity.setProperty("event", id);
    secondEntity.setProperty("rating", Interactions.SAVE_SCORE);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    datastore.put(firstEntity);
    datastore.put(secondEntity);

    // login and view the item
    HttpServletRequest request = mock(HttpServletRequest.class);
    RequestDispatcher dispatcher = mock(RequestDispatcher.class);
    when(request.getRequestDispatcher("/WEB-INF/jsp/display-event.jsp")).thenReturn(dispatcher);
    when(request.getParameter("Event"))
        .thenReturn(allEntities.get(0).getProperty("eventKey").toString());
    TestingUtil.mockFirebase(request, user);

    HttpServletResponse response = mock(HttpServletResponse.class);
    LoadEventServlet loadServlet = new LoadEventServlet();
    loadServlet.doGet(request, response);

    try {
      // previous two items should have been deleted
      assertExpectedInteraction(user, id, Interactions.VIEW_SCORE);
    } catch (TooManyResultsException e) {
      fail();
    }
  }

  /** Makes sure interactions have been recorded correctly in datastore. */
  public static void assertExpectedInteraction(String userId, long eventId, float value) {
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
      float score = Float.parseFloat(interactionEntity.getProperty("rating").toString());
      assertEquals(value, score, FLOAT_THRESHOLD);
    } catch (TooManyResultsException e) {
      fail();
    } catch (NullPointerException e) {
      fail();
    }
  }
}
