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

@WebServlet("/search")
public class SearchServlet extends HttpServlet {

  private static final Logger LOGGER = Logger.getLogger(SearchServlet.class.getName());

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    //get location
    //filter by location and cutoff outside it
    
    //get tags
    //drop all without first tag
    //those with most tags in common with search go to top
    //those closest to the user go to the top
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

  }

  /**
   * Returns keywords from an event (currently using just the title
   * and description) based off an algorithm
   */
  public List<String> getKeywords(String title, String desc) {
    //TODO: convert to lowercase in processing (figure out acronyms)
    List<String> titleWords = SearchServlet.getSeparateWords(title);
    //process title words
    Map<String, Integer> titleMap = new HashMap<String, Integer>();
    for (String w : titleWords) {
      if (titleMap.containsKey(w)) {
        titleMap.put(w, titleMap.get(w) + 1);
      } else {
        titleMap.put(w, 1);
      }
    }
    List<String> descWords = SearchServlet.getSeparateWords(desc);
    //process desc words
    Map<String, Integer> descMap = new HashMap<String, Integer>();
    for (String w : descWords) {
      if (descMap.containsKey(w)) {
        descMap.put(w, descMap.get(w) + 1);
      } else {
        descMap.put(w, 1);
      }
    }

    List<Map.Entry<String, Integer>> titleMapList = new ArrayList<Map.Entry<String, Integer>>(titleMap.entrySet()); 
    List<Map.Entry<String, Integer>> descMapList = new ArrayList<Map.Entry<String, Integer>>(descMap.entrySet());

    //merge lists
    List<Map.Entry<String, Integer>> mergeList = new ArrayList<Map.Entry<String, Integer>>(descMapList);
    for (Entry e : titleMapList) {
      if (mergeList.contains(e.title.getKey().toLowerCase()) {
        
      } else {
        mergeList.add(e);
      }
    }

    Collections.sort(mergeList, new Comparator<Map.Entry<String, Integer>>() { 
      public int compare(Map.Entry<String, Integer> o1,  
                         Map.Entry<String, Integer> o2) { 
        return (o1.getValue()).compareTo(o2.getValue()); 
      } 
    });


    return mergeList;
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
}
