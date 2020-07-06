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
import com.google.maps.model.DistanceMatrix;
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

    // get location
    String location = Utils.getParameter(request, "location", "");
    getDistance(new LatLng(-31.9522, 115.8589), new LatLng(-25.344677, 131.036692));
    // filter by location and cutoff outside it

    // get tags
    // drop all without first tag?
    // Sort list by most tags in common with search
    Collections.sort(
        events,
        new Comparator<Entity>() {
          public int compare(Entity o1, Entity o2) {
            int condition =
                intersection((List<String>) o2.getProperty("tags"), searchTags)
                    .compareTo(intersection((List<String>) o1.getProperty("tags"), searchTags));
            // For development purposes, if two events have the same number of tags
            // they are sorted by the event names (which in the test cases are integers)
            if (condition == 0) {
              return Integer.compare(
                  Integer.parseInt(o1.getProperty("eventName").toString()),
                  Integer.parseInt(o2.getProperty("eventName").toString()));
            } else {
              return condition;
            }
          }
        });
    // those closest to the user go to the top

    // Convert events list to json
    String json = Utils.convertToJson(events);

    response.setContentType("application/json;");
    response.getWriter().println(json);
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {}

  /**
   * Returns a count of the number of tags two lists have in common.
   *
   * @param tagListA List of tags to be compared
   * @param tagListB List of tags to be compared
   * @return Integer count of number of tags in common
   */
  public Integer intersection(List<String> tagListA, List<String> tagListB) {
    List<String> tagListC = new ArrayList<String>(tagListA);
    tagListC.retainAll(tagListB);
    return tagListC.size();
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

  public static int getDistance(LatLng from, LatLng to) {
    DistanceMatrix result = null;
    try {
      result =
          DistanceMatrixApi.newRequest(context)
              .origins(new LatLng(-31.9522, 115.8589))
              .destinations(new LatLng(-25.344677, 131.036692))
              .await();
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }

    int distance = (int) (result.rows[0].elements[0].distance.inMeters / 1000);
    return distance;
  }
}
