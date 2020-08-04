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
import com.google.maps.DistanceMatrixApi;
import com.google.maps.DistanceMatrixApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.GeocodingApiRequest;
import com.google.maps.model.Distance;
import com.google.maps.model.DistanceMatrix;
import com.google.maps.model.DistanceMatrixElement;
import com.google.maps.model.DistanceMatrixElementStatus;
import com.google.maps.model.DistanceMatrixRow;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.Geometry;
import com.google.maps.model.LatLng;
import com.google.sps.servlets.SearchServlet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@PowerMockIgnore("okhttp3.*")
@RunWith(PowerMockRunner.class)
@PrepareForTest({
  DistanceMatrixApi.class,
  DistanceMatrixApiRequest.class,
  GeocodingApi.class,
  GeocodingApiRequest.class
})
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
  public void getDistanceWorks() throws Exception {
    LatLng loc = new LatLng(-31.9522, 115.8589);
    LatLng loc2 = new LatLng(-25.344677, 131.036692);

    DistanceMatrix dm = createDistanceMatrix(2059000, DistanceMatrixElementStatus.OK);

    mockDistanceMatrixApiSetup(loc, loc2, dm);

    int distance = Utils.getDistance(loc, loc2);
    assertEquals(2059, distance);
  }

  @Test
  public void noDistance() throws Exception {
    LatLng loc = new LatLng(-31.9522, 115.8589);
    LatLng loc2 = loc;

    DistanceMatrix dm = createDistanceMatrix(1, DistanceMatrixElementStatus.OK);

    mockDistanceMatrixApiSetup(loc, loc2, dm);

    int distance = Utils.getDistance(loc, loc2);
    assertEquals(0, distance);
  }

  @Test
  public void nonDrivableDistance() throws Exception {
    LatLng loc = new LatLng(35.6585, 139.7013);
    LatLng loc2 = new LatLng(47.6062, 122.3321);

    DistanceMatrix dm = createDistanceMatrix(0, DistanceMatrixElementStatus.ZERO_RESULTS);

    mockDistanceMatrixApiSetup(loc, loc2, dm);

    int distance = Utils.getDistance(loc, loc2);
    assertEquals(-1, distance);
  }

  @Test
  public void getLatLngWorks() throws Exception {
    LatLng loc = new LatLng(-31.95220010, 115.85884740);
    String locStr = "3 Forrest Pl, Perth WA 6000, Australia";

    GeocodingResult[] gr = createGeocodingResult(loc);

    mockGeocodingApiSetup(locStr, gr);

    LatLng location = Utils.getLatLng(locStr);
    assertEquals(new LatLng(-31.95220010, 115.85884740), location);
  }

  @Test
  public void getDistanceStrings() throws Exception {
    LatLng loc = new LatLng(-31.95220010, 115.85884740);
    String locStr = "3 Forrest Pl, Perth WA 6000, Australia";
    GeocodingResult[] gr = createGeocodingResult(loc);
    mockGeocodingApiSetup(locStr, gr);

    LatLng loc2 = new LatLng(-25.344677, 131.036692);
    String locStr2 = "Uluru, Petermann NT 0872, Australia";
    GeocodingResult[] gr2 = createGeocodingResult(loc2);
    mockGeocodingApiSetup(locStr2, gr2);

    DistanceMatrix dm = createDistanceMatrix(2059000, DistanceMatrixElementStatus.OK);
    mockDistanceMatrixApiSetup(loc, loc2, dm);

    int distance = Utils.getDistance(locStr, locStr2);
    assertEquals(2059, distance);
  }

  @Test
  public void emptyStrings() throws Exception {
    String nullString = null;

    assertEquals(null, Utils.getLatLng(nullString));
    assertEquals(null, Utils.getLatLng(""));

    assertEquals(-1, Utils.getDistance("", null));
    assertEquals(-1, Utils.getDistance("", ""));
  }

  /**
   * Returns a DistanceMatrix with the desired parameters.
   *
   * @param desiredDistance Distance in meters to be returned in the DistanceMatrix, irrelevant if
   *     status is ZERO_RESULTS
   * @param status DistanceMatrixElementStatus to be returned in the DistanceMatrix
   * @return DistanceMatrix made according to parameters
   */
  private static DistanceMatrix createDistanceMatrix(
      long desiredDistance, DistanceMatrixElementStatus status) {
    DistanceMatrixRow[] rows = new DistanceMatrixRow[] {new DistanceMatrixRow()};
    rows[0].elements = new DistanceMatrixElement[] {new DistanceMatrixElement()};

    if (status == DistanceMatrixElementStatus.ZERO_RESULTS) {
      rows[0].elements[0].status = DistanceMatrixElementStatus.ZERO_RESULTS;
    } else {
      rows[0].elements[0].distance = new Distance();
      rows[0].elements[0].distance.inMeters = desiredDistance;
      rows[0].elements[0].status = DistanceMatrixElementStatus.OK;
    }

    DistanceMatrix dm = new DistanceMatrix(new String[] {"A"}, new String[] {"B"}, rows);
    return dm;
  }

  /**
   * Returns a GeocodingResult[] with the desired location inside.
   *
   * @param desiredLocation LatLng location to be returned in the GeocodingResult[]
   * @return GeocodingResult[] with the desired location inside
   */
  private static GeocodingResult[] createGeocodingResult(LatLng desiredLocation) {
    GeocodingResult[] gr = new GeocodingResult[] {new GeocodingResult()};
    gr[0].geometry = new Geometry();
    gr[0].geometry.location = desiredLocation;
    return gr;
  }

  /**
   * Sets up the mocking for a DistanceMatrixApi request.
   *
   * @param origin LatLng location to be finding the distance from
   * @param dest LatLng location to be finding the distance to
   * @param output DistanceMatrix containing the distance the mock should return
   */
  private static void mockDistanceMatrixApiSetup(LatLng origin, LatLng dest, DistanceMatrix output)
      throws Exception {
    DistanceMatrixApiRequest dmaRequest = PowerMockito.mock(DistanceMatrixApiRequest.class);
    PowerMockito.when(dmaRequest.origins(origin)).thenReturn(dmaRequest);
    PowerMockito.when(dmaRequest.destinations(dest)).thenReturn(dmaRequest);
    PowerMockito.when(dmaRequest.await()).thenReturn(output);

    PowerMockito.mockStatic(DistanceMatrixApi.class);
    GeoApiContext context = PowerMockito.mock(GeoApiContext.class);

    PowerMockito.when(DistanceMatrixApi.newRequest(any(GeoApiContext.class)))
        .thenReturn(dmaRequest);
  }

  /**
   * Sets up the mocking for a GeocodingApi request.
   *
   * @param locStr String of the address of the location to be Geocoded
   * @param output GeocodingResult[] containing the LatLng the mock should return
   */
  private static void mockGeocodingApiSetup(String locStr, GeocodingResult[] output)
      throws Exception {
    GeocodingApiRequest geoRequest = PowerMockito.mock(GeocodingApiRequest.class);
    PowerMockito.when(geoRequest.address(locStr)).thenReturn(geoRequest);
    PowerMockito.when(geoRequest.await()).thenReturn(output);

    PowerMockito.mockStatic(GeocodingApi.class);
    GeoApiContext context = PowerMockito.mock(GeoApiContext.class);

    PowerMockito.when(GeocodingApi.newRequest(any(GeoApiContext.class))).thenReturn(geoRequest);
  }
}
