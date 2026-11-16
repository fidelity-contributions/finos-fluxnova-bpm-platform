package org.finos.fluxnova.bpm.engine.impl.cmd;

import static org.finos.fluxnova.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

import java.io.Serializable;

import org.finos.fluxnova.bpm.engine.configuration.Configuration;
import org.finos.fluxnova.bpm.engine.impl.cfg.CommandChecker;
import org.finos.fluxnova.bpm.engine.impl.interceptor.Command;
import org.finos.fluxnova.bpm.engine.impl.interceptor.CommandContext;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.ConfigurationEntity;

public class GetConfigurationCmd implements Command<Configuration>, Serializable {

  private static final long serialVersionUID = 1L;

  protected String configurationId;

  public GetConfigurationCmd(String configurationId) {
    this.configurationId = configurationId;
  }

  public Configuration execute(CommandContext commandContext) {
    ensureNotNull("configurationId", configurationId);

    ConfigurationEntity configuration = commandContext
        .getConfigurationManager()
        .findConfigurationById(configurationId);
    if (configuration != null) {
      for (CommandChecker checker : commandContext.getProcessEngineConfiguration().getCommandCheckers()) {
        checker.checkReadConfiguration(configuration.getTenantId());
      }
    }
    return configuration;
  }

}
