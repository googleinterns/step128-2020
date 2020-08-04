/* Copyright 2020 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.sps.servlets;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.sps.Recommend;
import java.io.IOException;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/worker")
public class TaskHandler extends HttpServlet {

  private static final Logger LOGGER = Logger.getLogger(TaskHandler.class.getName());

  private static final int TWELVE_HOURS = 43_200_000;

  /** Makes sure there is a recommend job queued. */
  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    LOGGER.info("called GET. redirecting to POST");
    doPost(request, response);
  }

  /** Calculates recommendations and re-schedules the next calculation. */
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    // recalculates the recommendation model
    LOGGER.info("initializing recalculation of model.");
    Recommend.calculateRecommend();
    LOGGER.info("completed recalculation of model");

    // queues new task for next recalculation
    Queue queue = QueueFactory.getQueue("recommend-queue");
    queue.add(TaskOptions.Builder.withUrl("/worker").countdownMillis(TWELVE_HOURS));
    LOGGER.info("scheduled next task for 12 hours later");
  }
}
