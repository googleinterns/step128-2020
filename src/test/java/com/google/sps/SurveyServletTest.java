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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.sps.servlets.AuthServlet;
import com.google.sps.servlets.SurveyServlet;
import com.google.gson.Gson;
import java.io.IOException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.StringReader;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.TreeMap;

/** */
@RunWith(PowerMockRunner.class)
@PrepareForTest({URL.class, UserServiceFactory.class})
public final class SurveyServletTest {
  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
  private final Gson gson = new Gson();
  private AuthServlet testAuthServlet;
  private SurveyServlet testSurveyServlet;
  private MockedUserService mockService;

  private String activeUrl;

  /**
   * Use the current url to login/logout
   *
   * @param email If logging in, will log into this user's account.
   */
  private void toggleLogin(String email) throws MalformedURLException, IOException {
    URL mockurl = PowerMockito.mock(URL.class);
    when(mockurl.openConnection()).thenReturn(mockService.evaluateURL(AuthServletTest.makeLoginURL(activeUrl, email)));
    mockurl.openConnection();

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    StringWriter out = new StringWriter();
    PrintWriter writer = new PrintWriter(out);
    when(response.getWriter()).thenReturn(writer);

    testAuthServlet.doGet(request, response);
    out.flush();
    LoginObject result = gson.fromJson(out.toString(), LoginObject.class);
    activeUrl = result.url;
  }

  @Before
  public void setUp() throws IOException {
    helper.setUp();
    PowerMockito.mockStatic(UserServiceFactory.class);
    mockService = new MockedUserService();
    when(UserServiceFactory.getUserService()).thenReturn(mockService);
    testAuthServlet = new AuthServlet();
    testSurveyServlet = new SurveyServlet();

    // get the initial login url
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    StringWriter out = new StringWriter();
    PrintWriter writer = new PrintWriter(out);
    when(response.getWriter()).thenReturn(writer);
    testAuthServlet.doGet(request, response);
    out.flush();

    LoginObject result = gson.fromJson(out.toString(), LoginObject.class);
    activeUrl = result.url;
  }

  @After
  public void tearDown() {
    helper.tearDown();
    activeUrl = "";
  }

  @Test
  public void submitSurvey() throws IOException {
    // basic test to make sure all parameters have been posted to datastore

    String email = "test@example.com";
    toggleLogin(email);

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
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
    String email = "test@example.com";
    toggleLogin(email);

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
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

    String email = "test@example.com";
    // login then log back out
    toggleLogin(email);
    toggleLogin(email);

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
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

      assertEquals(false, userEntity.hasProperty("environment"));
      assertEquals(false, userEntity.hasProperty("blm"));
      assertEquals(false, userEntity.hasProperty("volunteer"));
      assertEquals(false, userEntity.hasProperty("education"));
      assertEquals(false, userEntity.hasProperty("LGBTQ+"));
    }
  }

  @Test
  public void checkInterestMetrics() throws IOException{
    // a basic test to make sure SurveyServlet.getInterestMetrics() works as intended
    String email = "test@example.com";
    toggleLogin(email);

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    when(request.getParameter("environment")).thenReturn("3");
    when(request.getParameter("blm")).thenReturn("4");
    when(request.getParameter("volunteer")).thenReturn("3");
    when(request.getParameter("education")).thenReturn("2");
    when(request.getParameter("LGBTQ+")).thenReturn("4");

    testSurveyServlet.doPost(request, response);

    Map<String, Integer> expectedSurvey = new TreeMap<>();
    expectedSurvey.put("environment", 3);
    expectedSurvey.put("blm", 4);
    expectedSurvey.put("volunteer", 3);
    expectedSurvey.put("education", 2);
    expectedSurvey.put("LGBTQ+", 4);

    assertEquals(expectedSurvey, SurveyServlet.getInterestMetrics(email));
  }

  @Test
  public void noUserForCheckMetrics() throws IOException {
    // call SurveyServlet.getInterestMetrics() on nonexistent user

    String email = "test@example.com";
    toggleLogin(email);

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    when(request.getParameter("environment")).thenReturn("3");
    when(request.getParameter("blm")).thenReturn("4");
    when(request.getParameter("volunteer")).thenReturn("3");
    when(request.getParameter("education")).thenReturn("2");
    when(request.getParameter("LGBTQ+")).thenReturn("4");
    testSurveyServlet.doPost(request, response);

    assertEquals(null, SurveyServlet.getInterestMetrics("other@example.com"));
  }

  /* the LoginObject structure used by AuthServlet */
  private static class LoginObject {
    private boolean loggedIn;
    private String url;
  }
}
