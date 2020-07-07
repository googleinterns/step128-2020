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

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.maps.model.LatLng;
import com.google.sps.servlets.SearchServlet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

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
    List<String> correctList =
        new ArrayList<String>(
            Arrays.asList("The", "quick", "brown", "fox", "jumps", "over", "the", "lazy", "dog"));
    List<String> testList = SearchServlet.getSeparateWords(text);
    assertEquals(correctList, testList);
  }

  @Test
  public void handlesAllSeparatorsCorrectly() throws IOException {
    String text = "Are you sure? I... well; I'm not:quite, " + "personally(hehe)[okay]";
    List<String> correctList =
        new ArrayList<String>(
            Arrays.asList(
                "Are",
                "you",
                "sure",
                "I",
                "well",
                "I'm",
                "not",
                "quite",
                "personally",
                "hehe",
                "okay"));
    List<String> testList = SearchServlet.getSeparateWords(text);
    assertEquals(correctList, testList);
  }

  @Test
  public void getsTitleAndDescKeywordsCorrectly() throws IOException {
    String titleText =
        "Climate climate climate climate Protest! Climate " + "protest: Protest protest!";
    String descText =
        "Important! Please come out in support of the environment. The "
            + "environment is so important to us! Protect the environment. Come!";
    // climate - 5, w/ weight 10
    // protest - 4, w/ weight 8
    // environment - 3
    // come - 2
    // important - 2
    List<String> correctList =
        new ArrayList<String>(
            Arrays.asList("climate", "protest", "environment", "come", "important"));
    List<String> testList = SearchServlet.getKeywords(titleText, descText);
    assertEquals(correctList, testList);
  }

  @Test
  public void getsTitleAndDescKeywordsCorrectlyWeighted() throws IOException {
    String titleText = "Climate! Climate change protest: Protest for our children!";
    String descText =
        "Important! Please come out in support of the environment. The "
            + "environment is so important important to us! Protect the environment. Come come!";
    // climate - 4, w/o weight 2
    // protest - 4, w/o weight 2
    // environment - 3
    // come - 3
    // important - 3
    List<String> correctList =
        new ArrayList<String>(
            Arrays.asList("protest", "climate", "come", "important", "environment"));
    List<String> testList = SearchServlet.getKeywords(titleText, descText);
    assertEquals(correctList, testList);
  }

  @Test
  public void keywordListsMergeCorrectly() throws IOException {
    String titleText = "Climate change protest";
    String descText = "Protest protest in support of the environment environment environment.";
    // protest - 4
    // environment - 3
    // change - 2
    // climate - 2
    // support - 1, so its cut off
    List<String> correctList =
        new ArrayList<String>(Arrays.asList("protest", "environment", "change", "climate"));
    List<String> testList = SearchServlet.getKeywords(titleText, descText);
    assertEquals(correctList, testList);
  }

  @Test
  public void getKeywordsHasMax5() throws IOException {
    String titleText = "";
    String descText =
        "Climate climate climate climate change change change"
            + " protest protest protest for Earth Earth. Important movement "
            + "movement to movement movement movement important important "
            + "important important important.";
    // important - 6
    // movement - 5
    // climate - 4
    // change - 3
    // protest - 3
    // earth should be cut off
    List<String> correctList =
        new ArrayList<String>(
            Arrays.asList("important", "movement", "climate", "change", "protest"));
    List<String> testList = SearchServlet.getKeywords(titleText, descText);
    assertEquals(correctList, testList);
  }

  @Test
  public void getDistanceWorks() throws IOException {
    int distance =
        SearchServlet.getDistance(
            new LatLng(-31.9522, 115.8589), new LatLng(-25.344677, 131.036692));
    assertEquals(2059, distance);
  }

  @Test
  public void getLatLngWorks() throws IOException {
    LatLng location = SearchServlet.getLatLng("3 Forrest Pl, Perth WA 6000, Australia");
    assertEquals(new LatLng(-31.95220010, 115.85884740), location);
  }
}
