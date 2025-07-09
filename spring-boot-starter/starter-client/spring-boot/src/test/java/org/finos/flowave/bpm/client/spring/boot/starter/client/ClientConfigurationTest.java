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
package org.finos.flowave.bpm.client.spring.boot.starter.client;

import org.finos.flowave.bpm.client.spring.boot.starter.ParsePropertiesHelper;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {
    "flowave.bpm.client.base-url=base-url",
    "flowave.bpm.client.worker-id=worker-id",
    "flowave.bpm.client.max-tasks=111",
    "flowave.bpm.client.use-priority=false",
    "flowave.bpm.client.default-serialization-format=serialization-format",
    "flowave.bpm.client.date-format=date-format",
    "flowave.bpm.client.async-response-timeout=555",
    "flowave.bpm.client.lock-duration=777",
    "flowave.bpm.client.disable-auto-fetching=true",
    "flowave.bpm.client.disable-backoff-strategy=true",
    "flowave.bpm.client.basic-auth.username=username",
    "flowave.bpm.client.basic-auth.password=password",
})
public class ClientConfigurationTest extends ParsePropertiesHelper {

  @Test
  public void shouldCheckProperties() {
    assertThat(properties.getBaseUrl()).isEqualTo("base-url");
    assertThat(properties.getWorkerId()).isEqualTo("worker-id");
    assertThat(properties.getMaxTasks()).isEqualTo(111);
    assertThat(properties.getUsePriority()).isEqualTo(false);
    assertThat(properties.getDefaultSerializationFormat()).isEqualTo("serialization-format");
    assertThat(properties.getDateFormat()).isEqualTo("date-format");
    assertThat(properties.getAsyncResponseTimeout()).isEqualTo(555);
    assertThat(properties.getLockDuration()).isEqualTo(777);
    assertThat(properties.getDisableAutoFetching()).isEqualTo(true);
    assertThat(properties.getDisableBackoffStrategy()).isEqualTo(true);
    assertThat(basicAuth.getUsername()).isEqualTo("username");
    assertThat(basicAuth.getPassword()).isEqualTo("password");
    assertThat(subscriptions).isEmpty();
  }

}
