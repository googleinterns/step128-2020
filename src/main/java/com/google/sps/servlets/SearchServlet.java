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
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.FetchOptions.Builder;
import com.google.gson.Gson;

@WebServlet("/search")
public class SearchServlet extends HttpServlet {

  private static final Logger LOGGER = Logger.getLogger(SearchServlet.class.getName());

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // List of all the tags we are searching for
    List<String> searchTags = new ArrayList<String>(Arrays.asList(request.getParameterValues("tags")));
    // Filter to check if the event has any of tags we're searching for
    Filter tagsFilter =
        new FilterPredicate("tags", FilterOperator.IN, searchTags);
    Query query =
        new Query("Event").setFilter(tagsFilter);

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);
    List<Entity> events = new ArrayList<Entity>(results.asList(FetchOptions.Builder.withDefaults()));

    // get location
    // filter by location and cutoff outside it
    
    // get tags
    // drop all without first tag?
    // Sort list by most tags in common with search
    Collections.sort(events, new Comparator<Entity>() { 
      public int compare(Entity o1, Entity o2) {
        int condition = tagsInCommon((List<String>) o2.getProperty("tags"), searchTags).compareTo(
            tagsInCommon((List<String>) o1.getProperty("tags"), searchTags));
        // For development purposes, if two events have the same number of tags
        // they are sorted by the event names (which in the test cases are integers)
        if (condition == 0) {
          return Integer.compare(Integer.parseInt(o1.getProperty("eventName").toString()),
              Integer.parseInt(o2.getProperty("eventName").toString()));
        } else return condition;
      } 
    });
    // those closest to the user go to the top
    
    // Convert events list to json
    String json = Utility.convertToJson(events);
    
    response.setContentType("application/json;");
    response.getWriter().println(json);
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

  }

  public Integer tagsInCommon(List<String> tagListA, List<String> tagListB) {
    List<String> tagListC = new ArrayList<String>(tagListA);
    tagListC.retainAll(tagListB);
    return tagListC.size();
  }
}