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
      String logoutUrl = userService.createLogoutURL("/");

      DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
      Key userKey = KeyFactory.createKey("User", userEmail);
      try {
        Entity entity = datastore.get(userKey);
        String eventsType = "";

        try {
          // get the lists
          List<Entity> results = new ArrayList<>();
          if(request.getParameter("get").equals("saved")) {
            eventsType = "saved";
            @SuppressWarnings("unchecked")
              List<Long> savedEvents = (ArrayList<Long>) entity.getProperty("saved");
              if(savedEvents != null) {
                for(long l: savedEvents) {
                  results.add(datastore.get(KeyFactory.createKey("Event", l)));
                }
              }
            } else if(request.getParameter("get").equals("created")) {
              eventsType = "created";
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

            ResultObject info = new ResultObject(logoutUrl, eventsType, results);
            response.getWriter().println(gson.toJson(info));
            
        } catch(EntityNotFoundException exception) {
            LOGGER.info("entity not found");
        }
        LOGGER.info("queried for " + eventsType+ " events @ account " + userEmail + ". Created logout URL " + logoutUrl);
      } catch(EntityNotFoundException exception) {
        Entity entity = new Entity(userKey);
        entity.setProperty("id", userEmail);
        entity.setProperty("saved", new ArrayList<Long>());
        datastore.put(entity);
        
        ResultObject info = new ResultObject(logoutUrl, request.getParameter("get"), new ArrayList<>());
        response.getWriter().println(gson.toJson(info));
      }
    } else {
      String loginUrl = userService.createLoginURL("/");
      LOGGER.info("not currently logged in. Created login URL " + loginUrl);

      ResultObject info = new ResultObject(loginUrl, null, null);
      response.getWriter().println(gson.toJson(info));
    }
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

      // add event to a list
  }

  private static class ResultObject {
    private String url;
    private String eventType;
    private List<Entity> eventResults;

    private ResultObject(String url, String type, List<Entity> list) {
      this.url = url;
      eventType = type;
      eventResults = list;
    }

  }
}