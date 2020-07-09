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

import com.google.cloud.secretmanager.v1.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretVersionName;
import java.io.IOException;

public class SecretHandler {
  private static final String PROJECT_ID = "unitebystep";
  private static final String API_KEY_ID = "api-key";
  private static final String VERSION_ID = "1";

  public static String getApiKey() throws IOException {
    try (SecretManagerServiceClient client = SecretManagerServiceClient.create()) {
      SecretVersionName secretVersionName =
          SecretVersionName.of(PROJECT_ID, API_KEY_ID, VERSION_ID);

      AccessSecretVersionResponse response = client.accessSecretVersion(secretVersionName);
      String result = response.getPayload().getData().toStringUtf8();
      client.close();

      return result;
    }
  }
}
