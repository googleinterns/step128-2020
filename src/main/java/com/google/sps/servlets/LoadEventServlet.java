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
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.sps.Firebase;
import com.google.sps.Utils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
        int alreadySaved = -1;

        String userToken = request.getParameter("userToken");
        if (userToken != null) {
          if (Firebase.isUserLoggedIn(userToken)) {
            String userID = Firebase.authenticateUser(userToken);
            Key userKey = KeyFactory.createKey("User", userID);
            try {
              Entity userEntity = datastore.get(userKey);
              alreadySaved = alreadySaved(eventRequested.getKey().getId(), userEntity);

            } catch (EntityNotFoundException exception) {
              // datastore entry has not been created yet for this user, create it now
              Entity entity = new Entity(userKey);
              entity.setProperty("firebaseID", userID);
              datastore.put(entity);
            }
          }
        }
        request = populateRequest(request, eventRequested, alreadySaved);

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
  private HttpServletRequest populateRequest(
      HttpServletRequest request, Entity event, int alreadySaved) {
    String name = event.getProperty("eventName").toString();
    String description = event.getProperty("eventDescription").toString();
    String date = event.getProperty("date").toString();
    String start = event.getProperty("startTime").toString();
    String end = event.getProperty("endTime").toString();
    String address = event.getProperty("address").toString();
    String tags = Utils.convertToJson(event.getProperty("tags"));
    String attendeeCount = event.getProperty("attendeeCount").toString();
    String key = event.getProperty("eventKey").toString();
    long eventId = event.getKey().getId();

    request.setAttribute("name", name);
    request.setAttribute("description", description);
    request.setAttribute("date", date);
    request.setAttribute("start", start);
    request.setAttribute("end", end);
    request.setAttribute("address", address);
    request.setAttribute("tags", tags);
    request.setAttribute("id", eventId);
    request.setAttribute("attendees", attendeeCount);
    request.setAttribute("saved", alreadySaved);
    request.setAttribute("key", key);

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

  /**
   * Checks if user has already saved an event.
   *
   * @return the index of the event in user's saved list, or -1 if not found
   */
  private int alreadySaved(long eventId, Entity userEntity) {
    List<Long> saved = (ArrayList<Long>) userEntity.getProperty("saved");
    if (saved == null) {
      saved = new ArrayList<>();
    }
    for (int i = 0; i < saved.size(); i++) {
      if (saved.get(i) == eventId) {
        return i;
      }
    }
    return -1;
  }
}
