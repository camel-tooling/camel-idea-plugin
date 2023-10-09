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
import java.io.Writer;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Function;

import com.github.cameltooling.idea.runner.CamelQuarkusRunConfigurationType;
import com.github.cameltooling.idea.runner.CamelRunConfiguration;
import com.github.cameltooling.idea.runner.CamelSpringBootRunConfigurationType;
import com.github.cameltooling.idea.runner.debugger.breakpoint.CamelBreakpointHandler;
import com.github.cameltooling.idea.service.CamelCatalogService;
import com.github.cameltooling.idea.service.CamelProjectPreferenceService;
import com.github.cameltooling.idea.service.CamelRuntime;
import com.github.cameltooling.idea.service.CamelService;
import com.github.cameltooling.idea.service.MavenArtifactRetrieverContext;
import com.github.cameltooling.idea.util.ArtifactCoordinates;
import com.intellij.execution.Executor;
import com.intellij.execution.JavaRunConfigurationBase;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.ParametersList;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.runners.JavaProgramPatcher;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.task.ExecuteRunConfigurationTask;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.XDebuggerManagerListener;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration;
import org.jetbrains.plugins.gradle.settings.GradleSettings;

/**
 * A specific {@link JavaProgramPatcher} allowing to add the Camel Debugger when possible and configure it automatically
 * when launching a Java program.
 * <p>
 * Only {@link CamelRunConfiguration}, {@link JavaRunConfigurationBase} and {@link ExecuteRunConfigurationTask} are supported.
 * <p>
 * If the auto setup of the Camel Debugger has been enabled in the preferences and at least one Camel breakpoint has been detected,
 * it detects the version of Camel used and if the version is recent enough, when possible it adds the artifacts {@code camel-debug} and {@code camel-management}
 * that match the best with the detected Camel runtime either to a temporary pom/gradle build file or to the classpath in case of respectively
 * a {@link CamelRunConfiguration}/{@link ExecuteRunConfigurationTask} or a {@link JavaRunConfigurationBase}. Finally, it automatically configures the Camel
 * Debugger by adding System properties, environment variables and/or program parameters to the original command.
 * <p>
 * As Quarkus has a very specific ClassLoader which prevents to add the Camel Debugger on the fly, for now, it is only
 * possible with the {@link CamelRunConfiguration}, for the other supported configurations the addition must be made manually.
 * <p>
 * In case of a Gradle project, if a main method is launched, the automatic addition of the Camel Debugger is not yet
 * supported, thus the addition must be made manually.
 */
public class CamelDebuggerPatcher extends JavaProgramPatcher {

    private static final Logger LOG = Logger.getInstance(CamelDebuggerPatcher.class);

    /**
     * The prefix of the generated file used to add the Camel Debugger.
     */
    private static final String GENERATED_FILE_NAME_PREFIX = ".camel.debugger.";
    /**
     * The minimal Camel version that supports the Camel Debugger.
     */
    private static final String MIN_CAMEL_VERSION = "3.15.0";

    @Override
    public void patchJavaParameters(Executor executor, RunProfile configuration, JavaParameters parameters) {
        if (executor.getId().equals(DefaultDebugExecutor.EXECUTOR_ID)) {
            if (configuration instanceof CamelRunConfiguration camelRunConfiguration) {
                patchJavaParametersOnDebug(
                    camelRunConfiguration.getProject(), ExecutionMode.getCamelExecutionMode(camelRunConfiguration), parameters
                );
            } else if (configuration instanceof JavaRunConfigurationBase javaRunConfiguration) {
                final Project project = javaRunConfiguration.getProject();
                patchJavaParametersOnDebug(project, CamelRuntime.getCamelRuntime(project) == CamelRuntime.QUARKUS ? ExecutionMode.JAVA_QUARKUS : ExecutionMode.JAVA, parameters);
            }
        } else if (configuration instanceof CamelRunConfiguration camelRunConfiguration) {
            ExecutionMode.getCamelExecutionMode(camelRunConfiguration).addRequiredParameters(parameters);
        }
    }

    /**
     * Patches the parameters of the given task when applicable.
     *
     * @param executor the executor of the task
     * @param task the task to execute
     */
    static void patchExecuteRunConfigurationTask(@Nullable Executor executor, @NotNull ExecuteRunConfigurationTask task) {
        if (executor != null && executor.getId().equals(DefaultDebugExecutor.EXECUTOR_ID)) {
            RunProfile runProfile = task.getRunProfile();
            if (runProfile instanceof ApplicationConfiguration applicationConfiguration) {
                final Project project = applicationConfiguration.getProject();
                if (isGradleProject(project)) {
                    patchApplicationConfigurationOnDebug(project, applicationConfiguration);
                }
            } else if (runProfile instanceof GradleRunConfiguration runConfiguration) {
                patchGradleRunConfigurationOnDebug(runConfiguration.getProject(), runConfiguration);
            }
        }
    }

    /**
     * Indicates whether the given project is a Gradle project or not.
     * @param project the project to check.
     * @return {@code true} if it is a Gradle project, {@code false} otherwise.
     */
    private static boolean isGradleProject(@NotNull Project project) {
        return !GradleSettings.getInstance(project).getLinkedProjectsSettings().isEmpty();
    }

    /**
     * Patches the parameters of the provided gradle configuration when needed.
     *
     * @param project the project from which the Java program is launched.
     * @param configuration the gradle configuration to patch.
     */
    private static void patchGradleRunConfigurationOnDebug(Project project, GradleRunConfiguration configuration) {
        JavaParameters parameters = new JavaParameters();
        ExternalSystemTaskExecutionSettings settings = configuration.getSettings();
        File moduleRootDir = new File(settings.getExternalProjectPath());
        if (moduleRootDir.exists()) {
            parameters.setWorkingDirectory(moduleRootDir);
        } else {
            LOG.warn("The external project path %s doesn't exist".formatted(settings.getExternalProjectPath()));
        }
        parameters.getProgramParametersList().addAll(settings.getTaskNames());
        String sScriptParametersBefore = settings.getScriptParameters();
        if (sScriptParametersBefore != null) {
            parameters.getProgramParametersList().addAll(Arrays.asList(sScriptParametersBefore.split(" ")));
        }
        patchJavaParametersOnDebug(project, ExecutionMode.GRADLE_GENERIC, parameters);
        Map<String, String> env = parameters.getEnv();
        if (env.isEmpty()) {
            return;
        }
        Map<String, String> before = settings.getEnv();
        // The map from getEnv() is immutable so let's create a new map
        Map<String, String> after = new HashMap<>(settings.getEnv());
        after.putAll(env);
        settings.setEnv(after);
        settings.setScriptParameters(String.join(" ", parameters.getProgramParametersList().getParameters()));
        registerCleanUpTask(project, () -> {
            LOG.debug("Restore the environment variables and script parameters");
            settings.setEnv(before);
            settings.setScriptParameters(sScriptParametersBefore);
        });
    }

    /**
     * Patches the parameters of the provided application configuration when needed.
     *
     * @param project the project from which the Java program is launched.
     * @param configuration the application configuration to patch.
     */
    private static void patchApplicationConfigurationOnDebug(Project project, ApplicationConfiguration configuration) {
        JavaParameters parameters = new JavaParameters();
        patchJavaParametersOnDebug(project, ExecutionMode.GRADLE_JAVA_MAIN, parameters);
        List<String> vmParametersAdded = parameters.getVMParametersList().getParameters();
        if (vmParametersAdded.isEmpty()) {
            return;
        }
        StringJoiner allVMParameters = new StringJoiner(" ");
        String vmParameters = configuration.getVMParameters();
        Set<String> existingParams = new HashSet<>();
        if (vmParameters != null) {
            existingParams.addAll(Arrays.asList(vmParameters.split(" ")));
            allVMParameters.add(vmParameters);
        }
        vmParametersAdded.forEach(
            param -> {
                if (!existingParams.contains(param)) {
                    allVMParameters.add(param);
                }
            }
        );
        configuration.setVMParameters(allVMParameters.toString());
        registerCleanUpTask(project, () -> {
            LOG.debug("Restore the VM parameters");
            configuration.setVMParameters(vmParameters);
        });
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
        if (!service.isCamelProject()) {
            LOG.debug("The project is not a camel project so no need to patch the parameters");
        } else if (mode.canCheckCamelBreakpoints() && !CamelBreakpointHandler.hasBreakpoints(project)) {
            LOG.debug("The project has no camel breakpoints so no need to patch the parameters");
        } else {
            CamelProjectPreferenceService preferenceService = CamelProjectPreferenceService.getService(project);
            if (!preferenceService.isEnableCamelDebugger()) {
                LOG.debug("The Camel Debugger has been disabled so no need to patch the parameters");
            } else if (preferenceService.isCamelDebuggerAutoSetup()) {
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
                } else if (mode.canAutoAddCamelDebugger()) {
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
                } else {
                    notify(project, MessageType.WARNING,  "Camel Debugger is not found in classpath. \nPlease add camel-debug or camel-debug-starter or camel-quarkus-debug"
                        + " to your project dependencies.");
                    LOG.debug("The camel-debug is absent and cannot be added automatically");
                }
            } else {
                notify(project, MessageType.WARNING,
                    "Camel Debugger is not found in classpath. \nPlease add camel-debug or camel-debug-starter or camel-quarkus-debug"
                        + " to your project dependencies or enable auto setup in the preferences.");
                LOG.debug("The camel-debug is absent and should not be added automatically");
            }
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
        mode.configureCamelDebugger(parameters);
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
    private static void autoAddCamelDebuggerToCustomPom(ExecutionMode mode, Project project, JavaParameters parameters,
                                                        String version) throws IOException, XmlPullParserException {
        autoDelete(project, List.of(generatePomFileWithCamelDebugger(mode, project, parameters, version)));
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
        final ParametersList parametersList = parameters.getProgramParametersList();
        final int index = Math.max(parametersList.getParameters().indexOf("-f"), parametersList.getParameters().indexOf("--file"));
        String targetFileName = index == -1 ? "pom.xml" : parametersList.get(index + 1);
        try (FileReader fileReader = new FileReader(new File(parameters.getWorkingDirectory(), targetFileName))) {
            model = new MavenXpp3Reader().read(fileReader);
        }
        // Remove camel-management from the dependencies to prevent conflicts
        model.getDependencies().removeIf(CamelDebuggerPatcher::isCamelManagement);
        model.getDependencies().add(mode.createCamelDebugDependency(project, version));
        model.getDependencies().add(mode.createCamelManagementDependency(project, version));
        Dependency additionalDependency = mode.createAdditionalDependency(project, version);
        if (additionalDependency != null) {
            // Remove additional dependency from the dependencies to prevent conflicts
            model.getDependencies().removeIf(dependency ->
                additionalDependency.getGroupId().equals(dependency.getGroupId())
                && additionalDependency.getArtifactId().equals(dependency.getArtifactId()));
            model.getDependencies().add(additionalDependency);
        }
        final String fileName = String.format("%s%s", GENERATED_FILE_NAME_PREFIX, targetFileName);
        final File generatedPom = new File(parameters.getWorkingDirectory(), fileName);
        try (FileWriter writer = new FileWriter(generatedPom)) {
            new MavenXpp3Writer().write(writer, model);
        }
        if (index == -1) {
            parametersList.add("-f");
            parametersList.add(fileName);
        } else {
            parametersList.set(index + 1, fileName);
        }
        return generatedPom;
    }

    /**
     * Automatically adds the Camel Debugger and its dependencies to a temporary gradle build file and ensures that the temporary
     * gradle build file is deleted on exit.
     *
     * @param mode       the execution mode to use to add the Camel Debugger and its dependencies to the generated gradle build file.
     * @param project    the project for which the Camel debugger is added.
     * @param parameters the parameters to patch to take into account the temporary gradle build file.
     * @param version    the default version of the artifacts to add.
     * @throws IOException            if the temporary gradle build file could not be generated due to an IO error.
     */
    private static void autoAddCamelDebuggerToCustomGradleBuild(ExecutionMode mode, Project project, JavaParameters parameters,
                                                                String version) throws IOException {
        autoDelete(project, generateBuildFilesWithCamelDebugger(mode, project, parameters, version));
    }

    /**
     * Adds the artifacts {@code camel-debug} and {@code camel-management} corresponding to the given execution mode to the dependencies,
     * generates a new gradle build file with those changes and finally modifies the given parameters to refer to the new gradle build file.
     *
     * @param mode       the execution mode to use to add the Camel Debugger and its dependencies to the generated gradle build file.
     * @param project    the project for which the Camel debugger is added.
     * @param parameters the parameters to patch to take into account the generated gradle build file.
     * @param version    the default version of the artifacts to add.
     * @return a list of {@code File} corresponding to the generated gradle build file and potentially the settings file. An empty list
     * if the working directory could not be found.
     * @throws IOException            if the gradle build file or settings file could not be generated due to an IO error.
     */
    private static List<File> generateBuildFilesWithCamelDebugger(ExecutionMode mode, Project project, JavaParameters parameters,
                                                                  String version) throws IOException {
        String workingDirectory = parameters.getWorkingDirectory();
        if (workingDirectory == null) {
            return List.of();
        }
        Path parent = Path.of(workingDirectory);
        final ParametersList parametersList = parameters.getProgramParametersList();
        List<String> listParameters = parametersList.getParameters();
        final int indexBuildFile = Math.max(listParameters.indexOf("-b"), listParameters.indexOf("--build-file"));
        final GradleBuildWriter writer;
        final String targetBuildFileName;
        if (indexBuildFile == -1) {
            if (Files.exists(parent.resolve("build.gradle.kts")) || Files.exists(parent.resolve("settings.gradle.kts"))) {
                targetBuildFileName = "build.gradle.kts";
                writer = GradleBuildWriter.KOTLIN;
            } else {
                targetBuildFileName = "build.gradle";
                writer = GradleBuildWriter.GROOVY;
            }
        } else {
            targetBuildFileName = parametersList.get(indexBuildFile + 1);
            if (targetBuildFileName.endsWith(".kts")) {
                writer = GradleBuildWriter.KOTLIN;
            } else {
                writer = GradleBuildWriter.GROOVY;
            }
        }

        Path buildFile = createGeneratedFile(parent, targetBuildFileName);
        boolean added = false;
        try (FileWriter fileWriter = new FileWriter(buildFile.toFile(), true)) {
            List<Dependency> dependencies = List.of(
                mode.createCamelDebugDependency(project, version), mode.createCamelManagementDependency(project, version)
            );
            Dependency additional = mode.createAdditionalDependency(project, version);
            if (additional != null) {
                dependencies = new ArrayList<>(dependencies);
                dependencies.add(additional);
            }
            for (String parameter : listParameters) {
                if (parameter.startsWith("-") || parameter.startsWith("\"")) {
                    continue;
                }
                int expectedLength = parameter.startsWith(":") ? 3 : 2;
                if (parameter.split(":").length >= expectedLength) {
                    added = true;
                    String projectName = parameter.substring(0, parameter.lastIndexOf(':'));
                    writer.write(fileWriter, dependencies, projectName);
                }
            }
            if (!added) {
                for (Dependency dependency : dependencies) {
                    writer.write(fileWriter, dependency);
                }
            }
        }
        if (indexBuildFile == -1) {
            parametersList.add("-b");
            parametersList.add(buildFile.getFileName().toString());
        } else {
            parametersList.set(indexBuildFile + 1, buildFile.getFileName().toString());
        }
        if (added) {
            final int indexSettingsFile = Math.max(listParameters.indexOf("-c"), listParameters.indexOf("--settings-file"));
            final String targetSettingsFileName;
            if (indexSettingsFile == -1) {
                if (writer == GradleBuildWriter.KOTLIN) {
                    targetSettingsFileName = "settings.gradle.kts";
                } else {
                    targetSettingsFileName = "settings.gradle";
                }
            } else {
                targetSettingsFileName = parametersList.get(indexSettingsFile + 1);
            }
            Path settingsFile =  createGeneratedFile(parent, targetSettingsFileName);
            try (FileWriter fileWriter = new FileWriter(settingsFile.toFile(), true)) {
                writer.write(fileWriter, buildFile);
            }
            if (indexSettingsFile == -1) {
                parametersList.add("-c");
                parametersList.add(settingsFile.getFileName().toString());
            } else {
                parametersList.set(indexSettingsFile + 1, settingsFile.getFileName().toString());
            }
            return List.of(buildFile.toFile(), settingsFile.toFile());
        } else {
            return List.of(buildFile.toFile());
        }
    }

    /**
     * Creates a new generated file initialized with the content of the given source file if it
     * exists. The file is created directly under the given parent path.
     *
     * @param parent the parent folder of the generated file.
     * @param sourceFileName the name of the source file
     * @return the created file.
     * @throws IOException if the file could not be created.
     */
    private static Path createGeneratedFile(Path parent, String sourceFileName) throws IOException {
        if (sourceFileName.startsWith(GENERATED_FILE_NAME_PREFIX)) {
            sourceFileName = sourceFileName.substring(GENERATED_FILE_NAME_PREFIX.length());
        }
        final String generatedFileName = String.format("%s%s", GENERATED_FILE_NAME_PREFIX, sourceFileName);
        Path target = parent.resolve(generatedFileName);
        Path source = parent.resolve(sourceFileName);
        if (Files.exists(source)) {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
    }

    /**
     * Indicates whether the given dependency is a {@code camel-management} artifact.
     *
     * @param dependency the dependency to check
     * @return {@code true} if the given dependency is a {@code camel-management} artifact, {@code false} otherwise.
     */
    private static boolean isCamelManagement(Dependency dependency) {
        for (CamelRuntime runtime : CamelRuntime.values()) {
            if (runtime.getManagementArtifact().getArtifactId().equals(dependency.getArtifactId())) {
                return runtime.getGroupIds().stream().anyMatch(groupId -> groupId.equals(dependency.getGroupId()));
            }
        }
        return false;
    }

    /**
     * Deletes automatically the given temporary files when the debug process is stopped.
     *
     * @param project          the project for which the topic {@link XDebuggerManager#TOPIC} is observed to trigger the
     *                         deletion.
     * @param temporaryFiles the temporary files to automatically delete.
     */
    private static void autoDelete(Project project, List<File> temporaryFiles) {
        if (temporaryFiles.isEmpty()) {
            return;
        }
        registerCleanUpTask(project, () -> {
            for (File temporaryFile : temporaryFiles) {
                if (!temporaryFile.delete()) {
                    LOG.debug("The temporary file %s could not be deleted".formatted(temporaryFile));
                    temporaryFile.deleteOnExit();
                }
            }
        });
    }

    /**
     * Registers a cleanup task to be executed once the debugger is stopped.
     *
     * @param project the project for which the underlying callback must be registered.
     * @param task the task to execute once the debugger is stopped.
     */
    private static void registerCleanUpTask(Project project, Runnable task) {
        MessageBusConnection connection = project.getMessageBus().connect();
        connection.subscribe(XDebuggerManager.TOPIC, new XDebuggerManagerListener() {
            @Override
            public void processStopped(@NotNull XDebugProcess debugProcess) {
                try {
                    task.run();
                } finally {
                    connection.disconnect();
                }
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
                    String.format("Using the default %s component as dependency", runtime.getDebugArtifact())
                );
            }
        }
        for (URL url : downloadCamelDebugger(runtime, versionRuntime)) {
            try {
                parameters.getClassPath().add(new File(url.toURI()));
            } catch (URISyntaxException e) {
                LOG.warn(String.format("The URL %s could not be converted to an URI: %s", url, e.getMessage()));
            }
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
        try (MavenArtifactRetrieverContext context = new MavenArtifactRetrieverContext()) {
            ArtifactCoordinates debugArtifact = runtime.getDebugArtifact();
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Trying to download %s %s with all its dependencies", debugArtifact, version));
            }

            context.add(debugArtifact.getGroupId(), debugArtifact.getArtifactId(), version);

            ArtifactCoordinates additionalArtifact = runtime.getAdditionalArtifact();
            if (additionalArtifact != null) {
                context.add(additionalArtifact.getGroupId(), additionalArtifact.getArtifactId(), version);
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("The %s %s has been downloaded with all its dependencies", debugArtifact, version));
            }
            return Arrays.asList(context.getClassLoader().getURLs());
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
     * Add the environment variable needed to configure the Camel Debugger.
     *
     * @param parameters the parameters to which the environment variable is added.
     */
    private static void addCamelDebuggerEnvironmentVariable(JavaParameters parameters) {
        parameters.getEnv().put("CAMEL_DEBUGGER_SUSPEND", "true");
        parameters.getEnv().put("CAMEL_MAIN_DEBUGGING", "true");
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
            void configureCamelDebugger(JavaParameters parameters) {
                addCamelDebuggerProperties(parameters.getProgramParametersList());
            }

            @Override
            void addRequiredParameters(JavaParameters parameters) {
                super.addRequiredMavenGoals(parameters);
            }

            @Override
            void autoAddCamelDebugger(Project project, JavaParameters parameters, String version) throws Exception {
                autoAddCamelDebuggerToCustomPom(this, project, parameters, version);
            }

            @Override
            boolean canCheckCamelBreakpoints() {
                return false;
            }
        },
        /**
         * Execution mode corresponding to {@link com.github.cameltooling.idea.runner.CamelSpringBootRunConfigurationType}.
         */
        CAMEL_SPRING_BOOT(CamelRuntime.SPRING_BOOT) {
            @Override
            void configureCamelDebugger(JavaParameters parameters) {
                addCamelDebuggerProperties(parameters.getProgramParametersList());
            }

            @Override
            void addRequiredParameters(JavaParameters parameters) {
                super.addRequiredMavenGoals(parameters);
                // Added as required parameters for backward compatibility reasons
                parameters.getProgramParametersList().addProperty("spring-boot.run.fork", "false");
            }

            @Override
            void autoAddCamelDebugger(Project project, JavaParameters parameters, String version) throws Exception {
                autoAddCamelDebuggerToCustomPom(this, project, parameters, version);
            }

            @Override
            boolean canCheckCamelBreakpoints() {
                return false;
            }
        },
        /**
         * Execution mode corresponding to {@link com.github.cameltooling.idea.runner.CamelQuarkusRunConfigurationType}.
         */
        CAMEL_QUARKUS(CamelRuntime.QUARKUS) {
            @Override
            void configureCamelDebugger(JavaParameters parameters) {
                addCamelDebuggerProperties(parameters.getProgramParametersList());
            }

            @Override
            void addRequiredParameters(JavaParameters parameters) {
                // Avoid to compile as it can prevent the automatic addition of the Camel Debugger from working properly
                // Indeed otherwise, the addition of the Camel Debugger to a custom pom file is simply ignored
                parameters.getProgramParametersList().addAt(0, "clean");
                parameters.getProgramParametersList().addAt(1, runtime.getPluginGoal());
            }

            @Override
            void autoAddCamelDebugger(Project project, JavaParameters parameters, String version) throws Exception {
                autoAddCamelDebuggerToCustomPom(this, project, parameters, version);
            }

            @Override
            boolean canCheckCamelBreakpoints() {
                return false;
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
            void configureCamelDebugger(JavaParameters parameters) {
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
        },
        /**
         * Execution mode corresponding to any Java applications launched from the IDE by the Gradle plugin.
         */
        JAVA_QUARKUS(null) {
            @Override
            Dependency createCamelDebugDependency(Project project, String version) {
                throw new UnsupportedOperationException();
            }

            @Override
            Dependency createCamelManagementDependency(Project project, String version) {
                throw new UnsupportedOperationException();
            }

            @Override
            void configureCamelDebugger(JavaParameters parameters) {
                addCamelDebuggerEnvironmentVariable(parameters);
            }

            @Override
            void addRequiredParameters(JavaParameters parameters) {
                // Nothing to add by default
            }

            @Override
            void autoAddCamelDebugger(Project project, JavaParameters parameters, String version) throws Exception {
                throw new UnsupportedOperationException();
            }

            @Override
            boolean canAutoAddCamelDebugger() {
                return false;
            }
        },
        /**
         * Execution mode corresponding to any Java applications launched from the IDE by the Gradle plugin.
         */
        GRADLE_JAVA_MAIN(null) {
            @Override
            Dependency createCamelDebugDependency(Project project, String version) {
                throw new UnsupportedOperationException();
            }

            @Override
            Dependency createCamelManagementDependency(Project project, String version) {
                throw new UnsupportedOperationException();
            }

            @Override
            void configureCamelDebugger(JavaParameters parameters) {
                addCamelDebuggerProperties(parameters.getVMParametersList());
            }

            @Override
            void addRequiredParameters(JavaParameters parameters) {
                // Nothing to add by default
            }

            @Override
            void autoAddCamelDebugger(Project project, JavaParameters parameters, String version) throws Exception {
                throw new UnsupportedOperationException();
            }

            @Override
            boolean canAutoAddCamelDebugger() {
                return false;
            }
        },
        /**
         * Execution mode corresponding to any Gradle task launched from the IDE by the Gradle plugin.
         */
        GRADLE_GENERIC(null) {

            @Override
            void configureCamelDebugger(JavaParameters parameters) {
                addCamelDebuggerEnvironmentVariable(parameters);
            }

            @Override
            void addRequiredParameters(JavaParameters parameters) {
                // Nothing to add by default
            }

            @Override
            void autoAddCamelDebugger(Project project, JavaParameters parameters, String version) throws Exception {
                autoAddCamelDebuggerToCustomGradleBuild(this, project, parameters, version);
            }
        };

        /**
         * The corresponding Camel Runtime.
         */
        protected final CamelRuntime runtime;

        /**
         * Constructs a {@code ExecutionMode} with the given Camel Runtime.
         *
         * @param runtime the corresponding Camel Runtime.
         */
        ExecutionMode(CamelRuntime runtime) {
            this.runtime = runtime;
        }

        /**
         * Creates a {@code Dependency} instance corresponding to the artifact provided by {@code artifactProvider}
         * that matches the best with the execution mode. If the version of the underlying runtime cannot be
         * found, by default the {@code Dependency} instance is created from the artifact of the default Camel runtime
         * with the provided version.
         *
         * @param project            the project for which the {@code Dependency} is created.
         * @param version            the default version of the artifact to use.
         * @param artifactProvider the provider of artifact according to the Camel runtime used.
         * @return a new instance of {@code Dependency} matching with the context or {@code null} if no
         * corresponding artifact id could be found.
         */
        private Dependency createDependency(Project project, String version, Function<CamelRuntime, ArtifactCoordinates> artifactProvider) {
            CamelRuntime currentRuntime = runtime;
            if (currentRuntime == null) {
                currentRuntime = CamelRuntime.getCamelRuntime(project);
            }
            String versionRuntime = currentRuntime.getVersion(project);
            Dependency dependency = new Dependency();
            final CamelRuntime actualRuntime;
            if (versionRuntime == null) {
                actualRuntime = CamelRuntime.DEFAULT;
                dependency.setVersion(version);
            } else {
                actualRuntime = currentRuntime;
                dependency.setVersion(versionRuntime);
            }
            final ArtifactCoordinates artifact = artifactProvider.apply(actualRuntime);
            if (artifact == null) {
                return null;
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Using the artifact %s component as dependency", artifact));
            }
            dependency.setGroupId(artifact.getGroupId());
            dependency.setArtifactId(artifact.getArtifactId());
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
            return createDependency(project, version, CamelRuntime::getDebugArtifact);
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
            return createDependency(project, version, CamelRuntime::getManagementArtifact);
        }

        /**
         * Creates a {@code Dependency} instance corresponding to the additional debug dependency if
         * it exists.
         *
         * @param project the project for which the {@code Dependency} is created.
         * @param version the default version of the artifact to use.
         * @return a new instance of {@code Dependency} matching with the context and corresponding to the additional
         * debug dependency if it exists, {@code null} otherwise.
         */
        Dependency createAdditionalDependency(Project project, String version) {
            return createDependency(project, version, CamelRuntime::getAdditionalArtifact);
        }

        /**
         * Adds the parameters that are required to ensure that the program is launched properly.
         *
         * @param parameters the parameters to patch to add the required parameters.
         */
        abstract void addRequiredParameters(JavaParameters parameters);

        /**
         * Configures the Camel Debugger properly.
         *
         * @param parameters the parameters to patch to configure the Camel Debugger.
         */
        abstract void configureCamelDebugger(JavaParameters parameters);

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
         * Indicates whether the camel debugger can automatically be added.
         *
         * @return {@code true} if it can be added, {@code false} otherwise.
         */
        boolean canAutoAddCamelDebugger() {
            return true;
        }

        /**
         * Indicates whether the existence of camel breakpoints is needed.
         *
         * @return {@code true} if it is needed, {@code false} otherwise.
         */
        boolean canCheckCamelBreakpoints() {
            return true;
        }

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
         * Adds the required maven goals to the given parameters.
         *
         * @param parameters the parameters to patch.
         */
        private void addRequiredMavenGoals(JavaParameters parameters) {
            parameters.getProgramParametersList().addAt(0, "clean");
            parameters.getProgramParametersList().addAt(1, "compile");
            parameters.getProgramParametersList().addAt(2, runtime.getPluginGoal());
        }
    }

    /**
     * The supported writer of gradle build file.
     */
    private enum GradleBuildWriter {

        /**
         * The writer dedicated to gradle build file written in Groovy.
         */
        GROOVY {
            @Override
            void write(Writer writer, Dependency dependency) throws IOException {
                writer.write(
                    String.format(
                        """
                        project.dependencies.add('runtimeOnly', '%s:%s:%s')
                        """,
                        dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion()
                    )
                );
            }

            @Override
            void write(Writer writer, List<Dependency> dependencies, String projectName) throws IOException {
                writer.write(
                    String.format(
                        """
                        project('%s') {
                            plugins.withType(JavaPlugin.class) {
                                dependencies {
                        """,
                        projectName
                    )
                );
                for (Dependency dependency : dependencies) {
                    writer.write(
                        String.format(
                            """
                                        runtimeOnly '%s:%s:%s'
                            """,
                            dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion()
                        )
                    );
                }
                writer.write(
                    """
                            }
                        }
                    }
                    """
                );
            }

            @Override
            void write(Writer writer, Path buildFile) throws IOException {
                writer.write(
                    String.format(
                        """
                        rootProject.buildFileName = '%s'
                        """,
                        buildFile.getFileName()
                    )
                );
            }
        },
        /**
         * The writer dedicated to gradle build file written in Kotlin.
         */
        KOTLIN {
            @Override
            void write(Writer writer, Dependency dependency) throws IOException {
                writer.write(
                    String.format(
                        """
                        project.dependencies.add("runtimeOnly", "%s:%s:%s")
                        """,
                        dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion()
                    )
                );
            }

            @Override
            void write(Writer writer, List<Dependency> dependencies, String projectName) throws IOException {
                writer.write(
                    String.format(
                        """
                        project("%s") {
                            plugins.withType<JavaPlugin>() {
                                dependencies {
                        """,
                        projectName
                    )
                );
                for (Dependency dependency : dependencies) {
                    writer.write(
                        String.format(
                            """
                                        add("runtimeOnly", "%s:%s:%s")
                            """,
                            dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion()
                        )
                    );
                }
                writer.write(
                    """
                            }
                        }
                    }
                    """
                );
            }

            @Override
            void write(Writer writer, Path buildFile) throws IOException {
                writer.write(
                    String.format(
                        """
                        rootProject.buildFileName = "%s"
                        """,
                        buildFile.getFileName()
                    )
                );
            }
        };

        /**
         * Writes the given dependency to the given writer.
         *
         * @param writer the target writer
         * @param dependency the dependency to write
         * @throws IOException if the dependency could not be written
         */
        abstract void write(Writer writer, Dependency dependency) throws IOException;

        /**
         * Writes the given dependencies of a specific project to the given writer.
         *
         * @param writer the target writer
         * @param dependencies the dependencies to write
         * @param projectName the target project name
         * @throws IOException if the dependencies could not be written
         */
        abstract void write(Writer writer, List<Dependency> dependencies, String projectName) throws IOException;

        /**
         * Writes the location of the new build file.
         *
         * @param writer the target writer
         * @param buildFile the new build file to configure.
         * @throws IOException if the location of the build file could not be written
         */
        abstract void write(Writer writer, Path buildFile) throws IOException;
    }
}
