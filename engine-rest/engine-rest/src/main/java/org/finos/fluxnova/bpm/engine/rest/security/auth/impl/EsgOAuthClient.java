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
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public final class EsgOAuthClient implements AutoCloseable {
  
  private static final Logger LOG = Logger.getLogger(EsgOAuthClient.class.getName());
  
  private static final String GRANT_TYPE = "client_credentials";
  private static final String SCOPE = "AppIdClaimsTrust";
  private static final String ACCESS_TOKEN_FIELD = "access_token";
  
  private final CloseableHttpClient httpClient;
  private final Gson gson;
  
  public EsgOAuthClient() {
    this.httpClient = HttpClients.custom().useSystemProperties().build();
    this.gson = new Gson();
  }
  

  public String getToken(String username, String password, String esgOauthUrl) {
    validateParameters(username, password, esgOauthUrl);
    
    HttpPost request = buildTokenRequest(username, password, esgOauthUrl);
    
    try (CloseableHttpResponse response = httpClient.execute(request)) {
      return parseTokenResponse(response, esgOauthUrl);
    } catch (IOException | ParseException e) {
      LOG.log(Level.SEVERE, "Failed to obtain ESG OAuth token from: {0}", esgOauthUrl);
      throw new RuntimeException("Failed to obtain ESG OAuth token", e);
    }
  }
  
  @Override
  public void close() {
    try {
      httpClient.close();
    } catch (IOException e) {
      LOG.log(Level.WARNING, "Error closing HTTP client", e);
    }
  }
  
  private void validateParameters(String username, String password, String url) {
    if (username == null || password == null || url == null) {
      throw new IllegalArgumentException("Username, password, and ESG OAuth URL are required");
    }
  }
  
  private HttpPost buildTokenRequest(String username, String password, String url) {
    HttpPost httpPost = new HttpPost(url);
    
    List<NameValuePair> formParams = List.of(
        new BasicNameValuePair("client_id", username),
        new BasicNameValuePair("client_secret", password),
        new BasicNameValuePair("grant_type", GRANT_TYPE),
        new BasicNameValuePair("scope", SCOPE),
        new BasicNameValuePair("username", username),
        new BasicNameValuePair("password", password)
    );
    
    httpPost.setEntity(new UrlEncodedFormEntity(formParams, StandardCharsets.UTF_8));
    httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
    
    return httpPost;
  }
  
  private String parseTokenResponse(CloseableHttpResponse response, String url) 
      throws IOException, ParseException {
    int statusCode = response.getCode();
    String responseBody = EntityUtils.toString(response.getEntity());
    
    if (statusCode != 200) {
      LOG.log(Level.SEVERE, "ESG OAuth request failed. Status: {0}, URL: {1}", 
          new Object[]{statusCode, url});
      throw new RuntimeException(
          "ESG OAuth request failed with status: " + statusCode + ", Response: " + responseBody);
    }
    
    JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
    if (!jsonResponse.has(ACCESS_TOKEN_FIELD)) {
      throw new RuntimeException("ESG OAuth response missing " + ACCESS_TOKEN_FIELD + " field");
    }
    
    String accessToken = jsonResponse.get(ACCESS_TOKEN_FIELD).getAsString();
    LOG.log(Level.FINE, "Successfully obtained ESG OAuth token");
    return accessToken;
  }
}
