package org.finos.fluxnova.bpm.engine.rest;

import org.finos.fluxnova.bpm.engine.rest.dto.configuration.CreateConfigurationDto;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Produces(MediaType.APPLICATION_JSON)
public interface ConfigurationRestService {

  public static final String PATH = "/configurations";

  /**
   * Creates a new configuration entry.
   *
   * <p>If {@code tenantId} is omitted or blank the entry is treated as a global
   * configuration applicable to all tenants. A duplicate {@code (configKey, tenantId)}
   * combination with {@code STATUS_=ACTIVE} results in a {@code 409 Conflict}.</p>
   *
   * @param configurationDto the create request containing {@code configKey},
   *                         {@code configValue} and an optional {@code tenantId}
   * @param uriInfo          JAX-RS URI context used to build the {@code Location} header
   * @return {@code 201 Created} with the persisted configuration and a {@code Location}
   *         header pointing to the new resource
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  Response createConfiguration(CreateConfigurationDto configurationDto, @Context UriInfo uriInfo);

}
