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
package com.github.cameltooling.idea.runner.debugger;

import com.github.cameltooling.idea.runner.debugger.actions.CamelEvaluateAction;
import com.github.cameltooling.idea.runner.debugger.actions.CamelSetValueAction;
import com.github.cameltooling.idea.runner.debugger.breakpoint.CamelBreakpointHandler;
import com.github.cameltooling.idea.runner.debugger.evaluator.CamelExpressionEvaluator;
import com.github.cameltooling.idea.runner.debugger.stack.CamelMessageInfo;
import com.github.cameltooling.idea.runner.debugger.stack.CamelStackFrame;
import com.google.common.base.Objects;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.ArrayUtil;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.XBreakpointHandler;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator;
import com.intellij.xdebugger.frame.XSuspendContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CamelDebugProcess extends XDebugProcess {

    private static final Logger LOG = Logger.getInstance(CamelDebugProcess.class);

    private final CamelDebuggerEditorsProvider camelDebuggerEditorsProvider;

    private final CamelBreakpointHandler camelBreakpointHandler;
    private final CamelDebuggerSession camelDebuggerSession;
    private volatile boolean forceRefresh;
    private volatile CamelMessageInfo lastProcessed;

    protected CamelDebugProcess(@NotNull XDebugSession session, @NotNull CamelDebuggerSession camelDebuggerSession) {
        super(session);

        this.camelDebuggerEditorsProvider = new CamelDebuggerEditorsProvider();
        this.camelDebuggerSession = camelDebuggerSession;
        this.camelBreakpointHandler = new CamelBreakpointHandler(camelDebuggerSession);

        try {
            init();
        } catch (Exception e) {
            LOG.warn("Could not initialize the camel debug process", e);
        }
    }

    @Override
    public @NotNull XDebuggerEditorsProvider getEditorsProvider() {
        return camelDebuggerEditorsProvider;
    }

    @Nullable
    @Override
    public XDebuggerEvaluator getEvaluator() {
        return new CamelExpressionEvaluator(camelDebuggerSession);
    }

    public void init() {
        camelDebuggerSession.connect();

        camelDebuggerSession.addMessageReceivedListener(messages -> {
            final CamelMessageInfo camelMessageInfo = selectCamelMessageInfo(messages);
            if (camelMessageInfo != null) {
                //List frames
                List<CamelStackFrame> stackFrames = new ArrayList<>();
                for (CamelMessageInfo info : camelMessageInfo.getStack()) {
                    CamelStackFrame nextFrame = new CamelStackFrame(camelDebuggerSession, info);
                    stackFrames.add(nextFrame);
                }
                getSession().positionReached(new CamelSuspendContext(stackFrames.toArray(new CamelStackFrame[0])));
                this.lastProcessed = camelMessageInfo;
                LOG.debug("New camel message processed");
            }
        });
    }

    @Override
    public void stop() {
        camelDebuggerSession.disconnect();
    }

    @Override
    public void resume(@Nullable XSuspendContext context) {
        camelDebuggerSession.resume();
    }

    @Override
    public void startStepOver(@Nullable XSuspendContext context) {
        camelDebuggerSession.stepOver(context.getActiveExecutionStack().getTopFrame().getSourcePosition());
    }

    @Override
    public void startStepInto(@Nullable XSuspendContext context) {
        camelDebuggerSession.stepInto(context.getActiveExecutionStack().getTopFrame().getSourcePosition());
    }

    @Override
    public void startStepOut(@Nullable XSuspendContext context) {
        camelDebuggerSession.stepOut(context.getActiveExecutionStack().getTopFrame().getSourcePosition());
    }

    @Override
    public void runToPosition(@NotNull XSourcePosition xSourcePosition, @Nullable XSuspendContext context) {
        camelDebuggerSession.runToPosition(context.getActiveExecutionStack().getTopFrame().getSourcePosition(), xSourcePosition);
    }

    public void setValue(CamelDebuggerTarget target,
                         @Nullable String targetName,
                         String expression,
                         String language,
                         String resultType,
                         @Nullable String bodyMediaType,
                         @Nullable String outputMediaType) {
        camelDebuggerSession.setValue(target, targetName, expression, language, resultType, bodyMediaType, outputMediaType);
        forceRefresh = true;
    }

    @NotNull
    @Override
    public XBreakpointHandler<?>[] getBreakpointHandlers() {
        final XBreakpointHandler<?>[] breakpointHandlers = super.getBreakpointHandlers();
        return ArrayUtil.append(breakpointHandlers, camelBreakpointHandler);
    }

    @Override
    public void registerAdditionalActions(@NotNull DefaultActionGroup leftToolbar, @NotNull DefaultActionGroup topToolbar, @NotNull DefaultActionGroup settings) {
        super.registerAdditionalActions(leftToolbar, topToolbar, settings);

        CamelEvaluateAction evaluateAction = new CamelEvaluateAction();
        Presentation presentation = evaluateAction.getTemplatePresentation();
        presentation.setText("Evaluate Camel Expression...");
        presentation.setDescription("Evaluate Camel Expression");
        presentation.setIcon(CamelDebuggerIcons.EVALUATE_EXPRESSION_ICON);
        topToolbar.add(evaluateAction);

        CamelSetValueAction setValueAction = new CamelSetValueAction();
        presentation = setValueAction.getTemplatePresentation();
        presentation.setText("Set Value...");
        presentation.setDescription("Set Camel Message header, body or Exchange property value");
        presentation.setIcon(CamelDebuggerIcons.SET_VALUE_ICON);
        topToolbar.add(setValueAction);

        AnAction evaluate = ActionManager.getInstance().getAction("EvaluateExpression");
        topToolbar.remove(evaluate);
    }

    /**
     * Selects the {@code CamelMessageInfo} among the given list of message knowing that it tries to return the
     * message corresponding to the same exchange id if possible, otherwise it returns the first message
     * which is also the oldest message as the list of messages is sorted by timestamp.
     *
     * @param messages the messages among which a message is selected if possible
     * @return the best possible message if any, {@code null} otherwise.
     */
    private CamelMessageInfo selectCamelMessageInfo(List<CamelMessageInfo> messages) {
        final CamelMessageInfo current = lastProcessed;
        if (current == null) {
            LOG.debug("No camel message has been processed so far");
            // The application has never stopped at any breakpoint so far, so we can take any message received
            return messages.get(0);
        }
        for (CamelMessageInfo message : messages) {
            if (Objects.equal(current.getExchangeId(), message.getExchangeId())) {
                if (current.getXSourcePosition().equals(message.getXSourcePosition())) {
                    if (forceRefresh) {
                        // Same position but the stack needs to be refreshed
                        this.forceRefresh = false;
                        LOG.debug("The camel message needs to be refreshed");
                        return message;
                    }
                    // Same position and no need to refresh so let's ignore the messages
                    LOG.debug("The same camel message has been received, thus it will be ignored");
                    return null;
                }
                // Different position but still the same exchange so let's keep this one
                LOG.debug("Not the same camel message but the same exchange id");
                return message;
            }
        }
        // No message could be selected so far, so we can take any message received
        LOG.debug("New camel message to process");
        return messages.get(0);
    }
}
