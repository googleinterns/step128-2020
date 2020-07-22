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
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.sps.Firebase;
import com.google.sps.Utils;
import java.io.IOException;
import java.util.logging.Logger;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/location")
public class LocationServlet extends HttpServlet {

  private static final Logger LOGGER = Logger.getLogger(LocationServlet.class.getName());

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

    String location = "";

    String userToken = request.getParameter("userToken");
    String userID = "";
    if (Firebase.isUserLoggedIn(userToken)) {
      userID = Firebase.authenticateUser(userToken);
      Key userKey = KeyFactory.createKey("User", userID);
      Entity userEntity = null;
      try {
        userEntity = datastore.get(userKey);
      } catch (EntityNotFoundException exception) {
        userEntity = Utils.makeUserEntity(userID, "", true);
        LOGGER.info("No entity found for " + userID + ", creating one now.");
      }
      location = userEntity.getProperty("location").toString();
    }

    String json = Utils.convertToJson(location);

    response.setContentType("application/json;");
    response.getWriter().println(json);
    LOGGER.info(String.format("Got user %1$s's location", userID));
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

    String userToken = request.getParameter("userToken");
    if (!Firebase.isUserLoggedIn(userToken)) {
      throw new IOException("must be logged in");
    }
    if (request.getParameter("zip") == null) {
      throw new IOException("no location specified");
    }
    // Handle the logic
    String userID = Firebase.authenticateUser(userToken);
    Key userKey = KeyFactory.createKey("User", userID);
    Entity userEntity = null;

    try {
      userEntity = datastore.get(userKey);
    } catch (EntityNotFoundException exception) {
      // datastore entry has not been created yet for this user, create it now
      userEntity = new Entity(userKey);
      userEntity.setProperty("firebaseID", userID);
      datastore.put(userEntity);
    }

    String zip = request.getParameter("zip");
    userEntity.setProperty("location", zip);

    datastore.put(userEntity);
    LOGGER.info(String.format("Posted user %1$s's location", userID));
  }
}
