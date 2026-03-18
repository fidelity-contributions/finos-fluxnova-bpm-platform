
package org.finos.fluxnova.bpm.engine.rest.security.auth.impl;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;


public class IacConfigTest {

  private IacConfig config;

  @Before
  public void setUp() {
    config = new IacConfig();
  }

  @Test
  public void testDefaultValues() {
    assertEquals(5000, config.getConnectTimeout());
    assertEquals(10000, config.getReadTimeout());
  }

  @Test
  public void testSetBaseUrl() {
    String url = "https://api-dev-aws.fmr.com/dap-access-control/v1";
    config.setBaseUrl(url);
    assertEquals(url, config.getBaseUrl());
  }

  @Test
  public void testSetAppName() {
    String appName = "fluxnova-bpm";
    config.setAppName(appName);
    assertEquals(appName, config.getAppName());
  }

  @Test
  public void testSetServiceAccountCredentials() {
    String username = "svc-user";
    String password = "secret";
    config.setServiceAccountUsername(username);
    config.setServiceAccountPassword(password);
    
    assertEquals(username, config.getServiceAccountUsername());
    assertEquals(password, config.getServiceAccountPassword());
  }

  @Test
  public void testSetEsgOAuthUrl() {
    String url = "https://esg-oauth.fmr.com/token";
    config.setEsgOauthUrl(url);
    assertEquals(url, config.getEsgOauthUrl());
  }

  @Test
  public void testSetFidelityApiPlatformCredentials() {
    String clientId = "client-123";
    String clientSecret = "secret-456";
    config.setFidApiPlatformClientId(clientId);
    config.setFidApiPlatformClientSecret(clientSecret);
    
    assertEquals(clientId, config.getFidApiPlatformClientId());
    assertEquals(clientSecret, config.getFidApiPlatformClientSecret());
  }

  @Test
  public void testSetTimeouts() {
    config.setConnectTimeout(3000);
    config.setReadTimeout(15000);
    
    assertEquals(3000, config.getConnectTimeout());
    assertEquals(15000, config.getReadTimeout());
  }

  @Test
  public void testHasFidelityApiPlatformCredentials_WithCredentials() {
    config.setFidApiPlatformClientId("client-123");
    config.setFidApiPlatformClientSecret("secret-456");
    
    assertTrue(config.hasFidelityApiPlatformCredentials());
  }

  @Test
  public void testHasFidelityApiPlatformCredentials_WithoutClientId() {
    config.setFidApiPlatformClientSecret("secret-456");
    
    assertFalse(config.hasFidelityApiPlatformCredentials());
  }

  @Test
  public void testHasFidelityApiPlatformCredentials_WithoutClientSecret() {
    config.setFidApiPlatformClientId("client-123");
    
    assertFalse(config.hasFidelityApiPlatformCredentials());
  }

  @Test
  public void testHasFidelityApiPlatformCredentials_Empty() {
    assertFalse(config.hasFidelityApiPlatformCredentials());
  }

  @Test
  public void testValidate_ValidConfig() {
    config.setBaseUrl("https://api-dev-aws.fmr.com/dap-access-control/v1");
    config.setAppName("fluxnova-bpm");
    config.setServiceAccountUsername("svc-user");
    config.setServiceAccountPassword("secret");
    config.setEsgOauthUrl("https://esg-oauth.fmr.com/token");
    
    // Should not throw exception
    config.validate();
  }

  @Test(expected = IllegalStateException.class)
  public void testValidate_MissingBaseUrl() {
    config.setBaseUrl(null);
    config.setAppName("fluxnova-bpm");
    config.setServiceAccountUsername("svc-user");
    config.setServiceAccountPassword("secret");
    config.setEsgOauthUrl("https://esg-oauth.fmr.com/token");
    
    config.validate();
  }

  @Test(expected = IllegalStateException.class)
  public void testValidate_EmptyBaseUrl() {
    config.setBaseUrl("");
    config.setAppName("fluxnova-bpm");
    config.setServiceAccountUsername("svc-user");
    config.setServiceAccountPassword("secret");
    config.setEsgOauthUrl("https://esg-oauth.fmr.com/token");
    
    config.validate();
  }

  @Test(expected = IllegalStateException.class)
  public void testValidate_BlankBaseUrl() {
    config.setBaseUrl("   ");
    config.setAppName("fluxnova-bpm");
    config.setServiceAccountUsername("svc-user");
    config.setServiceAccountPassword("secret");
    config.setEsgOauthUrl("https://esg-oauth.fmr.com/token");
    
    config.validate();
  }

  @Test(expected = IllegalStateException.class)
  public void testValidate_MissingServiceAccountUsername() {
    config.setBaseUrl("https://api-dev-aws.fmr.com/dap-access-control/v1");
    config.setAppName("fluxnova-bpm");
    config.setServiceAccountUsername(null);
    config.setServiceAccountPassword("secret");
    config.setEsgOauthUrl("https://esg-oauth.fmr.com/token");
    
    config.validate();
  }

  @Test(expected = IllegalStateException.class)
  public void testValidate_MissingServiceAccountPassword() {
    config.setBaseUrl("https://api-dev-aws.fmr.com/dap-access-control/v1");
    config.setAppName("fluxnova-bpm");
    config.setServiceAccountUsername("svc-user");
    config.setServiceAccountPassword(null);
    config.setEsgOauthUrl("https://esg-oauth.fmr.com/token");
    
    config.validate();
  }

  @Test(expected = IllegalStateException.class)
  public void testValidate_MissingEsgOAuthUrl() {
    config.setBaseUrl("https://api-dev-aws.fmr.com/dap-access-control/v1");
    config.setAppName("fluxnova-bpm");
    config.setServiceAccountUsername("svc-user");
    config.setServiceAccountPassword("secret");
    config.setEsgOauthUrl(null);
    
    config.validate();
  }

  @Test
  public void testValidate_WithFidelityApiPlatformCredentials() {
    config.setBaseUrl("https://api-dev-aws.fmr.com/dap-access-control/v1");
    config.setAppName("fluxnova-bpm");
    config.setServiceAccountUsername("svc-user");
    config.setServiceAccountPassword("secret");
    config.setEsgOauthUrl("https://esg-oauth.fmr.com/token");
    config.setFidApiPlatformClientId("client-123");
    config.setFidApiPlatformClientSecret("secret-456");
    
    // Should not throw exception
    config.validate();
  }

  @Test
  public void testValidate_InvalidConnectTimeout() {
    config.setBaseUrl("https://api-dev-aws.fmr.com/dap-access-control/v1");
    config.setAppName("fluxnova-bpm");
    config.setServiceAccountUsername("svc-user");
    config.setServiceAccountPassword("secret");
    config.setEsgOauthUrl("https://esg-oauth.fmr.com/token");
    config.setConnectTimeout(-1);
    
    // Negative timeout should still validate (will be handled by HTTP client)
    config.validate();
  }

  @Test
  public void testConfigImmutabilityAfterValidation() {
    config.setBaseUrl("https://api-dev-aws.fmr.com/dap-access-control/v1");
    config.setAppName("fluxnova-bpm");
    config.setServiceAccountUsername("svc-user");
    config.setServiceAccountPassword("secret");
    config.setEsgOauthUrl("https://esg-oauth.fmr.com/token");
    
    config.validate();
    
    // Values should remain unchanged
    assertEquals("https://api-dev-aws.fmr.com/dap-access-control/v1", config.getBaseUrl());
    assertEquals("fluxnova-bpm", config.getAppName());
    assertEquals("svc-user", config.getServiceAccountUsername());
    assertEquals("secret", config.getServiceAccountPassword());
  }

  @Test
  public void testMultipleValidationCalls() {
    config.setBaseUrl("https://api-dev-aws.fmr.com/dap-access-control/v1");
    config.setAppName("fluxnova-bpm");
    config.setServiceAccountUsername("svc-user");
    config.setServiceAccountPassword("secret");
    config.setEsgOauthUrl("https://esg-oauth.fmr.com/token");
    
    // Multiple validation calls should not throw exception
    config.validate();
    config.validate();
    config.validate();
  }

  @Test
  public void testNullValuesHandling() {
    config.setBaseUrl(null);
    config.setAppName(null);
    config.setServiceAccountUsername(null);
    config.setServiceAccountPassword(null);
    config.setEsgOauthUrl(null);
    
    assertNull(config.getBaseUrl());
    assertNull(config.getAppName());
    assertNull(config.getServiceAccountUsername());
    assertNull(config.getServiceAccountPassword());
    assertNull(config.getEsgOauthUrl());
  }

  @Test
  public void testWhitespaceHandling() {
    config.setBaseUrl("  https://api.fmr.com  ");
    config.setAppName("  fluxnova-bpm  ");
    
    // Values should be stored as-is (trimming is responsibility of validation)
    assertTrue(config.getBaseUrl().contains("  "));
    assertTrue(config.getAppName().contains("  "));
  }
}
