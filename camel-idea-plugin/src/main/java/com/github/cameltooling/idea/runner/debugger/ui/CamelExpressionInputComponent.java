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
package com.github.cameltooling.idea.runner.debugger.ui;

import com.github.cameltooling.idea.language.CamelLanguages;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.components.BorderLayoutPanel;
import com.intellij.xdebugger.XDebuggerBundle;
import com.intellij.xdebugger.XExpression;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import com.intellij.xdebugger.impl.breakpoints.XExpressionImpl;
import com.intellij.xdebugger.impl.evaluate.EvaluationInputComponent;
import com.intellij.xdebugger.impl.evaluate.ExpressionInputForm;
import com.intellij.xdebugger.impl.ui.XDebuggerEditorBase;
import com.intellij.xdebugger.impl.ui.XDebuggerExpressionComboBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Font;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class CamelExpressionInputComponent extends EvaluationInputComponent {
    private final XDebuggerEditorBase myExpressionEditor;
    private final ExpressionInputForm myMainForm = new ExpressionInputForm();
    private BorderLayoutPanel expressionPanel = JBUI.Panels.simplePanel();
    private ComboBox<String> resultTypeCombo;
    private ComboBox<String> bodyMediaTypeCombo;
    private ComboBox<String> outputMediaTypeCombo;

    public CamelExpressionInputComponent(final @NotNull Project project,
                                         @NotNull XDebuggerEditorsProvider editorsProvider,
                                         @Nullable String historyId,
                                         final @Nullable XSourcePosition sourcePosition,
                                         @Nullable XExpression expression,
                                         @NotNull Disposable parentDisposable,
                                         boolean showHelp) {
        super(XDebuggerBundle.message("xdebugger.dialog.title.evaluate.expression"));
        myExpressionEditor = new XDebuggerExpressionComboBox(project, editorsProvider, historyId, sourcePosition, true, false) {
            @Override
            protected void prepareEditor(EditorEx editor) {
                Font font = EditorUtil.getEditorFont();
                editor.getColorsScheme().setEditorFontName(font.getFontName());
                editor.getColorsScheme().setEditorFontSize(font.getSize());
                editor.getSettings().setLineCursorWidth(EditorUtil.getDefaultCaretWidth());
            }

            @Override
            public void setExpression(@Nullable XExpression text) {
                super.setExpression(text);
                String customInfo = text.getCustomInfo();
                if (customInfo != null && customInfo.startsWith("{") && customInfo.endsWith("}")) {
                    customInfo = customInfo.replaceFirst("\\{", "").substring(0, customInfo.length() - 2);

                    Map<String, String> customInfoMap = Arrays.stream(customInfo.split(","))
                            .map(s -> s.split("="))
                            .collect(Collectors.toMap(
                                a -> a[0].trim(),  //key
                                a -> a[1].trim()   //value
                            ));
                    if (customInfoMap.containsKey("resultType")) {
                        resultTypeCombo.setSelectedItem(customInfoMap.get("resultType"));
                    }
                    if (customInfoMap.containsKey("bodyMediaType")) {
                        bodyMediaTypeCombo.setSelectedItem(customInfoMap.get("bodyMediaType"));
                    }
                    if (customInfoMap.containsKey("outputMediaType")) {
                        outputMediaTypeCombo.setSelectedItem(customInfoMap.get("outputMediaType"));
                    }
                }
            }

            @Override
            public XExpression getExpression() {
                XExpression xExpression = super.getExpression();

                if (xExpression.getLanguage().is(CamelLanguages.DATASONNET_LANGUAGE) || expression.getLanguage().is(CamelLanguages.SIMPLE_LANGUAGE)) {
                    Map<String, String> customInfo = new HashMap<>();
                    customInfo.put("resultType", resultTypeCombo.getItem());

                    if (xExpression.getLanguage().is(CamelLanguages.DATASONNET_LANGUAGE)) {
                        customInfo.put("bodyMediaType", bodyMediaTypeCombo.getItem());
                        customInfo.put("outputMediaType", outputMediaTypeCombo.getItem());
                    }

                    xExpression = new XExpressionImpl(xExpression.getExpression(), xExpression.getLanguage(), customInfo.toString(), xExpression.getMode());
                }

                return xExpression;
            }
        };
        myExpressionEditor.setExpression(expression);
        expressionPanel.addToCenter(myExpressionEditor.getComponent());
        final JBLabel help = new JBLabel(XDebuggerBundle.message("xdebugger.evaluate.addtowatches.hint",
                KeymapUtil.getKeystrokeText(CamelDebuggerEvaluationDialog.addWatchKeyStroke)),
                SwingConstants.RIGHT);
        help.setBorder(JBUI.Borders.empty(2, 0, 6, 0));
        help.setComponentStyle(UIUtil.ComponentStyle.SMALL);
        help.setFontColor(UIUtil.FontColor.BRIGHTER);
        expressionPanel.addToBottom(help);
        help.setVisible(showHelp);

        myMainForm.addExpressionComponent(expressionPanel);
        myMainForm.addLanguageComponent(myExpressionEditor.getLanguageChooser());
    }

    @Override
    public void addComponent(JPanel contentPanel, JPanel resultPanel) {
        contentPanel.add(resultPanel, BorderLayout.CENTER);
        contentPanel.add(myMainForm.getMainPanel(), BorderLayout.NORTH);
    }

    @Override
    public JPanel getMainComponent() {
        return myMainForm.getMainPanel();
    }

    @Override
    @NotNull
    public XDebuggerEditorBase getInputEditor() {
        return myExpressionEditor;
    }

    public void addExpressionParametersComponent(JPanel componentPanel) {
        expressionPanel.addToBottom(componentPanel);
    }

    public void setResultTypeCombo(ComboBox<String> resultTypeCombo) {
        this.resultTypeCombo = resultTypeCombo;
    }

    public void setBodyMediaTypeCombo(ComboBox<String> bodyMediaTypeCombo) {
        this.bodyMediaTypeCombo = bodyMediaTypeCombo;
    }

    public void setOutputMediaTypeCombo(ComboBox<String> outputMediaTypeCombo) {
        this.outputMediaTypeCombo = outputMediaTypeCombo;
    }
}
