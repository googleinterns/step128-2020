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
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import java.io.IOException;
import java.lang.IllegalArgumentException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@WebServlet("/load-event")
public class LoadEventServlet extends HttpServlet {

  private static final Logger LOGGER = Logger.getLogger(LoadEventServlet.class.getName());

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    Key keyRequested = getEventKey(request);
    Query query = new Query("Event", keyRequested);

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Entity eventRequested = datastore.prepare(query).asSingleEntity();
    System.out.println(eventRequested);
  }

  /**
   * @return the key from the request parameter.
   */ 
  private Key getEventKey(HttpServletRequest request) throws IllegalArgumentException {
    // Get the string from the request.
    String eventKeyString = request.getParameter("event");

    // Convert String to type Key.
    Key eventKey = KeyFactory.stringToKey(eventKeyString);
    return eventKey;
  }
}
