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

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.FetchOptions.Builder;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.sps.servlets.SearchServlet;
import com.google.sps.servlets.Utility;
import java.io.IOException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.io.PrintWriter;
import java.io.StringWriter;

/** */
@RunWith(JUnit4.class)
public final class SearchTagsServletTest {
  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
  private SearchServlet testSearchServlet;
  private List<Entity> testEntities;

  @Before
  public void setUp() {
    helper.setUp();
    testSearchServlet = new SearchServlet();
    List<String> possibleTags = new ArrayList<String>(Arrays.asList(
        "environment", "blm", "education", "volunteer", "LGBTQ+"
    ));
    testEntities = new ArrayList<Entity>();
    // Single tag events
    for (int i = 0; i < 5; i++) {
      Entity e = new Entity("Event");
      e.setProperty("eventName", i);
      e.setIndexedProperty("tags", new ArrayList<String>(Arrays.asList(possibleTags.get(i))));
      testEntities.add(e);
    }
    // Double tag events
    for (int i = 5; i < 9; i++) {
      Entity e = new Entity("Event");
      e.setProperty("eventName", i);
      e.setIndexedProperty("tags", new ArrayList<String>(Arrays.asList(possibleTags.get(i-5), possibleTags.get(i-4))));
      testEntities.add(e);
    }
    // Triple tag events
    for (int i = 9; i < 11; i++) {
      Entity e = new Entity("Event");
      e.setProperty("eventName", i);
      e.setIndexedProperty("tags", new ArrayList<String>(Arrays.asList(possibleTags.get(i-9), possibleTags.get(i-8), possibleTags.get(i-7))));
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
/*
  @Test
  public void anyQueryAndOutput() throws IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
  
    when(response.getWriter()).thenReturn(pw);

    // Send the request to the servlet with param
    when(request.getParameter("tags")).thenReturn("environment");
    testSearchServlet.doGet(request, response);
  
    // Get the JSON response from the server
    String result = sw.getBuffer().toString().trim();
    
    // Get the events we were expecting the search to return
    // from the datastore
    List<Integer> ids = new ArrayList<Integer>(Arrays.asList(0, 5));
    Filter idFilter =
        new FilterPredicate("eventName", FilterOperator.IN, ids);
    Query query =
        new Query("Event").setFilter(idFilter);

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);
    List<Entity> events = new ArrayList<Entity>(
        results.asList(FetchOptions.Builder.withDefaults()));
    // Convert expected events to JSON for comparison
    String expected = Utility.convertToJson(events);
    assertEquals(expected, result);
  }*/

  @Test
  public void checkSortedByCommonTags() throws IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
  
    when(response.getWriter()).thenReturn(pw);

    // Send the request to the servlet with param
    String[] paramArr = {"environment","blm","education"};
    when(request.getParameterValues("tags")).thenReturn(paramArr);
    testSearchServlet.doGet(request, response);
  
    // Get the JSON response from the server
    String result = sw.getBuffer().toString().trim();
    
    // Get the events we were expecting the search to return
    // from the datastore
    List<Integer> ids = new ArrayList<Integer>(Arrays.asList(0, 1, 2, 5, 6, 7, 9, 10));
    Filter idFilter =
        new FilterPredicate("eventName", FilterOperator.IN, ids);
    Query query =
        new Query("Event").setFilter(idFilter);

    // Assemble our expected
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);
    List<Entity> events = new ArrayList<Entity>(
        results.asList(FetchOptions.Builder.withDefaults()));
    // Order results like sorting algorithm will
    List<String> desiredOrder = new ArrayList<String>(
        Arrays.asList("9", "5", "6", "10", "0", "1", "2", "7"));
    List<Entity> orderedEvents = new ArrayList<Entity>();
    for (int o = 0; o < desiredOrder.size(); o++) {
      for (int i = 0; i < events.size(); i++) {
        if (events.get(i).getProperty("eventName").toString().equals(desiredOrder.get(o))) {
          orderedEvents.add(events.get(i));
        }
      }
    }

    // Convert expected events to JSON for comparison
    String expected = Utility.convertToJson(orderedEvents);
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
    testSearchServlet.doGet(request, response);
  
    // Get the JSON response from the server
    String result = sw.getBuffer().toString().trim();
    
    // Get the events we were expecting the search to return
    // from the datastore
    List<Integer> ids = new ArrayList<Integer>(Arrays.asList(2, 6, 7, 9, 10));
    Filter idFilter =
        new FilterPredicate("eventName", FilterOperator.IN, ids);
    Query query =
        new Query("Event").setFilter(idFilter);

    // Assemble our expected
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);
    List<Entity> events = new ArrayList<Entity>(
        results.asList(FetchOptions.Builder.withDefaults()));
    // Order results like sorting algorithm will
    List<String> desiredOrder = new ArrayList<String>(
        Arrays.asList("2", "6", "7", "9", "10"));
    List<Entity> orderedEvents = new ArrayList<Entity>();
    for (int o = 0; o < desiredOrder.size(); o++) {
      for (int i = 0; i < events.size(); i++) {
        if (events.get(i).getProperty("eventName").toString().equals(desiredOrder.get(o))) {
          orderedEvents.add(events.get(i));
        }
      }
    }

    // Convert expected events to JSON for comparison
    String expected = Utility.convertToJson(orderedEvents);
    assertEquals(expected, result);
  }
/*
  @Test
  public void correctNumberTagsInCommon() throws IOException {
    List<String> tagListA = 
        new ArrayList<String>(Arrays.asList("blm"));
    List<String> tagListB = 
        new ArrayList<String>(Arrays.asList("environment", "blm", "volunteer"));
    int result = testSearchServlet.tagsInCommon(tagListA, tagListB);
    assertEquals(1, result);
  }

  @Test
  public void tagsInCommonHandlesNoTags() throws IOException {
    List<String> tagListA = 
        new ArrayList<String>();
    List<String> tagListB = 
        new ArrayList<String>();
    int result = testSearchServlet.tagsInCommon(tagListA, tagListB);
    assertEquals(0, result);
  }

  @Test
  public void handlesNoTagsInCommon() throws IOException {
    List<String> tagListA = 
        new ArrayList<String>(Arrays.asList("education"));
    List<String> tagListB = 
        new ArrayList<String>(Arrays.asList("environment", "blm", "volunteer"));
    int result = testSearchServlet.tagsInCommon(tagListA, tagListB);
    assertEquals(0, result);
  }*/
}
