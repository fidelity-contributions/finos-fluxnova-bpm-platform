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
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link JwtAuthenticationAutoConfiguration} using {@link ApplicationContextRunner}
 * so the full Spring Boot context is never started — only the JWT beans are tested in isolation.
 */
class JwtAuthenticationAutoConfigurationTest {

  private static final String JWKS_URL  = "https://idp.example.com/.well-known/jwks.json";
  private static final String ISSUER    = "https://idp.example.com";
  private static final String AUDIENCE  = "test-api";

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(JwtAuthenticationAutoConfiguration.class));

  // -------------------------------------------------------------------------
  // Disabled by default
  // -------------------------------------------------------------------------

  @Test
  void noBeansCreatedWhenNotEnabled() {
    contextRunner.run(ctx -> {
      assertThat(ctx).doesNotHaveBean(JwtAuthenticationPlugin.class);
      assertThat(ctx).doesNotHaveBean("jwtAuthenticationFilter");
    });
  }

  @Test
  void noBeansCreatedWhenExplicitlyDisabled() {
    contextRunner
        .withPropertyValues("fluxnova.bpm.jwt.enabled=false")
        .run(ctx -> {
          assertThat(ctx).doesNotHaveBean(JwtAuthenticationPlugin.class);
          assertThat(ctx).doesNotHaveBean("jwtAuthenticationFilter");
        });
  }

  // -------------------------------------------------------------------------
  // Happy path — all required properties supplied
  // -------------------------------------------------------------------------

  @Test
  void pluginBeanCreatedWhenEnabled() {
    contextRunner
        .withPropertyValues(
            "fluxnova.bpm.jwt.enabled=true",
            "fluxnova.bpm.jwt.jwks-url=" + JWKS_URL,
            "fluxnova.bpm.jwt.issuer=" + ISSUER,
            "fluxnova.bpm.jwt.audience=" + AUDIENCE)
        .run(ctx -> assertThat(ctx).hasSingleBean(JwtAuthenticationPlugin.class));
  }

  @Test
  void filterBeanCreatedWhenEnabled() {
    contextRunner
        .withPropertyValues(
            "fluxnova.bpm.jwt.enabled=true",
            "fluxnova.bpm.jwt.jwks-url=" + JWKS_URL,
            "fluxnova.bpm.jwt.issuer=" + ISSUER,
            "fluxnova.bpm.jwt.audience=" + AUDIENCE)
        .run(ctx -> assertThat(ctx).hasBean("jwtAuthenticationFilter"));
  }

  @Test
  void pluginPicksUpCustomUserClaim() {
    contextRunner
        .withPropertyValues(
            "fluxnova.bpm.jwt.enabled=true",
            "fluxnova.bpm.jwt.jwks-url=" + JWKS_URL,
            "fluxnova.bpm.jwt.issuer=" + ISSUER,
            "fluxnova.bpm.jwt.audience=" + AUDIENCE,
            "fluxnova.bpm.jwt.user-claim-name=preferred_username")
        .run(ctx -> {
          JwtAuthenticationPlugin plugin = ctx.getBean(JwtAuthenticationPlugin.class);
          assertThat(plugin.getUserClaimName()).isEqualTo("preferred_username");
        });
  }

  @Test
  void pluginPicksUpGroupsClaim() {
    contextRunner
        .withPropertyValues(
            "fluxnova.bpm.jwt.enabled=true",
            "fluxnova.bpm.jwt.jwks-url=" + JWKS_URL,
            "fluxnova.bpm.jwt.issuer=" + ISSUER,
            "fluxnova.bpm.jwt.audience=" + AUDIENCE,
            "fluxnova.bpm.jwt.groups-claim-name=groups")
        .run(ctx -> {
          JwtAuthenticationPlugin plugin = ctx.getBean(JwtAuthenticationPlugin.class);
          assertThat(plugin.getGroupsClaimName()).isEqualTo("groups");
        });
  }

  // -------------------------------------------------------------------------
  // Missing required properties
  // -------------------------------------------------------------------------

  @Test
  void failsWhenJwksUrlMissing() {
    contextRunner
        .withPropertyValues(
            "fluxnova.bpm.jwt.enabled=true",
            "fluxnova.bpm.jwt.issuer=" + ISSUER,
            "fluxnova.bpm.jwt.audience=" + AUDIENCE)
        .run(ctx -> assertThat(ctx).hasFailed()
            .getFailure().hasMessageContaining("jwksUrl"));
  }

  @Test
  void failsWhenIssuerMissing() {
    contextRunner
        .withPropertyValues(
            "fluxnova.bpm.jwt.enabled=true",
            "fluxnova.bpm.jwt.jwks-url=" + JWKS_URL,
            "fluxnova.bpm.jwt.audience=" + AUDIENCE)
        .run(ctx -> assertThat(ctx).hasFailed()
            .getFailure().hasMessageContaining("issuer"));
  }

  @Test
  void failsWhenAudienceMissing() {
    contextRunner
        .withPropertyValues(
            "fluxnova.bpm.jwt.enabled=true",
            "fluxnova.bpm.jwt.jwks-url=" + JWKS_URL,
            "fluxnova.bpm.jwt.issuer=" + ISSUER)
        .run(ctx -> assertThat(ctx).hasFailed()
            .getFailure().hasMessageContaining("audience"));
  }

  // -------------------------------------------------------------------------
  // @ConditionalOnMissingBean guard — distro/run owns the filter
  // -------------------------------------------------------------------------

  @Test
  void jwtFilterSuppressedWhenProcessEngineAuthFilterAlreadyPresent() {
    contextRunner
        .withPropertyValues(
            "fluxnova.bpm.jwt.enabled=true",
            "fluxnova.bpm.jwt.jwks-url=" + JWKS_URL,
            "fluxnova.bpm.jwt.issuer=" + ISSUER,
            "fluxnova.bpm.jwt.audience=" + AUDIENCE)
        .withUserConfiguration(ExistingAuthFilterConfig.class)
        .run(ctx -> {
          // Plugin still created (shared between run and standalone usage)
          assertThat(ctx).hasSingleBean(JwtAuthenticationPlugin.class);
          // But our filter registration is suppressed
          assertThat(ctx).doesNotHaveBean("jwtAuthenticationFilter");
        });
  }

  /** Simulates the bean that distro/run registers when auth is enabled. */
  @Configuration
  static class ExistingAuthFilterConfig {
    @Bean
    FilterRegistrationBean<ProcessEngineAuthenticationFilter> processEngineAuthenticationFilter() {
      return new FilterRegistrationBean<>(new ProcessEngineAuthenticationFilter());
    }
  }
}
