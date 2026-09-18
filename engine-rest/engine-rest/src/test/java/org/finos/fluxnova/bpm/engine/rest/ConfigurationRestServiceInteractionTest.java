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
package org.finos.fluxnova.bpm.engine.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import jakarta.ws.rs.core.Response.Status;
import org.finos.fluxnova.bpm.engine.ConfigurationService;
import org.finos.fluxnova.bpm.engine.configuration.Configuration;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.ConfigurationEntity;
import org.finos.fluxnova.bpm.engine.rest.util.container.TestContainerRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.restassured.http.ContentType;

public class ConfigurationRestServiceInteractionTest extends AbstractRestServiceTest {

  @RegisterExtension
  public static TestContainerRule rule = new TestContainerRule();

  protected static final String CONFIGURATIONS_URL = TEST_RESOURCE_ROOT_PATH + ConfigurationRestService.PATH;

  private ConfigurationService configurationService;

  @BeforeEach
  public void setUpConfigurationService() {
    configurationService = mock(ConfigurationService.class);
    when(processEngine.getConfigurationService()).thenReturn(configurationService);
  }

  @Test
  public void shouldRetrieveActiveGlobalConfigurationsByDefault() {
    when(configurationService.getConfigurations(isNull(), eq(false)))
        .thenReturn(Arrays.asList(configuration("global-key", "global-value", null, Configuration.STATUS_ACTIVE)));

    given()
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
        .contentType(ContentType.JSON)
        .body("size()", equalTo(1))
        .body("[0].configKey", equalTo("global-key"))
        .body("[0].configValue", equalTo("global-value"))
        .body("[0].status", equalTo(Configuration.STATUS_ACTIVE))
    .when()
      .get(CONFIGURATIONS_URL);

    verify(configurationService).getConfigurations(null, false);
  }

  @Test
  public void shouldRetrieveActiveTenantConfigurations() {
    when(configurationService.getConfigurations(eq("tenant-a"), eq(false)))
        .thenReturn(Arrays.asList(configuration("tenant-key", "tenant-value", "tenant-a", Configuration.STATUS_ACTIVE)));

    given()
      .queryParam("tenantId", "tenant-a")
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
        .contentType(ContentType.JSON)
        .body("size()", equalTo(1))
        .body("[0].tenantId", equalTo("tenant-a"))
        .body("[0].status", equalTo(Configuration.STATUS_ACTIVE))
    .when()
      .get(CONFIGURATIONS_URL);

    verify(configurationService).getConfigurations("tenant-a", false);
  }

  @Test
  public void shouldIncludeInactiveConfigurationsWhenRequested() {
    when(configurationService.getConfigurations(isNull(), eq(true)))
        .thenReturn(Arrays.asList(configuration("deleted-key", "old-value", null, Configuration.STATUS_DELETED)));

    given()
      .queryParam("includeInactive", true)
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
        .contentType(ContentType.JSON)
        .body("size()", equalTo(1))
        .body("[0].status", equalTo(Configuration.STATUS_DELETED))
    .when()
      .get(CONFIGURATIONS_URL);

    verify(configurationService).getConfigurations(null, true);
  }

  @Test
  public void shouldRetrieveConfigurationById() {
    when(configurationService.getConfiguration("configuration-id"))
        .thenReturn(configuration("tenant-key", "tenant-value", "tenant-a", Configuration.STATUS_DELETED));

    given()
    .then()
      .expect()
        .statusCode(Status.OK.getStatusCode())
        .contentType(ContentType.JSON)
        .body("id", equalTo("configuration-id"))
        .body("configKey", equalTo("tenant-key"))
        .body("tenantId", equalTo("tenant-a"))
        .body("status", equalTo(Configuration.STATUS_DELETED))
    .when()
      .get(CONFIGURATIONS_URL + "/configuration-id");

    verify(configurationService).getConfiguration("configuration-id");
  }

  @Test
  public void shouldReturnNotFoundForMissingConfiguration() {
    when(configurationService.getConfiguration("missing-id")).thenReturn(null);

    given()
    .then()
      .expect()
        .statusCode(Status.NOT_FOUND.getStatusCode())
        .contentType(ContentType.JSON)
        .body("message", equalTo("Configuration with id 'missing-id' does not exist"))
    .when()
      .get(CONFIGURATIONS_URL + "/missing-id");
  }

  private ConfigurationEntity configuration(String key, String value, String tenantId, String status) {
    ConfigurationEntity configuration = new ConfigurationEntity();
    configuration.setId("configuration-id");
    configuration.setConfigKey(key);
    configuration.setConfigValue(value);
    configuration.setTenantId(tenantId);
    configuration.setVersion(1);
    configuration.setStatus(status);
    return configuration;
  }
}
