package org.finos.fluxnova.bpm.engine.test.api.authorization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.finos.fluxnova.bpm.engine.authorization.ConfigurationAuthorization.GLOBAL_RESOURCE_ID;
import static org.finos.fluxnova.bpm.engine.authorization.ConfigurationPermissions.CREATE;
import static org.finos.fluxnova.bpm.engine.authorization.ConfigurationPermissions.DELETE;
import static org.finos.fluxnova.bpm.engine.authorization.ConfigurationPermissions.READ;
import static org.finos.fluxnova.bpm.engine.authorization.ConfigurationPermissions.UPDATE;
import static org.finos.fluxnova.bpm.engine.authorization.Resources.CONFIGURATION;
import static org.finos.fluxnova.bpm.engine.impl.util.ResourceTypeUtil.getPermissionByNameAndResourceType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.finos.fluxnova.bpm.engine.AuthorizationException;
import org.finos.fluxnova.bpm.engine.ConfigurationService;
import org.finos.fluxnova.bpm.engine.ProcessEngineException;
import org.finos.fluxnova.bpm.engine.authorization.Authorization;
import org.finos.fluxnova.bpm.engine.configuration.Configuration;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.ConfigurationEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConfigurationAuthorizationTest extends AuthorizationTest {

  private final List<String> configurationIds = new ArrayList<>();
  private ConfigurationService configurationService;

  @Override
  @BeforeEach
  public void setUp() throws Exception {
    super.setUp();
    processEngineConfiguration.setTenantCheckEnabled(true);
    configurationService = processEngine.getConfigurationService();
  }

  @AfterEach
  void removeConfigurations() {
    processEngineConfiguration.getCommandExecutorTxRequired().execute(commandContext -> {
      for (String id : configurationIds) {
        ConfigurationEntity configuration = commandContext.getConfigurationManager().findConfigurationById(id);
        if (configuration != null) {
          commandContext.getDbEntityManager().delete(configuration);
        }
      }
      return null;
    });
    processEngineConfiguration.setTenantCheckEnabled(false);
  }

  @Test
  void requiresCreatePermissionForGlobalScope() {
    assertThatThrownBy(() -> configurationService.createConfiguration(uniqueKey(), "value", null))
        .isInstanceOf(AuthorizationException.class);

    createGrantAuthorization(CONFIGURATION, GLOBAL_RESOURCE_ID, userId, CREATE);
    Configuration created = configurationService.createConfiguration(uniqueKey(), "value", null);

    assertThat(created.getStatus()).isEqualTo(Configuration.STATUS_ACTIVE);
    configurationIds.add(created.getId());
  }

  @Test
  void supportsGroupConfigurationGrants() {
    createGrantAuthorizationGroup(CONFIGURATION, GLOBAL_RESOURCE_ID, groupId, CREATE);

    Configuration created = configurationService.createConfiguration(uniqueKey(), "value", null);

    assertThat(created.getStatus()).isEqualTo(Configuration.STATUS_ACTIVE);
    configurationIds.add(created.getId());
  }

  @Test
  void registersConfigurationPermissionsForRestAuthorizationValidation() {
    assertThat(getPermissionByNameAndResourceType("READ", CONFIGURATION.resourceType()))
        .isEqualTo(READ);
    assertThat(getPermissionByNameAndResourceType("CREATE", CONFIGURATION.resourceType()))
        .isEqualTo(CREATE);
    assertThat(getPermissionByNameAndResourceType("UPDATE", CONFIGURATION.resourceType()))
        .isEqualTo(UPDATE);
    assertThat(getPermissionByNameAndResourceType("DELETE", CONFIGURATION.resourceType()))
        .isEqualTo(DELETE);
  }

  @Test
  void requiresReadUpdateAndDeletePermissionsForPersistedTenantScope() {
    String key = uniqueKey();
    ConfigurationEntity configuration = createConfiguration(key, "tenant-a");
    identityService.setAuthentication(userId, Arrays.asList(groupId), Arrays.asList("tenant-a"));

    assertThatThrownBy(() -> configurationService.getConfiguration(configuration.getId()))
        .isInstanceOf(AuthorizationException.class);
    createGrantAuthorization(CONFIGURATION, GLOBAL_RESOURCE_ID, userId, READ);
    assertThatThrownBy(() -> configurationService.getConfiguration(configuration.getId()))
        .isInstanceOf(AuthorizationException.class);

    Authorization tenantGrant = createGrantAuthorization(CONFIGURATION, "tenant-a", userId, READ);
    assertThat(configurationService.getConfiguration(configuration.getId()).getId())
        .isEqualTo(configuration.getId());

    assertThatThrownBy(() -> configurationService.updateConfiguration(configuration.getId(), "updated"))
        .isInstanceOf(AuthorizationException.class);
    assertThat(configurationService.getConfiguration(configuration.getId()).getStatus())
        .isEqualTo(Configuration.STATUS_ACTIVE);

    tenantGrant.addPermission(UPDATE);
    authorizationService.saveAuthorization(tenantGrant);
    Configuration updated = configurationService.updateConfiguration(configuration.getId(), "updated");
    configurationIds.add(updated.getId());

    assertThatThrownBy(() -> configurationService.deleteConfiguration(updated.getId()))
        .isInstanceOf(AuthorizationException.class);
    assertThat(configurationService.getConfiguration(updated.getId()).getStatus())
        .isEqualTo(Configuration.STATUS_ACTIVE);

    tenantGrant.addPermission(DELETE);
    authorizationService.saveAuthorization(tenantGrant);
    configurationService.deleteConfiguration(updated.getId());
    assertThat(configurationService.getConfiguration(updated.getId()).getStatus())
        .isEqualTo(Configuration.STATUS_DELETED);
  }

  @Test
  void tenantListRequiresGlobalAndTenantReadPermissionsAndMembership() {
    String key = uniqueKey();
    createConfiguration(key, null);
    ConfigurationEntity tenantConfiguration = createConfiguration(key, "tenant-a");

    identityService.setAuthentication(userId, Arrays.asList(groupId), Arrays.asList("tenant-a"));
    createGrantAuthorization(CONFIGURATION, GLOBAL_RESOURCE_ID, userId, READ);
    assertThatThrownBy(() -> configurationService.getConfigurations("tenant-a", false))
        .isInstanceOf(AuthorizationException.class);

    createGrantAuthorization(CONFIGURATION, "tenant-a", userId, READ);
    identityService.setAuthentication(userId, Arrays.asList(groupId));
    assertThatThrownBy(() -> configurationService.getConfigurations("tenant-a", false))
        .isInstanceOf(ProcessEngineException.class);

    identityService.setAuthentication(userId, Arrays.asList(groupId), Arrays.asList("tenant-a"));
    assertThat(configurationService.getConfigurations("tenant-a", false))
        .extracting(Configuration::getId)
        .contains(tenantConfiguration.getId());
  }

  @Test
  void listsGlobalConfigurationsWithGlobalReadPermission() {
    ConfigurationEntity global = createConfiguration(uniqueKey(), null);

    assertThatThrownBy(() -> configurationService.getConfigurations(null, false))
        .isInstanceOf(AuthorizationException.class);

    createGrantAuthorization(CONFIGURATION, GLOBAL_RESOURCE_ID, userId, READ);
    assertThat(configurationService.getConfigurations(null, false))
        .extracting(Configuration::getId)
        .contains(global.getId());
  }

  private ConfigurationEntity createConfiguration(String key, String tenantId) {
    ConfigurationEntity configuration = runWithoutAuthorization(() -> {
      identityService.setAuthentication(userId, Arrays.asList(groupId),
          tenantId == null ? null : Arrays.asList(tenantId));
      return (ConfigurationEntity) configurationService.createConfiguration(key, "value", tenantId);
    });
    configurationIds.add(configuration.getId());
    identityService.setAuthentication(userId, Arrays.asList(groupId));
    return configuration;
  }

  private String uniqueKey() {
    return "configuration-auth-" + UUID.randomUUID();
  }
}
