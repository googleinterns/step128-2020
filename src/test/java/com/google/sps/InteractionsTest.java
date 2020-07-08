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

package com.google.sps;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.sps.servlets.SurveyServlet;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/** Tests for the utility methods in Interactions.java */
@RunWith(PowerMockRunner.class)
@PrepareForTest(UserServiceFactory.class)
public final class InteractionsTest {
  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
  private SurveyServlet testSurveyServlet;

  private void takeSurvey(String email) throws MalformedURLException, IOException {
    TestingUtil.toggleLogin(email);

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getParameter("environment")).thenReturn("3");
    when(request.getParameter("blm")).thenReturn("4");
    when(request.getParameter("volunteer")).thenReturn("3");
    when(request.getParameter("education")).thenReturn("2");
    when(request.getParameter("LGBTQ+")).thenReturn("4");

    HttpServletResponse response = mock(HttpServletResponse.class);
    testSurveyServlet.doPost(request, response);
    TestingUtil.toggleLogin(email);
  }

  @Before
  /** Sets up the datastore helper and authentication utility for each test */
  public void setUp() throws IOException {
    helper.setUp();
    testSurveyServlet = new SurveyServlet();
    TestingUtil.setUp();
  }

  @After
  public void tearDown() {
    helper.tearDown();
    TestingUtil.tearDown();
  }

  @Test
  public void checkInterestMetrics() throws IOException {
    // a basic test to make sure Interactions.getInterestMetrics() works as intended
    String email = "test@example.com";
    takeSurvey(email);

    Map<String, Integer> expectedSurvey = new HashMap<>();
    expectedSurvey.put("environment", 3);
    expectedSurvey.put("blm", 4);
    expectedSurvey.put("volunteer", 3);
    expectedSurvey.put("education", 2);
    expectedSurvey.put("LGBTQ+", 4);

    assertEquals(expectedSurvey, Interactions.getInterestMetrics(email));
  }

  @Test
  public void noUserForCheckMetrics() throws IOException {
    // call Interactions.getInterestMetrics() on nonexistent user

    String email = "test@example.com";
    TestingUtil.toggleLogin(email);

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getParameter("environment")).thenReturn("3");
    when(request.getParameter("blm")).thenReturn("4");
    when(request.getParameter("volunteer")).thenReturn("3");
    when(request.getParameter("education")).thenReturn("2");
    when(request.getParameter("LGBTQ+")).thenReturn("4");
    HttpServletResponse response = mock(HttpServletResponse.class);
    testSurveyServlet.doPost(request, response);

    assertEquals(null, Interactions.getInterestMetrics("other@example.com"));
  }

  @Test
  public void buildVector() throws IOException {
    Map<String, Integer> expectedVector = new HashMap<>();
    expectedVector.put("environment", 0);
    expectedVector.put("blm", 1);
    expectedVector.put("volunteer", 0);
    expectedVector.put("education", 0);
    expectedVector.put("LGBTQ+", 0);

 Entity entity = UserServletTest.createBlmProtestEvent();
    assertEquals(expectedVector, Interactions.buildVectorForEvent(entity));
  }

  @Test
  public void checkDotProduct() throws IOException {
    Map<String, Integer> v1 = new HashMap<>();
    v1.put("1", 1);
    v1.put("2", 2);
    v1.put("3", 3);

    Map<String, Integer> v2 = new HashMap<>();
    v2.put("2", 2);

    assertEquals(4, Interactions.dotProduct(v1, v2));
  }
}
