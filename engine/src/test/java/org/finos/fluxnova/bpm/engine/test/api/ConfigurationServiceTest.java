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
package org.finos.fluxnova.bpm.engine.test.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.finos.fluxnova.bpm.engine.AuthorizationException;
import org.finos.fluxnova.bpm.engine.ConfigurationService;
import org.finos.fluxnova.bpm.engine.authorization.Groups;
import org.finos.fluxnova.bpm.engine.configuration.Configuration;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.ConfigurationEntity;
import org.finos.fluxnova.bpm.engine.test.util.PluggableProcessEngineTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ConfigurationServiceTest extends PluggableProcessEngineTest {

  protected ConfigurationService configurationService;
  protected List<String> configurationIds = new ArrayList<String>();
  protected boolean tenantCheckEnabled;

  @BeforeEach
  public void setUpConfigurationService() {
    configurationService = processEngine.getConfigurationService();
    tenantCheckEnabled = processEngineConfiguration.isTenantCheckEnabled();
  }

  @AfterEach
  public void removeConfigurations() {
    identityService.clearAuthentication();
    processEngineConfiguration.setTenantCheckEnabled(tenantCheckEnabled);
    processEngineConfiguration.getCommandExecutorTxRequired().execute(commandContext -> {
      for (String configurationId : configurationIds) {
        ConfigurationEntity configuration = commandContext.getConfigurationManager()
            .findConfigurationById(configurationId);
        if (configuration != null) {
          commandContext.getDbEntityManager().delete(configuration);
        }
      }
      return null;
    });
  }

  @Test
  public void shouldFilterConfigurationsByScopeAndStatus() {
    String keyPrefix = "configuration-test-" + UUID.randomUUID();

    ConfigurationEntity globalActive = insertConfiguration(
        keyPrefix + "-global-active", null, Configuration.STATUS_ACTIVE);
    ConfigurationEntity globalDeleted = insertConfiguration(
        keyPrefix + "-global-deleted", null, Configuration.STATUS_DELETED);
    ConfigurationEntity tenantActive = insertConfiguration(
        keyPrefix + "-tenant-active", "tenant-a", Configuration.STATUS_ACTIVE);
    ConfigurationEntity tenantDeleted = insertConfiguration(
        keyPrefix + "-tenant-deleted", "tenant-a", Configuration.STATUS_DELETED);

    authenticateAsAdmin();

    assertThat(configurationService.getConfigurations(null, false))
        .extracting(Configuration::getId)
        .contains(globalActive.getId())
        .doesNotContain(globalDeleted.getId(), tenantActive.getId(), tenantDeleted.getId());

    assertThat(configurationService.getConfigurations(null, true))
        .extracting(Configuration::getId)
        .contains(globalActive.getId(), globalDeleted.getId())
        .doesNotContain(tenantActive.getId(), tenantDeleted.getId());

    assertThat(configurationService.getConfigurations("tenant-a", false))
        .extracting(Configuration::getId)
        .contains(tenantActive.getId())
        .doesNotContain(globalActive.getId(), globalDeleted.getId(), tenantDeleted.getId());

    assertThat(configurationService.getConfigurations("tenant-a", true))
        .extracting(Configuration::getId)
        .contains(tenantActive.getId(), tenantDeleted.getId())
        .doesNotContain(globalActive.getId(), globalDeleted.getId());
  }

  @Test
  public void shouldAllowTenantMemberToRetrieveOwnConfigurations() {
    ConfigurationEntity tenantConfiguration = insertConfiguration(
        "configuration-test-" + UUID.randomUUID(), "tenant-a", Configuration.STATUS_ACTIVE);
    identityService.setAuthentication("tenant-user", null, Collections.singletonList("tenant-a"));

    assertThat(configurationService.getConfigurations("tenant-a", false))
        .extracting(Configuration::getId)
        .contains(tenantConfiguration.getId());
  }

  @Test
  public void shouldRejectTenantMemberRetrievingAnotherTenantConfigurations() {
    identityService.setAuthentication("tenant-user", null, Collections.singletonList("tenant-a"));

    assertThatThrownBy(() -> configurationService.getConfigurations("tenant-b", true))
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining("not authorized for tenant 'tenant-b'");
  }

  @Test
  public void shouldRejectAnotherTenantWhenEngineTenantCheckIsDisabled() {
    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("tenant-user", null, Collections.singletonList("tenant-a"));

    assertThatThrownBy(() -> configurationService.getConfigurations("tenant-b", true))
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining("not authorized for tenant 'tenant-b'");
  }

  @Test
  public void shouldRejectTenantMemberRetrievingGlobalConfigurations() {
    identityService.setAuthentication("tenant-user", null, Collections.singletonList("tenant-a"));

    assertThatThrownBy(() -> configurationService.getConfigurations(null, false))
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining("Only platform administrators");
  }

  @Test
  public void shouldRejectUnauthenticatedConfigurationRetrieval() {
    identityService.clearAuthentication();

    assertThatThrownBy(() -> configurationService.getConfigurations("tenant-a", false))
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining("Authentication is required");
  }

  protected void authenticateAsAdmin() {
    identityService.setAuthentication(
        "admin",
        Collections.singletonList(Groups.CAMUNDA_ADMIN),
        null);
  }

  protected ConfigurationEntity insertConfiguration(String configKey, String tenantId, String status) {
    ConfigurationEntity configuration = processEngineConfiguration.getCommandExecutorTxRequired()
        .execute(commandContext -> {
          Date now = new Date();
          ConfigurationEntity entity = new ConfigurationEntity(configKey, "config-value", tenantId);
          entity.setVersion(1);
          entity.setStatus(status);
          entity.setCreatedAt(now);
          entity.setUpdatedAt(now);
          commandContext.getConfigurationManager().insertConfiguration(entity);
          return entity;
        });
    configurationIds.add(configuration.getId());
    return configuration;
  }
}
