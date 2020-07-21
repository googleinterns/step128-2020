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

import static com.google.sps.Utils.getParameter;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.gson.Gson;
import com.google.sps.Firebase;
import com.google.sps.Utils;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/edit-event")
public class EditEventServlet extends HttpServlet {

  private static final Logger LOGGER = Logger.getLogger(LoadEventServlet.class.getName());

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String userToken = request.getParameter("userToken");
    if (Firebase.isUserLoggedIn(userToken)) {
      String userID = Firebase.authenticateUser(userToken);

      Key keyRequested = getEventKey(request, "key");

      Query query = new Query("Event", keyRequested);

      DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
      Entity eventRequested = datastore.prepare(query).asSingleEntity();

      Entity eventEntity = updateEvent(request, eventRequested);
      datastore.put(eventEntity);
    } else {
      throw new IOException("Cannot edit an event while not logged in");
    }

    // Redirect back to the my-events HTML page.
    response.sendRedirect("/my-events.html");
  }

  /** @return the updated Event entity */
  private Entity updateEvent(HttpServletRequest request, Entity event) {
    // Get the input from the form.
    String eventName = getParameter(request, "event-name", "");
    String eventDescription = getParameter(request, "event-description", "");
    String streetAddress = getParameter(request, "street-address", "");
    String city = getParameter(request, "city", "");
    String state = getParameter(request, "state", "");
    String date = getParameter(request, "date", "");
    String startTime = getParameter(request, "start-time", "");
    String endTime = getParameter(request, "end-time", "");
    String coverPhoto = getParameter(request, "cover-photo", "");
    String tagsStr = getParameter(request, "all-tags", "");

    String fullAddress = String.format("%1$s, %2$s, %3$s", streetAddress, city, state);
    String formattedDate = Utils.formatDate(date);
    String formattedTime = Utils.formatTime(startTime);

    String formattedTimeEnd = "";
    if (endTime != "") {
      formattedTimeEnd = Utils.formatTime(endTime);
    }

    event.setProperty("eventName", eventName);
    event.setProperty("eventDescription", eventDescription);
    event.setProperty("address", fullAddress);
    event.setProperty("date", formattedDate);
    event.setProperty("startTime", formattedTime);
    event.setProperty("endTime", formattedTimeEnd);
    event.setProperty("coverPhoto", coverPhoto);
    event.setProperty("attendeeCount", 0);
    event.setProperty("unformattedStart", startTime);
    event.setProperty("unformattedEnd", endTime);
    event.setProperty("unformattedDate", date);

    Gson gson = new Gson();
    String[] tags = gson.fromJson(tagsStr, String[].class);
    event.setIndexedProperty("tags", Arrays.asList(tags));

    return event;
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    Key keyRequested;
    try {
      keyRequested = getEventKey(request, "Event");
      if (keyRequested != null) {
        Query query = new Query("Event", keyRequested);

        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Entity eventRequested = datastore.prepare(query).asSingleEntity();

        String userToken = request.getParameter("userToken");
        if (userToken != null) {
          if (Firebase.isUserLoggedIn(userToken)) {
            String userID = Firebase.authenticateUser(userToken);
            Key userKey = KeyFactory.createKey("User", userID);
            try {
              Entity userEntity = datastore.get(userKey);

              if (userEntity != null) {
                boolean belongsToUser = isUsersEvent(userID, keyRequested);

                if (belongsToUser == true) {
                  request = populateRequest(request, eventRequested);
                  request
                      .getRequestDispatcher("/WEB-INF/jsp/edit-event-form.jsp")
                      .forward(request, response);
                } else {
                  request
                      .getRequestDispatcher("/WEB-INF/jsp/access-denied.jsp")
                      .forward(request, response);
                }
              }
            } catch (EntityNotFoundException exception) {
              // datastore entry has not been created yet for this user, create it now
              Entity entity = new Entity(userKey);
              entity.setProperty("firebaseID", userID);
              datastore.put(entity);
              request
                  .getRequestDispatcher("/WEB-INF/jsp/access-denied.jsp")
                  .forward(request, response);
            }
          }
        }
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
    String address = event.getProperty("address").toString();
    String start = event.getProperty("unformattedStart").toString();
    String end = event.getProperty("unformattedEnd").toString();
    String date = event.getProperty("unformattedDate").toString();
    String tags = Utils.convertToJson(event.getProperty("tags"));
    String key = event.getProperty("eventKey").toString();
    long eventId = event.getKey().getId();

    request.setAttribute("name", name);
    request.setAttribute("description", description);
    request.setAttribute("date", date);
    request.setAttribute("start", start);
    request.setAttribute("end", end);
    request.setAttribute("address", address);
    request.setAttribute("tags", tags);
    request.setAttribute("key", key);
    request.setAttribute("id", eventId);
    request.setAttribute("tags", tags);
    request.setAttribute("key", key);
    request.setAttribute("id", eventId);

    return request;
  }

  /**
   * Retrieves the key string (if any) from the request parameter and converts it to type Key.
   *
   * @return the key from the request parameter.
   */
  private Key getEventKey(HttpServletRequest request, String name)
      throws IllegalArgumentException, IOException {
    Key eventKey = null;

    // Get the string from the request.
    if (request.getParameter(name) != null) {
      String eventKeyString = request.getParameter(name);

      // Convert String to type Key.
      eventKey = KeyFactory.stringToKey(eventKeyString);
    } else {
      throw new IOException("Request is missing parameter");
    }
    return eventKey;
  }

  /**
   * Checks if the event queried belongs to the user logged in.
   *
   * @return a boolean 'exists' which contains if the event belongs to the user
   */
  private boolean isUsersEvent(String userID, Key entityKey) {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    String eventKeyString = KeyFactory.keyToString(entityKey);
    boolean exists = false;

    // Query events created by user logged in.
    Query query =
        new Query("Event")
            .setFilter(new Query.FilterPredicate("creator", Query.FilterOperator.EQUAL, userID));
    PreparedQuery queried = datastore.prepare(query);

    // Checks if the event is in the list of events created by the user.
    for (Entity e : queried.asIterable()) {
      String userEventKey = e.getProperty("eventKey").toString();

      if (userEventKey.equals(eventKeyString)) {
        exists = true;
      }
    }
    return exists;
  }
}
