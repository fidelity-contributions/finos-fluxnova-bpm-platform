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

import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public final class IacAuthorizationService implements AutoCloseable {

  private static final Logger LOG = Logger.getLogger(IacAuthorizationService.class.getName());
  
  public static final String DEFAULT_IAC_BASE_URL = "https://api-dev-aws.fmr.com/dap-access-control/v1";
  public static final int DEFAULT_CONNECT_TIMEOUT = 5000;
  public static final int DEFAULT_READ_TIMEOUT = 10000;

  private final IacConfig config;
  private final IacTokenService tokenService;
  private final boolean enabled;
  private final boolean iacClientAvailable;


  public IacAuthorizationService(IacConfig config) {
    validateConfig(config);
    
    this.config = config;
    this.tokenService = new IacTokenService(config);
    this.enabled = true;
    this.iacClientAvailable = isIacClientAvailable();
    
    logInitializationStatus();
  }

  public IacAuthorizationService(String baseUrl, String appName, int connectTimeout, int readTimeout) {
    LOG.log(Level.WARNING, "Using deprecated constructor. Service account credentials not provided. " +
                           "ESG OAuth authentication will not be available.");
    
    IacConfig config = new IacConfig();
    config.setBaseUrl(baseUrl != null ? baseUrl : DEFAULT_IAC_BASE_URL);
    config.setAppName(appName);
    config.setConnectTimeout(connectTimeout);
    config.setReadTimeout(readTimeout);
    
    this.config = config;
    this.tokenService = null;
    this.enabled = false;
    this.iacClientAvailable = isIacClientAvailable();
    
    LOG.log(Level.WARNING, "IAC service is disabled due to missing service account credentials");
  }


  public IacAuthorizationService(String baseUrl, String appName) {
    this(baseUrl, appName, DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT);
  }


  public AuthorizationResult authorizePermissionsForTenant(
      String userJwtToken, String appName, String tenant, String tokenType, String trackingId) {
    
    LOG.log(Level.FINE, "Authenticating user for tenant: {0} (authorization checks disabled)", tenant);
    
    AuthorizationResult validationResult = validatePrerequisites();
    if (validationResult != null) {
      // TODO: AUTHORIZATION - When ready for authorization, return validationResult here to enforce prerequisites
      LOG.log(Level.WARNING, "IAC prerequisites not met - allowing access (authentication-only mode)");
      return emptyResult();
    }
    
    String effectiveAppName = appName != null ? appName : config.getAppName();
    
    String iacServiceToken = acquireIacServiceToken(effectiveAppName);
    if (iacServiceToken == null) {
      // TODO: AUTHORIZATION - When ready for authorization, return emptyResult() here to block access
      LOG.log(Level.WARNING, "Failed to acquire IAC token - allowing access (authentication-only mode)");
      return emptyResult();
    }
    
    return callIacApi(iacServiceToken, userJwtToken, effectiveAppName, tenant, tokenType, trackingId);
  }
  
  private void validateConfig(IacConfig config) {
    if (config == null) {
      throw new IllegalArgumentException("IacConfig is required");
    }
  }
  
  private void logInitializationStatus() {
    if (!iacClientAvailable) {
      LOG.log(Level.WARNING, "IAC client library not available. " +
          "Add dependency: com.fmr.AP134016.dap-bpm:iac-client:2.0.0");
    } else {
      LOG.log(Level.INFO, "IAC authorization service initialized");
    }
  }
  
  private AuthorizationResult validatePrerequisites() {
    if (!enabled) {
      LOG.warning("IAC integration is disabled");
      return emptyResult();
    }
    
    if (!iacClientAvailable) {
      LOG.severe("IAC client library is not available");
      return emptyResult();
    }
    
    if (tokenService == null) {
      LOG.severe("IAC token service not configured");
      return emptyResult();
    }
    
    return null;
  }
  
  private String acquireIacServiceToken(String appName) {
    try {
      return tokenService.getToken(appName);
    } catch (Exception e) {
      LOG.log(Level.SEVERE, "Failed to acquire IAC service token", e);
      return null;
    }
  }
  
  private AuthorizationResult emptyResult() {
    return new AuthorizationResult(Collections.emptyMap(), Collections.emptySet());
  }


  private AuthorizationResult callIacApi(
      String iacServiceToken, String userJwtToken, String appName,
      String tenant, String tokenType, String trackingId) {
    
    try {
      Class<?> apiClientClass = Class.forName("com.fmr.iac.client.invoker.ApiClient");
      Class<?> authorizeApiClass = Class.forName("com.fmr.iac.client.api.AuthorizeApi");
      Class<?> tokenPermissionsClass = Class.forName("com.fmr.iac.client.model.TokenPermissions");
      
      Object apiClient = apiClientClass.getDeclaredConstructor().newInstance();
      
      apiClientClass.getMethod("setBasePath", String.class)
          .invoke(apiClient, config.getBaseUrl());
      
      String headerName = "EJWT".equals(tokenType) ? "FID-ENT-IDENTITY-JWT" : "x-fid-auth";
      apiClientClass.getMethod("addDefaultHeader", String.class, String.class)
          .invoke(apiClient, headerName, userJwtToken);
      
      LOG.log(Level.FINE, "Added user token as header: {0}", headerName);
      
      if (tokenService != null) {
        try {
          Field authClientField = tokenService.getClass().getDeclaredField("fidApiPlatformAuthClient");
          authClientField.setAccessible(true);
          Object fidApiAuthClient = authClientField.get(tokenService);
          
          if (fidApiAuthClient != null) {
            String fidApiToken = (String) fidApiAuthClient.getClass()
                .getMethod("getTokenForTargetApi", String.class)
                .invoke(fidApiAuthClient, config.getBaseUrl());
            
            if (fidApiToken != null) {
              apiClientClass.getMethod("setBearerToken", String.class)
                  .invoke(apiClient, fidApiToken);
              LOG.log(Level.INFO, "Added Fidelity API Platform token for gateway authentication");
            } else {
              LOG.log(Level.WARNING, "Could not get Fidelity API Platform token");
            }
          } else {
            LOG.log(Level.WARNING, "FidelityApiPlatformAuthClient is null - gateway auth may fail");
          }
        } catch (Exception e) {
          LOG.log(Level.WARNING, "Could not add Fidelity API Platform token: {0}", e.getMessage());
        }
      }
      
      Object authorizeApi = authorizeApiClass.getDeclaredConstructor(apiClientClass)
          .newInstance(apiClient);
      
      List<Object> response = (List<Object>) authorizeApiClass
          .getMethod("authorizeControllerGetUserPermissions", String.class, String.class, String.class, String.class)
          .invoke(authorizeApi, trackingId, null, null, appName);
      
      Map<String, SortedSet<String>> tenantPermissions = extractPermissionsForTenant(response, tenant, tokenPermissionsClass);
      
      Set<String> tenantIds = extractTenantIds(response, tokenPermissionsClass);
      
      LOG.log(Level.INFO, "IAC authentication successful: user has {0} resource permissions for tenant {1} (authorization checks disabled)", 
          new Object[]{tenantPermissions.size(), tenant});
      
      return new AuthorizationResult(tenantPermissions, tenantIds);
      
    } catch (ClassNotFoundException e) {
      LOG.log(Level.SEVERE, "IAC client classes not found - allowing access (authentication-only mode). " +
          "Ensure iac-client dependency is available.", e);
      return new AuthorizationResult(Collections.emptyMap(), Collections.emptySet());
    } catch (Exception e) {
      LOG.log(Level.SEVERE, "Failed to call IAC service - allowing access (authentication-only mode)", e);
      return new AuthorizationResult(Collections.emptyMap(), Collections.emptySet());
    }
  }

  public void clearTokenCache() {
    if (tokenService != null) {
      tokenService.clearCache();
    }
  }
  
  @Override
  public void close() {
    if (tokenService != null) {
      tokenService.close();
    }
  }

  private Map<String, SortedSet<String>> extractPermissionsForTenant(
      List<Object> response, String tenant, Class<?> tokenPermissionsClass) {
    
    if (response == null || response.isEmpty()) {
      return Collections.emptyMap();
    }

    Map<String, SortedSet<String>> result = new HashMap<>();
    
    try {
      for (Object tp : response) {
        if (!permissionAppliesToTenant(tp, tenant, tokenPermissionsClass)) {
          continue;
        }
        Object rolesObj = tokenPermissionsClass.getMethod("getRoles").invoke(tp);
        if (rolesObj == null) {
          continue;
        }
        
        List<Object> roles = (List<Object>) rolesObj;
        
        for (Object role : roles) {
          if (role == null) {
            continue;
          }
          
          Object permissionsObj = role.getClass().getMethod("getPermissions").invoke(role);
          if (permissionsObj == null) {
            continue;
          }
          
          List<Object> permissions = (List<Object>) permissionsObj;
          
          for (Object permission : permissions) {
            if (permission == null) {
              continue;
            }
            
            String resourceName = (String) permission.getClass().getMethod("getResourceName").invoke(permission);
            Object actionsObj = permission.getClass().getMethod("getActions").invoke(permission);
            
            if (resourceName != null && actionsObj != null) {
              List<String> actions = (List<String>) actionsObj;
              
              SortedSet<String> resourceActions = result.computeIfAbsent(resourceName, k -> new TreeSet<>());
              resourceActions.addAll(actions);
            }
          }
        }
      }
      
      LOG.log(Level.INFO, "Extracted {0} resource permissions for tenant {1}", 
          new Object[]{result.size(), tenant});
      
    } catch (Exception e) {
      LOG.log(Level.SEVERE, "Failed to extract permissions from IAC response", e);
    }

    return result;
  }
  

  private boolean permissionAppliesToTenant(Object tp, String tenant, Class<?> tokenPermissionsClass) {
    try {
      String securityRealmId = (String) tokenPermissionsClass.getMethod("getSecurityRealmId").invoke(tp);
      if ("sr-64xm9z04lc-g".equalsIgnoreCase(securityRealmId)) {
        LOG.log(Level.FINE, "Permission is for global security realm - applies to all tenants");
        return true;
      }
      
      Object tenantsObj = tokenPermissionsClass.getMethod("getTenants").invoke(tp);
      if (tenantsObj != null) {
        List<String> tenants = (List<String>) tenantsObj;
        boolean hasTenant = tenants.stream().anyMatch(t -> t != null && t.equalsIgnoreCase(tenant));
        if (hasTenant) {
          LOG.log(Level.FINE, "Permission has tenant {0}", tenant);
        }
        return hasTenant;
      }
    } catch (Exception e) {
      LOG.log(Level.WARNING, "Failed to check if permission applies to tenant", e);
    }
    return false;
  }

  private Set<String> extractTenantIds(List<Object> response, Class<?> tokenPermissionsClass) {
    if (response == null || response.isEmpty()) {
      return Collections.emptySet();
    }

    Set<String> tenantIds = new HashSet<>();
    
    try {
      for (Object tp : response) {
        @SuppressWarnings("unchecked")
        List<String> tenants = (List<String>) tokenPermissionsClass.getMethod("getTenants").invoke(tp);
        
        if (tenants != null) {
          tenantIds.addAll(tenants);
        }
      }
    } catch (Exception e) {
      LOG.log(Level.SEVERE, "Failed to extract tenant IDs from IAC response", e);
    }

    return tenantIds;
  }


  private boolean isIacClientAvailable() {
    try {
      Class.forName("com.fmr.iac.client.invoker.ApiClient");
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }


  public static class AuthorizationResult {
    private final Map<String, SortedSet<String>> permissions;
    private final Set<String> tenantIds;

    public AuthorizationResult(Map<String, SortedSet<String>> permissions, Set<String> tenantIds) {
      this.permissions = permissions != null ? permissions : Collections.emptyMap();
      this.tenantIds = tenantIds != null ? tenantIds : Collections.emptySet();
    }

    public Map<String, SortedSet<String>> getPermissions() {
      return permissions;
    }

    public Set<String> getTenantIds() {
      return tenantIds;
    }


    public Set<String> getPermissionsForTenant(String tenant) {
      return permissions.getOrDefault(tenant, Collections.emptySortedSet());
    }


    public boolean hasPermission(String tenant, String permission) {
      Set<String> tenantPerms = getPermissionsForTenant(tenant);
      return tenantPerms.contains(permission);
    }
  }
}
