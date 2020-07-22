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
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.maps.model.LatLng;
import com.google.sps.Utils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/search")
public class SearchServlet extends HttpServlet {

  private static final Logger LOGGER = Logger.getLogger(SearchServlet.class.getName());
  // Conversion rate from mi to km
  private static final double MI_TO_KM = 1.609;
  // Tags retrieved from the frontend in the doGet method
  private static List<String> searchTags;
  /** Comparator used to compare the relevance of two events to the user and their search tags. */
  private static final Comparator<Entity> TAGS_SEARCH_RELEVANCE =
      new Comparator<Entity>() {
        @Override
        public int compare(Entity o1, Entity o2) {
          List<String> o1List = (List<String>) o1.getProperty("tags");
          List<String> o2List = (List<String>) o2.getProperty("tags");
          // Sort by which event has more tags in common with the search tags
          int compareTagsInCommon =
              Double.compare(
                  intersection(o2List, searchTags) * o2List.size(),
                  intersection(o1List, searchTags) * o1List.size());
          if (compareTagsInCommon != 0) {
            return compareTagsInCommon;
          }
          // Sort by which event has a higher ratio of: tags in common with
          // the search tags to total number of tags
          int compareRatioOfTagsInCommon =
              Double.compare(intersection(o2List, searchTags), intersection(o1List, searchTags));
          if (compareRatioOfTagsInCommon != 0) {
            return compareRatioOfTagsInCommon;
          }
          // Sort by which event is closer to the user
          return Integer.compare(
              Integer.parseInt(o1.getProperty("distance").toString()),
              Integer.parseInt(o2.getProperty("distance").toString()));
        }
      };

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // List of all the tags we are searching for
    searchTags = new ArrayList<String>(Arrays.asList(request.getParameter("tags").split(",")));

    Query query = null;
    // Check if there are no tags
    if (!searchTags.get(0).equals("")) {
      // Filter to check if the event has any of tags we're searching for
      Filter tagsFilter = new FilterPredicate("tags", FilterOperator.IN, searchTags);
      query = new Query("Event").setFilter(tagsFilter);
    } else {
      query = new Query("Event");
    }

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);
    List<Entity> events =
        new ArrayList<Entity>(results.asList(FetchOptions.Builder.withDefaults()));

    // Get location of user
    String location = request.getParameter("location");
    LatLng userLocation = Utils.getLatLng(location);

    // Get distance between user and the location of all the events
    for (Entity event : events) {
      LatLng eventLocation = Utils.getLatLng(event.getProperty("address").toString());

      int distance = Utils.getDistance(userLocation, eventLocation);
      event.setProperty("distance", distance);
    }

    // Get location cutoff converted from mi to km
    int cutoff =
        Math.toIntExact(
            Math.round(
                Integer.parseInt(Utils.getParameter(request, "searchDistance", "")) * MI_TO_KM));
    // Remove from the list any events outside the cutoff or that aren't drivable
    events.removeIf(
        e -> ((int) e.getProperty("distance")) > cutoff || ((int) e.getProperty("distance")) < 0);

    // Sort list by most tags in common with search
    Collections.sort(events, TAGS_SEARCH_RELEVANCE);

    // Convert events list to json
    String json = Utils.convertToJson(events);
    System.out.println("Actual:");
    for (Entity e : events) {
      System.out.println(e.getProperty("eventName"));
    }

    response.setContentType("application/json;");
    response.getWriter().println(json);
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {}

  /**
   * Returns a ratio of the number of tags a list has in common with another.
   *
   * @param tagListA List of tags to be compared
   * @param tagListB List of tags to be compared against
   * @return Double ratio of number of tags in common to total number of tags
   */
  public static Double intersection(List<String> tagListA, List<String> tagListB) {
    // Catches divide by zero
    if (tagListA == null || tagListB == null) {
      return 0.0;
    }
    if (tagListA.size() == 0) {
      return 0.0;
    }
    List<String> tagListC = new ArrayList<String>(tagListA);
    tagListC.retainAll(tagListB);
    return ((double) tagListC.size()) / tagListA.size();
  }
}
