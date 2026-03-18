package org.finos.fluxnova.bpm.engine.rest.security.auth.impl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;


public final class FidelityApiPlatformAuthClient implements AutoCloseable {
  
  private static final Logger LOG = Logger.getLogger(FidelityApiPlatformAuthClient.class.getName());
  
  private static final List<Pattern> GATEWAY_PATTERNS = List.of(
      Pattern.compile("aps-apigateway-(dev|test|pperf|stage|custqa|preprod|live)\\.fmr\\.com"),
      Pattern.compile("api-(pi|fbt)-(dev|test|pperf|stage|custqa|preprod|live|sit|sandbox)(-aws)?\\.fmr\\.com"),
      Pattern.compile("api-(dev|test|pperf|stage|custqa|preprod|live)(-aws)?\\.fmr\\.com"),
      Pattern.compile("api(-(pi|fbt))?(-aws)?\\.fmr\\.com")
  );
  
  private static final String AUTH_PATH = "/security-oauth2/v3/token";
  private static final String GRANT_TYPE = "client_credentials";
  private static final String ACCESS_TOKEN_FIELD = "access_token";
  private static final long TOKEN_EXPIRATION_BUFFER_MS = 60_000; // 1 minute
  private static final long DEFAULT_TOKEN_TTL_MS = 3_600_000; // 1 hour
  
  private final String clientId;
  private final String clientSecret;
  private final CloseableHttpClient httpClient;
  private final Gson gson;
  private final Map<String, TokenCacheEntry> tokenCache;
  
  public FidelityApiPlatformAuthClient(String clientId, String clientSecret) {
    validateCredentials(clientId, clientSecret);
    
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.httpClient = HttpClients.custom().useSystemProperties().build();
    this.gson = new Gson();
    this.tokenCache = new ConcurrentHashMap<>();
  }
  

  public boolean isKnownFidelityApiPlatformUrl(String url) {
    if (url == null) {
      return false;
    }
    
    try {
      URI uri = new URI(url);
      String host = uri.getHost();
      return host != null && GATEWAY_PATTERNS.stream().anyMatch(p -> p.matcher(host).matches());
    } catch (Exception e) {
      LOG.log(Level.WARNING, "Failed to parse URL: {0}", url);
      return false;
    }
  }
  

  public String getTokenForTargetApi(String targetApiUrl) {
    if (!isKnownFidelityApiPlatformUrl(targetApiUrl)) {
      LOG.log(Level.FINE, "URL does not require Fidelity API Platform authentication");
      return null;
    }
    
    String authUrl = buildAuthUrl(targetApiUrl);
    return getOrRefreshToken(authUrl);
  }
  

  public void clearCache() {
    tokenCache.clear();
    LOG.log(Level.INFO, "Cleared Fidelity API Platform token cache");
  }
  
  @Override
  public void close() {
    try {
      httpClient.close();
    } catch (IOException e) {
      LOG.log(Level.WARNING, "Error closing HTTP client", e);
    }
  }
  
  private void validateCredentials(String clientId, String clientSecret) {
    if (clientId == null || clientId.isBlank()) {
      throw new IllegalArgumentException("Fidelity API Platform clientId is required");
    }
    if (clientSecret == null || clientSecret.isBlank()) {
      throw new IllegalArgumentException("Fidelity API Platform clientSecret is required");
    }
  }
  
  private String buildAuthUrl(String apiUrl) {
    try {
      URI uri = new URI(apiUrl);
      return "%s://%s%s".formatted(uri.getScheme(), uri.getHost(), AUTH_PATH);
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid API URL: " + apiUrl, e);
    }
  }
  
  private String getOrRefreshToken(String authUrl) {
    TokenCacheEntry cached = tokenCache.get(authUrl);
    if (cached != null && !cached.isExpired()) {
      LOG.log(Level.FINE, "Using cached Fidelity API Platform token");
      return cached.token();
    }
    
    String token = requestToken(authUrl);
    long expiresAt = System.currentTimeMillis() + DEFAULT_TOKEN_TTL_MS - TOKEN_EXPIRATION_BUFFER_MS;
    tokenCache.put(authUrl, new TokenCacheEntry(token, expiresAt));
    
    LOG.log(Level.INFO, "Acquired new Fidelity API Platform token");
    return token;
  }
  
  private String requestToken(String authUrl) {
    HttpPost request = buildTokenRequest(authUrl);
    
    try (CloseableHttpResponse response = httpClient.execute(request)) {
      return parseTokenResponse(response);
    } catch (IOException | ParseException e) {
      LOG.log(Level.SEVERE, "Failed to obtain Fidelity API Platform token from: {0}", authUrl);
      throw new RuntimeException("Failed to obtain Fidelity API Platform token", e);
    }
  }
  
  private HttpPost buildTokenRequest(String authUrl) {
    HttpPost httpPost = new HttpPost(authUrl);
    
    List<NameValuePair> formParams = List.of(
        new BasicNameValuePair("client_id", clientId),
        new BasicNameValuePair("client_secret", clientSecret),
        new BasicNameValuePair("grant_type", GRANT_TYPE)
    );
    
    httpPost.setEntity(new UrlEncodedFormEntity(formParams, StandardCharsets.UTF_8));
    httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
    
    return httpPost;
  }
  
  private String parseTokenResponse(CloseableHttpResponse response) 
      throws IOException, ParseException {
    int statusCode = response.getCode();
    String responseBody = EntityUtils.toString(response.getEntity());
    
    if (statusCode != 200) {
      LOG.log(Level.SEVERE, "Fidelity API Platform token request failed. Status: {0}", statusCode);
      throw new RuntimeException("Token request failed with status: " + statusCode);
    }
    
    JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
    if (!jsonResponse.has(ACCESS_TOKEN_FIELD)) {
      throw new RuntimeException("Response missing " + ACCESS_TOKEN_FIELD + " field");
    }
    
    return jsonResponse.get(ACCESS_TOKEN_FIELD).getAsString();
  }
  

  private record TokenCacheEntry(String token, long expiresAt) {
    boolean isExpired() {
      return System.currentTimeMillis() >= expiresAt;
    }
  }
}
