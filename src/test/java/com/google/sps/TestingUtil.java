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

import static org.mockito.Mockito.*;

import com.google.appengine.api.users.UserServiceFactory;
import com.google.gson.Gson;
import com.google.sps.servlets.AuthServlet;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

/** */
@PrepareForTest(UserServiceFactory.class)
public final class TestingUtil {
  private static final Gson gson = new Gson();
  private static AuthServlet testAuthServlet;
  private static MockedUserService mockService;

  private static String activeUrl = null;

  // a utility class to handle login during testing

  public static void setUp() throws IOException {
    PowerMockito.mockStatic(UserServiceFactory.class);
    mockService = new MockedUserService();
    when(UserServiceFactory.getUserService()).thenReturn(mockService);
    testAuthServlet = new AuthServlet();

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

  public static void tearDown() {
    activeUrl = null;
  }

  /**
   * Use the current url to login/logout
   *
   * @param email If logging in, will log into this user's account.
   */
  public static void toggleLogin(String email) throws IOException {
    if (activeUrl == null) {
      throw new IllegalStateException("must call setUp() first");
    }
    mockService.evaluateURL(AuthServletTest.makeLoginURL(activeUrl, email));

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

  /* the LoginObject structure used by AuthServlet */
  private static class LoginObject {
    private boolean loggedIn;
    private String url;
  }
}