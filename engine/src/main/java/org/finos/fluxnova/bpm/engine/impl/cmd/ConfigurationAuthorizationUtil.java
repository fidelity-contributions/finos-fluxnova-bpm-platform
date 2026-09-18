/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.finos.fluxnova.bpm.engine.impl.cmd;

import org.finos.fluxnova.bpm.engine.AuthorizationException;
import org.finos.fluxnova.bpm.engine.impl.identity.Authentication;
import org.finos.fluxnova.bpm.engine.impl.interceptor.CommandContext;

final class ConfigurationAuthorizationUtil {

  private ConfigurationAuthorizationUtil() {
  }

  static Authentication ensureAuthenticated(CommandContext commandContext) {
    Authentication authentication = commandContext.getAuthentication();
    if (authentication == null) {
      throw new AuthorizationException("Authentication is required to retrieve configurations");
    }
    return authentication;
  }

  static void ensureAuthorized(CommandContext commandContext, String tenantId) {
    ensureAuthorized(commandContext, ensureAuthenticated(commandContext), tenantId);
  }

  static void ensureAuthorized(
      CommandContext commandContext,
      Authentication authentication,
      String tenantId) {
    boolean isFluxnovaAdmin = commandContext.getAuthorizationManager().isFluxnovaAdmin(authentication);
    if (tenantId == null && !isFluxnovaAdmin) {
      throw new AuthorizationException("Only platform administrators may retrieve global configurations");
    }

    if (tenantId != null
        && !isFluxnovaAdmin
        && (authentication.getTenantIds() == null
            || !authentication.getTenantIds().contains(tenantId))) {
      throw new AuthorizationException(
          "The authenticated user is not authorized for tenant '" + tenantId + "'");
    }
  }
}
