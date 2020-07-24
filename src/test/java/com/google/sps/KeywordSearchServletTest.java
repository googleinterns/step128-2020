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
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.maps.model.LatLng;
import com.google.sps.servlets.KeywordSearchServlet;
import java.io.IOException;
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
public final class KeywordSearchServletTest {
  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
  private KeywordSearchServlet testKeywordSearchServlet;
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
    testKeywordSearchServlet = new KeywordSearchServlet();

    testEntities = new ArrayList<Entity>();

    possibleTags =
        new ArrayList<String>(
            Arrays.asList("blm", "environment", "donation", "environment", "fundraiser"));

    possibleNames =
        new ArrayList<String>(
            Arrays.asList(
                "BLM Protest",
                "Climate Change Protest",
                "Toy Drive",
                "Park clean up",
                "Ballet Bake Sale",
                "BLM Protest in LA",
                "Environment Climate Change Protest",
                "Toy Drive for the Kids",
                "Athan Downs Park clean up",
                "Support Youth Ballet Bake Sale"));

    possibleDescriptions =
        new ArrayList<String>(
            Arrays.asList(
                "Support the Black Lives Matter movement. support",
                "Climate change is one of the most important issues facing us right now.",
                "Collecting donations of toys to support the children's hospital."
                    + "location location location support support",
                "Bring bags and gloves.",
                "Hi everyone, we're doing this to support the Youth America Grand Prix"
                    + "competitors this year.",
                "BLM. BLM. BLM.",
                "This is one of the most important issues facing us right now.",
                "Collecting donations of toys for the kids hospital.",
                "Bring bags and gloves.",
                "Hi everyone, we're doing this to support the Youth America Grand Prix "
                    + "competitors this year. location location location"));

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
      Map<String, Integer> keywordsMap = KeywordSearchServlet.getKeywords(name, desc);
      e.setProperty("keywords", KeywordSearchServlet.getKeywordMapKeys(keywordsMap));
      e.setProperty("keywordsValues", KeywordSearchServlet.getKeywordMapValues(keywordsMap));
      testEntities.add(e);
    }

    for (int i = 5; i < 10; i++) {
      Entity e = new Entity("Event");
      String name = possibleNames.get(i);
      String desc = possibleDescriptions.get(i);
      e.setProperty("eventName", name);
      e.setIndexedProperty("tags", new ArrayList<String>(Arrays.asList(possibleTags.get(i - 5))));
      e.setProperty("eventDescription", desc);
      e.setProperty("address", possibleLocations.get(i - 5));
      e.setProperty("distance", possibleDistances.get(i - 5));
      Map<String, Integer> keywordsMap = KeywordSearchServlet.getKeywords(name, desc);
      e.setProperty("keywords", KeywordSearchServlet.getKeywordMapKeys(keywordsMap));
      e.setProperty("keywordsValues", KeywordSearchServlet.getKeywordMapValues(keywordsMap));
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
  public void getsSeparateWordsCorrectly() throws IOException {
    String text = "The quick brown fox jumps over the lazy dog.";
    List<String> correctList =
        new ArrayList<String>(
            Arrays.asList("The", "quick", "brown", "fox", "jumps", "over", "the", "lazy", "dog"));
    List<String> testList = KeywordSearchServlet.getSeparateWords(text);
    assertEquals(correctList, testList);
  }

  @Test
  public void handlesAllSeparatorsCorrectly() throws IOException {
    String text = "Are you sure? I... well; I'm not:quite, " + "personally(hehe)[okay]";
    List<String> correctList =
        new ArrayList<String>(
            Arrays.asList(
                "Are",
                "you",
                "sure",
                "I",
                "well",
                "I'm",
                "not",
                "quite",
                "personally",
                "hehe",
                "okay"));
    List<String> testList = KeywordSearchServlet.getSeparateWords(text);
    assertEquals(correctList, testList);
  }

  @Test
  public void getsTitleAndDescKeywordsCorrectly() throws IOException {
    String titleText =
        "Climate climate climate climate Protest! Climate " + "protest: Protest protest!";
    String descText =
        "Important! Please come out in support of the environment. The "
            + "environment is so important to us! Protect the environment. Come!";
    // climate - 5, w/ weight 10
    // protest - 4, w/ weight 8
    // environment - 3
    // come - 2
    // important - 2
    List<String> correctList =
        new ArrayList<String>(
            Arrays.asList("climate", "protest", "environment", "important", "come"));
    List<String> testList =
        KeywordSearchServlet.getKeywordMapKeys(
            KeywordSearchServlet.getKeywords(titleText, descText));
    assertEquals(correctList, testList);
  }

  @Test
  public void getsTitleAndDescKeywordsCorrectlyWeighted() throws IOException {
    String titleText = "Climate! Climate change protest: Protest for our children!";
    String descText =
        "Important! Please come out in support of the environment. The "
            + "environment is so important important to us! Protect the environment. Come come!";
    // climate - 4, w/o weight 2
    // protest - 4, w/o weight 2
    // environment - 3
    // come - 3
    // important - 3
    List<String> correctList =
        new ArrayList<String>(
            Arrays.asList("protest", "climate", "important", "environment", "come"));
    List<String> testList =
        KeywordSearchServlet.getKeywordMapKeys(
            KeywordSearchServlet.getKeywords(titleText, descText));
    assertEquals(correctList, testList);
  }

  @Test
  public void keywordListsMergeCorrectly() throws IOException {
    String titleText = "Climate change protest";
    String descText = "Protest protest in support of the environment environment environment.";
    // protest - 4
    // environment - 3
    // change - 2
    // climate - 2
    // support - 1, so its cut off
    List<String> correctList =
        new ArrayList<String>(Arrays.asList("protest", "environment", "change", "climate"));
    List<String> testList =
        KeywordSearchServlet.getKeywordMapKeys(
            KeywordSearchServlet.getKeywords(titleText, descText));
    assertEquals(correctList, testList);
  }

  @Test
  public void getKeywordsHasMax5() throws IOException {
    String titleText = "";
    String descText =
        "Climate climate climate climate change change change"
            + " protest protest protest for Earth Earth. Important movement "
            + "movement to movement movement movement important important "
            + "important important important.";
    // important - 6
    // movement - 5
    // climate - 4
    // change - 3
    // protest - 3
    // earth should be cut off
    List<String> correctList =
        new ArrayList<String>(
            Arrays.asList("important", "movement", "climate", "change", "protest"));
    List<String> testList =
        KeywordSearchServlet.getKeywordMapKeys(
            KeywordSearchServlet.getKeywords(titleText, descText));
    assertEquals(correctList, testList);
  }

  @Test
  public void getKeywordsInputLessThan5() throws IOException {
    String titleText = "Climate";
    String descText = "";
    // climate
    List<String> correctList = new ArrayList<String>(Arrays.asList("climate"));
    List<String> testList =
        KeywordSearchServlet.getKeywordMapKeys(
            KeywordSearchServlet.getKeywords(titleText, descText));
    assertEquals(correctList, testList);
  }

  @Test
  public void testOccurrenceScore() {
    double occScore =
        testKeywordSearchServlet.occurrenceScore(
            new ArrayList<String>(Arrays.asList("blm", "protest")),
            new ArrayList<String>(Arrays.asList("blm")),
            new ArrayList<Long>(Arrays.asList(3L, 2L)));
    assertEquals(3, occScore, 0.0002);
  }

  @Test
  public void testOccurrenceScoreAdding() {
    double occScore =
        testKeywordSearchServlet.occurrenceScore(
            new ArrayList<String>(Arrays.asList("blm", "protest")),
            new ArrayList<String>(Arrays.asList("blm", "protest")),
            new ArrayList<Long>(Arrays.asList(3L, 2L)));
    assertEquals(5, occScore, 0.0002);
  }

  @Test
  public void testOccurrenceScoreNoKeywords() {
    double occScore =
        testKeywordSearchServlet.occurrenceScore(
            new ArrayList<String>(Arrays.asList()),
            new ArrayList<String>(Arrays.asList("blm")),
            new ArrayList<Long>(Arrays.asList(3L, 2L)));
    assertEquals(0, occScore, 0.0002);
  }

  @Test
  public void testOccurrenceScoreNoValues() {
    double occScore =
        testKeywordSearchServlet.occurrenceScore(
            new ArrayList<String>(Arrays.asList("blm", "protest")),
            new ArrayList<String>(Arrays.asList("blm")),
            new ArrayList<Long>(Arrays.asList()));
    assertEquals(0, occScore, 0.0002);
  }

  @Test
  public void testOccurrenceScoreMismatchValues() {
    double occScore =
        testKeywordSearchServlet.occurrenceScore(
            new ArrayList<String>(Arrays.asList("blm", "protest")),
            new ArrayList<String>(Arrays.asList("blm", "protest")),
            new ArrayList<Long>(Arrays.asList(3L)));
    assertEquals(3, occScore, 0.0002);
  }

  @Test
  public void testOccurrenceScoreMismatchKeywords() {
    double occScore =
        testKeywordSearchServlet.occurrenceScore(
            new ArrayList<String>(Arrays.asList("blm")),
            new ArrayList<String>(Arrays.asList("blm", "protest")),
            new ArrayList<Long>(Arrays.asList(3L, 2L)));
    assertEquals(3, occScore, 0.0002);
  }

  @Test
  public void checkSortedByNumberOfKeywords() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);

    when(response.getWriter()).thenReturn(pw);

    // Send the request to the servlet with param
    when(request.getParameter("searchKeywords")).thenReturn("Protest");
    when(request.getParameter("location")).thenReturn("Los Angeles, CA");
    when(request.getParameter("searchDistance")).thenReturn("5000");

    mockUtils();

    testKeywordSearchServlet.doGet(request, response);

    // Get the JSON response from the server
    String result = sw.getBuffer().toString().trim();

    // Get the events we were expecting the search to return
    // from the datastore and assemble our expected
    List<Integer> ids = new ArrayList<Integer>(Arrays.asList(0, 1));
    List<Entity> events =
        fetchIDsFromDataStore(
            new ArrayList<String>(
                Arrays.asList(
                    "BLM Protest",
                    "Climate Change Protest",
                    "BLM Protest in LA",
                    "Environment Climate Change Protest")));

    // Order results like sorting algorithm will
    List<String> desiredOrder =
        new ArrayList<String>(
            Arrays.asList(
                "BLM Protest",
                "BLM Protest in LA",
                "Climate Change Protest",
                "Environment Climate Change Protest"));
    List<Entity> orderedEvents = SearchTagsServletTest.orderEvents(desiredOrder, events);

    // Convert expected events to JSON for comparison
    String expected = Utils.convertToJson(orderedEvents);
    assertEquals(expected, result);
  }

  @Test
  public void checkSortedByOccurrenceScore() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);

    when(response.getWriter()).thenReturn(pw);

    // Send the request to the servlet with param
    when(request.getParameter("searchKeywords")).thenReturn("BLM");
    when(request.getParameter("location")).thenReturn("Los Angeles, CA");
    when(request.getParameter("searchDistance")).thenReturn("5000");

    mockUtils();

    testKeywordSearchServlet.doGet(request, response);

    // Get the JSON response from the server
    String result = sw.getBuffer().toString().trim();

    // Get the events we were expecting the search to return
    // from the datastore and assemble our expected
    List<Integer> ids = new ArrayList<Integer>(Arrays.asList(0, 1));
    List<Entity> events =
        fetchIDsFromDataStore(
            new ArrayList<String>(Arrays.asList("BLM Protest", "BLM Protest in LA")));

    // Order results like sorting algorithm will
    List<String> desiredOrder =
        new ArrayList<String>(Arrays.asList("BLM Protest in LA", "BLM Protest"));
    List<Entity> orderedEvents = SearchTagsServletTest.orderEvents(desiredOrder, events);

    // Convert expected events to JSON for comparison
    String expected = Utils.convertToJson(orderedEvents);
    assertEquals(expected, result);
  }

  @Test
  public void checkSortedByLocation() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);

    when(response.getWriter()).thenReturn(pw);

    // Send the request to the servlet with param
    when(request.getParameter("searchKeywords")).thenReturn("location");
    when(request.getParameter("location")).thenReturn("Los Angeles, CA");
    when(request.getParameter("searchDistance")).thenReturn("5000");

    mockUtils();

    testKeywordSearchServlet.doGet(request, response);

    // Get the JSON response from the server
    String result = sw.getBuffer().toString().trim();

    // Get the events we were expecting the search to return
    // from the datastore and assemble our expected
    List<Integer> ids = new ArrayList<Integer>(Arrays.asList(0, 1));
    List<Entity> events =
        fetchIDsFromDataStore(
            new ArrayList<String>(Arrays.asList("Toy Drive", "Support Youth Ballet Bake Sale")));

    // Order results like sorting algorithm will
    List<String> desiredOrder =
        new ArrayList<String>(Arrays.asList("Toy Drive", "Support Youth Ballet Bake Sale"));
    List<Entity> orderedEvents = SearchTagsServletTest.orderEvents(desiredOrder, events);

    // Convert expected events to JSON for comparison
    String expected = Utils.convertToJson(orderedEvents);
    assertEquals(expected, result);
  }

  @Test
  public void checkSortedMultipleWordQuery() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);

    when(response.getWriter()).thenReturn(pw);

    // Send the request to the servlet with param
    when(request.getParameter("searchKeywords")).thenReturn("support blm");
    when(request.getParameter("location")).thenReturn("Los Angeles, CA");
    when(request.getParameter("searchDistance")).thenReturn("5000");

    mockUtils();

    testKeywordSearchServlet.doGet(request, response);

    // Get the JSON response from the server
    String result = sw.getBuffer().toString().trim();

    // Get the events we were expecting the search to return
    // from the datastore and assemble our expected
    List<Integer> ids = new ArrayList<Integer>(Arrays.asList(0, 1));
    List<Entity> events =
        fetchIDsFromDataStore(
            new ArrayList<String>(
                Arrays.asList(
                    "BLM Protest",
                    "BLM Protest in LA",
                    "Toy Drive",
                    "Support Youth Ballet Bake Sale")));

    // Order results like sorting algorithm will
    List<String> desiredOrder =
        new ArrayList<String>(
            Arrays.asList(
                "BLM Protest", "BLM Protest in LA", "Toy Drive", "Support Youth Ballet Bake Sale"));
    List<Entity> orderedEvents = SearchTagsServletTest.orderEvents(desiredOrder, events);

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

  /**
   * Fetches entities from the datastore using the event names.
   *
   * @param eventNames List containing the names of the entities to fetch from the Datastore
   * @return List containing the requested entities
   */
  public static List<Entity> fetchIDsFromDataStore(List<String> eventNames) {
    Filter idFilter = new FilterPredicate("eventName", FilterOperator.IN, eventNames);
    Query query = new Query("Event").setFilter(idFilter);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);
    List<Entity> events =
        new ArrayList<Entity>(results.asList(FetchOptions.Builder.withDefaults()));
    return events;
  }

  /**
   * Orders a list of events by a given order.
   *
   * @param desiredOrder List containing the eventNames in the order they should be in
   * @param events List of events to be ordered
   * @return List containing the events ordered
   */
  public static List<Entity> orderEvents(List<String> desiredOrder, List<Entity> events) {
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
}
