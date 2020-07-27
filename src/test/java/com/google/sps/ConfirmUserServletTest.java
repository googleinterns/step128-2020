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
  private String goalKeyString;
  private Entity goalEntity;

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
    when(request.getParameter("Event")).thenReturn(goalKeyString);
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
    when(request.getParameter("Event")).thenReturn(goalKeyString);
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
    String[] tags = {"environment"};

    goalEntity = new Entity("Event");
    goalEntity.setProperty("eventName", "Lake Clean Up");
    goalEntity.setProperty("eventDescription", "We're cleaning up the lake");
    goalEntity.setProperty("address", "678 Lakeview Way, Lakeside, Michigan");
    goalEntity.setProperty("streetAddress", "678 Lakeview Way");
    goalEntity.setProperty("city", "Lakeside");
    goalEntity.setProperty("state", "Michigan");
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
