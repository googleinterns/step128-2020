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
import static org.mockito.Mockito.*;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.sps.servlets.SearchServlet;
import com.google.sps.servlets.Utils;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class SearchTagsServletTest {
  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
  private SearchServlet testSearchServlet;
  private List<Entity> testEntities;

  /** Sets up the tests with sample events put into the test datastore. */
  @Before
  public void setUp() {
    helper.setUp();
    testSearchServlet = new SearchServlet();
    List<String> possibleTags =
        new ArrayList<String>(
            Arrays.asList("environment", "blm", "education", "volunteer", "LGBTQ+"));
    List<String> possibleLocations =
        new ArrayList<String>(
            Arrays.asList(
                "1313 Disneyland Dr, Anaheim, CA",
                "111 W Harbor Dr, San Diego, CA",
                "Yosemite National Park, , CA",
                " ,Seattle, WA",
                "Broadway, New York, NY"));
    List<Integer> possibleDistances =
        new ArrayList<Integer>(Arrays.asList(42, 198, 593, 1826, 4497));
    testEntities = new ArrayList<Entity>();
    // Single tag events
    for (int i = 0; i < 5; i++) {
      Entity e = new Entity("Event");
      e.setProperty("eventName", i);
      e.setIndexedProperty("tags", new ArrayList<String>(Arrays.asList(possibleTags.get(i))));
      e.setProperty("streetAddress", possibleLocations.get(i).split(",")[0]);
      e.setProperty("city", possibleLocations.get(i).split(",")[1]);
      e.setProperty("state", possibleLocations.get(i).split(",")[2]);
      e.setProperty("distance", possibleDistances.get(i));
      testEntities.add(e);
    }
    // Double tag events
    for (int i = 5; i < 9; i++) {
      Entity e = new Entity("Event");
      e.setProperty("eventName", i);
      e.setIndexedProperty(
          "tags",
          new ArrayList<String>(Arrays.asList(possibleTags.get(i - 5), possibleTags.get(i - 4))));
      e.setProperty("streetAddress", possibleLocations.get(i - 5).split(",")[0]);
      e.setProperty("city", possibleLocations.get(i - 5).split(",")[1]);
      e.setProperty("state", possibleLocations.get(i - 5).split(",")[2]);
      e.setProperty("distance", possibleDistances.get(i - 5));
      testEntities.add(e);
    }
    // Triple tag events
    for (int i = 9; i < 11; i++) {
      Entity e = new Entity("Event");
      e.setProperty("eventName", i);
      e.setIndexedProperty(
          "tags",
          new ArrayList<String>(
              Arrays.asList(
                  possibleTags.get(i - 9), possibleTags.get(i - 8), possibleTags.get(i - 7))));
      e.setProperty("streetAddress", possibleLocations.get(i - 9).split(",")[0]);
      e.setProperty("city", possibleLocations.get(i - 9).split(",")[1]);
      e.setProperty("state", possibleLocations.get(i - 9).split(",")[2]);
      e.setProperty("distance", possibleDistances.get(i - 9));
      testEntities.add(e);
    }
    // Add all the events to the mock Datastore
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    for (Entity e : testEntities) {
      datastore.put(e);
    }
  }

  @After
  public void tearDown() {
    helper.tearDown();
  }

  @Test
  public void anyQueryAndOutput() throws IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);

    when(response.getWriter()).thenReturn(pw);

    // Send the request to the servlet with param
    String[] paramArr = {"environment"};
    when(request.getParameterValues("tags")).thenReturn(paramArr);
    when(request.getParameter("location")).thenReturn("Los Angeles, CA");
    when(request.getParameter("searchDistance")).thenReturn("5000");
    testSearchServlet.doGet(request, response);

    // Get the JSON response from the server
    String result = sw.getBuffer().toString().trim();

    // Get the events we were expecting the search to return
    // from the datastore
    List<Integer> ids = new ArrayList<Integer>(Arrays.asList(0, 5, 9));
    List<Entity> events = fetchIDsFromDataStore(ids);

    // Convert expected events to JSON for comparison
    String expected = Utils.convertToJson(events);
    assertEquals(expected, result);
  }

  @Test
  public void checkSortedByCommonTags() throws IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);

    when(response.getWriter()).thenReturn(pw);

    // Send the request to the servlet with param
    String[] paramArr = {"environment", "blm", "education"};
    when(request.getParameterValues("tags")).thenReturn(paramArr);
    when(request.getParameter("location")).thenReturn("Los Angeles, CA");
    when(request.getParameter("searchDistance")).thenReturn("5000");
    testSearchServlet.doGet(request, response);

    // Get the JSON response from the server
    String result = sw.getBuffer().toString().trim();

    // Get the events we were expecting the search to return
    // from the datastore and assemble our expected
    List<Integer> ids = new ArrayList<Integer>(Arrays.asList(0, 1, 2, 5, 6, 7, 9, 10));
    List<Entity> events = fetchIDsFromDataStore(ids);

    // Order results like sorting algorithm will
    List<String> desiredOrder =
        new ArrayList<String>(Arrays.asList("9", "5", "6", "10", "0", "1", "2", "7"));
    List<Entity> orderedEvents = orderEvents(desiredOrder, events);

    // Convert expected events to JSON for comparison
    String expected = Utils.convertToJson(orderedEvents);
    assertEquals(expected, result);
  }

  @Test
  public void checkDistanceCutoff() throws IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);

    when(response.getWriter()).thenReturn(pw);

    // Send the request to the servlet with param
    String[] paramArr = {"education"};
    when(request.getParameterValues("tags")).thenReturn(paramArr);
    when(request.getParameter("location")).thenReturn("Los Angeles, CA");
    when(request.getParameter("searchDistance")).thenReturn("50");
    testSearchServlet.doGet(request, response);

    // Get the JSON response from the server
    String result = sw.getBuffer().toString().trim();

    // Get the events we were expecting the search to return
    // from the datastore and assemble our expected
    List<Integer> ids = new ArrayList<Integer>(Arrays.asList(9));
    List<Entity> events = fetchIDsFromDataStore(ids);

    // Order results like sorting algorithm will
    List<String> desiredOrder = new ArrayList<String>(Arrays.asList("9"));
    List<Entity> orderedEvents = orderEvents(desiredOrder, events);

    // Convert expected events to JSON for comparison
    String expected = Utils.convertToJson(orderedEvents);
    assertEquals(expected, result);
  }

  @Test
  public void checkSingleTagSorted() throws IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);

    when(response.getWriter()).thenReturn(pw);

    // Send the request to the servlet with param
    String[] paramArr = {"education"};
    when(request.getParameterValues("tags")).thenReturn(paramArr);
    when(request.getParameter("location")).thenReturn("Los Angeles, CA");
    when(request.getParameter("searchDistance")).thenReturn("5000");
    testSearchServlet.doGet(request, response);

    // Get the JSON response from the server
    String result = sw.getBuffer().toString().trim();

    // Get the events we were expecting the search to return
    // from the datastore and assemble our expected
    List<Integer> ids = new ArrayList<Integer>(Arrays.asList(2, 6, 7, 9, 10));
    List<Entity> events = fetchIDsFromDataStore(ids);

    // Order results like sorting algorithm will
    List<String> desiredOrder = new ArrayList<String>(Arrays.asList("2", "6", "7", "9", "10"));
    List<Entity> orderedEvents = orderEvents(desiredOrder, events);

    // Convert expected events to JSON for comparison
    String expected = Utils.convertToJson(orderedEvents);
    assertEquals(expected, result);
  }

  @Test
  public void correctNumberTagsInCommon() throws IOException {
    List<String> tagListA = new ArrayList<String>(Arrays.asList("environment", "blm", "volunteer"));
    List<String> tagListB = new ArrayList<String>(Arrays.asList("blm"));
    double result = testSearchServlet.intersection(tagListA, tagListB);
    double correct = 1.0 / 3;
    assertEquals(correct, result, 0.0002);
  }

  @Test
  public void tagsInCommonHandlesNoTags() throws IOException {
    List<String> tagListA = new ArrayList<String>();
    List<String> tagListB = new ArrayList<String>();
    double result = testSearchServlet.intersection(tagListA, tagListB);
    assertEquals(0.0, result, 0.0002);
  }

  @Test
  public void handlesNoTagsInCommon() throws IOException {
    List<String> tagListA = new ArrayList<String>(Arrays.asList("education"));
    List<String> tagListB = new ArrayList<String>(Arrays.asList("environment", "blm", "volunteer"));
    double result = testSearchServlet.intersection(tagListA, tagListB);
    assertEquals(0.0, result, 0.0002);
  }

  /**
   * Orders a list of events by a given order.
   *
   * @param desiredOrder List containing the eventNames in the order they should be in
   * @param events List of events to be ordered
   * @return List containing the events ordered
   */
  private static List<Entity> orderEvents(List<String> desiredOrder, List<Entity> events) {
    List<Entity> orderedEvents = new ArrayList<Entity>();
    for (int o = 0; o < desiredOrder.size(); o++) {
      for (int i = 0; i < events.size(); i++) {
        if (events.get(i).getProperty("eventName").toString().equals(desiredOrder.get(o))) {
          orderedEvents.add(events.get(i));
        }
      }
    }
    return orderedEvents;
  }

  /**
   * Fetches entities from the datastore according to ids.
   *
   * @param ids List containing the ids of the entities to fetch from the Datastore
   * @return List containing the requested entities
   */
  private static List<Entity> fetchIDsFromDataStore(List<Integer> ids) {
    Filter idFilter = new FilterPredicate("eventName", FilterOperator.IN, ids);
    Query query = new Query("Event").setFilter(idFilter);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);
    List<Entity> events =
        new ArrayList<Entity>(results.asList(FetchOptions.Builder.withDefaults()));
    return events;
  }
}
