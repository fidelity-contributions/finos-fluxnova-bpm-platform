package org.finos.fluxnova.bpm.engine;

import java.util.List;

import org.finos.fluxnova.bpm.engine.configuration.Configuration;

/**
 * <p>Service which provides access to process configurations: key/value pairs
 * which are either global or scoped to a single tenant.</p>
 */
public interface ConfigurationService {

  /**
   * <p>Creates a new configuration entry.</p>
   *
   * <p>If {@code tenantId} is {@code null} the entry is created as a global
   * configuration which applies to all tenants.</p>
   *
   * @param configKey the configuration key, must not be {@code null}
   * @param configValue the configuration value, must not be {@code null}
   * @param tenantId the tenant to scope the entry to, or {@code null} for a global entry
   * @return the newly created configuration
   *
   * @throws org.finos.fluxnova.bpm.engine.exception.NotValidException
   *          if {@code configKey} or {@code configValue} is {@code null}
   * @throws BadUserRequestException
   *          if an active configuration with the same key already exists in the same scope
   */
  Configuration createConfiguration(String configKey, String configValue, String tenantId);

  /**
   * <p>Returns the configuration with the given id.</p>
   *
   * @param configurationId the id of the configuration, must not be {@code null}
   * @return the configuration, or {@code null} if no configuration exists for that id
   */
  Configuration getConfiguration(String configurationId);

  /**
   * Returns configurations for the requested scope.
   *
   * <p>If {@code tenantId} is {@code null}, the global default configurations
   * are returned. Inactive configurations are excluded unless
   * {@code includeInactive} is {@code true}.</p>
   *
   * @param tenantId the tenant scope, or {@code null} for global defaults
   * @param includeInactive whether to include inactive configurations
   * @return configurations in the requested scope
   */
  List<Configuration> getConfigurations(String tenantId, boolean includeInactive);

  /**
   * <p>Updates the value of an active configuration by creating a new version.</p>
   *
   * <p>The existing entry is marked {@link Configuration#STATUS_INACTIVE} and a new
   * {@link Configuration#STATUS_ACTIVE} entry with the same key and tenant scope is
   * created. Its version is one greater than the highest existing version for that
   * key and scope.</p>
   *
   * @param configurationId the id of the active configuration to update
   * @param configValue the new configuration value, must not be blank
   * @return the newly created active configuration version
   *
   * @throws org.finos.fluxnova.bpm.engine.exception.NotValidException
   *          if {@code configurationId} or {@code configValue} is missing or blank
   * @throws org.finos.fluxnova.bpm.engine.exception.NotFoundException
   *          if no configuration exists for {@code configurationId}
   * @throws BadUserRequestException
   *          if the configuration is not active
   */
  Configuration updateConfiguration(String configurationId, String configValue);

}
