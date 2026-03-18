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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.finos.fluxnova.bpm.engine.ProcessEngine;
import org.finos.fluxnova.bpm.engine.rest.security.auth.AuthenticationProvider;
import org.finos.fluxnova.bpm.engine.rest.security.auth.AuthenticationResult;


public class JwtBearerAuthenticationProvider implements AuthenticationProvider {

  private static final Logger LOG = Logger.getLogger(JwtBearerAuthenticationProvider.class.getName());

  public static final String DEFAULT_EJWT_HEADER = "FID-ENT-IDENTITY-JWT";
  public static final String DEFAULT_IAC_HEADER = "x-fid-auth";
  public static final String DEFAULT_TENANT_HEADER = "x-fid-tenant";
  public static final String AUTHORIZATION_HEADER = "Authorization";
  public static final String BEARER_PREFIX = "Bearer ";

  public static final String DEFAULT_EJWT_CLAIM = "fid_id";
  public static final String DEFAULT_IAC_CLAIM = "sub";
  public static final String DEFAULT_BEARER_CLAIM = "sub";

  private final TokenHelper tokenHelper;

  private IacAuthorizationService iacService;
  private boolean iacEnabled = false;
  private boolean iacInitialized = false;

  private String ejwtHeader;
  private String iacHeader;
  private String tenantHeader;
  private List<String> customHeaders;

  // Configurable claim names
  private String ejwtClaim;
  private String iacClaim;
  private String bearerClaim;


  public JwtBearerAuthenticationProvider() {
    this.tokenHelper = new TokenHelper();
    this.ejwtHeader = DEFAULT_EJWT_HEADER;
    this.iacHeader = DEFAULT_IAC_HEADER;
    this.tenantHeader = DEFAULT_TENANT_HEADER;
    this.ejwtClaim = DEFAULT_EJWT_CLAIM;
    this.iacClaim = DEFAULT_IAC_CLAIM;
    this.bearerClaim = DEFAULT_BEARER_CLAIM;
    this.customHeaders = new ArrayList<>();
  }


  public JwtBearerAuthenticationProvider(String ejwtHeader, String iacHeader,
                                        String tenantHeader, String ejwtClaim, String iacClaim) {
    this.tokenHelper = new TokenHelper();
    this.ejwtHeader = ejwtHeader != null ? ejwtHeader : DEFAULT_EJWT_HEADER;
    this.iacHeader = iacHeader != null ? iacHeader : DEFAULT_IAC_HEADER;
    this.tenantHeader = tenantHeader != null ? tenantHeader : DEFAULT_TENANT_HEADER;
    this.ejwtClaim = ejwtClaim != null ? ejwtClaim : DEFAULT_EJWT_CLAIM;
    this.iacClaim = iacClaim != null ? iacClaim : DEFAULT_IAC_CLAIM;
    this.bearerClaim = DEFAULT_BEARER_CLAIM;
    this.customHeaders = new ArrayList<>();
  }

  @Override
  public AuthenticationResult extractAuthenticatedUser(HttpServletRequest request, ProcessEngine engine) {
    initializeFromRequest(request, engine);

    UserTokenInfo tokenInfo = extractUserTokenInfoFromRequest(request);

    if (tokenInfo.getToken() == null || tokenInfo.getUsername() == null) {
      LOG.log(Level.FINE, "No valid token or username found in request");
      return AuthenticationResult.unsuccessful();
    }

    LOG.log(Level.INFO, "JWT authentication successful for user: {0}, tokenType: {1}", 
        new Object[]{tokenInfo.getUsername(), tokenInfo.getTokenType()});

    AuthenticationResult result = AuthenticationResult.successful(tokenInfo.getUsername());

    if (tokenInfo.getTenantId() != null) {
      List<String> tenants = new ArrayList<>();
      tenants.add(tokenInfo.getTenantId());
      result.setTenants(tenants);
    }

    if (iacEnabled && iacService != null) {
      try {
        LOG.log(Level.FINE, "Calling IAC service for user: {0}, tenant: {1}", 
            new Object[]{tokenInfo.getUsername(), tokenInfo.getTenantId()});

        IacAuthorizationService.AuthorizationResult iacResult =
            iacService.authorizePermissionsForTenant(
                tokenInfo.getToken(),
                null,
                tokenInfo.getTenantId(),
                tokenInfo.getTokenType().name(),
                generateTrackingId(request)
            );

        if (!iacResult.getTenantIds().isEmpty()) {
          List<String> groups = new ArrayList<>(iacResult.getTenantIds());
          result.setGroups(groups);
          result.setTenants(new ArrayList<>(iacResult.getTenantIds()));

          LOG.log(Level.INFO, "IAC authorization successful: tenants={0}", iacResult.getTenantIds());
        }
      } catch (Exception e) {
        LOG.log(Level.WARNING, "IAC authorization failed, continuing with basic auth: " + e.getMessage(), e);
      }
    } else {
      result.setGroups(new ArrayList<>());
    }

    return result;
  }

  @Override
  public void augmentResponseByAuthenticationChallenge(HttpServletResponse response, ProcessEngine engine) {
    response.setHeader("WWW-Authenticate", "Bearer realm=\"FluxNova BPM\"");
  }


  protected UserTokenInfo extractUserTokenInfoFromRequest(HttpServletRequest request) {
    UserTokenInfo context = new UserTokenInfo();

    String tenantId = request.getHeader(tenantHeader);
    context.setTenantId(tenantId);

    String iacToken = request.getHeader(iacHeader);
    if (iacToken != null && !iacToken.isEmpty()) {
      context.setToken(iacToken);
      context.setTokenType(TokenType.IAC);
      String username = tokenHelper.extractClaim(iacToken, iacClaim);
      context.setUsername(username);
      LOG.log(Level.FINE, "Extracted IAC token from header: {0}", iacHeader);
      return context;
    }

    String ejwtToken = request.getHeader(ejwtHeader);
    if (ejwtToken != null && !ejwtToken.isEmpty()) {
      context.setToken(ejwtToken);
      context.setTokenType(TokenType.EJWT);
      String username = tokenHelper.extractClaim(ejwtToken, ejwtClaim);
      context.setUsername(username);
      LOG.log(Level.FINE, "Extracted EJWT token from header: {0}", ejwtHeader);
      return context;
    }

    String authHeader = request.getHeader(AUTHORIZATION_HEADER);
    if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
      String bearerToken = authHeader.substring(BEARER_PREFIX.length()).trim();
      if (!bearerToken.isEmpty()) {
        context.setToken(bearerToken);
        context.setTokenType(TokenType.IAC);
        String username = tokenHelper.extractClaim(bearerToken, bearerClaim);
        context.setUsername(username);
        LOG.log(Level.FINE, "Extracted Bearer token from Authorization header");
        return context;
      }
    }

    for (String customHeader : customHeaders) {
      String customToken = request.getHeader(customHeader);
      if (customToken != null && !customToken.isEmpty()) {
        context.setToken(customToken);
        context.setTokenType(TokenType.EJWT);
        String username = tokenHelper.extractClaim(customToken, bearerClaim);
        context.setUsername(username);
        LOG.log(Level.FINE, "Extracted token from custom header: {0}", customHeader);
        return context;
      }
    }

    LOG.log(Level.FINE, "No token found in any configured headers");
    return context;
  }


  private void initializeFromRequest(HttpServletRequest request, ProcessEngine engine) {
    if (iacInitialized) {
      return;
    }

    this.ejwtHeader = getConfigParameter(request, engine, "ejwt-header", DEFAULT_EJWT_HEADER);
    this.iacHeader = getConfigParameter(request, engine, "iac-header", DEFAULT_IAC_HEADER);
    this.tenantHeader = getConfigParameter(request, engine, "tenant-header", DEFAULT_TENANT_HEADER);
    this.ejwtClaim = getConfigParameter(request, engine, "ejwt-claim", DEFAULT_EJWT_CLAIM);
    this.iacClaim = getConfigParameter(request, engine, "iac-claim", DEFAULT_IAC_CLAIM);
    this.bearerClaim = getConfigParameter(request, engine, "bearer-claim", DEFAULT_BEARER_CLAIM);

    String customHeadersParam = getConfigParameter(request, engine, "custom-headers", null);
    if (customHeadersParam != null && !customHeadersParam.isEmpty()) {
      this.customHeaders = Arrays.asList(customHeadersParam.split(","));
    }

    LOG.info("JWT authentication initialized: ejwtHeader=" + ejwtHeader + 
            ", iacHeader=" + iacHeader + ", ejwtClaim=" + ejwtClaim + 
            ", iacClaim=" + iacClaim);

    String iacEnabledParam = getConfigParameter(request, engine, "iac-enabled", "false");
    this.iacEnabled = Boolean.parseBoolean(iacEnabledParam);

    LOG.info("IAC enabled parameter: " + iacEnabledParam + ", parsed as: " + this.iacEnabled);

    if (iacEnabled) {
      try {
        IacConfig iacConfig = new IacConfig();

        String baseUrl = getConfigParameter(request, engine, "iac-base-url", IacAuthorizationService.DEFAULT_IAC_BASE_URL);
        String appName = getConfigParameter(request, engine, "iac-app-name", "fluxnova-bpm");
        String username = getConfigParameter(request, engine, "iac-service-account-username", null);
        String password = getConfigParameter(request, engine, "iac-service-account-password", null);
        String esgOauthUrl = getConfigParameter(request, engine, "iac-esg-oauth-url", null);
        String clientid = getConfigParameter(request, engine, "fid-api-platform-client-id", null);
        String clientSecret = getConfigParameter(request, engine, "fid-api-platform-client-secret", null);


        LOG.info("IAC config read: baseUrl=" + baseUrl +
                ", appName=" + appName + 
                ", username=" + (username != null ? "***SET***" : "NULL") + 
                ", password=" + (password != null ? "***SET***" : "NULL") + 
                ", esgOauthUrl=" + (esgOauthUrl != null ? esgOauthUrl : "NULL"));

        iacConfig.setBaseUrl(baseUrl);
        iacConfig.setAppName(appName);
        iacConfig.setServiceAccountUsername(username);
        iacConfig.setServiceAccountPassword(password);
        iacConfig.setEsgOauthUrl(esgOauthUrl);
        iacConfig.setFidApiPlatformClientId(clientid);
        iacConfig.setFidApiPlatformClientSecret(clientSecret);
        String connectTimeout = getConfigParameter(request, engine, "iac-timeout-connect", "5000");
        String readTimeout = getConfigParameter(request, engine, "iac-timeout-read", "10000");
        iacConfig.setConnectTimeout(Integer.parseInt(connectTimeout));
        iacConfig.setReadTimeout(Integer.parseInt(readTimeout));

        if (iacConfig.getServiceAccountUsername() == null ||
            iacConfig.getServiceAccountPassword() == null ||
            iacConfig.getEsgOauthUrl() == null) {
          LOG.warning("IAC enabled but missing required configuration: " +
                     "username=" + (iacConfig.getServiceAccountUsername() == null ? "MISSING" : "OK") +
                     ", password=" + (iacConfig.getServiceAccountPassword() == null ? "MISSING" : "OK") +
                     ", esgOauthUrl=" + (iacConfig.getEsgOauthUrl() == null ? "MISSING" : "OK") +
                     ". IAC integration disabled.");
          this.iacEnabled = false;
        } else {
          this.iacService = new IacAuthorizationService(iacConfig);
          LOG.info("IAC service initialized: baseUrl=" + iacConfig.getBaseUrl() + 
                  ", appName=" + iacConfig.getAppName());
        }
      } catch (Exception e) {
        LOG.log(Level.SEVERE, "Failed to initialize IAC service: " + e.getMessage(), e);
        this.iacEnabled = false;
      }
    } else {
      LOG.info("IAC integration is disabled (iac-enabled=" + iacEnabledParam + ")");
    }

    iacInitialized = true;
  }


  private String generateTrackingId(HttpServletRequest request) {
    String requestId = request.getHeader("x-request-id");
    if (requestId != null) {
      return requestId;
    }
    return "fluxnova-" + System.currentTimeMillis() + "-" + Thread.currentThread().hashCode();
  }


  private String getConfigParameter(HttpServletRequest request, ProcessEngine engine, String paramName, String defaultValue) {
    String value = request.getServletContext().getInitParameter(paramName);
    if (value != null && !value.trim().isEmpty()) {
      LOG.fine("Config [" + paramName + "] from servlet context: " + (paramName.contains("password") ? "***" : value));
      return value.trim();
    }

    String envName = paramName.toUpperCase().replace('-', '_');
    value = System.getenv(envName);
    if (value != null && !value.trim().isEmpty()) {
      LOG.fine("Config [" + paramName + "] from env var [" + envName + "]: " + (paramName.contains("password") ? "***" : value));
      return value.trim();
    }

    value = System.getProperty(paramName);
    if (value != null && !value.trim().isEmpty()) {
      LOG.fine("Config [" + paramName + "] from system property: " + (paramName.contains("password") ? "***" : value));
      return value.trim();
    }

    LOG.fine("Config [" + paramName + "] not found, using default: " + defaultValue);
    return defaultValue;
  }

}
