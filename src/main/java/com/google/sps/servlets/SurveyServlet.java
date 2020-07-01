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

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.google.gson.Gson;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.TreeMap;

@WebServlet("/submit-survey")
public class SurveyServlet extends HttpServlet {

  private static final Logger LOGGER = Logger.getLogger(SurveyServlet.class.getName());

  // interest categories, matches question order on survey pages
  public static final String[] metrics = {"environment", "blm", "volunteer", "education", "LGBTQ+"};

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // store survey results
    UserService userService = UserServiceFactory.getUserService();
    Gson gson = new Gson();
    response.setContentType("application/json");

    if (userService.isUserLoggedIn()) {
      String userEmail = userService.getCurrentUser().getEmail();
      String logoutUrl = userService.createLogoutURL("/");

      DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
      Key userKey = KeyFactory.createKey("User", userEmail);

      // make sure there is a datastore entry for the user
      Entity userEntity;
      try {
        userEntity = datastore.get(userKey);
      } catch (EntityNotFoundException e) {
        userEntity = new Entity(userKey);
        userEntity.setProperty("id", userEmail);
      }

      // save score of each survey metric as an entity property
      for (String param: metrics) {
        String score = request.getParameter(param);
        if (score == null) {
          throw new IOException("incomplete survey");
        }
        userEntity.setProperty(param, score);
      }

      datastore.put(userEntity);
      response.sendRedirect("/index.html");
    } else {
      throw new IOException ("Cannot take survey while not logged in");
    }
  }

  /**
   * returns a map of a user's interest levels with respect to each tag 
   * returns null if unsuccessful (user not found or survey not yet taken/incomplete)
   */
  public static Map<String, Integer> getInterestMetrics(String userEmail) {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Key userKey = KeyFactory.createKey("User", userEmail);
    Map<String, Integer> result = new TreeMap<>();
    Entity userEntity;
    try {
      userEntity = datastore.get(userKey);
      for (String param: metrics) {
        if (userEntity.hasProperty(param)) {
          int score = Integer.parseInt(userEntity.getProperty(param).toString());
          result.put(param, score);
        } else {
          LOGGER.warning("ERROR: entity did not contain " + param);
          return null;
        }
      }
    } catch (EntityNotFoundException e) {
      LOGGER.warning("ERROR: email not found " + userEmail);
      return null;
    }
    return result;
  }
}
