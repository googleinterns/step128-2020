package com.google.sps.servlets;

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.gson.Gson;
import java.util.List;
import java.util.ArrayList;

import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/user")
public class UserServlet extends HttpServlet {

  private static final Logger LOGGER = Logger.getLogger(UserServlet.class.getName());

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    UserService userService = UserServiceFactory.getUserService();
    Gson gson = new Gson();
    response.setContentType("application/json");

    if (userService.isUserLoggedIn()) {
      String userEmail = userService.getCurrentUser().getEmail();

      DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
      Key userKey = KeyFactory.createKey("User", userEmail);
      try {
        Entity entity = datastore.get(userKey);
        try {
          List<Entity> results = new ArrayList<>();
          if(request.getParameter("get").equals("saved")) {
            // get the list of saved events (stored by id)
            @SuppressWarnings("unchecked")
              List<Long> savedEvents = (ArrayList<Long>) entity.getProperty("saved");
              if(savedEvents != null) {
                for(long l: savedEvents) {
                  results.add(datastore.get(KeyFactory.createKey("Event", l)));
                }
              }
            } else if(request.getParameter("get").equals("created")) {
              // query for events that were created by this user
              results = new ArrayList<>();
              Query query = new Query("Event")
                    .setFilter(new Query.FilterPredicate("creator", Query.FilterOperator.EQUAL, userEmail));
              PreparedQuery queried = datastore.prepare(query);
              for(Entity e: queried.asIterable()) {
                results.add(datastore.get(KeyFactory.createKey("Event", (long) e.getProperty("id"))));
              }
            } else {
              throw new IOException("invalid parameters");
            }

            response.getWriter().println(gson.toJson(results));
            
        } catch(EntityNotFoundException exception) {
            LOGGER.info("entity not found");
        }
        LOGGER.info("queried for events @ account " + userEmail);
      } catch(EntityNotFoundException exception) {
        // datastore entry has not been created yet for this user
        Entity entity = new Entity(userKey);
        entity.setProperty("id", userEmail);
        entity.setProperty("saved", new ArrayList<Long>());
        datastore.put(entity);
        
        response.getWriter().println(gson.toJson(new ArrayList<>()));
      }
    } else {
      response.getWriter().println(gson.toJson(null));
    }
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

      // add event to a list
  }

}