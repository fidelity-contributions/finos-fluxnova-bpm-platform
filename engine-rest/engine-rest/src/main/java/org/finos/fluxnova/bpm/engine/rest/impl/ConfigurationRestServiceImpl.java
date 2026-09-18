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
package org.finos.fluxnova.bpm.engine.rest.impl;

import tools.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriInfo;
import org.finos.fluxnova.bpm.engine.BadUserRequestException;
import org.finos.fluxnova.bpm.engine.ConfigurationService;
import org.finos.fluxnova.bpm.engine.ProcessEngine;
import org.finos.fluxnova.bpm.engine.ProcessEngineException;
import org.finos.fluxnova.bpm.engine.configuration.Configuration;
import org.finos.fluxnova.bpm.engine.exception.NotValidException;
import org.finos.fluxnova.bpm.engine.impl.util.ExceptionUtil;
import org.finos.fluxnova.bpm.engine.rest.ConfigurationRestService;
import org.finos.fluxnova.bpm.engine.rest.dto.configuration.ConfigurationDto;
import org.finos.fluxnova.bpm.engine.rest.dto.configuration.CreateConfigurationDto;
import org.finos.fluxnova.bpm.engine.rest.exception.InvalidRequestException;

public class ConfigurationRestServiceImpl extends AbstractRestProcessEngineAware implements ConfigurationRestService {

  public ConfigurationRestServiceImpl(String engineName, final ObjectMapper objectMapper) {
    super(engineName, objectMapper);
  }

  @Override
  public Response createConfiguration(CreateConfigurationDto configurationDto, UriInfo uriInfo) {
    if (configurationDto == null) {
      throw new InvalidRequestException(Status.BAD_REQUEST, "Request body must not be null");
    }

    ProcessEngine engine = getProcessEngine();
    ConfigurationService configurationService = engine.getConfigurationService();

    Configuration newConfiguration;
    try {
      newConfiguration = configurationService.createConfiguration(
          trimToNull(configurationDto.getConfigKey()),
          trimToNull(configurationDto.getConfigValue()),
          trimToNull(configurationDto.getTenantId()));

    } catch (NotValidException e) {
      throw new InvalidRequestException(Status.BAD_REQUEST, e, "Could not save configuration: " + e.getMessage());

    } catch (BadUserRequestException e) {
      throw new InvalidRequestException(Status.CONFLICT, e, "Could not save configuration: " + e.getMessage());

    } catch (ProcessEngineException e) {
      if (isConfigurationUniqueConstraintViolation(e)) {
        throw new InvalidRequestException(Status.CONFLICT, e,
            "Could not save configuration: An active configuration with key '"
                + trimToNull(configurationDto.getConfigKey()) + "' already exists for "
                + getScopeDescription(trimToNull(configurationDto.getTenantId())));
      }
      throw e;
    }

    URI location = uriInfo.getBaseUriBuilder()
        .path(relativeRootResourcePath)
        .path(ConfigurationRestService.PATH)
        .path(newConfiguration.getId())
        .build();

    return Response.created(location).entity(ConfigurationDto.fromConfiguration(newConfiguration)).build();
  }

  @Override
  public List<ConfigurationDto> getConfigurations(String tenantId, Boolean includeInactive) {
    List<ConfigurationDto> configurations = new ArrayList<ConfigurationDto>();
    for (Configuration configuration : getProcessEngine().getConfigurationService()
        .getConfigurations(trimToNull(tenantId), Boolean.TRUE.equals(includeInactive))) {
      configurations.add(ConfigurationDto.fromConfiguration(configuration));
    }
    return configurations;
  }

  @Override
  public ConfigurationDto getConfiguration(String configurationId) {
    String normalizedConfigurationId = trimToNull(configurationId);
    if (normalizedConfigurationId == null) {
      throw new InvalidRequestException(Status.BAD_REQUEST, "Configuration id must not be blank");
    }

    Configuration configuration = getProcessEngine().getConfigurationService()
        .getConfiguration(normalizedConfigurationId);
    if (configuration == null) {
      throw new InvalidRequestException(
          Status.NOT_FOUND,
          "Configuration with id '" + normalizedConfigurationId + "' does not exist");
    }

    return ConfigurationDto.fromConfiguration(configuration);
  }

  protected static String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  protected static boolean isConfigurationUniqueConstraintViolation(ProcessEngineException exception) {
    return ExceptionUtil.checkConstraintViolationException(exception)
        && containsMessage(exception, "ACT_UNIQ_GE_CONFIG");
  }

  protected static boolean containsMessage(Throwable throwable, String value) {
    while (throwable != null) {
      String message = throwable.getMessage();
      if (message != null && message.toUpperCase().contains(value)) {
        return true;
      }
      throwable = throwable.getCause();
    }
    return false;
  }

  protected static String getScopeDescription(String tenantId) {
    return tenantId == null ? "the global scope" : "tenant '" + tenantId + "'";
  }

}
