package org.finos.fluxnova.bpm.engine.rest.security.auth.impl;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;


public class FidelityApiPlatformAuthClientTest {

  private String clientId;
  private String clientSecret;
  private FidelityApiPlatformAuthClient client;

  @Before
  public void setUp() {
    clientId = "client-123";
    clientSecret = "secret-456";
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructor_NullClientId() {
    new FidelityApiPlatformAuthClient(null, clientSecret);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructor_NullClientSecret() {
    new FidelityApiPlatformAuthClient(clientId, null);
  }

  @Test
  public void testConstructor_ValidCredentials() {
    client = new FidelityApiPlatformAuthClient(clientId, clientSecret);
    assertNotNull(client);
    client.close();
  }

  @Test
  public void testIsKnownFidelityApiPlatformUrl_ValidUrls() {
    client = new FidelityApiPlatformAuthClient(clientId, clientSecret);
    
    assertTrue(client.isKnownFidelityApiPlatformUrl("https://api-dev-aws.fmr.com/service"));
    assertTrue(client.isKnownFidelityApiPlatformUrl("https://api-pi-dev-aws.fmr.com/service"));
    assertTrue(client.isKnownFidelityApiPlatformUrl("https://api-live-aws.fmr.com/service"));
    assertTrue(client.isKnownFidelityApiPlatformUrl("https://api-aws.fmr.com/service"));
    assertTrue(client.isKnownFidelityApiPlatformUrl("https://api.fmr.com/service"));
    assertTrue(client.isKnownFidelityApiPlatformUrl("https://aps-apigateway-dev.fmr.com/service"));
    
    client.close();
  }

  @Test
  public void testIsKnownFidelityApiPlatformUrl_InvalidUrls() {
    client = new FidelityApiPlatformAuthClient(clientId, clientSecret);
    
    assertFalse(client.isKnownFidelityApiPlatformUrl("https://api-cat-aws.fmr.com/service"));
    assertFalse(client.isKnownFidelityApiPlatformUrl("https://api-np-aws.fmr.com/service"));
    assertFalse(client.isKnownFidelityApiPlatformUrl("https://api-prod-aws.fmr.com/service"));
    assertFalse(client.isKnownFidelityApiPlatformUrl(null));
    assertFalse(client.isKnownFidelityApiPlatformUrl(""));
    assertFalse(client.isKnownFidelityApiPlatformUrl("   "));
    
    client.close();
  }

  @Test
  public void testClearCache() {
    client = new FidelityApiPlatformAuthClient(clientId, clientSecret);
    
    client.clearCache();
    client.clearCache();
    
    client.close();
  }

  @Test
  public void testClose() {
    client = new FidelityApiPlatformAuthClient(clientId, clientSecret);
    
    client.close();
    client.close();
  }

  @Test
  public void testResourceCleanup() {
    try (FidelityApiPlatformAuthClient autoCloseClient = new FidelityApiPlatformAuthClient(clientId, clientSecret)) {
      assertNotNull(autoCloseClient);
      autoCloseClient.clearCache();
    }
  }

  @Test
  public void testMultipleClients() {
    FidelityApiPlatformAuthClient client1 = new FidelityApiPlatformAuthClient(clientId, clientSecret);
    FidelityApiPlatformAuthClient client2 = new FidelityApiPlatformAuthClient(clientId, clientSecret);
    
    assertNotNull(client1);
    assertNotNull(client2);
    assertNotEquals(client1, client2);
    
    client1.clearCache();
    client2.clearCache();
    
    client1.close();
    client2.close();
  }

  @Test
  public void testUrlValidation_AllEnvironments() {
    client = new FidelityApiPlatformAuthClient(clientId, clientSecret);
    
    String[] environments = {
      "api-dev-aws.fmr.com",
      "api-test-aws.fmr.com",
      "api-live-aws.fmr.com",
      "api-pi-dev-aws.fmr.com",
      "api-fbt-dev-aws.fmr.com",
      "api-aws.fmr.com",
      "api.fmr.com"
    };
    
    for (String env : environments) {
      String url = "https://" + env + "/test";
      assertTrue("URL should be recognized: " + url, 
                 client.isKnownFidelityApiPlatformUrl(url));
    }
    
    client.close();
  }

  @Test
  public void testCloseIdempotency() {
    client = new FidelityApiPlatformAuthClient(clientId, clientSecret);
    client.close();
    client.close();
    client.close();
  }
}
