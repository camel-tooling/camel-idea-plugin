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
import com.github.cameltooling.idea.runner.debugger.ui.CamelDebuggerEvaluationDialog;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.ui.AppUIUtil;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XExpression;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.evaluation.EvaluationMode;
import com.intellij.xdebugger.evaluation.ExpressionInfo;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator;
import com.intellij.xdebugger.frame.XStackFrame;
import com.intellij.xdebugger.frame.XValue;
import com.intellij.xdebugger.impl.actions.handlers.XDebuggerActionHandler;
import com.intellij.xdebugger.impl.breakpoints.XExpressionImpl;
import com.intellij.xdebugger.impl.ui.tree.actions.XDebuggerTreeActionBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.Promise;
import org.jetbrains.concurrency.Promises;

public class CamelEvaluateActionHandler extends XDebuggerActionHandler {
    @Override
    protected void perform(@NotNull final XDebugSession session, final DataContext dataContext) {
        final XDebuggerEditorsProvider editorsProvider = session.getDebugProcess().getEditorsProvider();
        final XStackFrame stackFrame = session.getCurrentStackFrame();
        final XDebuggerEvaluator evaluator = session.getDebugProcess().getEvaluator();
        if (evaluator == null) {
            return;
        }

        Editor editor = CommonDataKeys.EDITOR.getData(dataContext);

        EvaluationMode mode = EvaluationMode.EXPRESSION;
        String selectedText = editor != null ? editor.getSelectionModel().getSelectedText() : null;
        if (selectedText != null) {
            selectedText = evaluator.formatTextForEvaluation(selectedText);
            mode = evaluator.getEvaluationMode(selectedText,
                    editor.getSelectionModel().getSelectionStart(),
                    editor.getSelectionModel().getSelectionEnd(),
                    CommonDataKeys.PSI_FILE.getData(dataContext));
        }
        Promise<String> expressionTextPromise = Promises.resolvedPromise(selectedText);

        if (selectedText == null && editor != null) {
            expressionTextPromise = getExpressionText(evaluator, CommonDataKeys.PROJECT.getData(dataContext), editor);
        }

        EvaluationMode finalMode = mode;
        XValue value = XDebuggerTreeActionBase.getSelectedValue(dataContext);
        expressionTextPromise.onSuccess(expressionText -> {
            if (expressionText == null && value != null) {
                value.calculateEvaluationExpression().onSuccess(
                    expression -> AppUIUtil.invokeOnEdt(() -> showDialog(session, editorsProvider, stackFrame, evaluator, expression)));
            } else {
                AppUIUtil.invokeOnEdt(() -> showDialog(session, editorsProvider, stackFrame, evaluator,
                        XExpressionImpl.fromText(expressionText, finalMode)));
            }
        });
    }

    private static void showDialog(@NotNull XDebugSession session,
                                   XDebuggerEditorsProvider editorsProvider,
                                   XStackFrame stackFrame,
                                   XDebuggerEvaluator evaluator,
                                   @Nullable XExpression expression) {
        //Hack to register languages before deserialization of stored expressions
        CamelLanguages.ALL.stream().map(Language::getID);

        if (expression == null) {
            expression = XExpressionImpl.EMPTY_EXPRESSION; //NOPMD - suppressed AvoidReassigningParameters - TODO explain reason for suppression
        }
        if (expression.getLanguage() == null) {
            //Assume Camel Simple by default
            expression = new XExpressionImpl(expression.getExpression(), CamelLanguages.SIMPLE_LANGUAGE, expression.getCustomInfo(), expression.getMode());
        }
        XSourcePosition position = stackFrame == null ? null : stackFrame.getSourcePosition();
        new CamelDebuggerEvaluationDialog(session, editorsProvider, expression, position, evaluator.isCodeFragmentEvaluationSupported()).show();
    }

    /**
     * The value of resulting Promise can be null
     */
    @NotNull
    public static Promise<String> getExpressionText(@Nullable XDebuggerEvaluator evaluator, @Nullable Project project, @NotNull Editor editor) {
        if (project == null || evaluator == null) {
            return Promises.resolvedPromise(null);
        }

        Document document = editor.getDocument();
        Promise<ExpressionInfo> expressionInfoPromise = evaluator.getExpressionInfoAtOffsetAsync(project, document, editor.getCaretModel().getOffset(), true);
        return expressionInfoPromise.then(expressionInfo -> getExpressionText(expressionInfo, document));
    }

    @Nullable
    public static String getExpressionText(@Nullable ExpressionInfo expressionInfo, @NotNull Document document) {
        if (expressionInfo == null) {
            return null;
        }
        String text = expressionInfo.getExpressionText();
        return text == null ? document.getText(expressionInfo.getTextRange()) : text;
    }

    @Override
    protected boolean isEnabled(final @NotNull XDebugSession session, final DataContext dataContext) {
        return session.getDebugProcess().getEvaluator() != null;
    }
}
