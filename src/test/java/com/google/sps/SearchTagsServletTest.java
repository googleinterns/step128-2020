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

import static com.google.appengine.api.datastore.FetchOptions.Builder.withLimit;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.sps.servlets.SearchServlet;
import com.google.sps.servlets.Utility;
import java.io.IOException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.Test;

/** */
@RunWith(JUnit4.class)
public final class SearchTagsServletTest {
  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
  private SearchServlet testSearchServlet;
  private List<Entity> testEntities;

  @Before
  public void setUp() {
    helper.setUp();
    testSearchServlet = new SearchServlet();
    List<String> possibleTags = new ArrayList<String>(Arrays.asList(
        "environment", "blm", "education", "volunteer", "LGBTQ+"
    ));
    testEntities = new ArrayList<Entity>();
    //single tag events
    for (int i = 0; i < 5; i++) {
      Entity e = new Entity("Event");
      e.setProperty("eventName", i);
      e.setIndexedProperty("tags", new ArrayList<String>(possibleTags[i]));
      testEntities.add(e);
    }
    //double tag events
    for (int i = 0; i < 4; i++) {
      Entity e = new Entity("Event");
      e.setProperty("eventName", i);
      e.setIndexedProperty("tags", new ArrayList<String>(possibleTags[i], possibleTags[i+1]));
      testEntities.add(e);
    }
  }

  @After
  public void tearDown() {
    helper.tearDown();
  }

  @Test
  public void anyQueryAndOutput() throws IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);

    // Add a mock request to pass as a parameter to doPost.
    when(request.getParameter("tags")).thenReturn("environment");

    // Post event to Datastore.
    String jsonResponse = testSearchServlet.doGet(request, response);

    // Assert only one Entity was posted to Datastore.
    testEntities.get
    DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
    assertEquals(1, ds.prepare(new Query("Event")).countEntities(withLimit(10)));
  }
}
