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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/ksearch")
public class KeywordSearchServlet extends HttpServlet {

  private static final Logger LOGGER = Logger.getLogger(KeywordSearchServlet.class.getName());
  // Number of keywords to produce
  private static final int NUM_KEYWORDS = 5;
  // Minimum number of instances to be considered a keyword
  private static final int MIN_INSTANCES = 2;
  // List of words to ignore
  // This list is incomplete
  private static final List<String> IRRELEVANT_WORDS =
      new ArrayList<String>(Arrays.asList("the", "is", "for", "in", "of", "so", "to"));
  // Conversion rate from mi to km
  private static final double MI_TO_KM = 1.609;
  // Keywords retrieved from the frontend in the doGet method
  private List<String> searchKeywords;
  /**
   * Comparator used to compare the relevance of two events to the user and their search keywords
   */
  private final Comparator<Entity> KEYWORD_SEARCH_RELEVANCE =
      new Comparator<Entity>() {
        @Override
        public int compare(Entity o1, Entity o2) {
          List<String> o1List = (List<String>) o1.getProperty("keywords");
          List<String> o2List = (List<String>) o2.getProperty("keywords");
          List<Long> o1Values = (List<Long>) o1.getProperty("keywordsValues");
          List<Long> o2Values = (List<Long>) o2.getProperty("keywordsValues");
          double intersectionRatioO1 = SearchServlet.intersection(o1List, searchKeywords);
          double intersectionRatioO2 = SearchServlet.intersection(o2List, searchKeywords);
          // Sort by which event has more keywords in common with the search keywords
          int compareKeywordsInCommon =
              Double.compare(
                  intersectionRatioO2 * o2List.size(), intersectionRatioO1 * o1List.size());
          if (compareKeywordsInCommon != 0) {
            return compareKeywordsInCommon;
          }
          // Sort by which has a higher occurrence score
          int compareOccurrence =
              Double.compare(
                  occurrenceScore(o2List, searchKeywords, o2Values),
                  occurrenceScore(o1List, searchKeywords, o1Values));
          if (compareOccurrence != 0) {
            return compareOccurrence;
          }

          // Sort by which event has less keywords
          int compareSize = Integer.compare(o1List.size(), o2List.size());
          if (compareSize != 0) {
            return compareSize;
          } else {
            // Sort by which event is closer to the user
            return Integer.compare(
                Integer.parseInt(o1.getProperty("distance").toString()),
                Integer.parseInt(o2.getProperty("distance").toString()));
          }
        }
      };

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // String search query input
    String searchQuery = request.getParameter("searchQuery");

    // List of keywords we're using to search
    searchKeywords = new ArrayList<String>(getSeparateWords(searchQuery));
    for (int i = 0; i < searchKeywords.size(); i++) {
      searchKeywords.set(i, searchKeywords.get(i).toLowerCase());
    }
    searchKeywords.removeAll(IRRELEVANT_WORDS);

    Query query = null;
    // Check if there are no keywords
    if (!searchQuery.equals("")) {
      // Filter to check if the event has any of the keywords we're searching for
      Filter keywordsFilter = new FilterPredicate("keywords", FilterOperator.IN, searchKeywords);
      query = new Query("Event").setFilter(keywordsFilter);
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

    // Sort list by most keywords in common with search
    Collections.sort(events, KEYWORD_SEARCH_RELEVANCE);

    // Convert events list to json
    String json = Utils.convertToJson(events);

    response.setContentType("application/json;");
    response.getWriter().println(json);
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {}

  /**
   * Returns keywords from an event (currently using just the title and description) based off their
   * frequency and appearance in the title vs in the description.
   *
   * @param title String representing the title text to be processed
   * @param desc String representing the description text to be processed
   * @return Map containing most important words and their weighted occurrences from the string
   */
  public static Map<String, Integer> getKeywords(String title, String desc) {
    // TODO: convert to lowercase in processing (figure out acronyms)
    Map<String, Integer> titleMap = KeywordSearchServlet.wordCount(title);
    Map<String, Integer> descMap = KeywordSearchServlet.wordCount(desc);

    // Merge maps
    // Title occurrence values multiplied by 2 to give more weight than description
    titleMap.forEach((key, value) -> descMap.merge(key, value * 2, (v1, v2) -> v1 + v2));
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
    Map<String, Integer> finalMap = new HashMap<String, Integer>();
    int count = 0;
    while (finalMap.size() < NUM_KEYWORDS && count < mergeList.size()) {
      Map.Entry e = mergeList.get(count);
      // Exclude words with less appearances than the cutoff
      if (((int) e.getValue()) < MIN_INSTANCES) {
        break;
      } else if (count >= mergeList.size()) {
        break;
      } else if (!IRRELEVANT_WORDS.contains(e.getKey().toString())) {
        // Exclude common useless words (in, a, the, etc)
        finalMap.put(e.getKey().toString(), (int) e.getValue());
      }
      count++;
    }

    return finalMap;
  }

  /**
   * Returns the keywords for a map.
   *
   * @param map Map containing the keywords and their weighted occurrences
   * @return List containing the keywords only
   */
  public static List<String> getKeywordMapKeys(Map<String, Integer> map) {
    List<String> list = new ArrayList<String>();
    List<Map.Entry<String, Integer>> mapList =
        new ArrayList<Map.Entry<String, Integer>>(map.entrySet());
    Collections.sort(
        mapList,
        new Comparator<Map.Entry<String, Integer>>() {
          public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
            return (o2.getValue()).compareTo(o1.getValue());
          }
        });
    for (int i = 0; i < mapList.size(); i++) {
      list.add(mapList.get(i).getKey().toString());
    }
    return list;
  }

  /**
   * Returns the keyword values for a map.
   *
   * @param map Map containing the keywords and their weighted occurrences
   * @return List containing the keyword values only
   */
  public static List<Integer> getKeywordMapValues(Map<String, Integer> map) {
    List<Integer> list = new ArrayList<Integer>();
    List<Map.Entry<String, Integer>> mapList =
        new ArrayList<Map.Entry<String, Integer>>(map.entrySet());
    Collections.sort(
        mapList,
        new Comparator<Map.Entry<String, Integer>>() {
          public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
            return (o2.getValue()).compareTo(o1.getValue());
          }
        });
    for (int i = 0; i < mapList.size(); i++) {
      list.add(mapList.get(i).getValue());
    }
    return list;
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
    List<String> words = KeywordSearchServlet.getSeparateWords(input);
    // process title words
    Map<String, Integer> map = new HashMap<String, Integer>();
    for (String w : words) {
      w = w.toLowerCase();
      map.put(w, map.getOrDefault(w, 0) + 1);
    }
    return map;
  }

  /**
   * Returns the weighted occurrence (occurrences of title keywords * 2 + occurrences of desc) of
   * keywords contained in both the search query and event.
   *
   * @param eventKeywords List of keywords generated for an event
   * @param searchKeywords List of search keywords
   * @param eventKeywordValues List of values corresponding to the number of each keyword from the
   *     event
   * @return Double total weighted occurrences of keywords
   */
  public static Double occurrenceScore(
      List<String> eventKeywords, List<String> searchKeywords, List<Long> eventKeywordValues) {
    List<String> mutualKeywords = new ArrayList<String>(eventKeywords);
    mutualKeywords.retainAll(searchKeywords);
    double total = 0;
    for (String key : mutualKeywords) {
      int index = eventKeywords.indexOf(key);
      if (index != -1 && index < eventKeywordValues.size()) {
        total += eventKeywordValues.get(index);
      }
    }
    return total;
  }
}
