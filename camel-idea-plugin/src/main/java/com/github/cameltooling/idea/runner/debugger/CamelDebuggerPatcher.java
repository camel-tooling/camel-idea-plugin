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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.github.cameltooling.idea.runner.CamelQuarkusRunConfigurationType;
import com.github.cameltooling.idea.runner.CamelRunConfiguration;
import com.github.cameltooling.idea.runner.CamelSpringBootRunConfigurationType;
import com.github.cameltooling.idea.service.CamelCatalogService;
import com.github.cameltooling.idea.service.CamelPreferenceService;
import com.github.cameltooling.idea.service.CamelRuntime;
import com.github.cameltooling.idea.service.CamelService;
import com.intellij.execution.Executor;
import com.intellij.execution.JavaRunConfigurationBase;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.ParametersList;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.runners.JavaProgramPatcher;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.XDebuggerManagerListener;
import groovy.grape.Grape;
import groovy.lang.GroovyClassLoader;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jetbrains.annotations.NotNull;

/**
 * A specific {@link JavaProgramPatcher} allowing to add the Camel Debugger and configure it automatically when launching
 * a Java program.
 * <p>
 * Only {@link CamelRunConfiguration} and {@link JavaRunConfigurationBase} are supported.
 * <p>
 * If the auto setup of the Camel Debugger has been enabled in the preferences, it detects the version
 * of Camel used and if the version is recent enough, it adds the artifacts {@code camel-debug} and {@code camel-management}
 * that match the best with the detected Camel runtime either to a temporary pom file or to the classpath in case of respectively
 * a {@link CamelRunConfiguration} or a {@link JavaRunConfigurationBase}. Finally, it automatically configures the Camel
 * Debugger by adding System properties and/or program parameters to the original command.
 * <p>
 * As Quarkus has a very specific ClassLoader which prevents to add the Camel Debugger on the fly, for now, only
 * {@link CamelRunConfiguration} is supported for the Camel Quarkus runtime.
 */
public class CamelDebuggerPatcher extends JavaProgramPatcher {

    private static final Logger LOG = Logger.getInstance(CamelDebuggerPatcher.class);

    /**
     * The name of the generated pom file used to add the Camel Debugger.
     */
    private static final String GENERATED_POM_FILE_NAME = ".camel.debugger.pom.xml";
    /**
     * The minimal Camel version that supports the Camel Debugger.
     */
    private static final String MIN_CAMEL_VERSION = "3.15.0";

    @Override
    public void patchJavaParameters(Executor executor, RunProfile configuration, JavaParameters parameters) {
        if (executor.getId().equals(DefaultDebugExecutor.EXECUTOR_ID)) {
            if (configuration instanceof CamelRunConfiguration) {
                final CamelRunConfiguration camelRunConfiguration = (CamelRunConfiguration) configuration;
                patchJavaParametersOnDebug(
                    camelRunConfiguration.getProject(), ExecutionMode.getCamelExecutionMode(camelRunConfiguration), parameters
                );
            } else if (configuration instanceof JavaRunConfigurationBase) {
                final JavaRunConfigurationBase javaRunConfiguration = (JavaRunConfigurationBase) configuration;
                final Project project = javaRunConfiguration.getProject();
                if (ExecutionMode.isSupportedJavaExecutionMode(project)) {
                    patchJavaParametersOnDebug(project, ExecutionMode.JAVA, parameters);
                }
            }
        } else if (configuration instanceof CamelRunConfiguration) {
            ExecutionMode.getCamelExecutionMode((CamelRunConfiguration) configuration).addRequiredParameters(parameters);
        }
    }

    /**
     * Patches the given parameters to ensure that the Camel Debugger is properly added and configured to have it working
     * automatically if applicable and possible otherwise only the required parameters are added.
     *
     * @param project    the project from which the Java program is launched.
     * @param mode       the execution mode to use to set up the Camel Debugger.
     * @param parameters the parameters to patch if needed
     */
    private static void patchJavaParametersOnDebug(Project project, ExecutionMode mode, JavaParameters parameters) {
        mode.addRequiredParameters(parameters);
        final CamelService service = project.getService(CamelService.class);
        if (!service.isCamelPresent()) {
            LOG.debug("The project is not a camel project so no need to patch the parameters");
        } else if (CamelPreferenceService.getService().isCamelDebuggerAutoSetup()) {
            if (service.containsLibrary("camel-debug", false)) {
                LOG.debug("The component camel-debug has been detected in the dependencies of the project");
                configureCamelDebugger(mode, parameters);
                return;
            }
            LOG.debug("The component camel-debug could not be found in the dependencies of the project and needs to be added");
            final String version = project.getService(CamelCatalogService.class).get().getLoadedVersion();
            if (new ComparableVersion(version).compareTo(new ComparableVersion(MIN_CAMEL_VERSION)) < 0) {
                //This is an older version of Camel, debugger is not supported
                if (LOG.isDebugEnabled()) {
                    LOG.debug("The version of camel is too old (" + version + ") to use camel-debug");
                }
                notify(project, MessageType.WARNING, "Camel version is " + version + ". Minimum required version for Camel Debugger is " + MIN_CAMEL_VERSION);
            } else {
                LOG.debug("The detected camel version is recent enough to try to add camel-debug automatically");
                try {
                    mode.autoAddCamelDebugger(project, parameters, version);
                    configureCamelDebugger(mode, parameters);
                    notify(project, MessageType.INFO, "Camel Debugger has been added automatically with success.");
                    LOG.debug("The camel-debug has been added with success");
                } catch (Exception e) {
                    LOG.error("Could not add the Camel Debugger automatically", e);
                    notify(project, MessageType.ERROR, "Camel Debugger could not be added automatically.");
                }
            }
        } else {
            notify(project, MessageType.WARNING,
                "Camel Debugger is not found in classpath. \nPlease add camel-debug or camel-debug-starter or camel-quarkus-debug"
                    + " to your project dependencies or enable auto setup in the preferences.");
            LOG.debug("The camel-debug is absent and should not be added automatically");
        }
    }

    /**
     * Configures the Camel Debugger.
     *
     * @param mode       the execution mode to use to configure the Camel Debugger.
     * @param parameters the parameters to patch to configure the Camel Debugger.
     */
    private static void configureCamelDebugger(ExecutionMode mode, JavaParameters parameters) {
        // Clear the environment variable
        parameters.getEnv().remove("CAMEL_DEBUGGER_SUSPEND");
        mode.addCamelDebuggerParameters(parameters);
    }

    /**
     * Provides the given message as notification to the end user.
     *
     * @param project the project for which the notification is delivered.
     * @param type    the type of message to deliver
     * @param message the message to provide to the end user.
     */
    private static void notify(Project project, MessageType type, String message) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Debugger messages")
            .createNotification(message, type).notify(project);
    }

    /**
     * Automatically adds the Camel Debugger and its dependencies to a temporary pom file and ensures that the temporary
     * pom file is deleted on exit.
     *
     * @param mode       the execution mode to use to add the Camel Debugger and its dependencies to a temporary pom file.
     * @param project    the project for which the Camel debugger is added.
     * @param parameters the parameters to patch to take into account the temporary pom file.
     * @param version    the default version of the artifacts to add.
     * @throws IOException            if the temporary pom file could not be generated due to an IO error.
     * @throws XmlPullParserException if the current pom file could not be read.
     */
    private static void autoAddCamelDebugger(ExecutionMode mode, Project project, JavaParameters parameters,
                                             String version) throws IOException, XmlPullParserException {
        autoDelete(project, generatePomFileWithCamelDebugger(mode, project, parameters, version));
    }

    /**
     * Reads the current pom file, removes any {@code camel-management} artifacts from the dependencies to prevent conflicts,
     * adds the artifacts {@code camel-debug} and {@code camel-management} corresponding to the given execution mode to the dependencies,
     * generates a new pom file with those changes and finally modifies the given parameters to refer to the new pom file.
     *
     * @param mode       the execution mode to use to add the Camel Debugger and its dependencies to the generated pom file.
     * @param project    the project for which the Camel debugger is added.
     * @param parameters the parameters to patch to take into account the generated pom file.
     * @param version    the default version of the artifacts to add.
     * @return a {@code File} corresponding to the generated pom file.
     * @throws IOException            if the generated pom file could not be generated due to an IO error.
     * @throws XmlPullParserException if the current pom file could not be read.
     */
    private static File generatePomFileWithCamelDebugger(ExecutionMode mode, Project project, JavaParameters parameters,
                                                         String version) throws IOException, XmlPullParserException {
        final Model model;
        try (FileReader fileReader = new FileReader(new File(parameters.getWorkingDirectory(), "pom.xml"))) {
            model = new MavenXpp3Reader().read(fileReader);
        }
        // Remove camel-management from the dependencies to prevent conflicts
        model.getDependencies().removeIf(CamelDebuggerPatcher::isCamelManagement);
        model.getDependencies().add(mode.createCamelDebugDependency(project, version));
        model.getDependencies().add(mode.createCamelManagementDependency(project, version));
        final File generatedPom = new File(parameters.getWorkingDirectory(), GENERATED_POM_FILE_NAME);
        try (FileWriter writer = new FileWriter(generatedPom)) {
            new MavenXpp3Writer().write(writer, model);
        }
        final ParametersList parametersList = parameters.getProgramParametersList();
        final int index = parametersList.getParameters().indexOf("-f");
        if (index == -1) {
            parametersList.add("-f");
            parametersList.add(GENERATED_POM_FILE_NAME);
        } else {
            parametersList.set(index + 1, GENERATED_POM_FILE_NAME);
        }
        return generatedPom;
    }

    /**
     * Indicates whether the given dependency is a {@code camel-management} artifact.
     *
     * @param dependency the dependency to check
     * @return {@code true} if the given dependency is a {@code camel-management} artifact, {@code false} otherwise.
     */
    private static boolean isCamelManagement(Dependency dependency) {
        for (CamelRuntime runtime : CamelRuntime.values()) {
            if (runtime.getManagementArtifactId().equals(dependency.getArtifactId())) {
                return runtime.getGroupIds().stream().anyMatch(groupId -> groupId.equals(dependency.getGroupId()));
            }
        }
        return false;
    }

    /**
     * Deletes automatically the given temporary pom file when the debug process is stopped.
     *
     * @param project          the project for which the topic {@link XDebuggerManager#TOPIC} is observed to trigger the
     *                         deletion.
     * @param temporaryPomFile the temporary pom file to automatically delete.
     */
    private static void autoDelete(Project project, File temporaryPomFile) {
        MessageBusConnection connection = project.getMessageBus().connect();
        connection.subscribe(XDebuggerManager.TOPIC, new XDebuggerManagerListener() {
            @Override
            public void processStopped(@NotNull XDebugProcess debugProcess) {
                if (!temporaryPomFile.delete()) {
                    LOG.debug("The temporary pom file could not be deleted");
                    temporaryPomFile.deleteOnExit();
                }
                connection.disconnect();
            }
        });
    }

    /**
     * Automatically downloads the Camel Debugger and its dependencies and adds them to the classpath.
     *
     * @param project    the project for which the Camel Debugger needs to be downloaded.
     * @param parameters the parameters in which the classpath changes are applied.
     * @param version    the default version of the Camel Debugger to download.
     * @throws IOException if the Camel Debugger and its dependencies could not be downloaded.
     */
    private static void autoDownloadCamelDebugger(Project project, JavaParameters parameters, String version) throws IOException {
        CamelRuntime runtime = CamelRuntime.getCamelRuntime(project);
        String versionRuntime = runtime.getVersion(project);
        if (versionRuntime == null) {
            versionRuntime = version;
            runtime = CamelRuntime.DEFAULT;
            if (LOG.isDebugEnabled()) {
                LOG.debug(
                    String.format("Using the default %s component as dependency", runtime.getDebugArtifactId())
                );
            }
        }
        for (URL url : downloadCamelDebugger(runtime, versionRuntime)) {
            parameters.getClassPath().add(url.getPath());
        }
    }

    /**
     * Downloads the Camel Debugger and its dependencies corresponding to the given runtime.
     *
     * @param runtime the target runtime for which the Camel Debugger is downloaded.
     * @param version the version of the Camel Debugger to download.
     * @return the list of {@link URL} corresponding to the location of the Camel Debugger and its dependencies that have
     * been downloaded.
     * @throws IOException if an error occurs while downloading the Camel Debugger and its dependencies.
     */
    @NotNull
    private static List<URL> downloadCamelDebugger(@NotNull CamelRuntime runtime, @NotNull String version) throws IOException {
        try (URLClassLoader classLoader = new GroovyClassLoader()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Trying to download %s %s", runtime.getDebugArtifactId(), version));
            }
            Grape.setEnableAutoDownload(true);

            final Map<String, Object> param = new HashMap<>();
            List<String> groupIds = runtime.getGroupIds();
            // Use the last group id as it is only supported in recent versions
            param.put("group", groupIds.get(groupIds.size() - 1));
            param.put("module", runtime.getDebugArtifactId());
            param.put("version", version);
            param.put("classLoader", classLoader);

            Grape.grab(param);

            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("The %s %s has been downloaded", runtime.getDebugArtifactId(), version));
            }
            return Arrays.asList(classLoader.getURLs());
        }
    }

    /**
     * Add the System properties needed to configure the Camel Debugger.
     *
     * @param parametersList the parameters' list to which the System properties are added.
     */
    private static void addCamelDebuggerProperties(ParametersList parametersList) {
        parametersList.addProperty("org.apache.camel.debugger.suspend", "true");
        parametersList.addProperty("org.apache.camel.jmx.disabled", "false");
    }

    /**
     * The supported execution modes.
     */
    private enum ExecutionMode {
        /**
         * Execution mode corresponding to {@link com.github.cameltooling.idea.runner.CamelMainRunConfigurationType}.
         */
        CAMEL_MAIN(CamelRuntime.DEFAULT) {
            @Override
            void addCamelDebuggerParameters(JavaParameters parameters) {
                addCamelDebuggerProperties(parameters.getProgramParametersList());
            }

            @Override
            void addRequiredParameters(JavaParameters parameters) {
                super.addPluginGoal(parameters);
            }

            @Override
            void autoAddCamelDebugger(Project project, JavaParameters parameters, String version) throws Exception {
                CamelDebuggerPatcher.autoAddCamelDebugger(this, project, parameters, version);
            }
        },
        /**
         * Execution mode corresponding to {@link com.github.cameltooling.idea.runner.CamelSpringBootRunConfigurationType}.
         */
        CAMEL_SPRING_BOOT(CamelRuntime.SPRING_BOOT) {
            @Override
            void addCamelDebuggerParameters(JavaParameters parameters) {
                addCamelDebuggerProperties(parameters.getProgramParametersList());
            }

            @Override
            void addRequiredParameters(JavaParameters parameters) {
                super.addPluginGoal(parameters);
                // Added as required parameters for backward compatibility reasons
                parameters.getProgramParametersList().addProperty("spring-boot.run.fork", "false");
            }

            @Override
            void autoAddCamelDebugger(Project project, JavaParameters parameters, String version) throws Exception {
                CamelDebuggerPatcher.autoAddCamelDebugger(this, project, parameters, version);
            }
        },
        /**
         * Execution mode corresponding to {@link com.github.cameltooling.idea.runner.CamelQuarkusRunConfigurationType}.
         */
        CAMEL_QUARKUS(CamelRuntime.QUARKUS) {
            @Override
            void addCamelDebuggerParameters(JavaParameters parameters) {
                addCamelDebuggerProperties(parameters.getProgramParametersList());
            }

            @Override
            void addRequiredParameters(JavaParameters parameters) {
                super.addPluginGoal(parameters);
            }

            @Override
            void autoAddCamelDebugger(Project project, JavaParameters parameters, String version) throws Exception {
                CamelDebuggerPatcher.autoAddCamelDebugger(this, project, parameters, version);
            }
        },
        /**
         * Execution mode corresponding to any Java applications launched from the IDE (main or test classes).
         */
        JAVA(null) {
            @Override
            Dependency createCamelDebugDependency(Project project, String version) {
                throw new UnsupportedOperationException();
            }

            @Override
            Dependency createCamelManagementDependency(Project project, String version) {
                throw new UnsupportedOperationException();
            }

            @Override
            void addCamelDebuggerParameters(JavaParameters parameters) {
                addCamelDebuggerProperties(parameters.getVMParametersList());
            }

            @Override
            void addRequiredParameters(JavaParameters parameters) {
                // Nothing to add by default
            }

            @Override
            void autoAddCamelDebugger(Project project, JavaParameters parameters, String version) throws Exception {
                autoDownloadCamelDebugger(project, parameters, version);
            }
        };

        /**
         * The corresponding Camel Runtime.
         */
        private final CamelRuntime runtime;

        /**
         * Constructs a {@code ExecutionMode} with the given Camel Runtime.
         *
         * @param runtime the corresponding Camel Runtime.
         */
        ExecutionMode(CamelRuntime runtime) {
            this.runtime = runtime;
        }

        /**
         * Creates a {@code Dependency} instance corresponding to the artifact provided by {@code artifactIdProvider}
         * that matches the best with the execution mode. If the version of the underlying runtime cannot be
         * found, by default the {@code Dependency} instance is created from the artifact of the default Camel runtime
         * with the provided version.
         *
         * @param project            the project for which the {@code Dependency} is created.
         * @param version            the default version of the artifact to use.
         * @param artifactIdProvider the provider of artifact id according to the Camel runtime used.
         * @return a new instance of {@code Dependency} matching with the context.
         */
        private Dependency createDependency(Project project, String version, Function<CamelRuntime, String> artifactIdProvider) {
            String versionRuntime = runtime.getVersion(project);
            Dependency dependency = new Dependency();
            final CamelRuntime actualRuntime;
            if (versionRuntime == null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(
                        String.format("Using the default %s component as dependency", artifactIdProvider.apply(CamelRuntime.DEFAULT))
                    );
                }
                actualRuntime = CamelRuntime.DEFAULT;
                dependency.setVersion(version);
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(
                        String.format("Using the specific %s component as dependency", artifactIdProvider.apply(CamelRuntime.DEFAULT))
                    );
                }
                actualRuntime = runtime;
                dependency.setVersion(versionRuntime);
            }
            List<String> groupIds = actualRuntime.getGroupIds();
            // Use the last group id as it is only supported in recent versions
            dependency.setGroupId(groupIds.get(groupIds.size() - 1));
            dependency.setArtifactId(artifactIdProvider.apply(actualRuntime));
            return dependency;
        }

        /**
         * Creates a {@code Dependency} instance corresponding to the artifact {@code camel-debug}.
         *
         * @param project the project for which the {@code Dependency} is created.
         * @param version the default version of the artifact to use.
         * @return a new instance of {@code Dependency} matching with the context and corresponding to the artifact
         * {@code camel-debug}.
         */
        Dependency createCamelDebugDependency(Project project, String version) {
            return createDependency(project, version, CamelRuntime::getDebugArtifactId);
        }

        /**
         * Creates a {@code Dependency} instance corresponding to the artifact {@code camel-management}.
         *
         * @param project the project for which the {@code Dependency} is created.
         * @param version the default version of the artifact to use.
         * @return a new instance of {@code Dependency} matching with the context and corresponding to the artifact
         * {@code camel-management}.
         */
        Dependency createCamelManagementDependency(Project project, String version) {
            return createDependency(project, version, CamelRuntime::getManagementArtifactId);
        }

        /**
         * Adds the parameters that are required to ensure that the program is launched properly.
         *
         * @param parameters the parameters to patch to add the required parameters.
         */
        abstract void addRequiredParameters(JavaParameters parameters);

        /**
         * Adds the parameters that are needed to configure the Camel Debugger properly.
         *
         * @param parameters the parameters to patch to configure the Camel Debugger.
         */
        abstract void addCamelDebuggerParameters(JavaParameters parameters);

        /**
         * Automatically add the Camel Debugger to the given parameters.
         *
         * @param project    the project for which the Camel Debugger should be added.
         * @param parameters the parameters to patch to add the Camel Debugger.
         * @param version    the default version of the Camel Debugger to add.
         * @throws Exception if an error occurs while adding the Camel Debugger.
         */
        abstract void autoAddCamelDebugger(Project project, JavaParameters parameters, String version) throws Exception;

        /**
         * @param camelRunConfiguration the Camel configuration from which the {@code ExecutionMode} is retrieved.
         * @return the corresponding {@code ExecutionMode}.
         */
        static ExecutionMode getCamelExecutionMode(CamelRunConfiguration camelRunConfiguration) {
            String id = camelRunConfiguration.getType().getId();
            if (CamelSpringBootRunConfigurationType.ID.equals(id)) {
                return CAMEL_SPRING_BOOT;
            } else if (CamelQuarkusRunConfigurationType.ID.equals(id)) {
                return CAMEL_QUARKUS;
            }
            return CAMEL_MAIN;
        }

        /**
         * @param project the project against which the Java program is launched.
         * @return {@code true} if the Java program is a Camel application and not a Quarkus application, {@code false}
         * otherwise.
         */
        static boolean isSupportedJavaExecutionMode(Project project) {
            final CamelService service = project.getService(CamelService.class);
            if (!service.isCamelPresent() || CamelRuntime.getCamelRuntime(project) == CamelRuntime.QUARKUS) {
                LOG.debug("Camel Debugger won't be added automatically as it is either not a Camel application or it is a Quarkus application");
                return false;
            }
            LOG.debug("The application is a Camel application and not a Quarkus application");
            return true;
        }

        /**
         * Adds the pair {@code maven-plugin-name:goal-name} to the given parameters.
         *
         * @param parameters the parameters to patch.
         */
        private void addPluginGoal(JavaParameters parameters) {
            parameters.getProgramParametersList().add(runtime.getPluginGoal());
        }
    }
}
