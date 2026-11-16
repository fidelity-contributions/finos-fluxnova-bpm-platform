package org.finos.fluxnova.bpm.engine.authorization;

public final class ConfigurationAuthorization {

  public static final String GLOBAL_RESOURCE_ID = "__global__";

  private ConfigurationAuthorization() {
  }

  public static String resourceId(String tenantId) {
    return tenantId == null ? GLOBAL_RESOURCE_ID : tenantId;
  }
}
