/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.gcloud;

import com.google.appengine.api.NamespaceManager;
import com.google.appengine.api.appidentity.AppIdentityServiceFactory;
import com.google.auth.oauth2.AccessToken;

import java.io.IOException;
import java.util.Collection;

/**
 * Helper for accessing App Engine runtime information, such as project ID and credentials.
 */
public class AppEngineHelper {

  /**
   * Get the project ID using the App Identity service.
   */
  static String getProjectId() {
    try {
      String serviceAccountName =
          AppIdentityServiceFactory.getAppIdentityService().getServiceAccountName();
      return serviceAccountName.substring(0, serviceAccountName.indexOf('@'));
    } catch (Exception ignore) {
      // return null if can't determine
      return null;
    }
  }

  /**
   * Refresh the access token by getting it from the App Identity service.
   */
  static AccessToken refreshAccessToken(Collection<String> scopes) throws IOException {
    try {
      return new AccessToken(
          AppIdentityServiceFactory.getAppIdentityService().getAccessToken(scopes).getAccessToken(),
          null);
    } catch (Exception ex) {
      throw new IOException("Could not get the access token via App Identity Service.", ex);
    }
  }

  /**
   * Get the default Datastore namespace.
   */
  public static String defaultNamespace() {
    try {
      String namespace = NamespaceManager.get();
      return namespace == null || namespace.isEmpty() ? null : namespace;
    } catch (Exception ignore) {
      // return null (Datastore default namespace) if could not automatically determine
      return null;
    }
  }
}
