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
package com.github.cameltooling.idea.runner.debugger.actions;

import com.github.cameltooling.idea.language.CamelLanguages;
import com.github.cameltooling.idea.runner.debugger.ui.CamelSetValueDialog;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.AppUIUtil;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XExpression;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator;
import com.intellij.xdebugger.frame.XStackFrame;
import com.intellij.xdebugger.impl.actions.handlers.XDebuggerActionHandler;
import com.intellij.xdebugger.impl.breakpoints.XExpressionImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CamelSetValueActionHandler extends XDebuggerActionHandler {

    @Override
    protected void perform(@NotNull final XDebugSession session, final DataContext dataContext) {
        final XDebuggerEditorsProvider editorsProvider = session.getDebugProcess().getEditorsProvider();
        final XStackFrame stackFrame = session.getCurrentStackFrame();
        final XDebuggerEvaluator evaluator = session.getDebugProcess().getEvaluator();
        if (evaluator == null) {
            return;
        }

        final VirtualFile file = CommonDataKeys.VIRTUAL_FILE.getData(dataContext);

        AppUIUtil.invokeOnEdt(() -> showDialog(session, file, editorsProvider, stackFrame, evaluator,
                XExpressionImpl.EMPTY_EXPRESSION));
    }

    @Override
    protected boolean isEnabled(@NotNull XDebugSession session, DataContext dataContext) {
        return session.isSuspended();
    }

    private static void showDialog(@NotNull XDebugSession session,
                                   VirtualFile file,
                                   XDebuggerEditorsProvider editorsProvider,
                                   XStackFrame stackFrame,
                                   XDebuggerEvaluator evaluator,
                                   @Nullable XExpression expression) {
        //Hack to register languages before deserialization of stored expressions
        CamelLanguages.ALL.stream().map(l -> l.getID());

        if (expression == null) {
            expression = XExpressionImpl.EMPTY_EXPRESSION;
        }
        if (expression.getLanguage() == null) {
            //Assume Camel Constant by default
            expression = new XExpressionImpl(expression.getExpression(), CamelLanguages.CONSTANT_LANGUAGE, expression.getCustomInfo(), expression.getMode());
        }
        XSourcePosition position = stackFrame == null ? null : stackFrame.getSourcePosition();
        new CamelSetValueDialog(session, editorsProvider, expression, evaluator.isCodeFragmentEvaluationSupported()).show();
    }

}
