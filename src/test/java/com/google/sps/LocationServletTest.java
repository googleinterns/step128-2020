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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.gson.Gson;
import com.google.sps.servlets.LocationServlet;
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

/** Tests for the LocationServlet.java class */
@PowerMockIgnore("okhttp3.*")
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
  public void changeLocation() throws IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);

    String email = "test@example.com";
    TestingUtil.mockFirebase(request, email);
    setPriorLocation("test@example.com", "0");

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
  public void changeLocationNotLoggedIn() throws IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);

    String email = "";
    TestingUtil.mockFirebase(request, email);
    setPriorLocation("test@example.com", "0");

    when(request.getParameter("zip")).thenReturn("90001");

    HttpServletResponse response = mock(HttpServletResponse.class);

    try {
      testLocationServlet.doPost(request, response);
      fail();
    } catch (IOException e) {
      // ensure that location was not changed
      DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
      Key userKey = KeyFactory.createKey("User", "test@example.com");
      try {
        Entity userEntity = ds.get(userKey);
        assertEquals("0", userEntity.getProperty("location"));
      } catch (EntityNotFoundException exception) {
        fail();
      }
    }
  }

  @Test
  public void changeLocationNewUser() throws IOException {
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
  public void getLocation() throws IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);

    String email = "test@example.com";
    TestingUtil.mockFirebase(request, email);
    setPriorLocation("test@example.com", "90001");

    HttpServletResponse response = mock(HttpServletResponse.class);
    StringWriter out = new StringWriter();
    PrintWriter writer = new PrintWriter(out);
    when(response.getWriter()).thenReturn(writer);

    testLocationServlet.doGet(request, response);
    out.flush();

    String output = out.toString();
    output = output.substring(1, output.length() - 2);

    assertEquals("90001", output);
  }

  @Test
  public void getLocationNotLoggedIn() throws IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);

    String email = "";
    TestingUtil.mockFirebase(request, email);
    setPriorLocation("test@example.com", "90001");

    HttpServletResponse response = mock(HttpServletResponse.class);
    StringWriter out = new StringWriter();
    PrintWriter writer = new PrintWriter(out);
    when(response.getWriter()).thenReturn(writer);

    testLocationServlet.doGet(request, response);
    out.flush();

    String output = out.toString();
    output = output.substring(1, output.length() - 2);

    assertEquals("", output);
  }

  @Test
  public void getLocationNewUser() throws IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);

    String email = "test@example.com";
    TestingUtil.mockFirebase(request, email);

    HttpServletResponse response = mock(HttpServletResponse.class);
    StringWriter out = new StringWriter();
    PrintWriter writer = new PrintWriter(out);
    when(response.getWriter()).thenReturn(writer);

    testLocationServlet.doGet(request, response);
    out.flush();

    String output = out.toString();
    output = output.substring(1, output.length() - 2);

    assertEquals("", output);
  }

  /**
   * Adds a prior location for the requested user.
   *
   * @param email String containing the identifying email for the user
   * @param zip String containing the zip code of the prior location
   */
  public void setPriorLocation(String email, String zip) throws IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    TestingUtil.mockFirebase(request, email);
    when(request.getParameter("zip")).thenReturn(zip);
    testLocationServlet.doPost(request, response);
  }
}
