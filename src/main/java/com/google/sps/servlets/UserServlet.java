package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
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
        List<Entity> results = new ArrayList<>();
        Entity userEntity = datastore.get(userKey);
        switch (request.getParameter("get")) {
          case "saved":
            results = handleSaved(userEntity);
            break;
          case "created":
            results = handleCreated(userEmail);
            break;
          default:
            throw new IOException("missing parameters");
        }
        response.getWriter().println(gson.toJson(results));
        LOGGER.info("queried for events @ account " + userEmail);

      } catch (EntityNotFoundException exception) {
        // datastore entry has not been created yet for this user, create it now
        Entity entity = new Entity(userKey);
        entity.setProperty("id", userEmail);
        datastore.put(entity);

        // new user will not have any saved or created events (empty list)
        response.getWriter().println(gson.toJson(new ArrayList<>()));
      }
    } else {
      // return a list with all created events
      PreparedQuery results =
          datastore.prepare(new Query("Event").addSort("eventName", SortDirection.ASCENDING));
      List<Entity> events = new ArrayList<>();
      for (Entity e : results.asIterable()) {
        events.add(e);
      }
      response.getWriter().println(gson.toJson(events));
    }
  }

  // returns a list of all events saved by a user entity
  private List<Entity> handleSaved(Entity userEntity) {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    List<Entity> results = new ArrayList<>();
    // get the list of saved events (stored by id)
    @SuppressWarnings("unchecked")
    List<Long> savedEvents = (ArrayList<Long>) userEntity.getProperty("saved");
    if (savedEvents != null) {
      for (long l : savedEvents) {
        try {
          results.add(datastore.get(KeyFactory.createKey("Event", l)));
        } catch (EntityNotFoundException exception) {
          LOGGER.info("entity not found for event id " + l);
        }
      }
    }
    // TODO: apply sort param choices

    return results;
  }

  // returns a list of all events created by a user (identified by email id)
  private List<Entity> handleCreated(String userEmail) {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    List<Entity> results = new ArrayList<>();

    Query query =
        new Query("Event")
            .setFilter(new Query.FilterPredicate("creator", Query.FilterOperator.EQUAL, userEmail))
            .addSort("eventName", SortDirection.ASCENDING);
    PreparedQuery queried = datastore.prepare(query);
    for (Entity e : queried.asIterable()) {
      results.add(e);
    }
    // TODO: apply sort param choices

    return results;
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

    // TODO: add event to a list

  }
}
