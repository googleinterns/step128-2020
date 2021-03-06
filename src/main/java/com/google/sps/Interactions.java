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

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.PreparedQuery.TooManyResultsException;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class Interactions {

  private static final Logger LOGGER = Logger.getLogger(Interactions.class.getName());

  // contributions to user's interest metrics for each action
  public static final float VIEW_SCORE = 4;
  public static final float SAVE_SCORE = 8;
  public static final float CREATE_SCORE = 10;

  // effects on scores when "undoing" actions
  public static final float UNSAVE_DELTA = -2;
  public static final float DELETE_DELTA = -3;

  // interest categories, matches question order on survey pages
  public static final String[] metrics = {
    "environment",
    "blm",
    "education",
    "LGBTQ+",
    "healthcare",
    "civics",
    "volunteer",
    "fundraiser",
    "activism",
    "item donation"
  };

  /**
   * returns a map of a user's interest levels with respect to each tag. returns null if user not
   * found.
   */
  public static Map<String, Float> buildVectorForUser(Entity userEntity) {
    if (!userEntity.getKind().equals("User")) {
      throw new IllegalArgumentException("entity must be of type User");
    }
    Map<String, Float> result = new HashMap<>();
    for (String param : metrics) {
      if (userEntity.hasProperty(param)) {
        float score = Float.parseFloat(userEntity.getProperty(param).toString());
        result.put(param, score);
      }
    }
    return result;
  }

  /** Simplistic method that builds a vector from an entity (subject to change). */
  public static Map<String, Integer> buildVectorForEvent(Entity eventEntity) {
    if (!eventEntity.getKind().equals("Event")) {
      throw new IllegalArgumentException("entity must be of type Event");
    }
    String tags = eventEntity.getProperty("tags").toString();
    if (tags == null) {
      return new HashMap<>();
    }
    Map<String, Integer> eventMetrics = new HashMap<>();
    for (String field : metrics) {
      if (tags.contains(field)) {
        eventMetrics.put(field, 1);
      }
    }
    return eventMetrics;
  }

  /** utility method that computes the dot product between two vectors. */
  public static double dotProduct(Map<String, Float> v1, Map<String, Integer> v2) {
    double result = 0;
    double v1Magnitude = 0f;
    for (String field : v1.keySet()) {
      float v1Val = v1.get(field);
      if (v2.containsKey(field)) {
        result += v2.get(field) * v1Val;
      }
      v1Magnitude += v1Val * v1Val;
    }
    double v2Magnitude = 0;
    for (String field : v2.keySet()) {
      int v2Val = v2.get(field);
      v2Magnitude += v2Val * v2Val;
    }
    v1Magnitude = Math.pow(v1Magnitude, .5);
    v2Magnitude = Math.pow(v2Magnitude, .5);
    if (v1Magnitude == 0.0 || v2Magnitude == 0.0) {
      return 0.0;
    }
    return result / (v1Magnitude * v2Magnitude);
  }

  /**
   * Check if there exists an interaction entry between a given user and event.
   *
   * @param userId the user's id as identified in datastore
   * @param eventId the event's id as identified in datastore
   * @return the interaction entity, or null if none exist.
   */
  public static Entity hasInteraction(String userId, long eventId) {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Query q =
        new Query("Interaction")
            .setFilter(
                CompositeFilterOperator.and(
                    new FilterPredicate("user", FilterOperator.EQUAL, userId),
                    new FilterPredicate("event", FilterOperator.EQUAL, eventId)));
    PreparedQuery pq = datastore.prepare(q);
    Entity interaction = null;
    try {
      interaction = pq.asSingleEntity();
    } catch (TooManyResultsException e) {
      // clear all to "reset" if there are too many
      List<Key> toDelete = new ArrayList<>();
      for (Entity entity : pq.asIterable()) {
        toDelete.add(entity.getKey());
      }
      LOGGER.warning(
          "multiple interactions found for "
              + userId
              + " and "
              + eventId
              + ". Deleting "
              + toDelete.size()
              + " entries.");
      datastore.delete(toDelete);
    }
    return interaction;
  }

  /**
   * Creates an Interaction entity in datastore recording an interaction between user and event.
   *
   * @param userId the user's id as identified in datastore
   * @param eventId the event's id as identified in datastore
   * @param score the new rating by the user for the event
   * @param forceOverride if false, only overwrites if new score > old score
   * @return change of user's rating on an item (saves highest score only)
   */
  public static float recordInteraction(
      String userId, long eventId, float score, boolean forceOverride) {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Entity interactionEntity = hasInteraction(userId, eventId);
    float delta = score;
    if (interactionEntity == null) {
      interactionEntity = new Entity("Interaction");
      interactionEntity.setProperty("user", userId);
      interactionEntity.setProperty("event", eventId);
      interactionEntity.setProperty("rating", score);
    } else {
      if (interactionEntity.hasProperty("rating")) {
        float prevScore = Float.parseFloat(interactionEntity.getProperty("rating").toString());
        if (forceOverride || prevScore < score) {
          interactionEntity.setProperty("rating", score);
          delta = score - prevScore;
        } else {
          delta = 0;
        }
      } else {
        interactionEntity.setProperty("rating", score);
      }
    }
    interactionEntity.setProperty("timestamp", System.currentTimeMillis());
    datastore.put(interactionEntity);
    return delta;
  }

  /** Updates user preference map. */
  public static void updatePrefs(Entity userEntity, List<String> tags, float score) {
    if (!userEntity.getKind().equals("User")) {
      throw new IllegalArgumentException("must be user item");
    }
    if (score == 0) {
      return;
    }
    for (String s : tags) {
      if (userEntity.hasProperty(s)) {
        userEntity.setProperty(s, score + Float.parseFloat(userEntity.getProperty(s).toString()));
      } else {
        userEntity.setProperty(s, score);
      }
    }
  }

  /**
   * Creates and returns a new Entity for a given userId.
   *
   * @param userId to identify this user in datastore
   * @param location user's location
   * @param addToDatastore if true, will add this entity to datastore as well
   */
  public static Entity makeUserEntity(String userId, String location, boolean addToDatastore) {
    Entity userEntity = new Entity("User", userId);
    userEntity.setProperty("firebaseID", userId);
    userEntity.setProperty("location", location);
    if (addToDatastore) {
      DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
      datastore.put(userEntity);
    }
    return userEntity;
  }

  /**
   * Creates and returns a new Entity for a given userId, no location given.
   *
   * @param userId to identify this user in datastore
   * @param addToDatastore if true, will add this entity to datastore as well
   */
  public static Entity makeUserEntity(String userId, boolean addToDatastore) {
    return makeUserEntity(userId, "", addToDatastore);
  }
}
