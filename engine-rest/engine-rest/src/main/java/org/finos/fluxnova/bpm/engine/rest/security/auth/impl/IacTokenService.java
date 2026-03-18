package org.finos.fluxnova.bpm.engine.rest.security.auth.impl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;


public final class IacTokenService implements AutoCloseable {
  
  private static final Logger LOG = Logger.getLogger(IacTokenService.class.getName());
  
  private static final String DEFAULT_PROVIDER = "esg-oauth2";
  private static final String AUTHORIZE_IDENTITY_PATH = "/authorize/identity";
  private static final long TOKEN_EXPIRATION_BUFFER_MS = 60_000; // 1 minute
  private static final long DEFAULT_TOKEN_TTL_MS = 3_600_000; // 1 hour
  
  private final IacConfig config;
  private final EsgOAuthClient esgOAuthClient;
  private final FidelityApiPlatformAuthClient fidApiPlatformAuthClient;
  private final CloseableHttpClient httpClient;
  private final Gson gson;
  private final Map<String, TokenCacheEntry> tokenCache;
  
  public IacTokenService(IacConfig config) {
    validateConfig(config);
    
    this.config = config;
    this.esgOAuthClient = new EsgOAuthClient();
    this.fidApiPlatformAuthClient = initializeFidelityAuthClient(config);
    this.httpClient = HttpClients.custom().useSystemProperties().build();
    this.gson = new Gson();
    this.tokenCache = new ConcurrentHashMap<>();
  }
  

  public String getToken(String appName) {
    if (appName == null || appName.isBlank()) {
      throw new IllegalArgumentException("appName is required");
    }
    
    TokenCacheEntry cached = tokenCache.get(appName);
    if (cached != null && !cached.isExpired()) {
      LOG.log(Level.FINE, "Using cached IAC token for appName: {0}", appName);
      return cached.token();
    }
    
    LOG.log(Level.INFO, "Acquiring new IAC token for appName: {0}", appName);
    String iacToken = acquireIacToken(appName);
    
    long expiresAt = System.currentTimeMillis() + DEFAULT_TOKEN_TTL_MS - TOKEN_EXPIRATION_BUFFER_MS;
    tokenCache.put(appName, new TokenCacheEntry(iacToken, expiresAt));
    
    return iacToken;
  }

  public void clearCache() {
    tokenCache.clear();
    LOG.log(Level.INFO, "Cleared IAC token cache");
  }
  
  @Override
  public void close() {
    try {
      esgOAuthClient.close();
      if (fidApiPlatformAuthClient != null) {
        fidApiPlatformAuthClient.close();
      }
      httpClient.close();
    } catch (IOException e) {
      LOG.log(Level.WARNING, "Error closing HTTP clients", e);
    }
  }
  
  private void validateConfig(IacConfig config) {
    if (config == null) {
      throw new IllegalArgumentException("IacConfig is required");
    }
  }
  
  private FidelityApiPlatformAuthClient initializeFidelityAuthClient(IacConfig config) {
    if (!config.hasFidelityApiPlatformCredentials()) {
      LOG.log(Level.WARNING, "Fidelity API Platform credentials not configured. " +
                             "IAC calls through gateway will fail!");
      return null;
    }
    
    LOG.log(Level.INFO, "Fidelity API Platform authentication enabled");
    return new FidelityApiPlatformAuthClient(
        config.getFidApiPlatformClientId(),
        config.getFidApiPlatformClientSecret()
    );
  }
  
  private String acquireIacToken(String appName) {
    String esgToken = getEsgOAuthToken();
    String iacUrl = buildIacAuthorizeIdentityUrl();
    return exchangeEsgTokenForIacToken(esgToken, appName, iacUrl);
  }
  
  private String getEsgOAuthToken() {
    return esgOAuthClient.getToken(
        config.getServiceAccountUsername(),
        config.getServiceAccountPassword(),
        config.getEsgOauthUrl()
    );
  }
  
  private String buildIacAuthorizeIdentityUrl() {
    return config.getBaseUrl() + AUTHORIZE_IDENTITY_PATH;
  }
  
  private String exchangeEsgTokenForIacToken(String esgToken, String appName, String iacUrl) {
    HttpPost request = buildIacTokenRequest(esgToken, appName, iacUrl);
    
    try (CloseableHttpResponse response = httpClient.execute(request)) {
      return parseIacTokenResponse(response);
    } catch (IOException | ParseException e) {
      LOG.log(Level.SEVERE, "Failed to exchange ESG token for IAC token", e);
      throw new RuntimeException("Failed to exchange ESG token for IAC token", e);
    }
  }
  
  private HttpPost buildIacTokenRequest(String esgToken, String appName, String iacUrl) {
    HttpPost httpPost = new HttpPost(iacUrl);
    
    addFidelityApiPlatformAuth(httpPost, iacUrl);
    
    Map<String, String> requestBody = Map.of(
        "appName", appName,
        "provider", DEFAULT_PROVIDER,
        "token", esgToken
    );
    
    httpPost.setEntity(new StringEntity(gson.toJson(requestBody), ContentType.APPLICATION_JSON));
    httpPost.setHeader("Content-Type", "application/json");
    
    LOG.log(Level.INFO, "Calling IAC /authorize/identity");
    return httpPost;
  }
  
  private void addFidelityApiPlatformAuth(HttpPost httpPost, String iacUrl) {
    if (fidApiPlatformAuthClient == null) {
      return;
    }
    
    if (!fidApiPlatformAuthClient.isKnownFidelityApiPlatformUrl(iacUrl)) {
      LOG.log(Level.FINE, "IAC URL not behind Fidelity API Platform gateway");
      return;
    }
    
    LOG.log(Level.INFO, "Adding Fidelity API Platform authentication");
    String gatewayToken = fidApiPlatformAuthClient.getTokenForTargetApi(iacUrl);
    
    if (gatewayToken != null) {
      httpPost.setHeader("Authorization", "Bearer " + gatewayToken);
      LOG.log(Level.FINE, "Added Authorization header with gateway token");
    } else {
      LOG.log(Level.WARNING, "Failed to obtain Fidelity API Platform token");
    }
  }
  
  private String parseIacTokenResponse(CloseableHttpResponse response) 
      throws IOException, ParseException {
    int statusCode = response.getCode();
    String responseBody = EntityUtils.toString(response.getEntity());
    
    LOG.log(Level.INFO, "IAC /authorize/identity response status: {0}", statusCode);
    
    if (statusCode != 200) {
      LOG.log(Level.SEVERE, "IAC token exchange failed. Status: {0}", statusCode);
      throw new RuntimeException("IAC token exchange failed with status: " + statusCode);
    }
    
    JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
    
    if (jsonResponse.has("iacToken")) {
      return jsonResponse.get("iacToken").getAsString();
    } else if (jsonResponse.has("access_token")) {
      return jsonResponse.get("access_token").getAsString();
    } else {
      throw new RuntimeException("IAC response missing token field");
    }
  }

  private record TokenCacheEntry(String token, long expiresAt) {
    boolean isExpired() {
      return System.currentTimeMillis() >= expiresAt;
    }
  }
}
