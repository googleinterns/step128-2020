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
import com.google.appengine.api.datastore.GeoPt;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.maps.model.LatLng;
import com.google.sps.servlets.EditEventServlet;
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
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

/** Tests for the SurveyServlet.java class */
@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor({"com.google.sps.Firebase"})
@PrepareForTest({Utils.class, Firebase.class})
@PowerMockIgnore("okhttp3.*")
public final class EditEventServletTest {
  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
  private EditEventServlet testServlet;
  private String correctKey;
  private Entity goalEntity;

  /** Set up Entity and Entity key to test for. */
  @Before
  public void setUp() {
    helper.setUp();
    testServlet = new EditEventServlet();

    TestingUtil.mockUtilsForLocation();
    PowerMockito.when(Utils.getGeopt("13364 Lakeview Way, Lakeside, California"))
        .thenReturn(new GeoPt(32.858758f, -116.904991f));
    PowerMockito.when(Utils.getGeopt("11852 S Main Street, Los Angeles, California"))
        .thenReturn(new GeoPt(33.925076f, -118.27369f));
    PowerMockito.when(Utils.getLatLng("13364 Lakeview Way, Lakeside, California"))
        .thenReturn(new LatLng(32.858758, -116.904991));
    PowerMockito.when(Utils.getLatLng("11852 S Main Street, Los Angeles, California"))
        .thenReturn(new LatLng(33.925076, -118.27369));

    createGoalEntity();
    createUser("test");
    createUser("wrong");
  }

  @After
  public void tearDown() {
    helper.tearDown();
  }

  @Test
  public void updateEventField() throws IOException, ServletException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);

    String email = "test@example.com";
    TestingUtil.mockFirebase(request, email);

    // Full request params. The edit event form should be called.
    when(request.getParameter("key")).thenReturn(correctKey);
    when(request.getParameter("userToken")).thenReturn(email);
    when(request.getParameter("event-name")).thenReturn("UPDATED: Lake Clean Up");
    when(request.getParameter("event-description")).thenReturn("We're cleaning up the lake");
    when(request.getParameter("street-address")).thenReturn("13364 Lakeview Way");
    when(request.getParameter("city")).thenReturn("Lakeside");
    when(request.getParameter("state")).thenReturn("California");
    when(request.getParameter("date")).thenReturn("2020-05-17");
    when(request.getParameter("start-time")).thenReturn("14:00");
    when(request.getParameter("end-time")).thenReturn("15:00");
    when(request.getParameter("cover-photo")).thenReturn("/img-2030121");
    String[] tagsArr = {"environment"};
    String tagsStr = Utils.convertToJson(tagsArr);
    when(request.getParameter("all-tags")).thenReturn(tagsStr);

    testServlet.doPost(request, response);

    Key key = KeyFactory.stringToKey(correctKey);
    Query query = new Query("Event", key);

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Entity postedEntity = datastore.prepare(query).asSingleEntity();

    assertEquals("UPDATED: Lake Clean Up", postedEntity.getProperty("eventName"));
  }

  @Test
  public void updateMultipleEventFields() throws IOException, ServletException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);

    String email = "test@example.com";
    TestingUtil.mockFirebase(request, email);

    // Full request params. The edit event form should be called.
    when(request.getParameter("key")).thenReturn(correctKey);
    when(request.getParameter("userToken")).thenReturn(email);
    when(request.getParameter("event-name")).thenReturn("UPDATED: Lake Clean Up");
    when(request.getParameter("event-description")).thenReturn("We're cleaning up the lake");
    when(request.getParameter("street-address")).thenReturn("11852 S Main Street");
    when(request.getParameter("city")).thenReturn("Los Angeles");
    when(request.getParameter("state")).thenReturn("California");
    when(request.getParameter("date")).thenReturn("2020-05-17");
    when(request.getParameter("start-time")).thenReturn("14:00");
    when(request.getParameter("end-time")).thenReturn("");
    when(request.getParameter("cover-photo")).thenReturn("/img-2030121");
    String[] tagsArr = {"environment, volunteer"};
    String tagsStr = Utils.convertToJson(tagsArr);
    when(request.getParameter("all-tags")).thenReturn(tagsStr);

    testServlet.doPost(request, response);

    Key key = KeyFactory.stringToKey(correctKey);
    Query query = new Query("Event", key);

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Entity postedEntity = datastore.prepare(query).asSingleEntity();

    // Assert multiple fields have been updated.
    assertEquals(Arrays.asList(tagsArr), postedEntity.getProperty("tags"));
    assertEquals("", postedEntity.getProperty("endTime"));
    assertEquals(33.925076, Double.parseDouble(postedEntity.getProperty("lat").toString()), 0.01);
    assertEquals(-118.27369, Double.parseDouble(postedEntity.getProperty("lng").toString()), 0.01);
  }

  @Test(expected = IOException.class)
  public void updateAttemptNotLoggedIn() throws IOException, ServletException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);

    String email = "notloggedin@example.com";
    TestingUtil.mockFirebase(request, email);

    // Response should be called.
    when(request.getParameter("userToken")).thenReturn(email);
    when(Firebase.isUserLoggedIn(anyString())).thenReturn(false);
    doNothing().when(response).sendRedirect(anyString());

    // IOException should be thrown.
    testServlet.doPost(request, response);
  }

  @Test(expected = Test.None.class)
  public void accessEditEventPage() throws IOException, ServletException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    RequestDispatcher dispatcher = mock(RequestDispatcher.class);

    String email = "test@example.com";
    TestingUtil.mockFirebase(request, email);

    // Full request params. The edit event form should be called.
    when(request.getParameter("Event")).thenReturn(correctKey);
    when(request.getParameter("userToken")).thenReturn(email);
    when(request.getRequestDispatcher("/WEB-INF/jsp/edit-event-form.jsp")).thenReturn(dispatcher);
    doNothing().when(dispatcher).forward(request, response);
    doNothing().when(request).setAttribute(anyString(), any(Object.class));

    try {
      testServlet.doGet(request, response);
    } catch (Exception e) {
      fail();
    }
  }

  @Test(expected = Test.None.class)
  public void exceptionHandelingForNoParams() throws IOException, ServletException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    RequestDispatcher dispatcher = mock(RequestDispatcher.class);

    String email = "test@example.com";
    TestingUtil.mockFirebase(request, email);

    // No request params. Should invoke event not found JSP- If not error will be thrown.
    when(request.getRequestDispatcher("/WEB-INF/jsp/event-not-found.jsp")).thenReturn(dispatcher);
    doNothing().when(dispatcher).forward(request, response);

    // Should handle IOException.
    try {
      testServlet.doGet(request, response);
    } catch (Exception e) {
      fail();
    }
  }

  @Test(expected = Test.None.class)
  public void exceptionHandelingForWrongEventKey() throws IOException, ServletException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    RequestDispatcher dispatcher = mock(RequestDispatcher.class);

    String email = "test@example.com";
    TestingUtil.mockFirebase(request, email);

    // Invalid key. Should invoke event not found JSP- If not error will be thrown.
    when(request.getParameter("Event")).thenReturn("abcde");
    when(request.getParameter("userToken")).thenReturn(email);
    when(request.getRequestDispatcher("/WEB-INF/jsp/event-not-found.jsp")).thenReturn(dispatcher);
    doNothing().when(dispatcher).forward(request, response);

    // Should handle IllegalArgumentException.
    try {
      testServlet.doGet(request, response);
    } catch (Exception e) {
      fail();
    }
  }

  @Test(expected = Test.None.class)
  public void exceptionHandelingForNoEventKey() throws IOException, ServletException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    RequestDispatcher dispatcher = mock(RequestDispatcher.class);

    String email = "test@example.com";
    TestingUtil.mockFirebase(request, email);

    // No event key. Should invoke event not found JSP- If not error will be thrown.
    when(request.getParameter("Event")).thenReturn("");
    when(request.getParameter("userToken")).thenReturn(email);
    when(request.getRequestDispatcher("/WEB-INF/jsp/event-not-found.jsp")).thenReturn(dispatcher);
    doNothing().when(dispatcher).forward(request, response);

    // Should handle IllegalArgumentException.
    try {
      testServlet.doGet(request, response);
    } catch (Exception e) {
      fail();
    }
  }

  @Test(expected = Test.None.class)
  public void exceptionHandelingForNoUser() throws IOException, ServletException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    RequestDispatcher dispatcher = mock(RequestDispatcher.class);

    String email = "test@example.com";
    TestingUtil.mockFirebase(request, email);

    // No userToken. Should invoke access denied JSP- If not error will be thrown.
    when(request.getParameter("Event")).thenReturn(correctKey);
    when(request.getParameter("userToken")).thenReturn(null);
    when(request.getRequestDispatcher("/WEB-INF/jsp/access-denied.html")).thenReturn(dispatcher);
    doNothing().when(dispatcher).forward(request, response);

    // Should handle IOException.
    try {
      testServlet.doGet(request, response);
    } catch (Exception e) {
      fail();
    }
  }

  @Test(expected = Test.None.class)
  public void exceptionHandelingForNotLoggedIn() throws IOException, ServletException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    RequestDispatcher dispatcher = mock(RequestDispatcher.class);

    String email = "notloggedin@example.com";
    TestingUtil.mockFirebase(request, email);

    // Should invoke access denied JSP- If not error will be thrown.
    when(request.getParameter("Event")).thenReturn(correctKey);
    when(request.getParameter("userToken")).thenReturn(email);
    when(Firebase.isUserLoggedIn(anyString())).thenReturn(false);
    when(request.getRequestDispatcher("/WEB-INF/jsp/access-denied.html")).thenReturn(dispatcher);
    doNothing().when(dispatcher).forward(request, response);

    // Should handle IOException.
    try {
      testServlet.doGet(request, response);
    } catch (Exception e) {
      fail();
    }
  }

  @Test(expected = Test.None.class)
  public void exceptionHandelingForWrongUser() throws IOException, ServletException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    RequestDispatcher dispatcher = mock(RequestDispatcher.class);

    String email = "wrong@example.com";
    TestingUtil.mockFirebase(request, email);

    // Should invoke access denied JSP- If not error will be thrown.
    when(request.getParameter("Event")).thenReturn(correctKey);
    when(request.getParameter("userToken")).thenReturn(email);
    when(request.getRequestDispatcher("/WEB-INF/jsp/access-denied.html")).thenReturn(dispatcher);
    doNothing().when(dispatcher).forward(request, response);

    try {
      testServlet.doGet(request, response);
    } catch (Exception e) {
      fail();
    }
  }

  @Test(expected = Test.None.class)
  public void exceptionHandelingNotCreated() throws IOException, ServletException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    RequestDispatcher dispatcher = mock(RequestDispatcher.class);

    String email = "newemail@example.com";
    TestingUtil.mockFirebase(request, email);

    // Should invoke access denied JSP- If not error will be thrown.
    when(request.getParameter("Event")).thenReturn(correctKey);
    when(request.getParameter("userToken")).thenReturn(email);
    when(request.getRequestDispatcher("/WEB-INF/jsp/access-denied.html")).thenReturn(dispatcher);
    doNothing().when(dispatcher).forward(request, response);

    // Should handle IOException.
    try {
      testServlet.doGet(request, response);
    } catch (Exception e) {
      fail();
    }
  }

  @Test(expected = Test.None.class)
  public void exceptionHandelingCallDeletedEvent() throws IOException, ServletException {
    HttpServletRequest request = mock(HttpServletRequest.class);

    String email = "test@example.com";
    TestingUtil.mockFirebase(request, email);

    // Delete the event to be queried.
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Key key = KeyFactory.stringToKey(correctKey);
    datastore.delete(key);

    // Should invoke event not found- If not error will be thrown.
    when(request.getParameter("Event")).thenReturn(correctKey);
    when(request.getParameter("userToken")).thenReturn(email);

    HttpServletResponse response = mock(HttpServletResponse.class);
    RequestDispatcher dispatcher = mock(RequestDispatcher.class);
    when(request.getRequestDispatcher("/WEB-INF/jsp/event-not-found.jsp")).thenReturn(dispatcher);
    doNothing().when(dispatcher).forward(request, response);

    // Should handle NullPointerException.
    try {
      testServlet.doGet(request, response);
    } catch (Exception e) {
      fail();
    }
  }

  private void createGoalEntity() {
    String[] tags = {"environment"};

    goalEntity = new Entity("Event");
    goalEntity.setProperty("eventName", "Lake Clean Up");
    goalEntity.setProperty("eventDescription", "We're cleaning up the lake");
    goalEntity.setProperty("address", "13364 Lakeview Way, Lakeside, California");
    goalEntity.setProperty("streetAddress", "13364 Lakeview Way");
    goalEntity.setProperty("city", "Lakeside");
    goalEntity.setProperty("state", "California");
    goalEntity.setProperty("date", "May 17, 2020");
    goalEntity.setProperty("startTime", "2:00 PM");
    goalEntity.setProperty("endTime", "3:00 PM");
    goalEntity.setProperty("coverPhoto", "/img-2030121");
    goalEntity.setIndexedProperty("tags", Arrays.asList(tags));
    goalEntity.setProperty("creator", "test@example.com");
    goalEntity.setProperty("attendeeCount", 0L);
    goalEntity.setProperty("unformattedStart", "14:00");
    goalEntity.setProperty("unformattedEnd", "15:00");
    goalEntity.setProperty("unformattedDate", "2020-05-17");
    goalEntity.setProperty("latlng", Utils.getGeopt("13364 Lakeview Way, Lakeside, California"));
    LatLng location = Utils.getLatLng("13364 Lakeview Way, Lakeside, California");
    goalEntity.setProperty("lat", location.lat);
    goalEntity.setProperty("lng", location.lng);

    DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
    ds.put(goalEntity);

    Key goalKey = goalEntity.getKey();
    correctKey = KeyFactory.keyToString(goalKey);

    goalEntity.setProperty("eventKey", correctKey);
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
