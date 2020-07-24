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

import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class FormattingTest {

  @Test
  public void formatMidnight() throws IOException {
    String time = "00:00";
    String formattedTime = Utils.formatTime(time);

    assertEquals("12:00 AM", formattedTime);
  }

  @Test
  public void formatNoon() throws IOException {
    String time = "12:00";
    String formattedTime = Utils.formatTime(time);

    assertEquals("12:00 PM", formattedTime);
  }

  @Test
  public void formatSingleDigitAm() throws IOException {
    String time = "07:00";
    String formattedTime = Utils.formatTime(time);

    assertEquals("7:00 AM", formattedTime);
  }

  @Test
  public void formatSingleDigitPm() throws IOException {
    String time = "13:00";
    String formattedTime = Utils.formatTime(time);

    assertEquals("1:00 PM", formattedTime);
  }

  @Test
  public void unformattableTime() throws IOException {
    String time = "";
    String formattedTime = Utils.formatTime(time);

    assertEquals("", formattedTime);
  }

  @Test
  public void formatSingleDigitMonth() throws IOException {
    String date = "2020-05-17";
    String formattedDate = Utils.formatDate(date);

    assertEquals("Sunday, May 17, 2020", formattedDate);
  }

  @Test
  public void formatDoubleDigitMonth() throws IOException {
    String date = "2020-12-17";
    String formattedDate = Utils.formatDate(date);

    assertEquals("Thursday, December 17, 2020", formattedDate);
  }

  @Test
  public void unformattableDate() throws IOException {
    String date = "";
    String formattedDate = Utils.formatDate(date);

    assertEquals("", formattedDate);
  }
}
