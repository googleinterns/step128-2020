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
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.sps.servlets.ConfirmUserServlet;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
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

/** Tests for the SurveyServlet.java class */
@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor({"com.google.sps.Firebase"})
@PrepareForTest({Firebase.class})
@PowerMockIgnore("okhttp3.*")
public final class ConfirmUserServletTest {
  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
  private ConfirmUserServlet testServlet;
  private Entity event;
  private String correctKey;

  /** Set up Entity and Entity key to test for. */
  @Before
  public void setUp() {
    helper.setUp();
    testServlet = new ConfirmUserServlet();

    createGoalEntity();
    createUser("test");
    createUser("wrong");
  }

  @After
  public void tearDown() {
    helper.tearDown();
  }

  @Test
  public void accessCorrectEvent() throws IOException, ServletException {
    HttpServletRequest request = mock(HttpServletRequest.class);

    String email = "test@example.com";
    TestingUtil.mockFirebase(request, email);
    when(request.getParameter("Event")).thenReturn(correctKey);
    when(request.getParameter("userToken")).thenReturn(email);

    HttpServletResponse response = mock(HttpServletResponse.class);
    StringWriter out = new StringWriter();
    PrintWriter writer = new PrintWriter(out);
    when(response.getWriter()).thenReturn(writer);

    testServlet.doGet(request, response);

    String output = (out.toString()).trim();
    assertEquals("true", output);
  }

  @Test
  public void accessIncorrectEvent() throws IOException, ServletException {
    HttpServletRequest request = mock(HttpServletRequest.class);

    String email = "wrong@example.com";
    TestingUtil.mockFirebase(request, email);
    when(request.getParameter("Event")).thenReturn(correctKey);
    when(request.getParameter("userToken")).thenReturn(email);

    HttpServletResponse response = mock(HttpServletResponse.class);
    StringWriter out = new StringWriter();
    PrintWriter writer = new PrintWriter(out);
    when(response.getWriter()).thenReturn(writer);

    testServlet.doGet(request, response);

    String output = (out.toString()).trim();
    assertEquals("false", output);
  }

  private void createGoalEntity() {
    event = new Entity("Event");
    event.setProperty("eventName", "Book Drive");
    event.setProperty("eventDescription", "We're collecting books.");
    event.setProperty("address", "456 Library Way, Bookville, Washington");
    event.setProperty("streetAddress", "456 Library Way");
    event.setProperty("city", "Bookville");
    event.setProperty("state", "Washington");
    event.setProperty("date", "May 17, 2020");
    event.setProperty("startTime", "2:00 PM");
    event.setProperty("endTime", "4:00 PM");
    event.setProperty("coverPhoto", "/img-2030121");
    String[] tags = {"volunteer"};
    event.setIndexedProperty("tags", Arrays.asList(tags));
    event.setProperty("creator", "test@example.com");
    event.setProperty("attendeeCount", 0L);
    event.setProperty("unformattedStart", "14:00");
    event.setProperty("unformattedEnd", "16:00");
    event.setProperty("unformattedDate", "2020-05-17");

    DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
    ds.put(event);

    Key key = event.getKey();
    correctKey = KeyFactory.keyToString(key);

    event.setProperty("eventKey", correctKey);
    ds.put(event);
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
