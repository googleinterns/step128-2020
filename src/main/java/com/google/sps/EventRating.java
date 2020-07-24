// Copyright 2020 Google LLC
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

import com.google.appengine.api.datastore.Entity;
import java.io.Serializable;

/** Utility class to help with Spark's dataframe parsing */
public class EventRating implements Serializable {
  private int userId;
  private int eventId;
  private float rating;
  private long timestamp;

  /** Creates an EventRating object. */
  private EventRating(String userId, long eventId, float rating, long timestamp) {
    // all fields must be numeric
    this.userId = userId.hashCode();
    // spark implicitly casts long to int otherwise
    this.eventId = (Long.toString(eventId)).hashCode();
    this.rating = rating;
    this.timestamp = timestamp;
  }

  /** Returns hashcode value of original user ID. */
  public int getUserId() {
    return userId;
  }

  /** Returns hashcode of String representation of original event ID. */
  public int getEventId() {
    return eventId;
  }

  /** Returns rating value. */
  public float getRating() {
    return rating;
  }

  /** Returns timestamp in millis of when interaction was recorded. */
  public long getTimestamp() {
    return timestamp;
  }

  /** Creates an EventRating object from an Interaction Entity. */
  public static EventRating parseEntity(Entity entity) {
    if (!entity.getKind().equals("Interaction")) {
      return null;
    }
    Object userField = entity.getProperty("user");
    Object eventField = entity.getProperty("event");
    Object ratingField = entity.getProperty("rating");
    Object timeField = entity.getProperty("timestamp");
    if (userField == null || eventField == null || ratingField == null || timeField == null) {
      return null;
    }
    try {
      String userId = userField.toString();
      long eventId = Long.parseLong(eventField.toString());
      float rating = Float.parseFloat(ratingField.toString());
      long timestamp = Long.parseLong(timeField.toString());
      return new EventRating(userId, eventId, rating, timestamp);
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
