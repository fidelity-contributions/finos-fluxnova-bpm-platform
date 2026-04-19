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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.script.CompiledScript;

import org.finos.fluxnova.bpm.engine.impl.interceptor.Command;
import org.finos.fluxnova.bpm.engine.impl.interceptor.CommandContext;
import org.finos.fluxnova.bpm.engine.impl.scripting.ExecutableScript;
import org.finos.fluxnova.bpm.engine.impl.scripting.ScriptFactory;
import org.finos.fluxnova.bpm.engine.impl.scripting.SourceExecutableScript;
import org.finos.fluxnova.bpm.engine.impl.scripting.env.ScriptingEnvironment;
import org.finos.fluxnova.bpm.engine.impl.scripting.preprocessor.ScriptPreprocessor;
import org.finos.fluxnova.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Integration tests for the interaction between script preprocessing and script compilation
 * in {@link SourceExecutableScript}.
 *
 * <p>The suite verifies configuration combinations, preprocessor chaining behavior, fallback
 * semantics, and handling of null or empty preprocessor lists. Each test restores the original
 * engine configuration in {@link #tearDown()} to maintain isolation.</p>
 */
@RunWith(Arquillian.class)
public class ScriptPreprocessingCompilationTest extends AbstractFoxPlatformIntegrationTest {

  @Deployment
  public static WebArchive createProcessApplication() {
    return initWebArchiveDeployment();
  }

  protected static final String SCRIPT_LANGUAGE = "groovy";

  /** Base script; evaluates to {@code 2}. */
  protected static final String BASE_SCRIPT = "1 + 1";

  /** Preprocessed once; evaluates to {@code 3}. */
  protected static final String PREPROCESSED_ONCE = "1 + 2";

  /** Preprocessed twice; evaluates to {@code 4}. */
  protected static final String PREPROCESSED_TWICE = "1 + 3";

  protected ScriptFactory scriptFactory;

  private boolean compilationEnabledBefore;
  private boolean preprocessingEnabledBefore;
  private List<ScriptPreprocessor> preprocessorsBefore;

  @Before
  public void setUp() {
    scriptFactory = processEngineConfiguration.getScriptFactory();
    compilationEnabledBefore = processEngineConfiguration.isEnableScriptCompilation();
    preprocessingEnabledBefore = processEngineConfiguration.isEnableScriptPreprocessing();
    preprocessorsBefore = processEngineConfiguration.getScriptPreprocessors();
  }

  @After
  public void tearDown() {
    processEngineConfiguration.setEnableScriptCompilation(compilationEnabledBefore);
    processEngineConfiguration.setEnableScriptPreprocessing(preprocessingEnabledBefore);
    processEngineConfiguration.setScriptPreprocessors(preprocessorsBefore);
  }

  // -------------------------------------------------------------------------
  // Both preprocessing and compilation enabled
  // -------------------------------------------------------------------------

  @Test
  public void testPreprocessingAndCompilationEnabled_PreprocessedScriptIsCompiledAndExecuted() {
    // given
    processEngineConfiguration.setEnableScriptPreprocessing(true);
    processEngineConfiguration.setEnableScriptCompilation(true);
    processEngineConfiguration.addScriptPreprocessor(
        ScriptPreprocessorTestHelper.replacingPreprocessor(BASE_SCRIPT, PREPROCESSED_ONCE));

    SourceExecutableScript script = createScript(BASE_SCRIPT);

    // when
    Object result = executeScript(script);

    // then - preprocessing transforms the script, and the transformed script is compiled
    assertEquals(3, result);
    assertFalse(script.isShouldBeCompiled());
    assertNotNull(script.getCompiledScript());
  }

  // -------------------------------------------------------------------------
  // Preprocessing disabled, compilation enabled
  // -------------------------------------------------------------------------

  @Test
  public void testPreprocessingDisabled_CompilationEnabled_OriginalScriptCompiledAndExecuted() {
    // given - a preprocessor is registered, but preprocessing is disabled
    processEngineConfiguration.setEnableScriptPreprocessing(false);
    processEngineConfiguration.setEnableScriptCompilation(true);
    processEngineConfiguration.addScriptPreprocessor(
        ScriptPreprocessorTestHelper.replacingPreprocessor(BASE_SCRIPT, PREPROCESSED_ONCE));

    SourceExecutableScript script = createScript(BASE_SCRIPT);

    // when
    Object result = executeScript(script);

    // then - preprocessing is bypassed, and the original script is compiled
    assertEquals(2, result);
    assertFalse(script.isShouldBeCompiled());
    assertNotNull(script.getCompiledScript());
  }

  // -------------------------------------------------------------------------
  // Preprocessing enabled, compilation disabled
  // -------------------------------------------------------------------------

  @Test
  public void testPreprocessingEnabled_CompilationDisabled_PreprocessedScriptInterpreted() {
    // given - preprocessing transforms the script, but compilation is disabled
    processEngineConfiguration.setEnableScriptPreprocessing(true);
    processEngineConfiguration.setEnableScriptCompilation(false);
    processEngineConfiguration.addScriptPreprocessor(
        ScriptPreprocessorTestHelper.replacingPreprocessor(BASE_SCRIPT, PREPROCESSED_ONCE));

    SourceExecutableScript script = createScript(BASE_SCRIPT);

    // when
    Object result = executeScript(script);

    // then - the preprocessed script is interpreted and no compiled artifact is cached
    assertEquals(3, result);
    assertFalse(script.isShouldBeCompiled());
    assertNull(script.getCompiledScript());
  }

  // -------------------------------------------------------------------------
  // Both disabled
  // -------------------------------------------------------------------------

  @Test
  public void testPreprocessingAndCompilationBothDisabled_OriginalScriptInterpreted() {
    // given
    processEngineConfiguration.setEnableScriptPreprocessing(false);
    processEngineConfiguration.setEnableScriptCompilation(false);

    SourceExecutableScript script = createScript(BASE_SCRIPT);

    // when
    Object result = executeScript(script);

    // then - the original script is interpreted without preprocessing or compilation
    assertEquals(2, result);
    assertFalse(script.isShouldBeCompiled());
    assertNull(script.getCompiledScript());
  }

  // -------------------------------------------------------------------------
  // Chained preprocessors
  // -------------------------------------------------------------------------

  @Test
  public void testChainingPreprocessors_AppliedInRegistrationOrderBeforeCompilation() {
    // given - two preprocessors are applied in order:
    // p1: BASE_SCRIPT → PREPROCESSED_ONCE (= 3)
    // p2: PREPROCESSED_ONCE → PREPROCESSED_TWICE (= 4)
    processEngineConfiguration.setEnableScriptPreprocessing(true);
    processEngineConfiguration.setEnableScriptCompilation(true);
    processEngineConfiguration.addScriptPreprocessor(
        ScriptPreprocessorTestHelper.replacingPreprocessor(BASE_SCRIPT, PREPROCESSED_ONCE));
    processEngineConfiguration.addScriptPreprocessor(
        ScriptPreprocessorTestHelper.replacingPreprocessor(PREPROCESSED_ONCE, PREPROCESSED_TWICE));

    SourceExecutableScript script = createScript(BASE_SCRIPT);

    // when
    Object result = executeScript(script);

    // then - preprocessors are applied in registration order, and the final script is compiled
    assertEquals(4, result);
    assertFalse(script.isShouldBeCompiled());
    assertNotNull(script.getCompiledScript());
  }

  @Test
  public void testChainingWithNullReturningFirstPreprocessor_SecondPreprocessorApplied() {
    // given - the first preprocessor returns null (ignored by the composite), and the second transforms the script
    processEngineConfiguration.setEnableScriptPreprocessing(true);
    processEngineConfiguration.setEnableScriptCompilation(true);
    processEngineConfiguration.addScriptPreprocessor(ScriptPreprocessorTestHelper.nullReturningPreprocessor());
    processEngineConfiguration.addScriptPreprocessor(
        ScriptPreprocessorTestHelper.replacingPreprocessor(BASE_SCRIPT, PREPROCESSED_ONCE));

    SourceExecutableScript script = createScript(BASE_SCRIPT);

    // when
    Object result = executeScript(script);

    // then - the null return from the first preprocessor is ignored, and the second is applied
    assertEquals(3, result);
    assertNotNull(script.getCompiledScript());
  }

  @Test
  public void testCompilationCacheIsReusedWhenProcessedScriptDoesNotChange() {
    // given
    processEngineConfiguration.setEnableScriptPreprocessing(true);
    processEngineConfiguration.setEnableScriptCompilation(true);
    processEngineConfiguration.addScriptPreprocessor(
        ScriptPreprocessorTestHelper.replacingPreprocessor(BASE_SCRIPT, PREPROCESSED_ONCE));

    SourceExecutableScript script = createScript(BASE_SCRIPT);

    // when
    Object firstResult = executeScript(script);
    CompiledScript firstCompiledScript = script.getCompiledScript();
    Object secondResult = executeScript(script);

    // then
    assertEquals(3, firstResult);
    assertEquals(3, secondResult);
    assertNotNull(firstCompiledScript);
    assertSame(firstCompiledScript, script.getCompiledScript());
  }

  @Test
  public void testCompilationCacheIsInvalidatedWhenPreprocessorChangesProcessedScript() {
    // given
    processEngineConfiguration.setEnableScriptPreprocessing(true);
    processEngineConfiguration.setEnableScriptCompilation(true);
    processEngineConfiguration.addScriptPreprocessor(
        ScriptPreprocessorTestHelper.replacingPreprocessor(BASE_SCRIPT, PREPROCESSED_ONCE));

    SourceExecutableScript script = createScript(BASE_SCRIPT);

    // when
    Object firstResult = executeScript(script);
    CompiledScript firstCompiledScript = script.getCompiledScript();

    processEngineConfiguration.setScriptPreprocessors(
        Collections.singletonList(
            ScriptPreprocessorTestHelper.replacingPreprocessor(BASE_SCRIPT, PREPROCESSED_TWICE)));

    Object secondResult = executeScript(script);

    // then
    assertEquals(3, firstResult);
    assertEquals(4, secondResult);
    assertNotNull(firstCompiledScript);
    assertNotNull(script.getCompiledScript());
    assertNotSame(firstCompiledScript, script.getCompiledScript());
  }

  // -------------------------------------------------------------------------
  // Null and empty preprocessor lists
  // -------------------------------------------------------------------------

  @Test
  public void testNullPreprocessorInList_NullEntryIgnored_ValidPreprocessorApplied() {
    // given - the list contains a null entry together with a valid preprocessor;
    // the composite filters null entries before execution
    processEngineConfiguration.setEnableScriptPreprocessing(true);
    processEngineConfiguration.setEnableScriptCompilation(true);
    processEngineConfiguration.setScriptPreprocessors(
        Arrays.asList(null, ScriptPreprocessorTestHelper.replacingPreprocessor(BASE_SCRIPT, PREPROCESSED_ONCE)));

    SourceExecutableScript script = createScript(BASE_SCRIPT);

    // when
    Object result = executeScript(script);

    // then - the null entry is filtered out, and the valid preprocessor transforms and compiles the script
    assertEquals(3, result);
    assertNotNull(script.getCompiledScript());
  }

  @Test
  public void testAllNullPreprocessorsInList_OriginalScriptCompiledAndExecuted() {
    // given - the list contains only null entries; all entries are filtered by the composite preprocessor,
    // leaving no effective transformation; original script is used
    processEngineConfiguration.setEnableScriptPreprocessing(true);
    processEngineConfiguration.setEnableScriptCompilation(true);
    processEngineConfiguration.setScriptPreprocessors(Arrays.asList(null, null));

    SourceExecutableScript script = createScript(BASE_SCRIPT);

    // when
    Object result = executeScript(script);

    // then - there are no effective preprocessors, and the original script is compiled
    assertEquals(2, result);
    assertNotNull(script.getCompiledScript());
  }

  @Test
  public void testEmptyPreprocessorList_OriginalScriptCompiledAndExecuted() {
    // given - an empty list means no preprocessors are active
    processEngineConfiguration.setEnableScriptPreprocessing(true);
    processEngineConfiguration.setEnableScriptCompilation(true);
    processEngineConfiguration.setScriptPreprocessors(Collections.emptyList());

    SourceExecutableScript script = createScript(BASE_SCRIPT);

    // when
    Object result = executeScript(script);

    // then - no preprocessors are configured, and the original script is compiled
    assertEquals(2, result);
    assertNotNull(script.getCompiledScript());
  }

  @Test
  public void testNullPreprocessorList_OriginalScriptCompiledAndExecuted() {
    // given - a null preprocessor list is treated as no preprocessors configured
    processEngineConfiguration.setEnableScriptPreprocessing(true);
    processEngineConfiguration.setEnableScriptCompilation(true);
    processEngineConfiguration.setScriptPreprocessors(null);

    SourceExecutableScript script = createScript(BASE_SCRIPT);

    // when
    Object result = executeScript(script);

    // then - a null list results in no preprocessing, and the original script is compiled
    assertEquals(2, result);
    assertNotNull(script.getCompiledScript());
  }

  // -------------------------------------------------------------------------
  // Fallback behavior
  // -------------------------------------------------------------------------
  @Test
  public void testPreprocessorReturnsNull_CompilationDisabled_OriginalScriptInterpreted() {
    // given - compilation is disabled, and a null result should fall back to the original interpreted script
    processEngineConfiguration.setEnableScriptPreprocessing(true);
    processEngineConfiguration.setEnableScriptCompilation(false);
    processEngineConfiguration.addScriptPreprocessor(ScriptPreprocessorTestHelper.nullReturningPreprocessor());

    SourceExecutableScript script = createScript(BASE_SCRIPT);

    // when
    Object result = executeScript(script);

    // then - there is no transformation and no compilation, and the original script is interpreted
    assertEquals(2, result);
    assertNull(script.getCompiledScript());
  }

  // -------------------------------------------------------------------------
  // Infrastructure
  // -------------------------------------------------------------------------

  /**
   * Creates a {@link SourceExecutableScript} from the given source using the configured
   * {@link ScriptFactory}.
   *
   * @param source the script source text
   * @return a new {@link SourceExecutableScript} instance
   */
  protected SourceExecutableScript createScript(String source) {
    return (SourceExecutableScript) scriptFactory.createScriptFromSource(SCRIPT_LANGUAGE, source);
  }

  /**
   * Executes the given script within a command context so that script preprocessing and
   * compilation run with the expected engine configuration and transaction boundaries.
   *
   * @param script the script to execute
   * @return the script evaluation result
   */
  protected Object executeScript(final ExecutableScript script) {
    final ScriptingEnvironment scriptingEnvironment = processEngineConfiguration.getScriptingEnvironment();
    return processEngineConfiguration.getCommandExecutorTxRequired()
        .execute(new Command<Object>() {
          @Override
          public Object execute(CommandContext commandContext) {
            return scriptingEnvironment.execute(script, null);
          }
        });
  }
}
