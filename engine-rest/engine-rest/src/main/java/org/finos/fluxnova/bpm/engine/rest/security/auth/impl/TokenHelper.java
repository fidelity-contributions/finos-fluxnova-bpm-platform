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

import com.nimbusds.jwt.SignedJWT;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class TokenHelper {

  private static final Logger LOG = Logger.getLogger(TokenHelper.class.getName());

  /**
   * Extracts a claim value from a JWT token without validating the signature.
   *
   * @param token The JWT token string
   * @param claimName The name of the claim to extract (e.g., "fid_id", "sub", "email")
   * @param <T> The expected type of the claim value
   * @return The claim value, or null if the token is invalid or claim doesn't exist
   */
  @SuppressWarnings("unchecked")
  public <T> T extractClaim(String token, String claimName) {
    if (token == null || token.isEmpty()) {
      LOG.log(Level.FINE, "Token is null or empty");
      return null;
    }

    try {
      SignedJWT signedJWT = SignedJWT.parse(token);
      Object claimValue = signedJWT.getJWTClaimsSet().getClaim(claimName);
      
      if (claimValue == null) {
        LOG.log(Level.FINE, "Claim ''{0}'' not found in token", claimName);
        return null;
      }
      
      return (T) claimValue;
    } catch (ParseException e) {
      LOG.log(Level.WARNING, "Error parsing JWT token", e);
      return null;
    } catch (ClassCastException e) {
      LOG.log(Level.WARNING, "Error casting claim ''{0}'' to requested type", claimName);
      return null;
    }
  }

}
