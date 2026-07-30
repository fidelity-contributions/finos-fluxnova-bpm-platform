/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.finos.fluxnova.bpm.spring.boot.starter.rest;

import org.finos.fluxnova.bpm.engine.rest.security.auth.ProcessEngineAuthenticationFilter;
import org.finos.fluxnova.bpm.engine.rest.security.auth.impl.JwtAuthenticationPlugin;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot auto-configuration for JWT-based REST API authentication.
 *
 * <p>This configuration is activated automatically when {@code fluxnova.bpm.jwt.enabled=true}
 * is set — no application code changes are required. All settings are read from
 * {@link JwtAuthenticationProperties} which can be supplied as environment variables,
 * application.properties, or application.yml.
 *
 * <p><b>Minimal environment-variable example (Microsoft Entra ID):</b>
 * <pre>
 * FLUXNOVA_BPM_JWT_ENABLED=true
 * FLUXNOVA_BPM_JWT_JWKS_URL=https://login.microsoftonline.com/{tenant}/discovery/v2.0/keys
 * FLUXNOVA_BPM_JWT_ISSUER=https://login.microsoftonline.com/{tenant}/v2.0
 * FLUXNOVA_BPM_JWT_AUDIENCE=api://your-client-id
 * FLUXNOVA_BPM_JWT_USER_CLAIM_NAME=preferred_username
 * FLUXNOVA_BPM_JWT_GROUPS_CLAIM_NAME=groups
 * </pre>
 *
 * <p>When disabled (the default), no beans are created and the existing authentication
 * mechanism is unaffected.
 */
@Configuration
@ConditionalOnProperty(prefix = JwtAuthenticationProperties.PREFIX, name = "enabled", havingValue = "true")
@EnableConfigurationProperties(JwtAuthenticationProperties.class)
@AutoConfigureAfter(FluxnovaBpmRestJerseyAutoConfiguration.class)
public class JwtAuthenticationAutoConfiguration {

  /**
   * Creates and initialises the {@link JwtAuthenticationPlugin} from the bound properties.
   * The plugin validates the required fields and sets up the underlying
   * {@link org.finos.fluxnova.bpm.engine.rest.security.auth.impl.JwtAuthenticationProvider}.
   */
  @Bean
  public JwtAuthenticationPlugin jwtAuthenticationPlugin(JwtAuthenticationProperties props) {
    JwtAuthenticationPlugin plugin = new JwtAuthenticationPlugin();
    plugin.setJwksUrl(props.getJwksUrl());
    plugin.setIssuer(props.getIssuer());
    plugin.setAudience(props.getAudience());
    plugin.setHeaderName(props.getHeaderName());
    plugin.setHeaderPrefix(props.getHeaderPrefix());
    plugin.setUserClaimName(props.getUserClaimName());
    if (props.getGroupsClaimName() != null && !props.getGroupsClaimName().isEmpty()) {
      plugin.setGroupsClaimName(props.getGroupsClaimName());
    }
    plugin.initializeProvider();
    return plugin;
  }

  /**
   * Registers the {@link ProcessEngineAuthenticationFilter} with the JWT provider
   * on the {@code /engine-rest/*} URL pattern.
   *
   * <p>The filter is ordered at {@code 1} so it runs before any other application filters.
   * If you need to change the order or URL pattern, define your own
   * {@code FilterRegistrationBean<ProcessEngineAuthenticationFilter>} bean — Spring Boot
   * will use yours instead via {@code @ConditionalOnMissingBean}.
   */
  @Bean
  public FilterRegistrationBean<ProcessEngineAuthenticationFilter> jwtAuthenticationFilter(
      JwtAuthenticationPlugin jwtAuthenticationPlugin) {
    ProcessEngineAuthenticationFilter filter = new ProcessEngineAuthenticationFilter();
    filter.setAuthenticationProvider(jwtAuthenticationPlugin.getAuthenticationProvider());

    FilterRegistrationBean<ProcessEngineAuthenticationFilter> registration =
        new FilterRegistrationBean<>(filter);
    registration.addUrlPatterns("/engine-rest/*");
    registration.setOrder(1);
    return registration;
  }
}
