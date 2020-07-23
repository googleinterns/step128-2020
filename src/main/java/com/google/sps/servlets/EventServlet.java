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
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.sps.Firebase;
import com.google.sps.Interactions;
import com.google.sps.Utils;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/event")
public class EventServlet extends HttpServlet {

  private static final Logger LOGGER = Logger.getLogger(EventServlet.class.getName());

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // TODO: Implement doGet function.
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String userToken = request.getParameter("userToken");
    if (!Firebase.isUserLoggedIn(userToken)) {
      throw new IOException("Cannot create an event while not logged in");
    }
    String userID = Firebase.authenticateUser(userToken);
    Entity eventEntity = populateEvent(request);
    eventEntity.setProperty("creator", userID);

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    datastore.put(eventEntity);

    long keyId = allocateEventKey(eventEntity);

    Key userKey = KeyFactory.createKey("User", userID);
    List<String> tags = (List<String>) eventEntity.getProperty("tags");

    // keep track of user interests and interactions
    Entity userEntity = null;
    try {
      userEntity = datastore.get(userKey);
    } catch (EntityNotFoundException exception) {
      userEntity = Utils.makeUserEntity(userID, false);
      LOGGER.info("No entity found for " + userID + ", creating one now.");
    }
    int delta = Interactions.recordInteraction(userID, keyId, Interactions.CREATE_SCORE, false);
    Interactions.updatePrefs(userEntity, tags, delta);
    datastore.put(userEntity);

    // Redirect back to the my-events HTML page.
    response.sendRedirect("/my-events.html");
  }

  private long allocateEventKey(Entity event) {
    Key eventKey = event.getKey();
    String keyString = KeyFactory.keyToString(eventKey);

    Query query = new Query("Event", eventKey);

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Entity eventCreated = datastore.prepare(query).asSingleEntity();

    eventCreated.setProperty("eventKey", keyString);
    datastore.put(eventCreated);
    return eventKey.getId();
  }

  /** @return the Event entity */
  private Entity populateEvent(HttpServletRequest request) {
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

    final String fullAddress = String.format("%1$s, %2$s, %3$s", streetAddress, city, state);
    final String formattedDate = formatDate(date);
    final String formattedTime = formatTime(startTime);

    if (endTime != "") {
      endTime = formatTime(endTime);
    }

    Entity eventEntity = new Entity("Event");
    eventEntity.setProperty("eventName", eventName);
    eventEntity.setProperty("eventDescription", eventDescription);
    eventEntity.setProperty("address", fullAddress);
    eventEntity.setProperty("date", formattedDate);
    eventEntity.setProperty("startTime", formattedTime);
    eventEntity.setProperty("endTime", endTime);
    eventEntity.setProperty("coverPhoto", coverPhoto);
    eventEntity.setProperty("attendeeCount", 0);

    Gson gson = new Gson();
    List<String> tagsList = gson.fromJson(tagsStr, new TypeToken<ArrayList<String>>() {}.getType());
    eventEntity.setIndexedProperty("tags", tagsList);

    Map<String, Integer> keywords = KeywordSearchServlet.getKeywords(eventName, eventDescription);
    eventEntity.setProperty("keywords", KeywordSearchServlet.getKeywordMapKeys(keywords));
    eventEntity.setProperty("keywordsValues", KeywordSearchServlet.getKeywordMapValues(keywords));

    return eventEntity;
  }

  /**
   * @return the request parameter, or the default value if the parameter was not specified by the
   *     client
   */
  private String getParameter(HttpServletRequest request, String name, String defaultValue) {
    String value = request.getParameter(name);
    if (value == null) {
      return defaultValue;
    }
    return value;
  }

  /** Format time to standard format. */
  private String formatTime(String time) {
    DateFormat inFormat = new SimpleDateFormat("HH:mm");
    DateFormat outFormat = new SimpleDateFormat("h:mm a");

    Date unformattedTime = null;
    String formattedTime = "";
    try {
      unformattedTime = inFormat.parse(time);
    } catch (ParseException e) {
      LOGGER.info("Could not parse time " + e);
    }

    if (unformattedTime != null) {
      formattedTime = outFormat.format(unformattedTime);
    }

    return formattedTime;
  }

  /** Format date to fit Month Day, Year format. */
  private String formatDate(String date) {
    DateFormat inFormat = new SimpleDateFormat("yyyy-MM-dd");
    DateFormat outFormat = new SimpleDateFormat("EEEE, MMMMM dd, yyyy");

    Date unformattedDate = null;
    String formattedDate = "";
    try {
      unformattedDate = inFormat.parse(date);
      System.out.println(unformattedDate);
    } catch (ParseException e) {
      LOGGER.info("Could not parse date " + e);
    }

    if (unformattedDate != null) {
      formattedDate = outFormat.format(unformattedDate);
      System.out.println(formattedDate);
    }

    return formattedDate;
  }
}
