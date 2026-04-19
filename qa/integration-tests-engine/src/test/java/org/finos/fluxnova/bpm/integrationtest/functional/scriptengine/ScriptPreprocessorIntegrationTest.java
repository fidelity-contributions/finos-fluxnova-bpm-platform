/*
 * Copyright 2025 FINOS
 *
 * The source files in this repository are made available under the Apache License Version 2.0.
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Fluxnova uses and includes third-party dependencies published under various licenses.
 * By downloading and using Fluxnova artifacts, you agree to their terms and conditions.
 */
package org.finos.fluxnova.bpm.integrationtest.functional.scriptengine;

import java.util.HashMap;
import java.util.Map;

import org.finos.fluxnova.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.finos.fluxnova.bpm.engine.impl.scripting.preprocessor.ScriptPreprocessor;
import org.finos.fluxnova.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;
import org.finos.fluxnova.bpm.model.bpmn.Bpmn;
import org.finos.fluxnova.bpm.model.bpmn.BpmnModelInstance;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

/**
 * Integration tests for script preprocessing behavior in BPMN script tasks.
 *
 * <p>The suite verifies that configured {@link ScriptPreprocessor} instances are applied only
 * when preprocessing is enabled and that execution safely falls back to the original script
 * when a preprocessor returns {@code null} or throws an exception.</p>
 *
 * <p>It also covers variable-binding scenarios to ensure preprocessing preserves access to
 * process variables.</p>
 */
@RunWith(Arquillian.class)
public class ScriptPreprocessorIntegrationTest extends AbstractFoxPlatformIntegrationTest {

  private static final String PROCESS_KEY = "scriptPreprocessorIntegrationProcess";
  private static final String VAR_PROCESS_KEY = "scriptPreprocessorVarBindingProcess";
  private static final String SCRIPT_LANGUAGE = "groovy";
  private static final String RESULT_VARIABLE = "result";
  private static final String ORIGINAL_SCRIPT_RESULT = "2";
  private static final String PREPROCESSED_SCRIPT_RESULT = "3";

  /**
   * Builds the Arquillian deployment with BPMN resources used by this test class.
   *
   * @return web archive containing baseline and variable-binding script task processes
   */
  @Deployment
  public static WebArchive createProcessApplication() {
    return initWebArchiveDeployment()
        .addAsResource(createScriptTaskProcess(), "process.bpmn20.xml")
        .addAsResource(createVarBindingProcess(), "varBindingProcess.bpmn20.xml");
  }

  @Before
  public void beforeEach() {
    resetScriptPreprocessing();
  }

  @After
  public void afterEach() {
    resetScriptPreprocessing();
  }

  @Test
  public void shouldApplyConfiguredPreprocessorWhenEnabled() {
    ProcessEngineConfigurationImpl configuration = processEngineConfiguration;
    configuration.setEnableScriptPreprocessing(true);
    configuration.addScriptPreprocessor(
        ScriptPreprocessorTestHelper.replacingPreprocessor(ORIGINAL_SCRIPT_RESULT, PREPROCESSED_SCRIPT_RESULT));

    String processInstanceId = runtimeService.startProcessInstanceByKey(PROCESS_KEY).getId();

    assertEquals(3, runtimeService.getVariable(processInstanceId, RESULT_VARIABLE));
  }

  @Test
  public void shouldNotApplyConfiguredPreprocessorWhenDisabled() {
    ProcessEngineConfigurationImpl configuration = processEngineConfiguration;
    configuration.setEnableScriptPreprocessing(false);
    configuration.addScriptPreprocessor(
        ScriptPreprocessorTestHelper.replacingPreprocessor(ORIGINAL_SCRIPT_RESULT, PREPROCESSED_SCRIPT_RESULT));

    String processInstanceId = runtimeService.startProcessInstanceByKey(PROCESS_KEY).getId();

    assertEquals(2, runtimeService.getVariable(processInstanceId, RESULT_VARIABLE));
  }

  @Test
  public void shouldFallBackToOriginalScriptWhenPreprocessorReturnsNull() {
    ProcessEngineConfigurationImpl configuration = processEngineConfiguration;
    configuration.setEnableScriptPreprocessing(true);
    configuration.addScriptPreprocessor(ScriptPreprocessorTestHelper.nullReturningPreprocessor());

    String processInstanceId = runtimeService.startProcessInstanceByKey(PROCESS_KEY).getId();

    assertEquals(2, runtimeService.getVariable(processInstanceId, RESULT_VARIABLE));
  }

  @Test
  public void shouldFallBackToOriginalScriptWhenPreprocessorThrowsException() {
    ProcessEngineConfigurationImpl configuration = processEngineConfiguration;
    configuration.setEnableScriptPreprocessing(true);
    configuration.addScriptPreprocessor(ScriptPreprocessorTestHelper.throwingPreprocessor());

    String processInstanceId = runtimeService.startProcessInstanceByKey(PROCESS_KEY).getId();

    assertEquals(2, runtimeService.getVariable(processInstanceId, RESULT_VARIABLE));
  }

  // -------------------------------------------------------------------------
  // Variable binding tests
  // -------------------------------------------------------------------------

  /**
   * Verifies that a script task can access process variables when preprocessing is disabled.
   * The script computes {@code x + y} and stores the result.
   */
  @Test
  public void shouldEvaluateScriptWithVariableBindingsWhenPreprocessingDisabled() {
    processEngineConfiguration.setEnableScriptPreprocessing(false);

    Map<String, Object> vars = new HashMap<>();
    vars.put("x", 4);
    vars.put("y", 6);

    String processInstanceId = runtimeService.startProcessInstanceByKey(VAR_PROCESS_KEY, vars).getId();

    assertEquals(10, runtimeService.getVariable(processInstanceId, RESULT_VARIABLE));
  }

  /**
   * Verifies that preprocessing is applied before execution when variable bindings are
   * present. The test rewrites {@code x + y} to {@code x * y} and validates the
   * transformed result.
   */
  @Test
  public void shouldApplyPreprocessorToScriptWithVariableBindings() {
    processEngineConfiguration.setEnableScriptPreprocessing(true);
    processEngineConfiguration.addScriptPreprocessor(
        ScriptPreprocessorTestHelper.replacingPreprocessor("x + y", "x * y"));

    Map<String, Object> vars = new HashMap<>();
    vars.put("x", 4);
    vars.put("y", 6);

    String processInstanceId = runtimeService.startProcessInstanceByKey(VAR_PROCESS_KEY, vars).getId();

    assertEquals(24, runtimeService.getVariable(processInstanceId, RESULT_VARIABLE));
  }

  /**
   * Verifies that if the preprocessor returns {@code null} for a script that uses variable
   * bindings, execution falls back to the original script.
   */
  @Test
  public void shouldFallBackToOriginalScriptWithVariableBindingsWhenPreprocessorReturnsNull() {
    processEngineConfiguration.setEnableScriptPreprocessing(true);
    processEngineConfiguration.addScriptPreprocessor(ScriptPreprocessorTestHelper.nullReturningPreprocessor());

    Map<String, Object> vars = new HashMap<>();
    vars.put("x", 3);
    vars.put("y", 7);

    String processInstanceId = runtimeService.startProcessInstanceByKey(VAR_PROCESS_KEY, vars).getId();

    assertEquals(10, runtimeService.getVariable(processInstanceId, RESULT_VARIABLE));
  }

  /**
   * Verifies that if preprocessing throws for a script that uses variable bindings,
   * execution falls back to the original script.
   */
  @Test
  public void shouldFallBackToOriginalScriptWithVariableBindingsWhenPreprocessorThrows() {
    processEngineConfiguration.setEnableScriptPreprocessing(true);
    processEngineConfiguration.addScriptPreprocessor(ScriptPreprocessorTestHelper.throwingPreprocessor());

    Map<String, Object> vars = new HashMap<>();
    vars.put("x", 3);
    vars.put("y", 7);

    String processInstanceId = runtimeService.startProcessInstanceByKey(VAR_PROCESS_KEY, vars).getId();

    assertEquals(10, runtimeService.getVariable(processInstanceId, RESULT_VARIABLE));
  }

  /**
   * Resets script preprocessing configuration to a known default state between tests.
   */
  private void resetScriptPreprocessing() {
    processEngineConfiguration.setEnableScriptPreprocessing(false);
    processEngineConfiguration.setScriptPreprocessors(null);
  }

  /**
   * Creates a baseline BPMN process with a single script task that writes a constant result.
   */
  private static StringAsset createScriptTaskProcess() {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(PROCESS_KEY)
        .fluxnovaHistoryTimeToLive(180)
        .startEvent()
        .scriptTask()
          .scriptFormat(SCRIPT_LANGUAGE)
          .scriptText("execution.setVariable('result', 2)")
        .userTask()
        .done();
    return new StringAsset(Bpmn.convertToString(modelInstance));
  }

  /**
   * Creates a BPMN process whose script task reads the input variables {@code x} and {@code y}
   * from the execution context, sums them, and stores the result in the {@code result} variable.
   * This process is used to verify that preprocessing interacts correctly with variable bindings.
   */
  private static StringAsset createVarBindingProcess() {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(VAR_PROCESS_KEY)
        .fluxnovaHistoryTimeToLive(180)
        .startEvent()
        .scriptTask()
          .scriptFormat(SCRIPT_LANGUAGE)
          .scriptText("execution.setVariable('result', x + y)")
        .userTask()
        .done();
    return new StringAsset(Bpmn.convertToString(modelInstance));
  }

}
