/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.cameltooling.idea.runner.debugger.evaluator;

import com.github.cameltooling.idea.runner.debugger.CamelDebuggerSession;
import com.github.cameltooling.idea.runner.debugger.stack.CamelMessageInfo;
import com.github.cameltooling.idea.runner.debugger.stack.ObjectFieldDefinitionValue;
import com.intellij.icons.AllIcons;
import com.intellij.xdebugger.XExpression;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class CamelExpressionEvaluator extends XDebuggerEvaluator {

    private CamelDebuggerSession session;

    public CamelExpressionEvaluator(@NotNull CamelDebuggerSession session) {
        this.session = session;
    }

    //This method should never be called because we override another `evaluate` method - but it has to be implemented because it's abstract in the parent class
    @Override
    public void evaluate(@NotNull String expression, @NotNull XEvaluationCallback xEvaluationCallback, @Nullable XSourcePosition expressionPosition) {
        Object result = session.evaluateExpression(expression, "simple", null);
        if (result != null) {
            xEvaluationCallback.evaluated(
                    new ObjectFieldDefinitionValue(session, new CamelMessageInfo.Value(result.getClass().getName(), result),
                            result instanceof Throwable ? AllIcons.General.Error : AllIcons.Nodes.Function));
        } else {
            xEvaluationCallback.evaluated(
                    new ObjectFieldDefinitionValue(session, new CamelMessageInfo.Value("null", "null"), AllIcons.General.Error));
        }
    }

    @Override
    public void evaluate(@NotNull XExpression expression, @NotNull XEvaluationCallback xEvaluationCallback, @Nullable XSourcePosition expressionPosition) {
        Map<String, String> customInfoMap = new HashMap<>();

        String customInfo = expression.getCustomInfo();
        if (customInfo != null && customInfo.startsWith("{") && customInfo.endsWith("}")) {
            customInfo = customInfo.replaceFirst("\\{", "").substring(0, customInfo.length() - 2);

            customInfoMap = Arrays.stream(customInfo.split(","))
                    .map(s -> s.split("="))
                    .collect(Collectors.toMap(
                            a -> a[0].trim(),  //key
                            a -> a[1].trim()   //value
                    ));
        }

        Object result = session.evaluateExpression(expression.getExpression(), expression.getLanguage().getID(), customInfoMap);
        if (result != null) {
            xEvaluationCallback.evaluated(
                    new ObjectFieldDefinitionValue(session, new CamelMessageInfo.Value(result.getClass().getName(), result),
                            result instanceof Throwable ? AllIcons.General.Error : AllIcons.Nodes.Function));
        } else {
            xEvaluationCallback.evaluated(new ObjectFieldDefinitionValue(session, new CamelMessageInfo.Value("null", "null"), AllIcons.General.Error));
        }
    }

}
