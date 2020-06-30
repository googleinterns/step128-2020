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
    // returns a list of events
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    UserService userService = UserServiceFactory.getUserService();
    Gson gson = new Gson();
    response.setContentType("application/json");

    if (userService.isUserLoggedIn()) {
      String userEmail = userService.getCurrentUser().getEmail();
      Key userKey = KeyFactory.createKey("User", userEmail);
      try {
        Entity userEntity = datastore.get(userKey);
        List<Entity> results = new ArrayList<>();
        if(request.getParameter("get")!= null && request.getParameter("get").equals("saved")) {
          // get the list of saved events (stored by id)
          @SuppressWarnings("unchecked")
            List<Long> savedEvents = (ArrayList<Long>) userEntity.getProperty("saved");
          if(savedEvents != null) {
            for(long l: savedEvents) {
              try {
                results.add(datastore.get(KeyFactory.createKey("Event", l)));
              } catch(EntityNotFoundException exception) {
                LOGGER.info("entity not found for event id " + l);
              }
            }
          }
          // TODO: apply sort
        } else if(request.getParameter("get") != null && request.getParameter("get").equals("created")) {
          // query for events that were created by this user
          results = new ArrayList<>();
          Query query = new Query("Event")
                .setFilter(new Query.FilterPredicate("creator", Query.FilterOperator.EQUAL, userEmail))
                .addSort("eventName", SortDirection.ASCENDING);
          PreparedQuery queried = datastore.prepare(query);
          for(Entity e: queried.asIterable()) {
            long eventId = (long) e.getProperty("id");
            try {
              results.add(datastore.get(KeyFactory.createKey("Event", eventId)));
            } catch(EntityNotFoundException exception) {
              LOGGER.info("entity not found for event id " + eventId);
            }
          }
          // TODO: apply choices for sort
        } else {
          throw new IOException("missing parameters");
        }
        // return a list of saved or created event entities by this user
        response.getWriter().println(gson.toJson(results));
        LOGGER.info("queried for events @ account " + userEmail);
      } catch(EntityNotFoundException exception) {
        // datastore entry has not been created yet for this user, create it now
        Entity entity = new Entity(userKey);
        entity.setProperty("id", userEmail);
        datastore.put(entity);
        
        // new user will not have any saved or created events (empty list)
        response.getWriter().println(gson.toJson(new ArrayList<>()));
      }
    } else {
      // return a list with all created events
      PreparedQuery results = datastore.prepare(new Query("Event").addSort("eventName", SortDirection.ASCENDING));
      List<Entity> events = new ArrayList<>();
      for(Entity e: results.asIterable()) {
        events.add(e);
      }
      response.getWriter().println(gson.toJson(events));
    }
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

      // TODO: add event to a list
      
  }

}