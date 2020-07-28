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

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.gson.Gson;
import com.google.sps.servlets.AuthServlet;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

/** Utility class to handle login operations during testing. */
@PrepareForTest({UserServiceFactory.class, Firebase.class})
public final class TestingUtil {
  private static final Gson gson = new Gson();
  private static AuthServlet testAuthServlet;
  private static MockedUserService mockService;

  private static String activeUrl = null;

  /** Initializes the MockedUserService and fetches the first login url. */
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

  /** Resets the state of the active url. */
  public static void tearDown() {
    activeUrl = null;
  }

  /**
   * Mock the functionality of Firebase.
   *
   * @param request HTTPServletRequest to mock parameter calls from
   * @param dummyToken String to be used in place of a client sent token
   */
  public static void mockFirebase(HttpServletRequest request, String dummyToken) {
    PowerMockito.mockStatic(Firebase.class);
    when(request.getParameter("userToken")).thenReturn(dummyToken);
    PowerMockito.when(Firebase.isUserLoggedIn(anyString())).thenCallRealMethod();
    PowerMockito.when(Firebase.authenticateUser(anyString())).thenReturn(dummyToken);
  }

  /**
   * Orders a list of events by a given order.
   *
   * @param desiredOrder List containing the eventNames in the order they should be in
   * @param events List of events to be ordered
   * @return List containing the events ordered
   */
  public static List<Entity> orderEvents(List<String> desiredOrder, List<Entity> events) {
    List<Entity> orderedEvents = new ArrayList<Entity>();
    for (int o = 0; o < desiredOrder.size(); o++) {
      for (int i = 0; i < events.size(); i++) {
        if (events.get(i).getProperty("eventName").toString().equals(desiredOrder.get(o))) {
          orderedEvents.add(events.get(i));
        }
      }
    }
    return orderedEvents;
  }

  /**
   * Fetches entities from the datastore using the event names.
   *
   * @param eventNames List containing the names of the entities to fetch from the Datastore
   * @return List containing the requested entities
   */
  public static List<Entity> fetchIDsFromDataStore(List<String> eventNames) {
    Filter idFilter = new FilterPredicate("eventName", FilterOperator.IN, eventNames);
    Query query = new Query("Event").setFilter(idFilter);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);
    List<Entity> events =
        new ArrayList<Entity>(results.asList(FetchOptions.Builder.withDefaults()));
    return events;
  }

  /**
   * Use the current url to login/logout (DEPRECATED)
   *
   * @param email If logging in, will log into this user's account.
   */
  public static void toggleLogin(String email) throws IOException {
    if (activeUrl == null) {
      throw new IllegalStateException("must call setUp() first");
    }
    mockService.evaluateUrl(AuthServletTest.makeLoginURL(activeUrl, email));

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
