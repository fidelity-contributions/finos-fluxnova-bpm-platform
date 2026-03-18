package org.finos.fluxnova.bpm.engine.rest.security.auth.impl;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;


public class EsgOAuthClientTest {

  private EsgOAuthClient client;

  @Before
  public void setUp() {
    client = new EsgOAuthClient();
  }

  @Test
  public void testConstructor_NoArg() {
    EsgOAuthClient testClient = new EsgOAuthClient();
    assertNotNull(testClient);
    testClient.close();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetToken_NullUsername() {
    client.getToken(null, "password", "app-name");
  }

  @Test(expected = RuntimeException.class)
  public void testGetToken_EmptyUsername() {
    client.getToken("", "password", "app-name");
  }

  @Test(expected = RuntimeException.class)
  public void testGetToken_BlankUsername() {
    client.getToken("   ", "password", "app-name");
  }

  @Test(expected = RuntimeException.class)
  public void testGetToken_NullPassword() {
    client.getToken("username", null, "app-name");
  }

  @Test(expected = RuntimeException.class)
  public void testGetToken_EmptyPassword() {
    client.getToken("username", "", "app-name");
  }

  @Test(expected = RuntimeException.class)
  public void testGetToken_NullAppName() {
    client.getToken("username", "password", null);
  }

  @Test(expected = RuntimeException.class)
  public void testGetToken_EmptyAppName() {
    client.getToken("username", "password", "");
  }

  @Test
  public void testClose() {
    client.close();
    
    EsgOAuthClient newClient = new EsgOAuthClient();
    newClient.close();
    newClient.close();
  }

  @Test
  public void testResourceCleanup() {
    try (EsgOAuthClient autoCloseClient = new EsgOAuthClient()) {
      assertNotNull(autoCloseClient);
    }

  }

  @Test
  public void testMultipleClients() {
    EsgOAuthClient client1 = new EsgOAuthClient();
    EsgOAuthClient client2 = new EsgOAuthClient();
    
    assertNotNull(client1);
    assertNotNull(client2);
    assertNotEquals(client1, client2);
    
    client1.close();
    client2.close();
  }

  @Test
  public void testClientReusability() {
    assertNotNull(client);
    client.close();
  }

  @Test
  public void testMultipleInstances() {
    for (int i = 0; i < 5; i++) {
      try (EsgOAuthClient testClient = new EsgOAuthClient()) {
        assertNotNull(testClient);
      }
    }
  }

  @Test
  public void testCloseIdempotency() {
    EsgOAuthClient testClient = new EsgOAuthClient();
    testClient.close();
    testClient.close();
    testClient.close();
  }
}
