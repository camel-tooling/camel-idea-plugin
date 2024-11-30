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
import com.github.cameltooling.idea.runner.debugger.CamelDebugProcess;
import com.github.cameltooling.idea.runner.debugger.CamelDebuggerTarget;
import com.github.cameltooling.idea.runner.debugger.ContextAwareDebugProcess;
import com.github.cameltooling.idea.util.StringUtils;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.components.BorderLayoutPanel;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebugSessionListener;
import com.intellij.xdebugger.XDebuggerBundle;
import com.intellij.xdebugger.XExpression;
import com.intellij.xdebugger.evaluation.EvaluationMode;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import com.intellij.xdebugger.impl.breakpoints.XExpressionImpl;
import com.intellij.xdebugger.impl.evaluate.CodeFragmentInputComponent;
import com.intellij.xdebugger.impl.evaluate.DebuggerEvaluationStatisticsCollector;
import com.intellij.xdebugger.impl.evaluate.EvaluationInputComponent;
import com.intellij.xdebugger.impl.settings.XDebuggerSettingManagerImpl;
import com.intellij.xdebugger.impl.ui.XDebuggerEditorBase;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class CamelSetValueDialog extends DialogWrapper {
    public static final DataKey<CamelSetValueDialog> KEY = DataKey.create("CAMEL_SET_VALUE_DIALOG");
    private final JPanel myMainPanel;
    private EvaluationInputComponent myInputComponent;
    private final XDebugSession mySession;
    private final Project myProject;
    private final XDebuggerEditorsProvider myEditorsProvider;
    private EvaluationMode myMode;
    private final SwitchModeAction mySwitchModeAction;
    private final CamelExpressionParameters myCamelExpressionParameters;
    private final CamelSetValueTargetPanel myCamelValueTargetPanel;

    public CamelSetValueDialog(@NotNull XDebugSession session,
                               @NotNull XDebuggerEditorsProvider editorsProvider,
                               @NotNull XExpression text,
                               boolean isCodeFragmentEvaluationSupported) {
        this(session, session.getProject(), editorsProvider, text, isCodeFragmentEvaluationSupported);
    }

    private CamelSetValueDialog(@Nullable XDebugSession session,
                                @NotNull Project project,
                                @NotNull XDebuggerEditorsProvider editorsProvider,
                                @NotNull XExpression text,
                                boolean myIsCodeFragmentEvaluationSupported) {
        super(project, true);
        mySession = session;
        myProject = project;
        myEditorsProvider = editorsProvider;
        setModal(false);
        setOKButtonText("Set Value");
        setCancelButtonText(XDebuggerBundle.message("xdebugger.evaluate.dialog.close"));

        myMainPanel = new SetValueMainPanel();

        mySwitchModeAction = new SwitchModeAction();

        myCamelExpressionParameters = new CamelExpressionParameters();
        myCamelValueTargetPanel = new CamelSetValueTargetPanel();

        // preserve old mode switch shortcut
        DumbAwareAction.create(e -> mySwitchModeAction.actionPerformed(null))
                .registerCustomShortcutSet(
                        new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.ALT_DOWN_MASK)),
                        getRootPane(), myDisposable);

        EvaluationMode mode = XDebuggerSettingManagerImpl.getInstanceImpl().getGeneralSettings().getEvaluationDialogMode();
        if (mode == EvaluationMode.CODE_FRAGMENT && !myIsCodeFragmentEvaluationSupported) {
            mode = EvaluationMode.EXPRESSION;
        }
        if (mode == EvaluationMode.EXPRESSION && text.getMode() == EvaluationMode.CODE_FRAGMENT && myIsCodeFragmentEvaluationSupported) {
            mode = EvaluationMode.CODE_FRAGMENT;
        }
        setTitle("Set Value");
        switchToMode(mode, text);
        // internal feature usage tracker
        //DebuggerEvaluationStatisticsCollector.DIALOG_OPEN.log(project, mode);
        if (mode == EvaluationMode.EXPRESSION) {
            myInputComponent.getInputEditor().selectAll();
        }
        init();

        if (mySession != null) {
            mySession.addSessionListener(new XDebugSessionListener() {
                @Override
                public void sessionStopped() {
                    ApplicationManager.getApplication().invokeLater(() -> close(CANCEL_EXIT_CODE));
                }
            }, myDisposable);
        }
    }


    @Override
    protected void dispose() {
        super.dispose();
        myMainPanel.removeAll();
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
        doSetValue();
    }

    @Override
    @Nullable
    protected ValidationInfo doValidate() {
        ValidationInfo validationInfo = null;
        CamelDebuggerTarget targetType = myCamelValueTargetPanel.getTargetType();
        if (targetType != CamelDebuggerTarget.BODY) {
            String targetName = myCamelValueTargetPanel.getTargetName();
            if (StringUtils.isEmpty(targetName)) {
                validationInfo = new ValidationInfo(targetType + " name cannot be empty", myCamelValueTargetPanel.getTargetNameComponent());
            }
        }
        return validationInfo;
    }

    @Override
    protected String getHelpId() {
        return "debugging.debugMenu.setValue";
    }

    @Override
    protected JButton createJButtonForAction(Action action) {
        final JButton button = super.createJButtonForAction(action);
        if (action == mySwitchModeAction) {
            int width1 = new JButton(getSwitchButtonText(EvaluationMode.EXPRESSION)).getPreferredSize().width;
            int width2 = new JButton(getSwitchButtonText(EvaluationMode.CODE_FRAGMENT)).getPreferredSize().width;
            final Dimension size = new Dimension(Math.max(width1, width2), button.getPreferredSize().height);
            button.setMinimumSize(size);
            button.setPreferredSize(size);
        }
        return button;
    }

    public XExpression getExpression() {
        return getInputEditor().getExpression();
    }

    private static @Nls String getSwitchButtonText(EvaluationMode mode) {
        return mode != EvaluationMode.EXPRESSION
                ? XDebuggerBundle.message("button.text.expression.mode")
                : XDebuggerBundle.message("button.text.code.fragment.mode");
    }

    private void switchToMode(EvaluationMode mode, XExpression text) {
        if (myMode == mode) {
            return;
        }

        myMode = mode;

        Editor oldEditor = (myInputComponent != null) ? myInputComponent.getInputEditor().getEditor() : null;

        myInputComponent = createInputComponent(mode, text);
        myMainPanel.removeAll();

        myMainPanel.add(myCamelExpressionParameters.getMainPanel(), BorderLayout.SOUTH);
        myMainPanel.add(myInputComponent.getMainComponent(), BorderLayout.CENTER);
        myMainPanel.add(myCamelValueTargetPanel.getPanel(), BorderLayout.NORTH);

        XDebuggerEditorBase.copyCaretPosition(oldEditor, myInputComponent.getInputEditor().getEditor());

        mySwitchModeAction.putValue(Action.NAME, getSwitchButtonText(mode));
        getInputEditor().requestFocusInEditor();
    }

    private XDebuggerEditorBase getInputEditor() {
        return myInputComponent.getInputEditor();
    }

    private EvaluationInputComponent createInputComponent(EvaluationMode mode, XExpression text) {
        text = XExpressionImpl.changeMode(text, mode);
        if (mode == EvaluationMode.EXPRESSION) {
            CamelExpressionInputComponent component =
                    new CamelExpressionInputComponent(myProject, myEditorsProvider, "setValueExpression", null, text, false);
            component.addExpressionParametersComponent(myCamelExpressionParameters.getMainPanel());
            component.setResultTypeCombo(myCamelExpressionParameters.getResultTypeCombo());
            component.setBodyMediaTypeCombo(myCamelExpressionParameters.getBodyMediaTypeCombo());
            component.setOutputMediaTypeCombo(myCamelExpressionParameters.getOutputMediaTypeCombo());

            component.getInputEditor().setExpandHandler(() -> mySwitchModeAction.actionPerformed(null));
            component.getInputEditor().getLanguageChooser().addPropertyChangeListener(evt -> {
                Object newValueObj = evt.getNewValue();
                if (newValueObj != null) {
                    String newValue = evt.getNewValue().toString();
                    myCamelExpressionParameters.getBodyMediaTypePanel().setVisible("DataSonnet".equals(newValue));
                    myCamelExpressionParameters.getOutputMediaTypePanel().setVisible("DataSonnet".equals(newValue));
                }
            });
            return component;
        } else {
            CodeFragmentInputComponent component = new CodeFragmentInputComponent(myProject, myEditorsProvider, null, text,
                    getDimensionServiceKey() + ".splitter", myDisposable);
            component.getInputEditor().addCollapseButton(() -> mySwitchModeAction.actionPerformed(null));
            component.getInputEditor().getLanguageChooser().addPropertyChangeListener(evt -> {
                Object newValueObj = evt.getNewValue();
                if (newValueObj != null) {
                    String newValue = evt.getNewValue().toString();
                    myCamelExpressionParameters.getBodyMediaTypePanel().setVisible("DataSonnet".equals(newValue));
                    myCamelExpressionParameters.getOutputMediaTypePanel().setVisible("DataSonnet".equals(newValue));
                }
            });
            return component;
        }
    }

    private void doSetValue() {
        final XDebuggerEditorBase inputEditor = getInputEditor();
        inputEditor.saveTextInHistory();
        XExpression expression = inputEditor.getExpression();
        String resultType = myCamelExpressionParameters.getResultTypeCombo().getItem();
        String bodyMediaType = null;
        String outputMediaType = null;
        if (expression.getLanguage().is(CamelLanguages.DATASONNET_LANGUAGE)) {
            bodyMediaType = myCamelExpressionParameters.getBodyMediaTypeCombo().getItem();
            outputMediaType = myCamelExpressionParameters.getOutputMediaTypeCombo().getItem();
        }

        CamelDebuggerTarget target = myCamelValueTargetPanel.getTargetType();
        String targetName = myCamelValueTargetPanel.getTargetName();

        ContextAwareDebugProcess debugProcess = (ContextAwareDebugProcess) mySession.getDebugProcess();
        CamelDebugProcess camelDebugProcess = (CamelDebugProcess) debugProcess.getCurrentDebugProcess();
        camelDebugProcess.setValue(target,
                targetName,
                expression.getExpression(),
                expression.getLanguage().getID(),
                resultType,
                bodyMediaType,
                outputMediaType);
    }

    @Override
    public void doCancelAction() {
        getInputEditor().saveTextInHistory();
        super.doCancelAction();
    }

    @Override
    protected String getDimensionServiceKey() {
        return "#cameldebugger.setvalue";
    }

    @Override
    protected JComponent createCenterPanel() {
        return myMainPanel;
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return getInputEditor().getPreferredFocusedComponent();
    }

    private class SwitchModeAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            XExpression text = getInputEditor().getExpression();
            EvaluationMode newMode = (myMode == EvaluationMode.EXPRESSION) ? EvaluationMode.CODE_FRAGMENT : EvaluationMode.EXPRESSION;
            // remember only on user selection
            XDebuggerSettingManagerImpl.getInstanceImpl().getGeneralSettings().setEvaluationDialogMode(newMode);
            // internal feature usage tracker
            //DebuggerEvaluationStatisticsCollector.MODE_SWITCH.log(myProject, newMode);
            switchToMode(newMode, text);
        }
    }

    private class SetValueMainPanel extends BorderLayoutPanel implements DataProvider {
        @Nullable
        @Override
        public Object getData(@NotNull @NonNls String dataId) {
            if (KEY.is(dataId)) {
                return CamelSetValueDialog.this;
            }
            return null;
        }

        @Override
        public Dimension getMinimumSize() {
            Dimension d = super.getMinimumSize();
            d.width = Math.max(d.width, JBUI.scale(450));
            return d;
        }
    }
}
