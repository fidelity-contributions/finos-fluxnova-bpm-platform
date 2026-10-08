package org.finos.fluxnova.bpm.engine.impl;

import org.finos.fluxnova.bpm.engine.ConfigurationService;
import org.finos.fluxnova.bpm.engine.configuration.Configuration;
import org.finos.fluxnova.bpm.engine.impl.cmd.CreateConfigurationCmd;
import org.finos.fluxnova.bpm.engine.impl.cmd.GetConfigurationCmd;

public class ConfigurationServiceImpl extends ServiceImpl implements ConfigurationService {

  public Configuration createConfiguration(String configKey, String configValue, String tenantId) {
    return commandExecutor.execute(new CreateConfigurationCmd(configKey, configValue, tenantId));
  }

  public Configuration getConfiguration(String configurationId) {
    return commandExecutor.execute(new GetConfigurationCmd(configurationId));
  }

}
