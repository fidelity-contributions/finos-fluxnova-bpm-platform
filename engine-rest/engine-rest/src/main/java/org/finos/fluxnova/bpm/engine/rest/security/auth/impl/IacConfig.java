/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.finos.fluxnova.bpm.engine.rest.security.auth.impl;


public final class IacConfig {
  
  private String baseUrl = "https://api-dev-aws.fmr.com/dap-access-control/v1";
  private String appName = "dap";
  
  private String serviceAccountUsername;
  private String serviceAccountPassword;
  private String esgOauthUrl = "https://esg-qa-oauth2-internal.fmr.com/as/resourceOwner";
  
  private String fidApiPlatformClientId;
  private String fidApiPlatformClientSecret;
  
  private int connectTimeout = 5000;
  private int readTimeout = 10000;
  
  public IacConfig() {
  }
  
  public IacConfig(String baseUrl, String serviceAccountUsername, String serviceAccountPassword, 
                   String esgOauthUrl, String appName) {
    this.baseUrl = baseUrl;
    this.serviceAccountUsername = serviceAccountUsername;
    this.serviceAccountPassword = serviceAccountPassword;
    this.esgOauthUrl = esgOauthUrl;
    this.appName = appName;
  }


  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public String getAppName() {
    return appName;
  }

  public void setAppName(String appName) {
    this.appName = appName;
  }

  public String getServiceAccountUsername() {
    return serviceAccountUsername;
  }

  public void setServiceAccountUsername(String serviceAccountUsername) {
    this.serviceAccountUsername = serviceAccountUsername;
  }

  public String getServiceAccountPassword() {
    return serviceAccountPassword;
  }

  public void setServiceAccountPassword(String serviceAccountPassword) {
    this.serviceAccountPassword = serviceAccountPassword;
  }

  public String getEsgOauthUrl() {
    return esgOauthUrl;
  }

  public void setEsgOauthUrl(String esgOauthUrl) {
    this.esgOauthUrl = esgOauthUrl;
  }

  public int getConnectTimeout() {
    return connectTimeout;
  }

  public void setConnectTimeout(int connectTimeout) {
    this.connectTimeout = connectTimeout;
  }

  public int getReadTimeout() {
    return readTimeout;
  }

  public void setReadTimeout(int readTimeout) {
    this.readTimeout = readTimeout;
  }

  public String getFidApiPlatformClientId() {
    return fidApiPlatformClientId;
  }

  public void setFidApiPlatformClientId(String fidApiPlatformClientId) {
    this.fidApiPlatformClientId = fidApiPlatformClientId;
  }

  public String getFidApiPlatformClientSecret() {
    return fidApiPlatformClientSecret;
  }

  public void setFidApiPlatformClientSecret(String fidApiPlatformClientSecret) {
    this.fidApiPlatformClientSecret = fidApiPlatformClientSecret;
  }

  public void validate() {
    if (baseUrl == null || baseUrl.isBlank()) {
      throw new IllegalStateException("IAC base URL is required");
    }
    if (serviceAccountUsername == null || serviceAccountUsername.isBlank()) {
      throw new IllegalStateException("Service account username is required");
    }
    if (serviceAccountPassword == null || serviceAccountPassword.isBlank()) {
      throw new IllegalStateException("Service account password is required");
    }
    if (esgOauthUrl == null || esgOauthUrl.isBlank()) {
      throw new IllegalStateException("ESG OAuth URL is required");
    }
  }
  

  public boolean hasFidelityApiPlatformCredentials() {
    return fidApiPlatformClientId != null && !fidApiPlatformClientId.isBlank()
        && fidApiPlatformClientSecret != null && !fidApiPlatformClientSecret.isBlank();
  }
}
