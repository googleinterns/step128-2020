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
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.sps.Firebase;
import com.google.sps.Utils;
import java.io.IOException;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/confirm-user")
public class ConfirmUserServlet extends HttpServlet {

  private static final Logger LOGGER = Logger.getLogger(ConfirmUserServlet.class.getName());

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    // Get the event key string from the request.
    String keyString = request.getParameter("Event");
    Key key = KeyFactory.stringToKey(keyString);

    Query query = new Query("Event", key);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Entity eventRequested = datastore.prepare(query).asSingleEntity();

    String creator = eventRequested.getProperty("creator").toString();
    String userToken = request.getParameter("userToken");
    String userID = Firebase.authenticateUser(userToken);

    boolean belongs = false;
    if (creator.equals(userID)) {
      belongs = true;
    }

    response.setContentType("application/json;");
    response.getWriter().println(Utils.convertToJson(belongs));
  }
}
