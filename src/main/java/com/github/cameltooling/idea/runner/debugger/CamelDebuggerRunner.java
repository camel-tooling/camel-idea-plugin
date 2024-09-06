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

import java.util.concurrent.atomic.AtomicReference;

import com.github.cameltooling.idea.runner.CamelJBangRunProfileState;
import com.github.cameltooling.idea.runner.CamelRemoteRunConfigurationOptions;
import com.github.cameltooling.idea.runner.CamelRemoteRunProfileState;
import com.github.cameltooling.idea.runner.debugger.breakpoint.CamelBreakpointHandler;
import com.github.cameltooling.idea.service.CamelProjectPreferenceService;
import com.github.cameltooling.idea.service.CamelService;
import com.intellij.debugger.DebuggerManagerEx;
import com.intellij.debugger.DefaultDebugEnvironment;
import com.intellij.debugger.engine.DebugProcessImpl;
import com.intellij.debugger.impl.DebuggerSession;
import com.intellij.debugger.impl.GenericDebuggerRunner;
import com.intellij.debugger.ui.tree.render.BatchEvaluator;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.configurations.RemoteConnection;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.impl.XDebugSessionImpl;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration;

public class CamelDebuggerRunner extends GenericDebuggerRunner {

    private static final Logger LOG = Logger.getInstance(CamelDebuggerRunner.class);
    @NonNls
    private static final String ID = "CamelDebuggerRunner";

    @NotNull
    @Override
    public String getRunnerId() {
        return ID;
    }

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        if (profile instanceof GradleRunConfiguration) {
            // GradleRunConfiguration must be excluded otherwise it won't be possible to debug a gradle task
            // see https://github.com/camel-tooling/camel-idea-plugin/issues/824
            return false;
        }
        if (profile instanceof RunConfigurationBase) {
            try {
                final RunConfigurationBase<?> base = (RunConfigurationBase<?>) profile;
                final Project project = base.getProject();
                if (!CamelProjectPreferenceService.getService(project).isEnableCamelDebugger()) {
                    return false;
                }
                final CamelService camelService = project.getService(CamelService.class);
                if (camelService != null) {
                    boolean isDebug = executorId.equals(DefaultDebugExecutor.EXECUTOR_ID);
                    boolean isCamelProject = camelService.isCamelProject();
                    boolean hasBreakPoints = CamelBreakpointHandler.hasBreakpoints(project);
                    boolean canRun = isDebug && isCamelProject && hasBreakPoints;
                    LOG.debug("Executor ID is %s ; Camel project = %s ; Camel breakpoints present = %s ; canRun is %s".formatted(executorId, isCamelProject, hasBreakPoints, canRun));
                    return canRun;
                }
            } catch (Exception e) {
                LOG.debug("Camel Debugger cannot run", e);
                return false;
            }
        }
        LOG.debug("Camel Debugger cannot run, profile is not RunConfiguration");
        return false;
    }

    @Nullable
    protected RunContentDescriptor createContentDescriptor(@NotNull RunProfileState state,
                                                           @NotNull ExecutionEnvironment environment) throws ExecutionException {
        if (state instanceof CamelJBangRunProfileState connection) {
            return attachVirtualMachine(state, environment, connection.createRemoteConnection(), false);
        } else if (state instanceof CamelRemoteRunProfileState connection) {
            AtomicReference<ExecutionException> ex = new AtomicReference<>();
            AtomicReference<RunContentDescriptor> result = new AtomicReference<>();

            ApplicationManager.getApplication().invokeAndWait(() -> {
                try {
                    result.set(attachRemoteServer(connection, environment));
                } catch (ExecutionException e) {
                    ex.set(e);
                }
            });
            if (ex.get() != null) {
                throw ex.get();
            }
            return result.get();
        }

        return super.createContentDescriptor(state, environment);
    }

    @Override
    @Nullable
    protected RunContentDescriptor attachVirtualMachine(final RunProfileState state, final @NotNull ExecutionEnvironment env, RemoteConnection connection, boolean pollConnection)
            throws ExecutionException {
        AtomicReference<ExecutionException> ex = new AtomicReference<>();
        AtomicReference<RunContentDescriptor> result = new AtomicReference<>();

        ApplicationManager.getApplication().invokeAndWait(() -> {
            try {
                result.set(attachVM(state, env, connection, pollConnection));
            } catch (ExecutionException e) {
                ex.set(e);
            }
        });
        if (ex.get() != null) {
            throw ex.get();
        }
        return result.get();
    }

    private RunContentDescriptor attachVM(final RunProfileState state, final @NotNull ExecutionEnvironment env, RemoteConnection connection, boolean pollConnection)
            throws ExecutionException {
        LOG.debug("Attaching VM...");
        DefaultDebugEnvironment environment = new DefaultDebugEnvironment(env, state, connection, pollConnection);
        Project project = env.getProject();
        final DebuggerSession debuggerSession = DebuggerManagerEx.getInstanceEx(project).attachVirtualMachine(environment);
        if (debuggerSession == null) {
            return null;
        }
        final DebugProcessImpl debugProcess = debuggerSession.getProcess();
        if (!debugProcess.isDetached() && !debugProcess.isDetaching()) {
            if (environment.isRemote()) {
                debugProcess.putUserData(BatchEvaluator.REMOTE_SESSION_KEY, Boolean.TRUE);
            }

            return XDebuggerManager.getInstance(project).startSession(env, new XDebugProcessStarter() {
                @NotNull
                public XDebugProcess start(@NotNull XDebugSession session) {
                    final XDebugSessionImpl sessionImpl = (XDebugSessionImpl) session;
                    final ExecutionResult executionResult = debugProcess.getExecutionResult();
                    return ContextAwareDebugProcess.createDebugProcess(session, project, debuggerSession, sessionImpl, executionResult);
                }
            }).getRunContentDescriptor();
        } else {
            debuggerSession.dispose();
            return null;
        }
    }

    private RunContentDescriptor attachRemoteServer(final CamelRemoteRunProfileState state, final @NotNull ExecutionEnvironment env)
            throws ExecutionException {
        LOG.debug("Attaching Remote Server...");
        Project project = env.getProject();
        return XDebuggerManager.getInstance(project).startSession(env, new XDebugProcessStarter() {
            @NotNull
            public XDebugProcess start(@NotNull XDebugSession session) {
                CamelRemoteRunConfigurationOptions options = state.getConfiguration().getOptions();
                return ContextAwareDebugProcess.createRemoteDebugProcess(session, project, options.getHost(), options.getPort());
            }
        }).getRunContentDescriptor();
    }
}