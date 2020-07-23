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
import java.util.Arrays;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
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

/** Tests for LoadEventServlet. */
@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor({"com.google.sps.Firebase"})
@PrepareForTest({Firebase.class})
@PowerMockIgnore("okhttp3.*")
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

    createUser("test");
    createEntity();
  }

  @After
  public void tearDown() {
    helper.tearDown();
  }

  @Test(expected = Test.None.class)
  public void eventLoadSucessful() throws IOException, ServletException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    RequestDispatcher dispatcher = mock(RequestDispatcher.class);

    String email = "test@example.com";
    TestingUtil.mockFirebase(request, email);

    // Display-event JSP should be called. If not, test will fail.
    when(request.getParameter("Event")).thenReturn(goalKeyString);
    when(request.getRequestDispatcher("/WEB-INF/jsp/display-event.jsp")).thenReturn(dispatcher);
    doNothing().when(dispatcher).forward(request, response);

    try {
      testServlet.doGet(request, response);
    } catch (Exception e) {
      fail();
    }
  }

  @Test(expected = Test.None.class)
  public void sendRequestWithNoParameter() throws IOException, ServletException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    RequestDispatcher dispatcher = mock(RequestDispatcher.class);

    // No url parameters. Should call event not found jsp - if not test will fail.
    when(request.getRequestDispatcher("/WEB-INF/jsp/event-not-found.jsp")).thenReturn(dispatcher);
    doNothing().when(dispatcher).forward(request, response);

    try {
      testServlet.doGet(request, response);
    } catch (Exception e) {
      fail();
    }
  }

  @Test(expected = Test.None.class)
  public void sendRequestWithParameterAndNoKey() throws IOException, ServletException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    RequestDispatcher dispatcher = mock(RequestDispatcher.class);

    // No Key. Should call event not found jsp - if not test will fail.
    when(request.getParameter("Event")).thenReturn("");
    when(request.getRequestDispatcher("/WEB-INF/jsp/event-not-found.jsp")).thenReturn(dispatcher);
    doNothing().when(dispatcher).forward(request, response);

    try {
      testServlet.doGet(request, response);
    } catch (Exception e) {
      fail();
    }
  }

  @Test(expected = Test.None.class)
  public void sendRequestWithLowercaseParameterAndValidKey() throws IOException, ServletException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    RequestDispatcher dispatcher = mock(RequestDispatcher.class);

    // The event parameter is lowercase. Should call event not found jsp - if not test will fail.
    when(request.getParameter("event")).thenReturn(goalKeyString);
    when(request.getRequestDispatcher("/WEB-INF/jsp/event-not-found.jsp")).thenReturn(dispatcher);
    doNothing().when(dispatcher).forward(request, response);

    try {
      testServlet.doGet(request, response);
    } catch (Exception e) {
      fail();
    }
  }

  @Test(expected = Test.None.class)
  public void sendRequestWithInvalidLongerKey() throws IOException, ServletException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    RequestDispatcher dispatcher = mock(RequestDispatcher.class);

    // An extra character has been added to key. Should call event not found jsp - if not test will
    // fail.
    when(request.getParameter("event")).thenReturn(goalKeyString + 'A');
    when(request.getRequestDispatcher("/WEB-INF/jsp/event-not-found.jsp")).thenReturn(dispatcher);
    doNothing().when(dispatcher).forward(request, response);

    try {
      testServlet.doGet(request, response);
    } catch (Exception e) {
      fail();
    }
  }

  @Test(expected = Test.None.class)
  public void sendRequestWithValidKeyNotInDatastore() throws IOException, ServletException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    RequestDispatcher dispatcher = mock(RequestDispatcher.class);

    // Key is replaced the valid key not in Datastore. Should call event not found jsp - if not test
    // will fail.
    when(request.getParameter("Event")).thenReturn("agR0ZXN0cgsLEgVFdmVudBgBDZ");
    when(request.getRequestDispatcher("/WEB-INF/jsp/event-not-found.jsp")).thenReturn(dispatcher);
    doNothing().when(dispatcher).forward(request, response);

    try {
      testServlet.doGet(request, response);
    } catch (Exception e) {
      fail();
    }
  }

  private void createEntity() {
    String[] tags = {"volunteer"};

    goalEntity = new Entity("Event");
    goalEntity.setProperty("eventName", "Book Drive");
    goalEntity.setProperty("eventDescription", "We're collecting books.");
    goalEntity.setProperty("address", "456 Library Way, Bookville, Washington");
    goalEntity.setProperty("streetAddress", "456 Library Way");
    goalEntity.setProperty("city", "Bookville");
    goalEntity.setProperty("state", "Washington");
    goalEntity.setProperty("date", "May 17, 2020");
    goalEntity.setProperty("startTime", "2:00 PM");
    goalEntity.setProperty("endTime", "4:00 PM");
    goalEntity.setProperty("coverPhoto", "/img-2030121");
    goalEntity.setIndexedProperty("tags", Arrays.asList(tags));
    goalEntity.setProperty("creator", "test@example.com");
    goalEntity.setProperty("attendeeCount", 0L);
    goalEntity.setProperty("unformattedStart", "14:00");
    goalEntity.setProperty("unformattedEnd", "16:00");
    goalEntity.setProperty("unformattedDate", "2020-05-17");

    DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
    ds.put(goalEntity);

    Key goalKey = goalEntity.getKey();
    goalKeyString = KeyFactory.keyToString(goalKey);

    goalEntity.setProperty("eventKey", goalKeyString);
    ds.put(goalEntity);
  }

  private void createUser(String user) {
    String email = user + "@example.com";
    Key userKey = KeyFactory.createKey("User", email);

    Entity entity = new Entity(userKey);
    entity.setProperty("firebaseID", email);

    DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
    ds.put(entity);
  }
}
