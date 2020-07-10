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
import com.google.appengine.api.datastore.Query;
import com.google.sps.Utils;
import java.io.IOException;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/load-event")
public class LoadEventServlet extends HttpServlet {

  private static final Logger LOGGER = Logger.getLogger(LoadEventServlet.class.getName());

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    Key keyRequested;
    try {
      keyRequested = getEventKey(request);
      if (keyRequested != null) {
        Query query = new Query("Event", keyRequested);

        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Entity eventRequested = datastore.prepare(query).asSingleEntity();
        request = populateRequest(request, eventRequested);

        request.getRequestDispatcher("/WEB-INF/jsp/display-event.jsp").forward(request, response);
      }
    } catch (IllegalArgumentException | IOException | NullPointerException | ServletException e) {
      LOGGER.info("Could not retrieve event " + e);
      request.getRequestDispatcher("/WEB-INF/jsp/event-not-found.jsp").forward(request, response);
    }
  }

  /**
   * Supplies the request with attributes from the event requested.
   *
   * @return the request with attributes set.
   */
  private HttpServletRequest populateRequest(HttpServletRequest request, Entity event) {
    String name = event.getProperty("eventName").toString();
    String description = event.getProperty("eventDescription").toString();
    String date = event.getProperty("date").toString();
    String start = event.getProperty("startTime").toString();
    String end = event.getProperty("endTime").toString();
    String address = event.getProperty("address").toString();
    String tags = Utils.convertToJson(event.getProperty("tags"));

    request.setAttribute("name", name);
    request.setAttribute("description", description);
    request.setAttribute("date", date);
    request.setAttribute("start", start);
    request.setAttribute("end", end);
    request.setAttribute("address", address);
    request.setAttribute("tags", tags);

    return request;
  }

  /**
   * Retrieves the key string (if any) from the request parameter and converts it to type Key.
   *
   * @return the key from the request parameter.
   */
  private Key getEventKey(HttpServletRequest request) throws IllegalArgumentException, IOException {
    Key eventKey = null;
    // Get the string from the request.
    if (request.getParameter("Event") != null) {
      String eventKeyString = request.getParameter("Event");

      // Convert String to type Key.
      eventKey = KeyFactory.stringToKey(eventKeyString);
    } else {
      throw new IOException("Request is missing parameter");
    }
    return eventKey;
  }
}
