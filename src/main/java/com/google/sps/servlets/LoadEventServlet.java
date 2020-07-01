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

package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import java.io.IOException;
import java.lang.IllegalArgumentException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

@WebServlet("/load-event")
public class LoadEventServlet extends HttpServlet {

  private static final Logger LOGGER = Logger.getLogger(LoadEventServlet.class.getName());

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    Key keyRequested = getEventKey(request);
    Query query = new Query("Event", keyRequested);

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Entity eventRequested = datastore.prepare(query).asSingleEntity();

    request = populateRequest(request, eventRequested);

    request.getRequestDispatcher("/WEB-INF/html/display-event.jsp").forward(request, response);
  }

  /**
   * @return the request with attributes set.
   */ 
  private HttpServletRequest populateRequest(HttpServletRequest request, Entity event) {
    String name = (String) event.getProperty("eventName");
    String description = (String) event.getProperty("eventDescription");
    String date = (String) event.getProperty("date");
    String start = (String) event.getProperty("startTime");
    String end = (String) event.getProperty("endTime");
    String street = (String) event.getProperty("streetAddress");
    String city = (String) event.getProperty("city");
    String state = (String) event.getProperty("state");

    request.setAttribute("name", name);
    request.setAttribute("description", description);
    request.setAttribute("date", date);
    request.setAttribute("start", start);
    request.setAttribute("end", end);
    request.setAttribute("street", street);
    request.setAttribute("city", city);
    request.setAttribute("state", state);

    return request;
  } 

  /**
   * @return the key from the request parameter.
   */ 
  private Key getEventKey(HttpServletRequest request) throws IllegalArgumentException {
    // Get the string from the request.
    String eventKeyString = request.getParameter("event");

    // Convert String to type Key.
    Key eventKey = KeyFactory.stringToKey(eventKeyString);
    return eventKey;
  }
}
