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
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.PreparedQuery.TooManyResultsException;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.sps.servlets.EventServlet;
import com.google.sps.servlets.LoadEventServlet;
import com.google.sps.servlets.SurveyServlet;
import com.google.sps.servlets.UserServlet;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

/** Tests for the utility methods in Interactions.java */
@PowerMockIgnore("okhttp3.*")
@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor({"com.google.sps.Firebase"})
@PrepareForTest({Firebase.class})
public final class InteractionsTest {
  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
  private static final SurveyServlet testSurveyServlet = new SurveySurvlet();

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
    // a basic test to make sure Interactions.getInterestMetrics() works as intended
    String email = "test@example.com";
    takeSurvey(email);

    Map<String, Integer> expectedSurvey = new HashMap<>();
    expectedSurvey.put("environment", 3);
    expectedSurvey.put("blm", 4);
    expectedSurvey.put("volunteer", 3);
    expectedSurvey.put("education", 2);
    expectedSurvey.put("LGBTQ+", 4);

    assertEquals(expectedSurvey, Interactions.getInterestMetrics(email));
  }

  @Test
  public void noUserForCheckMetrics() throws IOException {
    // call Interactions.getInterestMetrics() on nonexistent user
    HttpServletRequest request = mock(HttpServletRequest.class);
    String email = "test@example.com";
    TestingUtil.mockFirebase(request, email);

    when(request.getParameter("environment")).thenReturn("3");
    when(request.getParameter("blm")).thenReturn("4");
    when(request.getParameter("volunteer")).thenReturn("3");
    when(request.getParameter("education")).thenReturn("2");
    when(request.getParameter("LGBTQ+")).thenReturn("4");
    HttpServletResponse response = mock(HttpServletResponse.class);
    testSurveyServlet.doPost(request, response);

    assertEquals(null, Interactions.getInterestMetrics("other@example.com"));
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
  public void recordMiscInteractions() throws IOException {
    EventServlet eventServlet = new EventServlet();
    LoadEventServlet loadServlet = new LoadEventServlet();
    UserServlet userServlet = new UserServlet();

    // blm protest (test@example), book drive (another@example), lake clean up(test@example)
    UserServletTest.postEventsSetup();
    List<Entity> allEntities = UserServletTest.callGet("", "");
    long id0 = allEntities.get(0).getKey().getId(); // blm protest (test@example)
    long id1 = allEntities.get(1).getKey().getId(); // book drive (another @example)
    long id2 = allEntities.get(2).getKey().getId(); // lake clean up (test@example)

    UserServletTest.callPost(id1, "save", "test@example.com");
    UserServletTest.callPost(id1, "save", "test@example.com");
    UserServletTest.callPost(id1, "save", "another@example.com");
    UserServletTest.callPost(id1, "save", "person@example.com");

    //test@example.com -> created blm protest, created lake clean up, saved book drive, viewed all 3
    //another @example.com -> created book drive, saved book drive, viewed lake clean up
    // person@example.com -> saved blm protest, viewed blm protest, unsaved blm protest, viewed lake clean up
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
