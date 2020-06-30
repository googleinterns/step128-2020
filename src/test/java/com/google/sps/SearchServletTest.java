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
import java.io.IOException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

/** */
@RunWith(JUnit4.class)
public final class SearchServletTest {
  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
  private SearchServlet testSearchServlet;

  @Before
  public void setUp() {
    helper.setUp();
    testSearchServlet = new SearchServlet();
  }

  @After
  public void tearDown() {
    helper.tearDown();
  }

  @Test
  public void getsSeparateWordsCorrectly() throws IOException {
    String text = "The quick brown fox jumps over the lazy dog.";
    List<String> correctList = new ArrayList<String>(Arrays.asList("The","quick","brown","fox","jumps","over","the","lazy","dog"));  
    List<String> testList = SearchServlet.getSeparateWords(text);
    System.out.println(testList);
    assertEquals(correctList, testList);
  }

  @Test
  public void handlesAllSeparatorsCorrectly() throws IOException {
    String text = "Are you sure? I... well; I'm not:quite, personally(hehe)[okay]";
    List<String> correctList = new ArrayList<String>(Arrays.asList("Are","you","sure","I","well","I'm","not","quite","personally","hehe","okay"));  
    List<String> testList = SearchServlet.getSeparateWords(text);
    System.out.println(testList);
    assertEquals(correctList, testList);
  }

  @Test
  public void keywordListsMergeCorrectly() throws IOException {
    String titleText = "BLM protest";
    String descText = "Protest in support of black lives matter."
    List<String> correctList = new ArrayList<String>(Arrays.asList("protest"));  
    List<String> testList = SearchServlet.getSeparateWords(text);
    System.out.println(testList);
    assertEquals(correctList, testList);
  }
}