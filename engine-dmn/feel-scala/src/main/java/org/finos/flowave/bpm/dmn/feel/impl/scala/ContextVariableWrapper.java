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
package org.finos.flowave.bpm.dmn.feel.impl.scala;

import org.finos.flowave.bpm.engine.variable.context.VariableContext;
import org.finos.flowave.bpm.engine.variable.value.TypedValue;
import org.finos.flowave.feel.context.VariableProvider;
import flowavejar.impl.scala.Option;
import flowavejar.impl.scala.Some;
import flowavejar.impl.scala.collection.Iterable;

import java.util.Set;

import static flowavejar.impl.scala.jdk.CollectionConverters.SetHasAsScala;

public class ContextVariableWrapper implements VariableProvider {

  protected VariableContext context;

  public ContextVariableWrapper(VariableContext context) {
    this.context = context;
  }

  public Option getVariable(String name) {
    if (context.containsVariable(name)) {
      TypedValue typedValue = context.resolve(name);
      Object value = typedValue.getValue();
      return new Some(value);

    } else {
      return flowavejar.impl.scala.None$.MODULE$;

    }
  }

  public Iterable<String> keys() {
    Set<String> strings = context.keySet();
    return SetHasAsScala(strings).asScala();
  }

}
