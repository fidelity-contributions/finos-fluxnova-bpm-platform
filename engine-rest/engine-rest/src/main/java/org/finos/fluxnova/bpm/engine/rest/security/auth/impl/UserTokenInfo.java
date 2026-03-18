
package org.finos.fluxnova.bpm.engine.rest.security.auth.impl;

public class UserTokenInfo {

  private String token;
  private TokenType tokenType;
  private String username;
  private String tenantId;


  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public TokenType getTokenType() {
    return tokenType;
  }

  public void setTokenType(TokenType tokenType) {
    this.tokenType = tokenType;
  }


  public String getUsername() {
    return username;
  }


  public void setUsername(String username) {
    this.username = username;
  }


  public String getTenantId() {
    return tenantId;
  }


  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  @Override
  public String toString() {
    return "UserTokenInfo{" +
        "tokenType=" + tokenType +
        ", username='" + username + '\'' +
        ", tenantId='" + tenantId + '\'' +
        ", hasToken=" + (token != null) +
        '}';
  }
}
