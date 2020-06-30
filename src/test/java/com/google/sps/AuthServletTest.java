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
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.sps.servlets.AuthServlet;
import com.google.sps.MockedUserService;
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
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.StringReader;
import java.net.URL;
import java.net.MalformedURLException;

/** */
@RunWith(PowerMockRunner.class)
@PrepareForTest({URL.class, UserServiceFactory.class})
public final class AuthServletTest {
  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
  private final Gson gson = new Gson();
  private AuthServlet testAuthServlet;
  private MockedUserService mockService;

  private String makeLoginURL(String url, String user) {
    return url + "email=" + user;
  }

  @Before
  public void setUp() {
    helper.setUp();
    testAuthServlet = new AuthServlet();

    PowerMockito.mockStatic(UserServiceFactory.class);
    mockService = new MockedUserService();
    when(UserServiceFactory.getUserService()).thenReturn(mockService);
  }

  @After
  public void tearDown() {
    helper.tearDown();
    mockService = null;
  }

  @Test
  public void loggedOut() throws IOException {
    // logged out state
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);

    StringWriter out = new StringWriter();
    PrintWriter writer = new PrintWriter(out);
    when(response.getWriter()).thenReturn(writer);

    testAuthServlet.doGet(request, response);
    
    out.flush();
    LoginObject result = gson.fromJson(out.toString(), LoginObject.class);
    assertEquals(false, result.loggedIn);
    assertTrue(result.url.contains("login"));
  }

  @Test
  public void doLogin() throws IOException {
    // login from a logged out state
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);

    StringWriter out = new StringWriter();
    PrintWriter writer = new PrintWriter(out);
    when(response.getWriter()).thenReturn(writer);

    testAuthServlet.doGet(request, response);
    
    out.flush();
    LoginObject resultObj = gson.fromJson(out.toString(), LoginObject.class);

    String authUrl = resultObj.url;

    try {
      URL mockurl = PowerMockito.mock(URL.class);
      when(mockurl.openConnection()).thenReturn(mockService.evaluateURL(makeLoginURL(authUrl, "test@example.com")));
      mockurl.openConnection();

      // make a second request that logs in
      request = mock(HttpServletRequest.class);
      response = mock(HttpServletResponse.class);
      out = new StringWriter();
      writer = new PrintWriter(out);
      when(response.getWriter()).thenReturn(writer);

      testAuthServlet.doGet(request, response);
      out.flush();
      LoginObject result = gson.fromJson(out.toString(), LoginObject.class);
      assertEquals(true, result.loggedIn);
      assertTrue(result.url.contains("logout"));
      assertEquals("test@example.com",mockService.getCurrentUser().getEmail());

      DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
      assertEquals(1, ds.prepare(new Query("User")).countEntities());
    } catch (MalformedURLException e) {
      fail();
    } catch (IOException e) {
      fail();
    }
  }

  /* the LoginObject structure used by AuthServlet */
  private static class LoginObject {
    private boolean loggedIn;
    private String url;
  }
}
