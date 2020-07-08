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
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class Interactions {

  private static final Logger LOGGER = Logger.getLogger(Interactions.class.getName());

  // contributions to user's interest metrics for each action
  public static final int VIEW_SCORE = 1;
  public static final int SAVE_SCORE = 3;
  public static final int CREATE_SCORE = 5;

  // interest categories, matches question order on survey pages
  public static final String[] metrics = {"environment", "blm", "volunteer", "education", "LGBTQ+"};

  /**
   * returns a map of a user's interest levels with respect to each tag. returns null if user not
   * found.
   */
  public static Map<String, Integer> getInterestMetrics(String userEmail) {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Key userKey = KeyFactory.createKey("User", userEmail);
    Map<String, Integer> result = new HashMap<>();
    Entity userEntity;
    try {
      userEntity = datastore.get(userKey);
      for (String param : metrics) {
        if (userEntity.hasProperty(param)) {
          int score = Integer.parseInt(userEntity.getProperty(param).toString());
          result.put(param, score);
        } else {
          // default val is 0
          result.put(param, 0);
        }
      }
    } catch (EntityNotFoundException e) {
      LOGGER.warning("ERROR: email not found " + userEmail);
      return null;
    }
    return result;
  }

  /** simplistic method that builds a vector from an entity (subject to change) */
  public static Map<String, Integer> buildVectorForEvent(Entity eventEntity) {
    if (!eventEntity.getKind().equals("Event")) {
      throw new IllegalArgumentException("must be event items");
    }
    String tags = eventEntity.getProperty("tags").toString();
    if (tags == null) {
      return new HashMap<>();
    }
    Map<String, Integer> eventMetrics = new HashMap<>();
    for (String field : metrics) {
      if (tags.contains(field)) {
        eventMetrics.put(field, 1);
      } else {
        eventMetrics.put(field, 0);
      }
    }
    return eventMetrics;
  }

  /** utility method that computes the dot product between two vectors */
  public static int dotProduct(Map<String, Integer> v1, Map<String, Integer> v2) {
    int result = 0;
    for (String field : v1.keySet()) {
      if (v2.containsKey(field)) {
        result += v2.get(field) * v1.get(field);
      }
    }
    return result;
  }
}
