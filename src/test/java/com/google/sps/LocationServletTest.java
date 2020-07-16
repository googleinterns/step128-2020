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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;
import com.google.gson.Gson;

/** Tests for the SurveyServlet.java class */
@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor({"com.google.sps.Firebase"})
@PrepareForTest({Firebase.class})
public final class LocationServletTest {
  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
  private LocationServlet testLocationServlet;
  private final Gson gson = new Gson();

  @Before
  public void setUp() throws IOException {
    helper.setUp();
    testLocationServlet = new LocationServlet();
  }

  @After
  public void tearDown() {
    helper.tearDown();
  }

  @Test
  public void changeLocation() throws IOException{
    HttpServletRequest request = mock(HttpServletRequest.class);
    
    String email = "test@example.com";
    TestingUtil.mockFirebase(request, email);
    setPriorLocation();

    when(request.getParameter("zip")).thenReturn("90001");

    HttpServletResponse response = mock(HttpServletResponse.class);
    testLocationServlet.doPost(request, response);

    DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
    Key userKey = KeyFactory.createKey("User", email);
    try {
      Entity userEntity = ds.get(userKey);
      assertEquals("90001", userEntity.getProperty("location"));
    } catch (EntityNotFoundException exception) {
      fail();
    }
  }

  @Test
  public void changeLocationNotLoggedIn() throws IOException{
    HttpServletRequest request = mock(HttpServletRequest.class);
    
    String email = "";
    TestingUtil.mockFirebase(request, email);
    setPriorLocation();

    when(request.getParameter("zip")).thenReturn("90001");

    HttpServletResponse response = mock(HttpServletResponse.class);
    testLocationServlet.doPost(request, response);

    DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
    Key userKey = KeyFactory.createKey("User", "test@example.com");
    try {
      Entity userEntity = ds.get(userKey);
      assertEquals("0", userEntity.getProperty("location"));
    } catch (EntityNotFoundException exception) {
      fail();
    }
  }

  @Test
  public void changeLocationNewUser() throws IOException{
    HttpServletRequest request = mock(HttpServletRequest.class);
    
    String email = "test@example.com";
    TestingUtil.mockFirebase(request, email);

    when(request.getParameter("zip")).thenReturn("90001");

    HttpServletResponse response = mock(HttpServletResponse.class);
    testLocationServlet.doPost(request, response);

    DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
    Key userKey = KeyFactory.createKey("User", email);
    try {
      Entity userEntity = ds.get(userKey);
      assertEquals("90001", userEntity.getProperty("location"));
    } catch (EntityNotFoundException exception) {
      fail();
    }
  }

  @Test
  public void getLocation() throws IOException{
    HttpServletRequest request = mock(HttpServletRequest.class);
    
    String email = "test@example.com";
    TestingUtil.mockFirebase(request, email);
    setPriorLocation();

    HttpServletResponse response = mock(HttpServletResponse.class);
    StringWriter out = new StringWriter();
    PrintWriter writer = new PrintWriter(out);
    when(response.getWriter()).thenReturn(writer);

    testLocationServlet.doPost(request, response);
    out.flush();

    String output = out.toString();
    
    assertEquals("0", output);
  }

  @Test
  public void getLocationNotLoggedIn() throws IOException{
    
  }

  @Test
  public void getLocationNewUser() throws IOException{
    
  }

  
  public static void setPriorLocation() throws IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    TestingUtil.mockFirebase(request, email);
    when(request.getParameter("zip")).thenReturn("0");
    testEventServlet.doPost(request, response);
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

    testSurveyServlet.doPost(request, response);

    DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
    Entity userEntity = ds.prepare(new Query("User")).asSingleEntity();
    assertEquals("3", userEntity.getProperty("environment"));
    assertEquals("4", userEntity.getProperty("blm"));
    assertEquals("3", userEntity.getProperty("volunteer"));
    assertEquals("2", userEntity.getProperty("education"));
    assertEquals("4", userEntity.getProperty("LGBTQ+"));
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

      assertEquals(false, userEntity.hasProperty("environment"));
      assertEquals(false, userEntity.hasProperty("blm"));
      assertEquals(false, userEntity.hasProperty("volunteer"));
      assertEquals(false, userEntity.hasProperty("education"));
      assertEquals(false, userEntity.hasProperty("LGBTQ+"));
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