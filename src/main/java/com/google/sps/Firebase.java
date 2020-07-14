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

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import java.io.IOException;
import java.util.logging.Logger;

public class Firebase {

  private static final Logger LOGGER = Logger.getLogger(Firebase.class.getName());
  private static FirebaseApp defaultApp;

  static {
    defaultApp = null;
    try {
      defaultApp =
          FirebaseApp.initializeApp(
              new FirebaseOptions.Builder()
                  .setCredentials(SecretHandler.getFirebaseCred())
                  .build());
    } catch (IOException e) {
      LOGGER.warning(e.getMessage());
    }
  }

  /**
   * Returns the uID of a user based off the token provided by the client.
   *
   * @param userToken String token used to authenticate the user
   * @return String uID returned after authentication
   */
  public static String authenticateUser(String userToken) {
    FirebaseAuth defaultAuth = FirebaseAuth.getInstance(defaultApp);

    FirebaseToken decodedToken = null;
    String uid = "";
    try {
      decodedToken = FirebaseAuth.getInstance().verifyIdToken(userToken);
      uid = decodedToken.getUid();
    } catch (FirebaseAuthException e) {
      LOGGER.warning(e.getMessage());
    }

    return uid;
  }

  /**
   * Returns the logged in state of the user based off the token returned by the client.
   *
   * @param userToken String token used to check if logged in
   * @return Boolean condition
   */
  public static boolean isUserLoggedIn(String userToken) {
    return !userToken.equals("");
  }
}
