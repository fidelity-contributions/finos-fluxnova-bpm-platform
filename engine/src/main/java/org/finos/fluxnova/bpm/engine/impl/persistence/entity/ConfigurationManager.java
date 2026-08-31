package org.finos.fluxnova.bpm.engine.impl.persistence.entity;

import java.util.HashMap;
import java.util.Map;

import org.finos.fluxnova.bpm.engine.configuration.Configuration;
import org.finos.fluxnova.bpm.engine.impl.persistence.AbstractManager;

public class ConfigurationManager extends AbstractManager {

  public void insertConfiguration(ConfigurationEntity configuration) {
    getDbEntityManager().insert(configuration);
  }

  public ConfigurationEntity findConfigurationById(String configurationId) {
    return getDbEntityManager().selectById(ConfigurationEntity.class, configurationId);
  }

  /**
   * Checks whether an {@link Configuration#STATUS_ACTIVE} entry already exists for the
   * given scope. A {@code null} tenant id addresses the global scope.
   */
  public boolean existsActiveConfiguration(String configKey, String tenantId) {
    Map<String, Object> parameters = new HashMap<String, Object>();
    parameters.put("configKey", configKey);
    parameters.put("tenantId", tenantId);
    parameters.put("status", Configuration.STATUS_ACTIVE);

    Long count = (Long) getDbEntityManager().selectOne("selectActiveConfigurationCount", parameters);
    return count != null && count > 0;
  }

}
