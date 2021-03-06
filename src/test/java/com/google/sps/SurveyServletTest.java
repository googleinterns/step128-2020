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
import com.google.appengine.api.datastore.Query;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.sps.servlets.SurveyServlet;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
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
@PowerMockIgnore("okhttp3.*")
@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor({"com.google.sps.Firebase"})
@PrepareForTest({Firebase.class})
public final class SurveyServletTest {
  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
  private SurveyServlet testSurveyServlet;

  @Before
  public void setUp() throws IOException {
    helper.setUp();
    testSurveyServlet = new SurveyServlet();
  }

  @After
  public void tearDown() {
    helper.tearDown();
  }

  @Test
  public void noSurvey() throws IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    String email = "test@example.com";
    TestingUtil.mockFirebase(request, email);

    HttpServletResponse response = mock(HttpServletResponse.class);
    StringWriter out = new StringWriter();
    PrintWriter writer = new PrintWriter(out);
    when(response.getWriter()).thenReturn(writer);

    testSurveyServlet.doGet(request, response);
    out.flush();
    assertEquals("false", out.toString());
  }

  @Test
  public void checkSurveyNotLoggedIn() throws IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);

    String email = "";
    TestingUtil.mockFirebase(request, email);
    StringWriter out = new StringWriter();
    PrintWriter writer = new PrintWriter(out);
    when(response.getWriter()).thenReturn(writer);

    testSurveyServlet.doGet(request, response);
    out.flush();
    assertEquals("false", out.toString());
  }

  @Test
  public void submitSurvey() throws IOException {
    // basic test to make sure all parameters have been posted to datastore

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);

    String email = "test@example.com";
    TestingUtil.mockFirebase(request, email);

    when(request.getParameter("environment")).thenReturn("3");
    when(request.getParameter("blm")).thenReturn("4");
    when(request.getParameter("volunteer")).thenReturn("3");
    when(request.getParameter("education")).thenReturn("2");
    when(request.getParameter("LGBTQ+")).thenReturn("4");
    when(request.getParameter("healthcare")).thenReturn("4");
    when(request.getParameter("civics")).thenReturn("4");
    when(request.getParameter("fundraiser")).thenReturn("4");
    when(request.getParameter("activism")).thenReturn("4");
    when(request.getParameter("item donation")).thenReturn("4");

    testSurveyServlet.doPost(request, response);

    DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
    Entity userEntity = ds.prepare(new Query("User")).asSingleEntity();
    assertEquals("3", userEntity.getProperty("environment"));
    assertEquals("4", userEntity.getProperty("blm"));
    assertEquals("3", userEntity.getProperty("volunteer"));
    assertEquals("2", userEntity.getProperty("education"));
    assertEquals("4", userEntity.getProperty("LGBTQ+"));
    assertEquals("4", userEntity.getProperty("healthcare"));
    assertEquals("4", userEntity.getProperty("civics"));
    assertEquals("4", userEntity.getProperty("fundraiser"));
    assertEquals("4", userEntity.getProperty("activism"));
    assertEquals("4", userEntity.getProperty("item donation"));
    assertEquals("true", userEntity.getProperty("surveyCompleted"));

    StringWriter out = new StringWriter();
    PrintWriter writer = new PrintWriter(out);
    when(response.getWriter()).thenReturn(writer);

    testSurveyServlet.doGet(request, response);
    out.flush();
    assertEquals("true", out.toString());
  }

  @Test
  public void submitIncomplete() throws IOException {
    // try to submit a survey with incomplete fields

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);

    String email = "test@example.com";
    TestingUtil.mockFirebase(request, email);

    when(request.getParameter("environment")).thenReturn("3");
    when(request.getParameter("blm")).thenReturn("4");
    when(request.getParameter("volunteer")).thenReturn("3");
    when(request.getParameter("education")).thenReturn("2");

    try {
      testSurveyServlet.doPost(request, response);
      fail();
    } catch (IOException expected) {

      // make sure nothing has been posted
      DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
      Entity userEntity = ds.prepare(new Query("User")).asSingleEntity();
      assertEquals(null, userEntity.getProperty("surveyCompleted"));
      for (String param : Interactions.metrics) {
        assertEquals(false, userEntity.hasProperty(param));
      }

      StringWriter out = new StringWriter();
      PrintWriter writer = new PrintWriter(out);
      when(response.getWriter()).thenReturn(writer);

      testSurveyServlet.doGet(request, response);
      out.flush();
      assertEquals("false", out.toString());
    }
  }

  @Test
  public void submitWithoutLogin() throws IOException {
    // try to submit a survey without being logged in

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);

    String email = "";
    TestingUtil.mockFirebase(request, email);

    when(request.getParameter("environment")).thenReturn("3");
    when(request.getParameter("blm")).thenReturn("4");
    when(request.getParameter("volunteer")).thenReturn("3");
    when(request.getParameter("education")).thenReturn("2");
    when(request.getParameter("LGBTQ+")).thenReturn("4");
    when(request.getParameter("healthcare")).thenReturn("4");
    when(request.getParameter("civics")).thenReturn("4");
    when(request.getParameter("fundraiser")).thenReturn("4");
    when(request.getParameter("activism")).thenReturn("4");
    when(request.getParameter("item donation")).thenReturn("4");

    try {
      testSurveyServlet.doPost(request, response);

      fail();
    } catch (IOException expected) {

      // make sure nothing has been posted
      DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
      Entity userEntity = ds.prepare(new Query("User")).asSingleEntity();

      assertEquals(null, userEntity);
    }
  }
}
