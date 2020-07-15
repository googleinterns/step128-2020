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
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.gson.Gson;
import com.google.sps.Firebase;
import com.google.sps.Utils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/user")
public class UserServlet extends HttpServlet {

  private static final Logger LOGGER = Logger.getLogger(UserServlet.class.getName());

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // returns a list of events
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

    Gson gson = new Gson();
    List<Entity> events = new ArrayList<>();
    response.setContentType("application/json");

    String userToken = request.getParameter("userToken");
    String userID = "";
    if (Firebase.isUserLoggedIn(userToken)) {
      userID = Firebase.authenticateUser(userToken);
      Key userKey = KeyFactory.createKey("User", userID);
      Entity userEntity = null;
      try {
        userEntity = datastore.get(userKey);
      } catch (EntityNotFoundException exception) {
        // datastore entry has not been created yet for this user, create it now
        userEntity = new Entity(userKey);
        userEntity.setProperty("firebaseID", userID);
        datastore.put(userEntity);
      }
      switch (request.getParameter("get")) {
        case "saved":
          events = getHandleSaved(userEntity);
          break;
        case "created":
          events = getHandleCreated(userID);
          break;
        default:
          throw new IOException("missing or invalid parameters");
      }
      LOGGER.info("queried for events @ account " + userID);
    } else {
      // return a list with all created events
      PreparedQuery results =
          datastore.prepare(new Query("Event").addSort("eventName", SortDirection.ASCENDING));
      for (Entity e : results.asIterable()) {
        events.add(e);
      }
    }
    // TODO: apply any sort params
    Collections.sort(events, Utils.ORDER_BY_NAME);
    response.getWriter().println(gson.toJson(events));
  }

  // returns a list of all events saved by a user entity
  private List<Entity> getHandleSaved(Entity userEntity) {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    List<Entity> results = new ArrayList<>();
    // get the list of saved events (stored by id)
    @SuppressWarnings("unchecked")
    List<Long> savedEvents = (ArrayList<Long>) userEntity.getProperty("saved");
    if (savedEvents != null) {
      for (long l : savedEvents) {
        try {
          results.add(datastore.get(KeyFactory.createKey("Event", l)));
        } catch (EntityNotFoundException exception) {
          LOGGER.info("entity not found for event id " + l);
        }
      }
    }
    return results;
  }

  // returns a list of all events created by a user (identified by firebaseID)
  private List<Entity> getHandleCreated(String userID) {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    List<Entity> results = new ArrayList<>();

    Query query =
        new Query("Event")
            .setFilter(new Query.FilterPredicate("creator", Query.FilterOperator.EQUAL, userID));
    PreparedQuery queried = datastore.prepare(query);
    for (Entity e : queried.asIterable()) {
      results.add(e);
    }
    return results;
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // adds or removes events from user's saved events list
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

    String userToken = request.getParameter("userToken");
    if (!Firebase.isUserLoggedIn(userToken)) {
      throw new IOException("must be logged in");
    }
    if (request.getParameter("event") == null) {
      throw new IOException("no event key specified");
    }
    String userID = Firebase.authenticateUser(userToken);
    Long eventId = 0L;
    try {
      eventId = Long.parseLong(request.getParameter("event"));
    } catch (NumberFormatException e) {
      throw new IOException("invalid format for event key");
    }
    // Handle the logic
    Key userKey = KeyFactory.createKey("User", userID);

    Entity userEntity = null;
    try {
      userEntity = datastore.get(userKey);
    } catch (EntityNotFoundException exception) {
      // datastore entry has not been created yet for this user, create it now
      userEntity = new Entity(userKey);
      userEntity.setProperty("firebaseID", userID);
      datastore.put(userEntity);
    }
    List<Long> saved = (ArrayList<Long>) userEntity.getProperty("saved");
    if (saved == null) {
      saved = new ArrayList<>();
    }
    switch (request.getParameter("action")) {
      case "save":
        postHandleSave(saved, eventId);
        break;
      case "unsave":
        postHandleUnsave(saved, eventId);
        break;
      default:
        throw new IOException("missing or invalid parameters");
    }
    userEntity.setProperty("saved", saved);
    datastore.put(userEntity);

    response.sendRedirect("/my-events.html");
  }

  // adds event id to list if it is not already present
  private void postHandleSave(List<Long> saved, long eventId) {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Key eventKey = KeyFactory.createKey("Event", eventId);
    try {
      Entity eventEntity = datastore.get(eventKey);
      if (saved.contains(eventId)) {
        LOGGER.info("event " + eventId + " has already been saved");
        return;
      }
      Object attendees = eventEntity.getProperty("attendeeCount");
      int attendeeCount = 1;
      if (attendees != null) {
        try {
          attendeeCount += Integer.parseInt(attendees.toString());
        } catch (NumberFormatException num) {
          LOGGER.info("error parsing attendee count for event id " + eventId);
          attendeeCount = 0;
        }
      }
      eventEntity.setProperty("attendeeCount", attendeeCount);
      datastore.put(eventEntity);
      saved.add(eventId);
    } catch (EntityNotFoundException e) {
      LOGGER.info("event " + eventId + " does not exist");
    }
  }

  // removes event id from list if it is present
  private void postHandleUnsave(List<Long> saved, long eventId) {
    for (int i = 0; i < saved.size(); i++) {
      if (saved.get(i) == eventId) {
        saved.remove(i);

        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Key eventKey = KeyFactory.createKey("Event", eventId);
        try {
          Entity eventEntity = datastore.get(eventKey);
          Object attendees = eventEntity.getProperty("attendeeCount");
          int attendeeCount = -1;
          if (attendees != null) {
            try {
              attendeeCount += Integer.parseInt(attendees.toString());
            } catch (NumberFormatException num) {
              LOGGER.info("error parsing attendee count for event id " + eventId);
              attendeeCount = 0;
            }
          }
          if (attendeeCount < 0) {
            attendeeCount = 0;
          }
          eventEntity.setProperty("attendeeCount", attendeeCount);
          datastore.put(eventEntity);
        } catch (EntityNotFoundException e) {
          LOGGER.info("event " + eventId + " does not exist");
        }

        return;
      }
    }
    LOGGER.info("event " + eventId + " has not been saved yet");
  }
}
