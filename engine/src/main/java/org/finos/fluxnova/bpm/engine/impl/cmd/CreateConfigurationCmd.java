package org.finos.fluxnova.bpm.engine.impl.cmd;

import static org.finos.fluxnova.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

import java.io.Serializable;
import java.util.Date;

import org.finos.fluxnova.bpm.engine.BadUserRequestException;
import org.finos.fluxnova.bpm.engine.configuration.Configuration;
import org.finos.fluxnova.bpm.engine.exception.NotValidException;
import org.finos.fluxnova.bpm.engine.impl.context.Context;
import org.finos.fluxnova.bpm.engine.impl.identity.Authentication;
import org.finos.fluxnova.bpm.engine.impl.interceptor.Command;
import org.finos.fluxnova.bpm.engine.impl.interceptor.CommandContext;
import org.finos.fluxnova.bpm.engine.impl.persistence.entity.ConfigurationEntity;
import org.finos.fluxnova.bpm.engine.impl.util.ClockUtil;

public class CreateConfigurationCmd implements Command<Configuration>, Serializable {

  private static final long serialVersionUID = 1L;

  protected String configKey;
  protected String configValue;
  protected String tenantId;

  public CreateConfigurationCmd(String configKey, String configValue, String tenantId) {
    this.configKey = configKey;
    this.configValue = configValue;
    this.tenantId = tenantId;
  }

  public Configuration execute(CommandContext commandContext) {
    ensureNotNull(NotValidException.class, "configKey is mandatory", "configKey", configKey);
    ensureNotNull(NotValidException.class, "configValue is mandatory", "configValue", configValue);

    if (commandContext.getConfigurationManager().existsActiveConfiguration(configKey, tenantId)) {
      String scope = tenantId == null ? "the global scope" : "tenant '" + tenantId + "'";
      throw new BadUserRequestException(
          "An active configuration with key '" + configKey + "' already exists for " + scope);
    }

    Date now = ClockUtil.getCurrentTime();
    Authentication authentication = commandContext.getAuthentication();
    String userId =  authentication == null ? null : authentication.getUserId();

    ConfigurationEntity configuration = new ConfigurationEntity(configKey, configValue, tenantId);
    configuration.setVersion(1);
    configuration.setStatus(Configuration.STATUS_ACTIVE);
    configuration.setCreatedBy(userId);
    configuration.setCreatedAt(now);
    configuration.setUpdatedBy(userId);
    configuration.setUpdatedAt(now);

    commandContext.getConfigurationManager().insertConfiguration(configuration);

    return configuration;
  }

}
