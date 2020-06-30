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
import java.util.ArrayList;

@WebServlet("/auth")
public class AuthServlet extends HttpServlet {

  private static final Logger LOGGER = Logger.getLogger(AuthServlet.class.getName());

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    UserService userService = UserServiceFactory.getUserService();
    Gson gson = new Gson();
    response.setContentType("application/json");

    if (userService.isUserLoggedIn()) {
      String userEmail = userService.getCurrentUser().getEmail();
      String logoutUrl = userService.createLogoutURL("/");

      DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
      Key userKey = KeyFactory.createKey("User", userEmail);
      try {
        Entity entity = datastore.get(userKey);
      } catch (EntityNotFoundException e) {
        Entity entity = new Entity(userKey);
        entity.setProperty("id", userEmail);
        entity.setProperty("saved", new ArrayList<Long>());
        datastore.put(entity);
      }
      LoginObject info = new LoginObject(true, logoutUrl);
      response.getWriter().println(gson.toJson(info));
    } else {
      String loginUrl = userService.createLoginURL("/");
      LOGGER.info("not currently logged in. Created login URL " + loginUrl);

      LoginObject info = new LoginObject(false, loginUrl);
      response.getWriter().println(gson.toJson(info));
    }
  }

  private static class LoginObject {
    private boolean loggedIn;
    private String url;

    private LoginObject(boolean loggedIn, String url) {
      this.loggedIn = loggedIn;
      this.url = url;
    }
  }
}
