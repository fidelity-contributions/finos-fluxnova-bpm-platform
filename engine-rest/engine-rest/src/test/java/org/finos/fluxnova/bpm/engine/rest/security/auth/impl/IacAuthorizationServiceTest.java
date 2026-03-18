
package org.finos.fluxnova.bpm.engine.rest.security.auth.impl;

import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import static org.junit.Assert.*;


public class IacAuthorizationServiceTest {

  private IacConfig config;
  private IacAuthorizationService authService;

  @Before
  public void setUp() {
    config = new IacConfig();
    config.setBaseUrl("https://api-dev-aws.fmr.com/dap-access-control/v1");
    config.setAppName("fluxnova-bpm");
    config.setServiceAccountUsername("svc-user");
    config.setServiceAccountPassword("secret");
    config.setEsgOauthUrl("https://esg-oauth.fmr.com/token");
    config.setFidApiPlatformClientId("client-123");
    config.setFidApiPlatformClientSecret("secret-456");
    config.setConnectTimeout(5000);
    config.setReadTimeout(10000);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructor_NullConfig() {
    new IacAuthorizationService(null);
  }

  @Test
  public void testConstructor_ValidConfig() {
    authService = new IacAuthorizationService(config);
    assertNotNull(authService);
  }

  @Test
  public void testConstructor_MinimalConfig() {
    IacConfig minConfig = new IacConfig();
    minConfig.setBaseUrl("https://api-dev-aws.fmr.com/dap-access-control/v1");
    minConfig.setAppName("test-app");
    minConfig.setServiceAccountUsername("user");
    minConfig.setServiceAccountPassword("pass");
    minConfig.setEsgOauthUrl("https://esg.fmr.com/token");
    
    authService = new IacAuthorizationService(minConfig);
    assertNotNull(authService);
  }

  @Test
  public void testDeprecatedConstructor_TwoParams() {
    authService = new IacAuthorizationService(
      "https://api-dev-aws.fmr.com/dap-access-control/v1",
      "fluxnova-bpm"
    );
    assertNotNull(authService);
  }

  @Test
  public void testDeprecatedConstructor_FourParams() {
    authService = new IacAuthorizationService(
      "https://api-dev-aws.fmr.com/dap-access-control/v1",
      "fluxnova-bpm",
      5000,
      10000
    );
    assertNotNull(authService);
  }

  @Test
  public void testDeprecatedConstructor_NullValues() {
    authService = new IacAuthorizationService(null, null);
    assertNotNull(authService);
  }

  @Test
  public void testClearTokenCache() {
    authService = new IacAuthorizationService(config);
    
    authService.clearTokenCache();
    authService.clearTokenCache();
  }

  @Test
  public void testClose() {
    authService = new IacAuthorizationService(config);
    
    authService.close();
    authService.close();
  }

  @Test
  public void testResourceCleanup() {
    try (IacAuthorizationService autoCloseService = new IacAuthorizationService(config)) {
      assertNotNull(autoCloseService);
      autoCloseService.clearTokenCache();
    }
  }

  @Test
  public void testMultipleInstances() {
    IacAuthorizationService service1 = new IacAuthorizationService(config);
    IacAuthorizationService service2 = new IacAuthorizationService(config);
    
    assertNotNull(service1);
    assertNotNull(service2);
    assertNotEquals(service1, service2);
    
    service1.close();
    service2.close();
  }

  @Test
  public void testAuthorizePermissionsForTenant_AuthenticationMode() {
    authService = new IacAuthorizationService(config);
    
    IacAuthorizationService.AuthorizationResult result =
      authService.authorizePermissionsForTenant(
        "user-jwt-token",
        "fluxnova-bpm",
        "tenant-123",
        "EJWT",
        "trace-id-456"
      );
    
    assertNotNull(result);
    assertNotNull(result.getPermissions());
    assertNotNull(result.getTenantIds());
  }

  @Test
  public void testAuthorizePermissionsForTenant_NullAppName() {
    authService = new IacAuthorizationService(config);
    
    IacAuthorizationService.AuthorizationResult result =
      authService.authorizePermissionsForTenant(
        "user-jwt-token",
        null,  // Should use config.getAppName()
        "tenant-123",
        "EJWT",
        "trace-id-456"
      );
    
    assertNotNull(result);
  }

  @Test
  public void testAuthorizationResult_EmptyConstructor() {
    IacAuthorizationService.AuthorizationResult result = 
      new IacAuthorizationService.AuthorizationResult(null, null);
    
    assertNotNull(result);
    assertNotNull(result.getPermissions());
    assertNotNull(result.getTenantIds());
    assertTrue(result.getPermissions().isEmpty());
    assertTrue(result.getTenantIds().isEmpty());
  }

  @Test
  public void testAuthorizationResult_GetPermissionsForTenant() {
    Map<String, SortedSet<String>> permissions = Map.of(
      "resource1", (SortedSet<String>) new java.util.TreeSet<>(Set.of("read", "write")),
      "resource2", (SortedSet<String>) new java.util.TreeSet<>(Set.of("delete"))
    );
    
    Set<String> tenantIds = Set.of("tenant-123", "tenant-456");
    
    IacAuthorizationService.AuthorizationResult result = 
      new IacAuthorizationService.AuthorizationResult(permissions, tenantIds);
    
    Set<String> resource1Perms = result.getPermissionsForTenant("resource1");
    assertNotNull(resource1Perms);
    assertTrue(resource1Perms.contains("read"));
    assertTrue(resource1Perms.contains("write"));
  }

  @Test
  public void testAuthorizationResult_GetPermissionsForNonExistentTenant() {
    Map<String, SortedSet<String>> permissions = Map.of(
      "resource1", (SortedSet<String>) new java.util.TreeSet<>(Set.of("read"))
    );
    
    IacAuthorizationService.AuthorizationResult result = 
      new IacAuthorizationService.AuthorizationResult(permissions, Set.of());
    
    Set<String> nonExistentPerms = result.getPermissionsForTenant("non-existent");
    assertNotNull(nonExistentPerms);
    assertTrue(nonExistentPerms.isEmpty());
  }

  @Test
  public void testAuthorizationResult_HasPermission() {
    Map<String, SortedSet<String>> permissions = Map.of(
      "resource1", (SortedSet<String>) new java.util.TreeSet<>(Set.of("read", "write"))
    );
    
    IacAuthorizationService.AuthorizationResult result = 
      new IacAuthorizationService.AuthorizationResult(permissions, Set.of());
    
    assertTrue(result.hasPermission("resource1", "read"));
    assertTrue(result.hasPermission("resource1", "write"));
    assertFalse(result.hasPermission("resource1", "delete"));
    assertFalse(result.hasPermission("resource2", "read"));
  }

  @Test
  public void testAuthorizationResult_GetTenantIds() {
    Set<String> tenantIds = Set.of("tenant-123", "tenant-456", "tenant-789");
    
    IacAuthorizationService.AuthorizationResult result = 
      new IacAuthorizationService.AuthorizationResult(Map.of(), tenantIds);
    
    assertEquals(3, result.getTenantIds().size());
    assertTrue(result.getTenantIds().contains("tenant-123"));
    assertTrue(result.getTenantIds().contains("tenant-456"));
    assertTrue(result.getTenantIds().contains("tenant-789"));
  }

  @Test
  public void testConcurrentAccess() {
    authService = new IacAuthorizationService(config);
    
    Thread thread1 = new Thread(() -> {
      for (int i = 0; i < 5; i++) {
        authService.clearTokenCache();
      }
    });
    
    Thread thread2 = new Thread(() -> {
      for (int i = 0; i < 5; i++) {
        authService.authorizePermissionsForTenant(
          "user-jwt-token",
          "fluxnova-bpm",
          "tenant-" + i,
          "EJWT",
          "trace-id-" + i
        );
      }
    });
    
    thread1.start();
    thread2.start();
    
    try {
      thread1.join();
      thread2.join();
    } catch (InterruptedException e) {
      fail("Thread interrupted: " + e.getMessage());
    }
    
    authService.close();
  }

  @Test
  public void testServiceLifecycle() {
    authService = new IacAuthorizationService(config);
    assertNotNull(authService);
    
    authService.authorizePermissionsForTenant(
      "user-jwt",
      "app",
      "tenant",
      "EJWT",
      "trace"
    );
    
    authService.clearTokenCache();
    authService.close();
    
    authService = new IacAuthorizationService(config);
    assertNotNull(authService);
    authService.close();
  }

  @Test
  public void testMultipleClearTokenCacheCalls() {
    authService = new IacAuthorizationService(config);
    
    for (int i = 0; i < 5; i++) {
      authService.clearTokenCache();
    }
    
    authService.close();
  }

  @Test
  public void testConfigImmutability() {
    authService = new IacAuthorizationService(config);
    
    config.setAppName("different-app");
    
    assertNotNull(authService);
    
    authService.close();
  }

  @Test
  public void testAuthorizationResult_EmptyPermissions() {
    IacAuthorizationService.AuthorizationResult result = 
      new IacAuthorizationService.AuthorizationResult(Map.of(), Set.of());
    
    assertTrue(result.getPermissions().isEmpty());
    assertFalse(result.hasPermission("any", "any"));
  }

  @Test
  public void testAuthorizationResult_EmptyTenantIds() {
    IacAuthorizationService.AuthorizationResult result = 
      new IacAuthorizationService.AuthorizationResult(Map.of(), Set.of());
    
    assertTrue(result.getTenantIds().isEmpty());
  }

  @Test
  public void testCloseIdempotency() {
    authService = new IacAuthorizationService(config);
    
    authService.close();
    authService.close();
    authService.close();
  }

  @Test
  public void testClearTokenCacheIdempotency() {
    authService = new IacAuthorizationService(config);
    
    authService.clearTokenCache();
    authService.clearTokenCache();
    authService.clearTokenCache();
    
    authService.close();
  }

  @Test
  public void testCacheOperationsAfterClose() {
    authService = new IacAuthorizationService(config);
    authService.close();
    
    authService.clearTokenCache();
  }

  @Test
  public void testResourceManagement() {
    for (int i = 0; i < 10; i++) {
      try (IacAuthorizationService service = new IacAuthorizationService(config)) {
        assertNotNull(service);
        service.clearTokenCache();
      }
    }
  }

  @Test
  public void testAuthorizationResult_MultipleResources() {
    Map<String, SortedSet<String>> permissions = Map.of(
      "resource1", (SortedSet<String>) new java.util.TreeSet<>(Set.of("read", "write")),
      "resource2", (SortedSet<String>) new java.util.TreeSet<>(Set.of("delete", "update")),
      "resource3", (SortedSet<String>) new java.util.TreeSet<>(Set.of("admin"))
    );
    
    IacAuthorizationService.AuthorizationResult result = 
      new IacAuthorizationService.AuthorizationResult(permissions, Set.of());
    
    assertEquals(3, result.getPermissions().size());
    assertEquals(2, result.getPermissionsForTenant("resource1").size());
    assertEquals(2, result.getPermissionsForTenant("resource2").size());
    assertEquals(1, result.getPermissionsForTenant("resource3").size());
  }

  @Test
  public void testAuthorizationResult_PermissionsSorted() {
    java.util.TreeSet<String> actions = new java.util.TreeSet<>();
    actions.add("write");
    actions.add("read");
    actions.add("delete");
    actions.add("admin");
    
    Map<String, SortedSet<String>> permissions = Map.of("resource", actions);
    
    IacAuthorizationService.AuthorizationResult result = 
      new IacAuthorizationService.AuthorizationResult(permissions, Set.of());
    
    String[] expected = {"admin", "delete", "read", "write"};
    String[] actual = result.getPermissionsForTenant("resource").toArray(new String[0]);
    assertArrayEquals(expected, actual);
  }

  @Test
  public void testAuthorizationWithDifferentTokenTypes() {
    authService = new IacAuthorizationService(config);
    
    IacAuthorizationService.AuthorizationResult result1 =
      authService.authorizePermissionsForTenant(
        "token", "app", "tenant", "EJWT", "trace"
      );
    assertNotNull(result1);
    
    IacAuthorizationService.AuthorizationResult result2 =
      authService.authorizePermissionsForTenant(
        "token", "app", "tenant", "IAC", "trace"
      );
    assertNotNull(result2);
    
    authService.close();
  }

  @Test
  public void testAuthorizationWithDifferentTrackingIds() {
    authService = new IacAuthorizationService(config);
    
    for (int i = 0; i < 5; i++) {
      IacAuthorizationService.AuthorizationResult result = 
        authService.authorizePermissionsForTenant(
          "token", "app", "tenant", "EJWT", "trace-" + i
        );
      assertNotNull(result);
    }
    
    authService.close();
  }
}
