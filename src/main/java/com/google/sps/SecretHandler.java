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

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.secretmanager.v1.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretVersionName;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class SecretHandler {
  private static final String PROJECT_ID = "unitebystep";
  private static final String API_KEY_ID = "api-key";
  private static final String FIREBASE_KEY_ID = "firebase-key";
  private static final String VERSION_ID = "1";

  /**
   * Returns a String containing the API key. Throws an exception if the Secret manager client is
   * unable to be created.
   */
  public static String getApiKey() throws IOException {
    SecretManagerServiceClient client = null;
    String result = "";
    try {
      client = SecretManagerServiceClient.create();
      SecretVersionName secretVersionName =
          SecretVersionName.of(PROJECT_ID, API_KEY_ID, VERSION_ID);

      AccessSecretVersionResponse response = client.accessSecretVersion(secretVersionName);
      result = response.getPayload().getData().toStringUtf8();

    } catch (IOException e) {
      throw new IOException("failed to create secret manager service client");
    } finally {
      if (client != null) {
        client.close();
      }
      return result;
    }
  }

  /**
   * Returns a GoogleCredentials object containing the Firebase Admin key. Throws an exception if
   * the Secret manager client is unable to be created.
   */
  public static GoogleCredentials getFirebaseCred() throws IOException {
    SecretManagerServiceClient client = null;
    String result = "";
    GoogleCredentials cred = null;
    try {
      client = SecretManagerServiceClient.create();
      SecretVersionName secretVersionName =
          SecretVersionName.of(PROJECT_ID, FIREBASE_KEY_ID, VERSION_ID);

      AccessSecretVersionResponse response = client.accessSecretVersion(secretVersionName);
      result = response.getPayload().getData().toStringUtf8();
    } catch (IOException e) {
      throw new IOException("failed to create secret manager service client");
    } finally {
      if (client != null) {
        client.close();
      }
    }
    InputStream stream = new ByteArrayInputStream(result.getBytes());
    cred = GoogleCredentials.fromStream(stream);
    return cred;
  }
}
