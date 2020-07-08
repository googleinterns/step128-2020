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

import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.sps.servlets.LoadEventServlet;
import java.io.IOException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for LoadEventServlet. */
@RunWith(JUnit4.class)
public final class LoadEventServletTest {
  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
  private LoadEventServlet testServlet;
  private Entity goalEntity;
  private String goalKeyString;

  /** Set up Entity and Entity key to test for. */
  @Before
  public void setUp() {
    helper.setUp();
    testServlet = new LoadEventServlet();

    goalEntity = new Entity("Event");
    goalEntity.setProperty("eventName", "Lake Clean Up");
    goalEntity.setProperty("eventDescription", "We're cleaning up the lake");
    goalEntity.setProperty("streetAddress", "678 Lakeview Way");
    goalEntity.setProperty("city", "Lakeside");
    goalEntity.setProperty("state", "Michigan");
    goalEntity.setProperty("date", "May 17, 2020");
    goalEntity.setProperty("startTime", "14:00");
    goalEntity.setProperty("endTime", "15:00");
    goalEntity.setProperty("coverPhoto", "/img-2030121");
    goalEntity.setProperty("tags", "['environment']");

    DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
    ds.put(goalEntity);

    Key goalKey = goalEntity.getKey();
    goalKeyString = KeyFactory.keyToString(goalKey);
  }

  @After
  public void tearDown() {
    helper.tearDown();
  }

  @Test(expected = NullPointerException.class)
  public void sendRequestWithNoParameter() throws IOException, ServletException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    RequestDispatcher dispatcher = mock(RequestDispatcher.class);

    // There is no event parameter in the request.
    when(request.getRequestDispatcher("/WEB-INF/jsp/display-event.jsp")).thenReturn(dispatcher);

    testServlet.doGet(request, response);
    fail("Expected exception");
  }

  @Test(expected = NullPointerException.class)
  public void sendRequestWithParameterAndNoKey() throws IOException, ServletException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    RequestDispatcher dispatcher = mock(RequestDispatcher.class);

    // There is no Key associated with the event parameter in the request.
    when(request.getParameter("Event")).thenReturn("");
    when(request.getRequestDispatcher("/WEB-INF/jsp/display-event.jsp")).thenReturn(dispatcher);

    testServlet.doGet(request, response);
    fail("Expected exception");
  }

  @Test(expected = NullPointerException.class)
  public void sendRequestWithLowercaseParameterAndValidKey() throws IOException, ServletException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    RequestDispatcher dispatcher = mock(RequestDispatcher.class);

    // The event parameter is lowercase in the request.
    when(request.getParameter("event")).thenReturn("agR0ZXN0cgsLEgVFdmVudBgBDA");
    when(request.getRequestDispatcher("/WEB-INF/jsp/display-event.jsp")).thenReturn(dispatcher);

    testServlet.doGet(request, response);
    fail("Expected exception");
  }

  @Test(expected = NullPointerException.class)
  public void sendRequestWithInvalidLongerKey() throws IOException, ServletException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    RequestDispatcher dispatcher = mock(RequestDispatcher.class);

    // The goal Key String is 'agR0ZXN0cgsLEgVFdmVudBgBDA', an extra character has been added.
    when(request.getParameter("Event")).thenReturn("agR0ZXN0cgsLEgVFdmVudBgBDAX");
    when(request.getRequestDispatcher("/WEB-INF/jsp/display-event.jsp")).thenReturn(dispatcher);

    testServlet.doGet(request, response);
    fail("Expected exception");
  }

  @Test(expected = NullPointerException.class)
  public void sendRequestWithInvalidShorterKey() throws IOException, ServletException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    RequestDispatcher dispatcher = mock(RequestDispatcher.class);

    // The goal Key String is 'agR0ZXN0cgsLEgVFdmVudBgBDA', a character has been removed.
    when(request.getParameter("Event")).thenReturn("agR0ZXN0cgsLEgVFdmVudBgBD");
    when(request.getRequestDispatcher("/WEB-INF/jsp/display-event.jsp")).thenReturn(dispatcher);

    testServlet.doGet(request, response);
    fail("Expected exception");
  }

  @Test(expected = NullPointerException.class)
  public void sendRequestWithValidKeyNotInDatastore() throws IOException, ServletException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    RequestDispatcher dispatcher = mock(RequestDispatcher.class);

    // Key is replaced the valid key not in Datastore.
    when(request.getParameter("Event")).thenReturn("agR0ZXN0cgsLEgVFdmVudBgBDZ");
    when(request.getRequestDispatcher("/WEB-INF/jsp/display-event.jsp")).thenReturn(dispatcher);

    testServlet.doGet(request, response);
    fail("Expected exception");
  }
}
