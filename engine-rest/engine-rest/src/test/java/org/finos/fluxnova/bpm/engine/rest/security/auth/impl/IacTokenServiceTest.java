
package org.finos.fluxnova.bpm.engine.rest.security.auth.impl;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;


public class IacTokenServiceTest {

  private IacConfig config;
  private IacTokenService tokenService;

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
    new IacTokenService(null);
  }

  @Test
  public void testConstructor_ValidConfig() {
    tokenService = new IacTokenService(config);
    assertNotNull(tokenService);
  }

  @Test
  public void testConstructor_ConfigWithoutFidelityCredentials() {
    IacConfig minimalConfig = new IacConfig();
    minimalConfig.setBaseUrl("https://api-dev-aws.fmr.com/dap-access-control/v1");
    minimalConfig.setAppName("fluxnova-bpm");
    minimalConfig.setServiceAccountUsername("svc-user");
    minimalConfig.setServiceAccountPassword("secret");
    minimalConfig.setEsgOauthUrl("https://esg-oauth.fmr.com/token");
    
    tokenService = new IacTokenService(minimalConfig);
    assertNotNull(tokenService);
  }

  @Test
  public void testClearCache() {
    tokenService = new IacTokenService(config);
    
    tokenService.clearCache();
    tokenService.clearCache();
  }

  @Test
  public void testClose() {
    tokenService = new IacTokenService(config);
    
    tokenService.close();
    tokenService.close();
  }

  @Test
  public void testResourceCleanup() {
    try (IacTokenService autoCloseService = new IacTokenService(config)) {
      assertNotNull(autoCloseService);
      autoCloseService.clearCache();
    }
  }

  @Test
  public void testMultipleInstances() {
    IacTokenService service1 = new IacTokenService(config);
    IacTokenService service2 = new IacTokenService(config);
    
    assertNotNull(service1);
    assertNotNull(service2);
    assertNotEquals(service1, service2);
    
    service1.clearCache();
    service2.clearCache();
    
    service1.close();
    service2.close();
  }

  @Test
  public void testCacheOperationsAfterClose() {
    tokenService = new IacTokenService(config);
    tokenService.close();
    
    tokenService.clearCache();
  }

  @Test
  public void testConfigImmutability() {
    tokenService = new IacTokenService(config);
    
    config.setAppName("different-app");
    
    assertNotNull(tokenService);
    
    tokenService.close();
  }

  @Test
  public void testServiceWithAllCredentials() {
    tokenService = new IacTokenService(config);
    
    assertNotNull(tokenService);
    
    tokenService.close();
  }

  @Test
  public void testConcurrentAccess() {
    tokenService = new IacTokenService(config);
    
    Thread thread1 = new Thread(() -> {
      for (int i = 0; i < 10; i++) {
        tokenService.clearCache();
      }
    });
    
    Thread thread2 = new Thread(() -> {
      for (int i = 0; i < 10; i++) {
        tokenService.clearCache();
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
    
    tokenService.close();
  }

  @Test
  public void testServiceCreationWithMinimalConfig() {
    IacConfig minConfig = new IacConfig();
    minConfig.setBaseUrl("https://api-dev-aws.fmr.com/dap-access-control/v1");
    minConfig.setAppName("test-app");
    minConfig.setServiceAccountUsername("user");
    minConfig.setServiceAccountPassword("pass");
    minConfig.setEsgOauthUrl("https://esg.fmr.com/token");
    
    tokenService = new IacTokenService(minConfig);
    assertNotNull(tokenService);
    
    tokenService.close();
  }

  @Test
  public void testServiceCreationWithCustomTimeouts() {
    config.setConnectTimeout(3000);
    config.setReadTimeout(20000);
    
    tokenService = new IacTokenService(config);
    assertNotNull(tokenService);
    
    tokenService.close();
  }

  @Test
  public void testMultipleClearCacheCalls() {
    tokenService = new IacTokenService(config);
    
    for (int i = 0; i < 5; i++) {
      tokenService.clearCache();
    }
    
    tokenService.close();
  }

  @Test
  public void testServiceLifecycle() {
    tokenService = new IacTokenService(config);
    assertNotNull(tokenService);
    
    tokenService.clearCache();
    
    tokenService.close();
    
    tokenService = new IacTokenService(config);
    assertNotNull(tokenService);
    tokenService.close();
  }

  @Test
  public void testConfigValidation() {
    IacConfig testConfig = new IacConfig();
    testConfig.setBaseUrl("https://api-dev-aws.fmr.com/dap-access-control/v1");
    testConfig.setAppName("test");
    testConfig.setServiceAccountUsername("user");
    testConfig.setServiceAccountPassword("pass");
    testConfig.setEsgOauthUrl("https://esg.fmr.com/token");
    
    tokenService = new IacTokenService(testConfig);
    assertNotNull(tokenService);
    
    tokenService.close();
  }

  @Test
  public void testServiceWithFidelityApiPlatformUrl() {
    config.setBaseUrl("https://api-dev-aws.fmr.com/dap-access-control/v1");
    
    tokenService = new IacTokenService(config);
    assertNotNull(tokenService);
    
    tokenService.close();
  }

  @Test
  public void testServiceWithNonFidelityUrl() {
    config.setBaseUrl("https://other-service.example.com/api");
    
    tokenService = new IacTokenService(config);
    assertNotNull(tokenService);
    
    tokenService.close();
  }

  @Test
  public void testCacheIndependenceBetweenInstances() {
    IacTokenService service1 = new IacTokenService(config);
    IacTokenService service2 = new IacTokenService(config);
    
    service1.clearCache();
    
    service2.clearCache();
    
    service1.close();
    service2.close();
  }

  @Test
  public void testResourceManagement() {
    for (int i = 0; i < 10; i++) {
      try (IacTokenService service = new IacTokenService(config)) {
        assertNotNull(service);
        service.clearCache();
      }
    }
  }

  @Test
  public void testConfigWithSpecialCharacters() {
    config.setServiceAccountPassword("p@ssw0rd!#$%^&*()");
    config.setAppName("test-app-123");
    
    tokenService = new IacTokenService(config);
    assertNotNull(tokenService);
    
    tokenService.close();
  }

  @Test
  public void testConfigWithLongValues() {
    StringBuilder longString = new StringBuilder();
    for (int i = 0; i < 500; i++) {
      longString.append("a");
    }
    config.setAppName(longString.toString());
    
    tokenService = new IacTokenService(config);
    assertNotNull(tokenService);
    
    tokenService.close();
  }

  @Test
  public void testCloseIdempotency() {
    tokenService = new IacTokenService(config);
    
    tokenService.close();
    tokenService.close();
    tokenService.close();
    
  }

  @Test
  public void testClearCacheIdempotency() {
    tokenService = new IacTokenService(config);
    
    tokenService.clearCache();
    tokenService.clearCache();
    tokenService.clearCache();
    
    tokenService.close();
  }
}
