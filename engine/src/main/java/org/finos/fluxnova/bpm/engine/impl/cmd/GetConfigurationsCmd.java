package org.finos.fluxnova.bpm.engine.impl.cmd;

import java.io.Serializable;
import java.util.List;

import org.finos.fluxnova.bpm.engine.configuration.Configuration;
import org.finos.fluxnova.bpm.engine.impl.interceptor.Command;
import org.finos.fluxnova.bpm.engine.impl.interceptor.CommandContext;

public class GetConfigurationsCmd implements Command<List<Configuration>>, Serializable {

  private static final long serialVersionUID = 1L;

  protected String tenantId;
  protected boolean includeInactive;

  public GetConfigurationsCmd(String tenantId, boolean includeInactive) {
    this.tenantId = tenantId;
    this.includeInactive = includeInactive;
  }

  public List<Configuration> execute(CommandContext commandContext) {
    return commandContext.getConfigurationManager().findConfigurations(tenantId, includeInactive);
  }
}
