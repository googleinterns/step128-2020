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
import com.google.sps.Firebase;
import com.google.sps.Interactions;
import com.google.sps.Recommend;
import com.google.sps.Utils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/recommend")
public class RecommendServlet extends HttpServlet {

  private static final Logger LOGGER = Logger.getLogger(RecommendServlet.class.getName());
  private static final int EVENTS_LIMIT = 10;

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("application/json");
    Gson gson = new Gson();

    String userToken = request.getParameter("userToken");
    if (userToken == null || !Firebase.isUserLoggedIn(userToken)) {
      HomePageObject result = new HomePageObject(new ArrayList<Entity>(), "false");
      response.getWriter().println(gson.toJson(result));
      return;
    }

    HomePageObject events = null;
    String userID = Firebase.authenticateUser(userToken);
    Key recKey = KeyFactory.createKey("Recommendation", userID);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    try {
      // find previously computed recommendations
      Entity recommendations = datastore.get(recKey);
      List<Long> recIds = (List<Long>) recommendations.getProperty("recs");
      events = getFromStoredRecs(recIds, userID);
    } catch (EntityNotFoundException | NullPointerException exception) {
      // no prev recommendations, compute simple recommendations based on tags and distance only
      LOGGER.info("No recommendations found for " + userID + ". Computing now.");
      events = computeRecommendations(userID);
    }
    response.getWriter().println(gson.toJson(events));
  }

  private HomePageObject getFromStoredRecs(List<Long> recIds, String userId) {
    final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    int limit = EVENTS_LIMIT;
    List<Entity> events = new ArrayList<>();
    for (int i = 0; i < recIds.size() && i < limit; i++) {
      long eventId = recIds.get(i);
      Key eventKey = KeyFactory.createKey("Event", eventId);
      try {
        events.add(datastore.get(eventKey));
      } catch (EntityNotFoundException eventNotFound) {
        LOGGER.info("event " + eventId + " not found.");
        limit++;
      }
    }
    HomePageObject obj = new HomePageObject(events, "true");
    try {
      Entity userEntity = datastore.get(KeyFactory.createKey("User", userId));
      obj.surveyStatus = userEntity.getProperty("surveyCompleted").toString();
    } catch (EntityNotFoundException | NullPointerException exception) {
      Interactions.makeUserEntity(userId, true);
      obj.surveyStatus = "false";
    }
    return obj;
  }

  /**
   * Queries events from datastore and evaluates their match against a given user. Returns a list of
   * highest-scoring event entities after storing their ids to datastore as a Recommendation object
   * for the user. If there is no user data, returns an empty list.
   */
  private HomePageObject computeRecommendations(String userId) {
    final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Entity userEntity = null;
    try {
      userEntity = datastore.get(KeyFactory.createKey("User", userId));
    } catch (EntityNotFoundException exception) {
      // user does not exist (no data)
      userEntity = Interactions.makeUserEntity(userId, true);
      return new HomePageObject(new ArrayList<Entity>(), "false");
    }
    Map<String, Float> userParams = Interactions.buildVectorForUser(userEntity);
    if (userParams.size() == 0) {
      // no data for this user yet
      return new HomePageObject(new ArrayList<Entity>(), "false");
    }
    String userLocation = null;
    if (userEntity.hasProperty("location")) {
      userLocation = userEntity.getProperty("location").toString();
    }
    List<Entity> events = new ArrayList<>();
    Map<Double, Entity> bestEvents = new TreeMap<>(Recommend.SCORE_DESCENDING);
    // score and rank events
    for (Entity event : datastore.prepare(new Query("Event")).asIterable()) {
      long eventId = event.getKey().getId();
      Map<String, Integer> eventParams = Interactions.buildVectorForEvent(event);
      String eventLocation = event.getProperty("address").toString();
      double eventScore =
          computeScore(userId, userParams, userLocation, eventId, eventParams, eventLocation);

      // add item to ranking
      while (bestEvents.containsKey(eventScore)) {
        Entity otherWithScore = bestEvents.get(eventScore);
        bestEvents.put(eventScore, event);
        event = otherWithScore;
        eventScore -= 0.001;
      }
      bestEvents.put(eventScore, event);
    }
    // get info from ranking map and return for user
    List<Long> eventIds = new ArrayList<>();
    Iterator<Double> itr = bestEvents.keySet().iterator();
    int count = 0;
    while (itr.hasNext() && count < EVENTS_LIMIT) {
      count++;
      Entity entity = bestEvents.get(itr.next());
      events.add(entity);
      eventIds.add(entity.getKey().getId());
    }
    // save Recommendation entity for this user in datastore
    Entity recommendations = new Entity("Recommendation", userId);
    recommendations.setProperty("recs", eventIds);
    datastore.put(recommendations);

    return new HomePageObject(events, userEntity.getProperty("surveyCompleted"));
  }

  /**
   * Computes the correlation between a user and an event. A dot product is first computed as a
   * measure of similarity between the user's interests and the event, and then additional
   * multiplers are added based on the event's proximity to the user and the user's interaction
   * history with the event.
   *
   * @param userId Identifier for the user as used in datastore.
   * @param userParams User interest for various metrics according to interaction history.
   * @param userLocation Recorded location for the user
   * @param eventId Identifier for the event as used in datastore.
   * @param eventParams Map containing event tag information.
   * @param eventLocation Address of the event.
   */
  private static double computeScore(
      String userId,
      Map<String, Float> userParams,
      String userLocation,
      long eventId,
      Map<String, Integer> eventParams,
      String eventLocation) {

    // evaluate correlation between event tags and user interaction history
    double totalScore = Interactions.dotProduct(userParams, eventParams);
    if (Math.abs(totalScore) < Recommend.ZERO) {
      totalScore = Recommend.ZERO;
    }
    // adjust scaling based on user's past interaction with event
    Entity interactionEntity = Interactions.hasInteraction(userId, eventId);
    if (interactionEntity == null) {
      totalScore *= Recommend.NO_INTERACTION;
    } else {
      float interactionScore = Float.parseFloat(interactionEntity.getProperty("rating").toString());
      if (interactionScore >= Interactions.SAVE_SCORE) {
        totalScore *= Recommend.ALREADY_SAVED;
      }
    }
    // adjust scaling based on event distance to user
    if (userLocation != null && eventLocation != null) {
      int distance = Utils.getDistance(userLocation, eventLocation);
      if (distance < 0) {
        distance = Recommend.INVALID_DISTANCE;
      }
      totalScore /= Math.pow(Recommend.DISTANCE_BASE, distance);
    }

    totalScore = Math.round(totalScore * 1000.0) / 1000.0;
    return totalScore;
  }

  private static class HomePageObject {
    private List<Entity> recommendations;
    private String surveyStatus;

    public HomePageObject(List<Entity> recommendations, Object surveyStatus) {
      this.recommendations = recommendations;
      if (surveyStatus == null) {
        this.surveyStatus = "false";
      } else {
        this.surveyStatus = surveyStatus.toString();
      }
    }
  }
}
