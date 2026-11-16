package org.finos.fluxnova.bpm.engine.authorization;

public enum ConfigurationPermissions implements Permission {

  NONE("NONE", 0),
  ALL("ALL", Integer.MAX_VALUE),
  READ("READ", 2),
  UPDATE("UPDATE", 4),
  CREATE("CREATE", 8),
  DELETE("DELETE", 16);

  private static final Resource[] RESOURCES = new Resource[] { Resources.CONFIGURATION };

  private final String name;
  private final int id;

  ConfigurationPermissions(String name, int id) {
    this.name = name;
    this.id = id;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public int getValue() {
    return id;
  }

  @Override
  public Resource[] getTypes() {
    return RESOURCES;
  }
}
