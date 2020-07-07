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
import com.google.maps.DistanceMatrixApi;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.model.DistanceMatrix;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/search")
public class SearchServlet extends HttpServlet {

  private static final Logger LOGGER = Logger.getLogger(SearchServlet.class.getName());
  // Number of keywords to produce
  private static final int NUM_KEYWORDS = 5;
  // Minimum number of instances to be considered a keyword
  private static final int MIN_INSTANCES = 2;
  // List of words to ignore
  // This list is incomplete
  private static final List<String> IRRELEVANT_WORDS =
      new ArrayList<String>(Arrays.asList("the", "is", "for", "in", "of", "so", "to"));
  private static final String MAPS_API_KEY = "";
  private static final GeoApiContext context =
      new GeoApiContext.Builder().apiKey(MAPS_API_KEY).build();

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // List of all the tags we are searching for
    List<String> searchTags =
        new ArrayList<String>(Arrays.asList(request.getParameterValues("tags")));
    // Filter to check if the event has any of tags we're searching for
    Filter tagsFilter = new FilterPredicate("tags", FilterOperator.IN, searchTags);
    Query query = new Query("Event").setFilter(tagsFilter);

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);
    List<Entity> events =
        new ArrayList<Entity>(results.asList(FetchOptions.Builder.withDefaults()));

    // Get location of user
    String location = request.getParameter("location");
    LatLng userLocation = getLatLng(location);

    // Get distance between user and the location of all the events
    for (Entity event : events) {
      LatLng eventLocation =
          getLatLng(
              event.getProperty("streetAddress").toString()
                  + " "
                  + event.getProperty("city").toString()
                  + " "
                  + event.getProperty("state").toString());
      int distance = getDistance(userLocation, eventLocation);
      event.setProperty("distance", distance);
    }

    // Get location cutoff converted from mi to km
    int cutoff =
        Math.toIntExact(
            Math.round(
                Integer.parseInt(Utils.getParameter(request, "searchDistance", "")) * 1.609));
    // Remove from the list any events outside the cutoff or that aren't drivable
    events.removeIf(
        e -> ((int) e.getProperty("distance")) > cutoff || ((int) e.getProperty("distance")) < 0);

    // Sort list by most tags in common with search
    Collections.sort(
        events,
        new Comparator<Entity>() {
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
            // Sort by which event has more tags
            int compareSize = Integer.compare(o2List.size(), o1List.size());
            if (compareSize != 0) {
              return compareSize;
            } else {
              // Sort by which event is closer to the user
              return Integer.compare(
                  Integer.parseInt(o1.getProperty("distance").toString()),
                  Integer.parseInt(o2.getProperty("distance").toString()));
            }
          }
        });

    // Convert events list to json
    String json = Utils.convertToJson(events);

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
   * @return Long ratio of number of tags in common to total number of tags
   */
  public Double intersection(List<String> tagListA, List<String> tagListB) {
    // Catches divide by zero
    if (tagListA.size() == 0) {
      return 0.0;
    }
    List<String> tagListC = new ArrayList<String>(tagListA);
    tagListC.retainAll(tagListB);
    return ((double) tagListC.size()) / tagListA.size();
  }

  /**
   * Returns keywords from an event (currently using just the title and description) based off their
   * frequency and appearance in the title vs in the description.
   *
   * @param title String representing the title text to be processed
   * @param desc String representing the description text to be processed
   * @return List containing most important words from the string
   */
  public static List<String> getKeywords(String title, String desc) {
    // TODO: convert to lowercase in processing (figure out acronyms)
    Map<String, Integer> titleMap = SearchServlet.wordCount(title);
    Map<String, Integer> descMap = SearchServlet.wordCount(desc);

    // Merge maps
    // Title occurrence values multiplied by 2 to give more weight than description
    titleMap.forEach((key, value) -> descMap.merge(key, value * 2, (v1, v2) -> v1 + (v2 * 2)));
    List<Map.Entry<String, Integer>> mergeList =
        new ArrayList<Map.Entry<String, Integer>>(descMap.entrySet());

    Collections.sort(
        mergeList,
        new Comparator<Map.Entry<String, Integer>>() {
          public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
            return (o2.getValue()).compareTo(o1.getValue());
          }
        });

    // Add top results to the final list
    List<String> finalList = new ArrayList<String>();
    int count = 0;
    while (finalList.size() < NUM_KEYWORDS) {
      Map.Entry e = mergeList.get(count);
      // Exclude words with less appearances than the cutoff
      if (((int) e.getValue()) < MIN_INSTANCES) break;
      else if (count >= mergeList.size()) break;
      // Exclude common useless words (in, a, the, etc)
      else if (!IRRELEVANT_WORDS.contains(e.getKey().toString())) {
        finalList.add(e.getKey().toString());
      }
      count++;
    }

    return finalList;
  }

  /**
   * Returns a list of words contained in a string.
   *
   * @param str String to be processed
   * @return List containing every word in a string
   */
  public static List<String> getSeparateWords(String str) {
    // TODO: handle single quoted strings (right now keeps them in for
    // the common case of contractions

    // Split the string based on a regex: splits at every space and at
    // most common non-alphabet characters
    String[] list = str.split("([\\s.,!?:;()\\[\\]&\"\\s+])");
    // Trim all whitespace
    for (int i = 0; i < list.length; i++) {
      list[i] = list[i].replaceAll("\\s+", "");
    }
    List<String> listArr = new ArrayList<String>(Arrays.asList(list));
    // Remove all empty strings
    listArr.removeAll(Arrays.asList("", null));
    return listArr;
  }

  /**
   * Returns a map of the occurrences of words in a string.
   *
   * @param input String to be processed
   * @return Map containing word keys and instance count values
   */
  public static Map<String, Integer> wordCount(String input) {
    List<String> words = SearchServlet.getSeparateWords(input);
    // process title words
    Map<String, Integer> map = new HashMap<String, Integer>();
    for (String w : words) {
      w = w.toLowerCase();
      if (map.containsKey(w)) {
        map.put(w, map.get(w) + 1);
      } else {
        map.put(w, 1);
      }
    }
    return map;
  }

  /**
   * Gets the latitude and longitude of a location using the Google Maps API.
   *
   * @param location String containing the address of the location
   * @return the latitude and longitude of the location
   */
  public static LatLng getLatLng(String location) {
    GeocodingResult[] results = null;
    try {
      results = GeocodingApi.newRequest(context).address(location).await();
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }

    return new LatLng(results[0].geometry.location.lat, results[0].geometry.location.lng);
  }

  /**
   * Calculates the distance in km between two locations using driving routes generated by the
   * Google Maps API.
   *
   * @param from Latitude and longitude of the beginning location
   * @param to Latitude and longitude of the ending location
   * @return the distance in km between the two locations
   */
  public static int getDistance(LatLng from, LatLng to) {
    DistanceMatrix result = null;
    try {
      result = DistanceMatrixApi.newRequest(context).origins(from).destinations(to).await();
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }

    if (result.rows[0].elements[0].status.toString().equals("ZERO_RESULTS")) return -1;
    int distance = (int) (result.rows[0].elements[0].distance.inMeters / 1000);
    return distance;
  }
}
