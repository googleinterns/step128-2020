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
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyObject;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.maps.model.LatLng;
import com.google.sps.servlets.SearchServlet;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@PowerMockIgnore("okhttp3.*")
@RunWith(PowerMockRunner.class)
@PrepareForTest({Utils.class})
public final class CombineSearchTest {
  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
  private SearchServlet testSearchServlet;
  private List<Entity> testEntities;
  private List<String> possibleTags;
  private List<String> possibleNames;
  private List<String> possibleDescriptions;
  private List<String> possibleLocations;
  private List<LatLng> possibleLatLngs;
  private List<Integer> possibleDistances;

  /** Sets up the datastore helper and adds a bunch of events for testing purposes. */
  @Before
  public void setUp() {
    helper.setUp();
    testSearchServlet = new SearchServlet();

    testEntities = new ArrayList<Entity>();

    possibleTags =
        new ArrayList<String>(
            Arrays.asList("blm", "environment", "environment", "environment", "fundraiser"));

    possibleNames =
        new ArrayList<String>(
            Arrays.asList(
                "BLM Protest",
                "Climate Change Protest",
                "Beach clean up",
                "Park clean up",
                "Ballet Bake Sale"));

    possibleDescriptions =
        new ArrayList<String>(
            Arrays.asList(
                "Protest",
                "Climate change is one of the most important issues facing us right now.",
                "Let's clean and save some turtles.",
                "Bring bags and gloves.",
                "Hi everyone, we're doing this to support the Youth America Grand Prix"
                    + "competitors this year."));

    possibleLocations =
        new ArrayList<String>(
            Arrays.asList(
                "1313DisneylandDr Anaheim CA",
                "111WHarborDr SanDiego CA",
                "YosemiteNationalPark  CA",
                " Seattle WA",
                "Broadway NewYork NY"));
    possibleLatLngs =
        new ArrayList<LatLng>(
            Arrays.asList(
                new LatLng(33.8153959, -117.9263991),
                new LatLng(32.7068214, -117.1628713),
                new LatLng(37.8027472, -119.8739089),
                new LatLng(47.6062095, -122.3320708),
                new LatLng(40.81241989999999, -73.96053429999999)));
    possibleDistances = new ArrayList<Integer>(Arrays.asList(42, 198, 593, 1826, 4497));

    for (int i = 0; i < 5; i++) {
      Entity e = new Entity("Event");
      String name = possibleNames.get(i);
      String desc = possibleDescriptions.get(i);
      e.setProperty("eventName", name);
      e.setIndexedProperty("tags", new ArrayList<String>(Arrays.asList(possibleTags.get(i))));
      e.setProperty("eventDescription", desc);
      e.setProperty("address", possibleLocations.get(i));
      e.setProperty("distance", possibleDistances.get(i));
      Map<String, Integer> keywordsMap = SearchServlet.getKeywords(name, desc);
      e.setProperty("keywords", SearchServlet.getKeywordMapKeys(keywordsMap));
      e.setProperty("keywordsValues", SearchServlet.getKeywordMapValues(keywordsMap));
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
  public void sortsUsingTagsFirst() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);

    when(response.getWriter()).thenReturn(pw);

    // Send the request to the servlet with param
    when(request.getParameter("searchKeywords")).thenReturn("Protest");
    when(request.getParameter("tags")).thenReturn("environment,blm");
    when(request.getParameter("location")).thenReturn("Los Angeles, CA");
    when(request.getParameter("searchDistance")).thenReturn("5000");

    mockUtils();

    testSearchServlet.doGet(request, response);

    // Get the JSON response from the server
    String result = sw.getBuffer().toString().trim();

    // Get the events we were expecting the search to return
    // from the datastore and assemble our expected
    List<Integer> ids = new ArrayList<Integer>(Arrays.asList(0, 1, 2, 3));
    List<Entity> events =
        TestingUtil.fetchIDsFromDataStore(
            new ArrayList<String>(
                Arrays.asList(
                    "BLM Protest", "Climate Change Protest", "Beach clean up", "Park clean up")));

    // Order results like sorting algorithm will
    List<String> desiredOrder =
        new ArrayList<String>(
            Arrays.asList(
                "BLM Protest", "Climate Change Protest", "Beach clean up", "Park clean up"));
    List<Entity> orderedEvents = TestingUtil.orderEvents(desiredOrder, events);

    // Convert expected events to JSON for comparison
    String expected = Utils.convertToJson(orderedEvents);
    assertEquals(expected, result);
  }

  @Test
  public void sortsUsingKeywordsSecond() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);

    when(response.getWriter()).thenReturn(pw);

    // Send the request to the servlet with param
    when(request.getParameter("searchKeywords")).thenReturn("clean");
    when(request.getParameter("tags")).thenReturn("environment");
    when(request.getParameter("location")).thenReturn("Los Angeles, CA");
    when(request.getParameter("searchDistance")).thenReturn("5000");

    mockUtils();

    testSearchServlet.doGet(request, response);

    // Get the JSON response from the server
    String result = sw.getBuffer().toString().trim();

    // Get the events we were expecting the search to return
    // from the datastore and assemble our expected
    List<Integer> ids = new ArrayList<Integer>(Arrays.asList(1, 2, 3));
    List<Entity> events =
        TestingUtil.fetchIDsFromDataStore(
            new ArrayList<String>(
                Arrays.asList("Climate Change Protest", "Beach clean up", "Park clean up")));

    // Order results like sorting algorithm will
    List<String> desiredOrder =
        new ArrayList<String>(
            Arrays.asList("Beach clean up", "Park clean up", "Climate Change Protest"));
    List<Entity> orderedEvents = TestingUtil.orderEvents(desiredOrder, events);

    // Convert expected events to JSON for comparison
    String expected = Utils.convertToJson(orderedEvents);
    System.out.println(orderedEvents);
    assertEquals(expected, result);
  }

  @Test
  public void sortsUsingLocationLast() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);

    when(response.getWriter()).thenReturn(pw);

    // Send the request to the servlet with param
    when(request.getParameter("searchKeywords")).thenReturn("");
    when(request.getParameter("tags")).thenReturn("environment");
    when(request.getParameter("location")).thenReturn("Los Angeles, CA");
    when(request.getParameter("searchDistance")).thenReturn("5000");

    mockUtils();

    testSearchServlet.doGet(request, response);

    // Get the JSON response from the server
    String result = sw.getBuffer().toString().trim();

    // Get the events we were expecting the search to return
    // from the datastore and assemble our expected
    List<Integer> ids = new ArrayList<Integer>(Arrays.asList(1, 2, 3));
    List<Entity> events =
        TestingUtil.fetchIDsFromDataStore(
            new ArrayList<String>(
                Arrays.asList("Climate Change Protest", "Beach clean up", "Park clean up")));

    // Order results like sorting algorithm will
    List<String> desiredOrder =
        new ArrayList<String>(
            Arrays.asList("Climate Change Protest", "Beach clean up", "Park clean up"));
    List<Entity> orderedEvents = TestingUtil.orderEvents(desiredOrder, events);

    // Convert expected events to JSON for comparison
    String expected = Utils.convertToJson(orderedEvents);
    assertEquals(expected, result);
  }

  @Test
  public void distanceCutoff50Mi() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);

    when(response.getWriter()).thenReturn(pw);

    // Send the request to the servlet with param
    when(request.getParameter("searchKeywords")).thenReturn("Protest");
    when(request.getParameter("tags")).thenReturn("environment,blm");
    when(request.getParameter("location")).thenReturn("Los Angeles, CA");
    when(request.getParameter("searchDistance")).thenReturn("50");

    mockUtils();

    testSearchServlet.doGet(request, response);

    // Get the JSON response from the server
    String result = sw.getBuffer().toString().trim();

    // Get the events we were expecting the search to return
    // from the datastore and assemble our expected
    List<Integer> ids = new ArrayList<Integer>(Arrays.asList(0, 1, 2, 3));
    List<Entity> events =
        TestingUtil.fetchIDsFromDataStore(
            new ArrayList<String>(
                Arrays.asList(
                    "BLM Protest")));

    // Order results like sorting algorithm will
    List<String> desiredOrder =
        new ArrayList<String>(
            Arrays.asList(
                "BLM Protest"));
    List<Entity> orderedEvents = TestingUtil.orderEvents(desiredOrder, events);

    // Convert expected events to JSON for comparison
    String expected = Utils.convertToJson(orderedEvents);
    assertEquals(expected, result);
  }

  @Test
  public void distanceCutoff500mi() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);

    when(response.getWriter()).thenReturn(pw);

    // Send the request to the servlet with param
    when(request.getParameter("searchKeywords")).thenReturn("Protest");
    when(request.getParameter("tags")).thenReturn("environment,blm");
    when(request.getParameter("location")).thenReturn("Los Angeles, CA");
    when(request.getParameter("searchDistance")).thenReturn("500");

    mockUtils();

    testSearchServlet.doGet(request, response);

    // Get the JSON response from the server
    String result = sw.getBuffer().toString().trim();

    // Get the events we were expecting the search to return
    // from the datastore and assemble our expected
    List<Integer> ids = new ArrayList<Integer>(Arrays.asList(0, 1, 2, 3));
    List<Entity> events =
        TestingUtil.fetchIDsFromDataStore(
            new ArrayList<String>(
                Arrays.asList(
                    "BLM Protest", "Climate Change Protest", "Beach clean up")));

    // Order results like sorting algorithm will
    List<String> desiredOrder =
        new ArrayList<String>(
            Arrays.asList(
                "BLM Protest", "Climate Change Protest", "Beach clean up"));
    List<Entity> orderedEvents = TestingUtil.orderEvents(desiredOrder, events);

    // Convert expected events to JSON for comparison
    String expected = Utils.convertToJson(orderedEvents);
    assertEquals(expected, result);
  }

  /** Sets up and executes mocking for the Utils class. */
  private void mockUtils() throws Exception {
    // Mock the functionality of the methods in Utils that require the Google Maps API
    PowerMockito.mockStatic(Utils.class);
    PowerMockito.when(Utils.getLatLng("Los Angeles, CA"))
        .thenReturn(new LatLng(34.0522342, -118.2436849));
    // Mock getLatLng
    for (int i = 0; i < possibleLocations.size(); i++) {
      PowerMockito.when(Utils.getLatLng(possibleLocations.get(i)))
          .thenReturn(possibleLatLngs.get(i));
    }
    // Mock getDistance
    for (int i = 0; i < possibleLocations.size(); i++) {
      PowerMockito.when(
              Utils.getDistance(new LatLng(34.0522342, -118.2436849), possibleLatLngs.get(i)))
          .thenReturn(possibleDistances.get(i));
    }
    // Call the actual getParameter and convertToJson methods
    PowerMockito.when(Utils.getParameter(anyObject(), anyString(), anyString()))
        .thenCallRealMethod();
    PowerMockito.when(Utils.convertToJson(any())).thenCallRealMethod();
  }
}
