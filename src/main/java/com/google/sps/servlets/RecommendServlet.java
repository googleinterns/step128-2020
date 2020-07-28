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
import java.util.HashMap;
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
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    final Gson gson = new Gson();
    response.setContentType("application/json");

    String userToken = request.getParameter("userToken");
    if (userToken == null || !Firebase.isUserLoggedIn(userToken)) {
      response.getWriter().println(gson.toJson(new ArrayList<Entity>()));
      return;
    }

    List<Entity> events = new ArrayList<>();
    String userID = Firebase.authenticateUser(userToken);
    Key recKey = KeyFactory.createKey("Recommendation", userID);
    Entity recommendations = null;
    try {
      // find previously computed recommendations
      recommendations = datastore.get(recKey);
      List<Long> recIds = (List<Long>) recommendations.getProperty("recs");
      int limit = EVENTS_LIMIT;
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
    } catch (EntityNotFoundException | NullPointerException exception) {
      // no prev recommendations, compute simple recommendations based on tags and distance only
      LOGGER.info("No recommendations found for " + userID + ". Computing now.");
      recommendations = new Entity("Recommendation", userID);
      Key userKey = KeyFactory.createKey("User", userID);
      Entity userEntity = null;
      try {
        userEntity = datastore.get(userKey);
        Map<String, Float> userParams = Interactions.buildVectorForUser(userEntity);
        if (userParams.size() > 0) {
          // compute recommendation if user has data
          String userLocation = null;
          if (userEntity.hasProperty("location")) {
            userLocation = userEntity.getProperty("location").toString();
          }
          Map<Double, Long> bestEvents = new TreeMap<>(Recommend.SCORE_DESCENDING);
          Map<Long, Entity> eventsWithId = new HashMap<>();
          for (Entity event : datastore.prepare(new Query("Event")).asIterable()) {
            long eventId = event.getKey().getId();
            eventsWithId.put(eventId, event);
            Map<String, Integer> eventParams = Interactions.buildVectorForEvent(event);
            String eventLocation = event.getProperty("address").toString();
            double eventScore =
                computeScore(userID, userParams, userLocation, eventId, eventParams, eventLocation);
            Recommend.addToRanking(eventId, eventScore, bestEvents);
          }
          List<Long> eventIds = new ArrayList<>();
          Iterator<Double> itr = bestEvents.keySet().iterator();
          int count = 0;
          while (itr.hasNext() && count < EVENTS_LIMIT) {
            count++;
            long eventId = bestEvents.get(itr.next());
            events.add(eventsWithId.get(eventId));
            eventIds.add(eventId);
          }
          Entity recEntity = new Entity("Recommendation", userID);
          recEntity.setProperty("recs", eventIds);
          datastore.put(recEntity);
        }
      } catch (EntityNotFoundException userNotFound) {
        // user does not exist (no data)
        userEntity = Utils.makeUserEntity(userID, true);
      }
    }
    response.getWriter().println(gson.toJson(events));
  }

  /** Computes the correlation between a user and an event. */
  private static double computeScore(
      String userId,
      Map<String, Float> userParams,
      String userLocation,
      long eventId,
      Map<String, Integer> eventParams,
      String eventLocation) {
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
}
