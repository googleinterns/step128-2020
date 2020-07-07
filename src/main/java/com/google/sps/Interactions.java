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

import java.util.logging.Logger;

public class Interactions {

  private static final Logger LOGGER = Logger.getLogger(Interactions.class.getName());

  // contributions to user's interest metrics for each action
  public static final int VIEW_SCORE = 1;
  public static final int SAVE_SCORE = 3;
  public static final int CREATE_SCORE = 5;

  //   public static getPreferences(String userId) {}
  // TODO: move the getInterestMetrics from the survey servlet to here
}
