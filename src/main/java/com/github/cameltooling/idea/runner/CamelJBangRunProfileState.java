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
package com.github.cameltooling.idea.runner;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.HashSet;
import java.util.Set;

import com.github.cameltooling.idea.service.CamelJBangService;
import com.github.cameltooling.idea.service.CamelProjectPreferenceService;
import com.github.cameltooling.idea.service.CamelRuntime;
import com.github.cameltooling.idea.util.ArtifactCoordinates;
import com.intellij.debugger.impl.DebuggerManagerImpl;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.RemoteConnection;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.process.KillableColoredProcessHandler;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessTerminatedListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.target.RunTargetsEnabled;
import com.intellij.execution.target.TargetEnvironment;
import com.intellij.execution.target.TargetEnvironmentAwareRunProfileState;
import com.intellij.execution.target.TargetEnvironmentRequest;
import com.intellij.execution.target.TargetProgressIndicator;
import com.intellij.execution.target.TargetedCommandLine;
import com.intellij.execution.target.TargetedCommandLineBuilder;
import com.intellij.execution.target.local.LocalTargetEnvironmentRequest;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class CamelJBangRunProfileState extends CommandLineState implements TargetEnvironmentAwareRunProfileState {

    private static final Logger LOG = Logger.getInstance(CamelJBangRunProfileState.class);
    private TargetedCommandLineBuilder commandLine;
    private final Executor executor;
    private final CamelJBangRunConfiguration configuration;
    private RemoteConnection connection;

    CamelJBangRunProfileState(Executor executor, ExecutionEnvironment environment, CamelJBangRunConfiguration configuration) {
        super(environment);
        this.executor = executor;
        this.configuration = configuration;
    }

    public RemoteConnection createRemoteConnection() {
        if (connection == null) {
            connection = new RemoteConnection(true, DebuggerManagerImpl.LOCALHOST_ADDRESS_FALLBACK, Integer.toString(findPort()), false);
        }
        return connection;
    }

    private static int findPort() {
//        Restore me when https://github.com/jbangdev/jbang/issues/1689 will be fixed
//        try (ServerSocket ss = new ServerSocket()) {
//            ss.setReuseAddress(false);
//            ss.bind(new InetSocketAddress(0));
//            return ss.getLocalPort();
//        } catch (IOException e) {
//            LOG.debug("Could not find a port", e);
//        }
        return 4004;
    }

    protected OSProcessHandler startProcess() throws ExecutionException {
        TargetEnvironment remoteEnvironment = getEnvironment().getPreparedTargetEnvironment(this, TargetProgressIndicator.EMPTY);
        TargetedCommandLineBuilder targetedCommandLineBuilder = getTargetedCommandLine();
        TargetedCommandLine targetedCommandLine = targetedCommandLineBuilder.build();
        Process process = remoteEnvironment.createProcess(targetedCommandLine, new EmptyProgressIndicator());

        OSProcessHandler handler = new KillableColoredProcessHandler.Silent(process,
            targetedCommandLine.getCommandPresentation(remoteEnvironment),
            targetedCommandLine.getCharset(),
            targetedCommandLineBuilder.getFilesToDeleteOnTermination());
        ProcessTerminatedListener.attach(handler);
        awaitForPortToBeReady(process);
        return handler;
    }

    /**
     * Ensures that the debug port is ready to accept connections
     */
    private void awaitForPortToBeReady(Process process) {
        if (connection != null) {
            try {
                // Don't close the reader since the process is still running
                String line = process.inputReader().readLine();
                if (line == null) {
                    LOG.warn("Could not debug the Camel JBang application");
                    try (BufferedReader reader = process.errorReader()) {
                        reader.lines().forEach(LOG::warn);
                    }
                } else {
                    LOG.debug("The line contains the expected pattern: " + line.contains("Listening for transport"));
                }
            } catch (IOException e) {
                LOG.debug("Could not ensure that the debug port is ready to accept connection");
            }
        }
    }

    @NotNull
    private TargetedCommandLineBuilder getTargetedCommandLine() {
        if (commandLine != null) {
            return commandLine;
        }

        if (RunTargetsEnabled.get() && !(getEnvironment().getTargetEnvironmentRequest() instanceof LocalTargetEnvironmentRequest)) {
            LOG.error("Command line hasn't been built yet. "
                + "Probably you need to run environment#getPreparedTargetEnvironment first, "
                + "or it return the environment from the previous run session");
        }
        try {
            // force re-prepareTargetEnvironment in order to drop previous environment
            getEnvironment().prepareTargetEnvironment(this, TargetProgressIndicator.EMPTY);
            return commandLine;
        } catch (ExecutionException e) {
            LOG.error(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void prepareTargetEnvironmentRequest(
        @NotNull TargetEnvironmentRequest request,
        @NotNull TargetProgressIndicator targetProgressIndicator) {
        this.commandLine = createTargetedCommandLine(request);
    }

    @Override
    public void handleCreatedTargetEnvironment(@NotNull TargetEnvironment targetEnvironment,
                                               @NotNull TargetProgressIndicator targetProgressIndicator) {
        // Nothing to do
    }

    @NotNull
    protected TargetedCommandLineBuilder createTargetedCommandLine(@NotNull TargetEnvironmentRequest request) {
        TargetedCommandLineBuilder builder = new TargetedCommandLineBuilder(request);
        builder.setExePath("jbang");
        CamelJBangRunConfigurationOptions options = configuration.getOptions();
        boolean debug = executor.getId().equals(DefaultDebugExecutor.EXECUTOR_ID);
        if (debug) {
            builder.addParameter(String.format("--debug=%s:%s", connection.getDebuggerHostName(), connection.getDebuggerAddress()));
            builder.addParameter("-Dorg.apache.camel.debugger.suspend=true");
            builder.addParameter("-Dorg.apache.camel.jmx.disabled=false");
        }
        Project project = configuration.getProject();
        String version = project.getService(CamelJBangService.class).getCamelJBangVersion();
        if (version != null) {
            builder.addParameter(String.format("-Dcamel.jbang.version=%s", version));
        }
        builder.addParameter("camel@apache/camel");
        builder.addParameter("run");
        options.getCmdOptions().forEach(builder::addParameter);
        Set<String> dependencies = new HashSet<>(options.getDependencies());
        if (debug) {
            CamelProjectPreferenceService preferenceService = CamelProjectPreferenceService.getService(project);
            CamelRuntime runtime = preferenceService.getCamelCatalogProvider().getRuntime();
            dependencies.add(runtime.getDebugArtifact().getArtifactId());
            dependencies.add(runtime.getManagementArtifact().getArtifactId());
            ArtifactCoordinates additionalArtifact = runtime.getAdditionalArtifact();
            if (additionalArtifact != null) {
                dependencies.add(additionalArtifact.getArtifactId());
            }
        }
        if (!dependencies.isEmpty()) {
            builder.addParameter(String.format("--deps=%s", String.join(",", dependencies)));
        }
        options.getFiles().forEach(builder::addParameter);
        String basePath = project.getBasePath();
        if (basePath != null) {
            builder.setWorkingDirectory(basePath);
        }
        return builder;
    }
}
