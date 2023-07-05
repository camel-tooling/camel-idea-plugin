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

import com.intellij.debugger.engine.JavaDebugProcess;
import com.intellij.debugger.impl.DebuggerSession;
import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.XBreakpointHandler;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator;
import com.intellij.xdebugger.frame.XSuspendContext;
import com.intellij.xdebugger.frame.XValueMarkerProvider;
import com.intellij.xdebugger.impl.XDebugSessionImpl;
import com.intellij.xdebugger.stepping.XSmartStepIntoHandler;
import com.intellij.xdebugger.ui.XDebugTabLayouter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.Promise;

import javax.swing.event.HyperlinkListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.github.cameltooling.idea.runner.debugger.CamelDebuggerContext.CAMEL;
import static com.github.cameltooling.idea.runner.debugger.CamelDebuggerContext.JAVA;

public final class ContextAwareDebugProcess extends XDebugProcess {
    private final ProcessHandler processHandler;
    private final Map<CamelDebuggerContext, XDebugProcess> debugProcesses;
    private CamelDebuggerContext currentContext;
    private final CamelDebuggerContext defaultContext;

    private ContextAwareDebugProcess(@NotNull XDebugSession session, ProcessHandler processHandler,
                                    Map<CamelDebuggerContext, XDebugProcess> debugProcesses, CamelDebuggerContext defaultContext) {
        super(session);
        this.processHandler = processHandler;
        this.debugProcesses = debugProcesses;
        this.currentContext = defaultContext;
        this.defaultContext = defaultContext;
    }

    public void setContext(CamelDebuggerContext context) {
        this.currentContext = context;
    }

    @Override
    public XBreakpointHandler<?>@NotNull[] getBreakpointHandlers() {
        List<XBreakpointHandler<?>> breakpointHandlers = new ArrayList<>();
        final Collection<XDebugProcess> values = debugProcesses.values();
        for (XDebugProcess value : values) {
            breakpointHandlers.addAll(Arrays.asList(value.getBreakpointHandlers()));
        }
        return breakpointHandlers.toArray(new XBreakpointHandler[0]);
    }

    @Override
    @NotNull
    public XDebuggerEditorsProvider getEditorsProvider() {
        return getCurrentDebugProcess().getEditorsProvider();
    }

    @Override
    public void sessionInitialized() {
        getCurrentDebugProcess().sessionInitialized();
    }

    @Override
    public void startPausing() {
        getCurrentDebugProcess().startPausing();
    }

    @Override
    public void startStepOver(@Nullable XSuspendContext context) {
        getCurrentDebugProcess().startStepOver(context);
    }

    @Override
    public void startForceStepInto(@Nullable XSuspendContext context) {
        getCurrentDebugProcess().startForceStepInto(context);
    }

    @Override
    public void startStepInto(@Nullable XSuspendContext context) {
        getCurrentDebugProcess().startStepInto(context);
    }

    @Override
    public void startStepOut(@Nullable XSuspendContext context) {
        getCurrentDebugProcess().startStepOut(context);
    }

    @Override
    @Nullable
    public XSmartStepIntoHandler<?> getSmartStepIntoHandler() {
        return getCurrentDebugProcess().getSmartStepIntoHandler();
    }

    @Override
    public void stop() {
        final Collection<XDebugProcess> values = debugProcesses.values();
        for (XDebugProcess value : values) {
            value.stop();
        }
    }

    @Override
    @NotNull
    public Promise<Object> stopAsync() {
        for (XDebugProcess value : debugProcesses.values()) {
            value.stopAsync();
        }
        return getDefaultDebugProcess().stopAsync();
    }

    @Override
    public void resume(@Nullable XSuspendContext context) {
        getCurrentDebugProcess().resume(context);
    }

    @Override
    public void runToPosition(@NotNull XSourcePosition xSourcePosition, @Nullable XSuspendContext context) {
        getCurrentDebugProcess().runToPosition(xSourcePosition, context);
    }

    @Override
    public boolean checkCanPerformCommands() {
        return getCurrentDebugProcess().checkCanPerformCommands();
    }

    @Override
    public boolean checkCanInitBreakpoints() {
        return getCurrentDebugProcess().checkCanInitBreakpoints();
    }

    @Override
    @Nullable
    public ProcessHandler doGetProcessHandler() {
        return processHandler;
    }

    @Override
    @NotNull
    public ExecutionConsole createConsole() {
        return getDefaultDebugProcess().createConsole();
    }

    @Override
    @Nullable
    public XValueMarkerProvider<?, ?> createValueMarkerProvider() {
        return getDefaultDebugProcess().createValueMarkerProvider();
    }

    @Override
    public void registerAdditionalActions(@NotNull DefaultActionGroup leftToolbar, @NotNull DefaultActionGroup topToolbar,
                                          @NotNull DefaultActionGroup settings) {
        for (XDebugProcess value : debugProcesses.values()) {
            value.registerAdditionalActions(leftToolbar, topToolbar, settings);
        }
    }

    @Override
    public String getCurrentStateMessage() {
        return getCurrentDebugProcess().getCurrentStateMessage();
    }

    @Override
    @Nullable
    public HyperlinkListener getCurrentStateHyperlinkListener() {
        return getCurrentDebugProcess().getCurrentStateHyperlinkListener();
    }

    @Override
    @NotNull
    public XDebugTabLayouter createTabLayouter() {
        return getDefaultDebugProcess().createTabLayouter();
    }

    @Override
    public boolean isValuesCustomSorted() {
        return getCurrentDebugProcess().isValuesCustomSorted();
    }

    @Override
    @Nullable
    public XDebuggerEvaluator getEvaluator() {
        return getCurrentDebugProcess().getEvaluator();
    }

    public XDebugProcess getDefaultDebugProcess() {
        return debugProcesses.get(defaultContext);
    }

    public <T extends XDebugProcess> T getDebugProcess(CamelDebuggerContext name, Class<T> type) {
        return type.cast(debugProcesses.get(name));
    }

    public XDebugProcess getCurrentDebugProcess() {
        final XDebugProcess debugProcess = currentContext != null ? debugProcesses.get(currentContext) : null;
        return debugProcess != null ? debugProcess : getDefaultDebugProcess();
    }

    @NotNull
    static XDebugProcess createRemoteDebugProcess(@NotNull XDebugSession session, Project project, String jmxHost, int jmxPort) {
        final Map<CamelDebuggerContext, XDebugProcess> context = new EnumMap<>(CamelDebuggerContext.class);
        final ContextAwareDebugProcess contextAwareDebugProcess = new ContextAwareDebugProcess(session, null, context, CAMEL);

        final CamelDebuggerSession camelDebuggerSession = new CamelDebuggerSession(project, session, jmxHost, jmxPort);
        camelDebuggerSession.addMessageReceivedListener(messages -> contextAwareDebugProcess.setContext(CAMEL));

        //Init Camel Debug Process
        final CamelDebugProcess camelDebugProcess = new CamelDebugProcess(session, camelDebuggerSession);

        //Register Process
        context.put(CAMEL, camelDebugProcess);
        return contextAwareDebugProcess;
    }

    @NotNull
    static XDebugProcess createDebugProcess(@NotNull XDebugSession session, Project project, DebuggerSession debuggerSession, XDebugSessionImpl sessionImpl, ExecutionResult executionResult) {
        final Map<CamelDebuggerContext, XDebugProcess> context = new EnumMap<>(CamelDebuggerContext.class);
        final ContextAwareDebugProcess contextAwareDebugProcess = new ContextAwareDebugProcess(session, executionResult.getProcessHandler(), context, JAVA);

        debuggerSession.getContextManager().addListener((newContext, event) -> contextAwareDebugProcess.setContext(JAVA));

        sessionImpl.addExtraActions(executionResult.getActions());
        if (executionResult instanceof DefaultExecutionResult defaultExecutionResult) {
            sessionImpl.addRestartActions(defaultExecutionResult.getRestartActions());
        }
        final JavaDebugProcess javaDebugProcess = JavaDebugProcess.create(session, debuggerSession);
        final CamelDebuggerSession camelDebuggerSession = new CamelDebuggerSession(project, session, javaDebugProcess.getProcessHandler());
        camelDebuggerSession.addMessageReceivedListener(messages -> contextAwareDebugProcess.setContext(CAMEL));

        //Init Camel Debug Process
        final CamelDebugProcess camelDebugProcess = new CamelDebugProcess(session, camelDebuggerSession);

        //Register All Processes
        context.put(JAVA, javaDebugProcess);
        context.put(CAMEL, camelDebugProcess);
        return contextAwareDebugProcess;
    }
}
