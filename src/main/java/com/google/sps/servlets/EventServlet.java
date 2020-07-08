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
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import java.io.IOException;
import java.util.logging.Logger;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/event")
public class EventServlet extends HttpServlet {

  private static final Logger LOGGER = Logger.getLogger(EventServlet.class.getName());

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // TODO: Implement doGet function.
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    UserService userService = UserServiceFactory.getUserService();
    if (userService.isUserLoggedIn()) {
      String email = userService.getCurrentUser().getEmail();
      Entity eventEntity = populateEvent(request);
      eventEntity.setProperty("creator", email);

      DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
      datastore.put(eventEntity);

    } else {
      throw new IOException("Cannot create an event while not logged in");
    }

    // Redirect back to the my-events HTML page.
    response.sendRedirect("/my-events.html");
  }

  /** @return the Event entity */
  private Entity populateEvent(HttpServletRequest request) {
    // Get the input from the form.
    String eventName = getParameter(request, "event-name", "");
    String eventDescription = getParameter(request, "event-description", "");
    String streetAddress = getParameter(request, "street-address", "");
    String city = getParameter(request, "city", "");
    String state = getParameter(request, "state", "");
    String date = getParameter(request, "date", "");
    String startTime = getParameter(request, "start-time", "");
    String endTime = getParameter(request, "end-time", "");
    String coverPhoto = getParameter(request, "cover-photo", "");
    String tags = getParameter(request, "all-tags", "");

    String formattedDate = formatDate(date);
    String formattedTime = formatTime(startTime);
    String fullAddress = String.format("%1$s %2$s, %3$s", streetAddress, city, state);

    Entity eventEntity = new Entity("Event");
    eventEntity.setProperty("eventName", eventName);
    eventEntity.setProperty("eventDescription", eventDescription);
    eventEntity.setProperty("address", fullAddress);
    eventEntity.setProperty("date", formattedDate);
    eventEntity.setProperty("startTime", formattedTime);
    eventEntity.setProperty("endTime", endTime);
    eventEntity.setProperty("coverPhoto", coverPhoto);
    eventEntity.setProperty("tags", tags);

    return eventEntity;
  }

  /**
   * @return the request parameter, or the default value if the parameter was not specified by the
   *     client
   */
  private String getParameter(HttpServletRequest request, String name, String defaultValue) {
    String value = request.getParameter(name);
    if (value == null) {
      return defaultValue;
    }
    return value;
  }

  /** Format time to standard format. */
  private String formatTime(String time) {
    String hour = time.substring(0, 2);
    String minutes = time.substring(3, 5);
    Integer hourInt = Integer.parseInt(hour);

    String standardHour = getHour(hourInt);
    String period = getPeriod(hourInt);

    String formattedTime = String.format("%1$s:%2$s %3$s", standardHour, minutes, period);

    return formattedTime;
  }

  /** Get Hour in standard format. */
  private String getHour(Integer hour) {
    Integer standard = hour % 12;
    String standardString = Integer.toString(standard);

    return standardString;
  }

  /** Get Period (AM/PM). */
  private String getPeriod(Integer hour) {
    String period = "";

    if (hour > 12) {
      period = "PM";
    } else {
      period = "AM";
    }

    return period;
  }

  /** Format date to fit Month Day, Year format. */
  private String formatDate(String date) {
    String month = date.substring(5, 7);
    String day = date.substring(8, 10);
    String year = date.substring(0, 4);

    String fullMonth = getMonth(month);

    String formattedDate = String.format("%1$s %2$s, %3$s", fullMonth, day, year);

    return formattedDate;
  }

  /** Return the month corresponding to the number. */
  private String getMonth(String month) {
    String monthString = "";
    switch (month) {
      case "01":
        monthString = "January";
        break;
      case "02":
        monthString = "February";
        break;
      case "03":
        monthString = "March";
        break;
      case "04":
        monthString = "April";
        break;
      case "05":
        monthString = "May";
        break;
      case "06":
        monthString = "June";
        break;
      case "07":
        monthString = "July";
        break;
      case "08":
        monthString = "August";
        break;
      case "09":
        monthString = "September";
        break;
      case "10":
        monthString = "October";
        break;
      case "11":
        monthString = "November";
        break;
      case "12":
        monthString = "December";
    }
    return monthString;
  }
}
