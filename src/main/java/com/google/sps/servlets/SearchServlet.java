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

import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.Comparator;
import java.util.AbstractMap;

@WebServlet("/search")
public class SearchServlet extends HttpServlet {

  private static final Logger LOGGER = Logger.getLogger(SearchServlet.class.getName());
  //this list is incomplete
  private static final List<String> irrelevantWords = new ArrayList<String>(Arrays.asList(
      "the", "is", "for", "in", "of", "so", "to"
      ));

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // get location
    // filter by location and cutoff outside it
    
    // get tags
    // drop all without first tag
    // those with most tags in common with search go to top
    // those closest to the user go to the top
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

  }

  /**
   * Returns keywords from an event (currently using just the title
   * and description) based off their frequency and appearance in
   * the title vs in the description
   */
  public static List<String> getKeywords(String title, String desc) {
    // TODO: convert to lowercase in processing (figure out acronyms)
    Map<String, Integer> titleMap = SearchServlet.wordCount(title);
    Map<String, Integer> descMap = SearchServlet.wordCount(desc);
   
    List<Map.Entry<String, Integer>> titleMapList = new ArrayList<Map.Entry<String, Integer>>(titleMap.entrySet()); 

    // merge lists
    /*
    List<Map.Entry<String, Integer>> mergeList = new ArrayList<Map.Entry<String, Integer>>(descMap.entrySet());
    for (Map.Entry titleEntry : titleMapList) {
      boolean found = false;
      for (int i = 0; i < mergeList.size(); i++) {
        Map.Entry mergeEntry = mergeList.get(i);
        if (mergeEntry.getKey().toString().equals(titleEntry.getKey().toString())) {
          mergeEntry.setValue(((int) mergeEntry.getValue()) + ((int) titleEntry.getValue() * 2));
          found = true;
        }
      }
      if (!found) {
        Map.Entry<String,Integer> entry = 
            new AbstractMap.SimpleEntry<String, Integer>(
            titleEntry.getKey().toString(), ((int) titleEntry.getValue()) * 2 );
        mergeList.add(entry);
      }
    }*/

    titleMap.forEach(
      (key, value) -> descMap.merge(key, value * 2, (v1, v2) -> v1 + (v2 * 2)));

    List<Map.Entry<String, Integer>> mergeList = new ArrayList<Map.Entry<String, Integer>>(descMap.entrySet());

    Collections.sort(mergeList, new Comparator<Map.Entry<String, Integer>>() { 
      public int compare(Map.Entry<String, Integer> o1,  
                         Map.Entry<String, Integer> o2) { 
        return (o2.getValue()).compareTo(o1.getValue()); 
      } 
    });

    // add top results to the final list
    List<String> finalList = new ArrayList<String>();
    int count = 0;
    // minimum number of instances to be considered a keyword
    int cutoff = 2;
    while (finalList.size() < 5) {
      Map.Entry e = mergeList.get(count);
      // exclude words with less appearances than the cutoff
      if (((int) e.getValue()) < cutoff) break;
      else if (count >= mergeList.size()) break;
      // exclude common useless words (in, a, the, etc)
      else if (!irrelevantWords.contains(e.getKey().toString())) {
        finalList.add(e.getKey().toString());
      }
      count++;
    }

    return finalList;
  }

  /**
   * Returns a list of words contained in a string
   */
  public static List<String> getSeparateWords(String str) {
    // TODO: handle single quoted strings (right now keeps them in for
    // the common case of contractions

    String[] list = str.split("([\\s.,!?:;()\\[\\]&\"\\s+])");
    for (int i = 0; i < list.length; i++) {
      list[i] = list[i].replaceAll("\\s+","");
    }
    List<String> listArr = new ArrayList<String>(Arrays.asList(list));
    listArr.removeAll(Arrays.asList("", null));
    return listArr;
  }

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
}
