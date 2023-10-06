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
package com.github.cameltooling.idea.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.cameltooling.idea.util.ArtifactCoordinates;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.process.KillableColoredProcessHandler;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessTerminatedListener;
import com.intellij.execution.target.TargetedCommandLineBuilder;
import com.intellij.execution.target.local.LocalTargetEnvironment;
import com.intellij.execution.target.local.LocalTargetEnvironmentRequest;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.jetbrains.annotations.NotNull;

import static com.github.cameltooling.idea.service.CamelService.CAMEL_NOTIFICATION_GROUP;

/**
 * {@code CamelJBangService} is a service allowing to detect, download and add the dependencies of a project.
 */
public class CamelJBangService implements Disposable {
    private static final Pattern DEPENDENCY_FORMAT = Pattern.compile("^(.*):(.*):(.*)$");

    private static final Logger LOG = Logger.getInstance(CamelJBangService.class);

    private final Project project;
    private volatile Notification notification;

    public CamelJBangService(@NotNull Project project) {
        this.project = project;
    }

    /**
     * @return true if the current project is a Camel JBang project, {@code false} otherwise.
     */
    public boolean isCamelJBangProject() {
        CamelProjectPreferenceService preferenceService = CamelProjectPreferenceService.getService(project);
        final Boolean isCamelProject = preferenceService.isCamelProject();
        final Boolean isJBangProject = preferenceService.isJBangProject();
        if (isJBangProject == null && (isCamelProject == null || isCamelProject)) {
            String basePath = project.getBasePath();
            return basePath != null && new File(basePath, ".camel-jbang").exists();
        }
        return isCamelProject && isJBangProject;
    }

    /**
     * @return the version of Camel JBang that is configured or auto-detected.
     */
    public String getCamelJBangVersion() {
        CamelProjectPreferenceService preferenceService = CamelProjectPreferenceService.getService(project);
        String version = preferenceService.getCamelVersion();
        if (version == null) {
            version = loadCamelJBangVersion();
        }
        return version;
    }

    /**
     * Load the Camel JBang version for the properties file.
     * @return the Camel JBang version that could be loaded from the properties file, {@code null} otherwise.
     */
    private String loadCamelJBangVersion() {
        String basePath = project.getBasePath();
        if (basePath != null) {
            File propertiesFile = new File(new File(basePath, ".camel-jbang"), "camel-jbang-run.properties");
            if (propertiesFile.exists()) {
                Properties properties = new Properties();
                try (InputStream input = new FileInputStream(propertiesFile)) {
                    properties.load(input);
                } catch (Exception e) {
                    LOG.warn("Could load the Camel JBang properties", e);
                }
                return properties.getProperty("camel.jbang.camel-version");
            }
        }
        return null;
    }

    /**
     * Automatically download and add all the Camel JBang dependencies to the project.
     */
    public void addDependencies() {
        try {
            ModuleManager manager = ModuleManager.getInstance(project);
            // 1. Identify the module to which the dependencies must be added
            Module[] modules = manager.getModules();
            if (modules.length == 0) {
                modules = new Module[]{createNewModule(manager)};
            }
            // 2. Execute the Camel JBang command to get the list of dependencies
            Process process = executeCommand();
            // 3. Extract the dependencies for the output
            List<ArtifactCoordinates> dependencies = extractDependencies(process);
            if (dependencies.isEmpty()) {
                notifyError("No dependency could be found");
                return;
            }
            // 4. Download the dependencies
            Map<ArtifactCoordinates, URL> libraries = downloadArtifacts(dependencies);
            if (libraries.isEmpty()) {
                notifyError("No library could be found");
                return;
            }
            // 5. Add the libraries to the first module available
            addLibraries(modules[0], libraries);
            // 6. Add a source folder to the module if it does not exist
            addSourceFolderIfMissing(modules[0]);
            // 7. Notify that the libraries could be added
            notifySuccess();
        } catch (Exception e) {
            notifyError(e.getMessage());
        }
    }

    /**
     * Create a new module with the name of the project.
     *
     * @param manager the module manager to use to create the module.
     * @return the newly created module.
     */
    private Module createNewModule(ModuleManager manager) {
        String basePath = project.getBasePath();
        if (basePath == null) {
            throw new IllegalStateException("A new module cannot be created without a base path");
        }
        Computable<Module> moduleSupplier = () -> manager.newModule(Path.of(basePath), project.getName());
        return ApplicationManager.getApplication().runWriteAction(moduleSupplier);
    }

    /**
     * Notify that the libraries could be added with success.
     */
    private void notifySuccess() {
        notification = CAMEL_NOTIFICATION_GROUP.createNotification("Dependencies added with success", NotificationType.INFORMATION)
            .setImportant(false).setIcon(CamelPreferenceService.getService().getCamelIcon());
        notification.notify(project);
    }

    /**
     * Notify that an error occurred.
     *
     * @param message the error message to provide in the notification.
     */
    private void notifyError(String message) {
        String notificationMessage;
        if (message == null) {
            notificationMessage = "Could not add the dependencies to the project for an unknown reason";
        } else {
            notificationMessage = String.format("Could not add the dependencies: %s", message);
        }
        notification = CAMEL_NOTIFICATION_GROUP.createNotification(notificationMessage, NotificationType.ERROR)
            .setImportant(true).setIcon(CamelPreferenceService.getService().getCamelIcon());
        notification.notify(project);
    }

    /**
     * Add a source folder to the given module if it does not exist.
     *
     * @param module the module to which the source folder must be added.
     */
    private void addSourceFolderIfMissing(Module module) {
        ModuleRootModificationUtil.updateModel(module, model -> {
            // Check if the module already has a source folder
            for (ContentEntry e : model.getContentEntries()) {
                if (e.getSourceFolders().length > 0) {
                    return;
                }
            }
            // If not, add the base path of the project as a source folder
            String basePath = project.getBasePath();
            if (basePath != null) {
                VirtualFile sourceFolderVirtualFile = LocalFileSystem.getInstance()
                    .refreshAndFindFileByIoFile(new File(basePath));
                if (sourceFolderVirtualFile != null) {
                    model.addContentEntry(sourceFolderVirtualFile)
                        .addSourceFolder(sourceFolderVirtualFile, false);
                }
            }
        });
    }

    /**
     * Add all the libraries located at the given URLs to the given module.
     *
     * @param module    the module to which the dependencies must be added.
     * @param libraries the coordinated and the URL corresponding to the local path of all the dependencies to add.
     */
    private void addLibraries(@NotNull final Module module, @NotNull Map<ArtifactCoordinates, URL> libraries) {
        ModuleRootModificationUtil.updateModel(module, model -> {
            for (Map.Entry<ArtifactCoordinates, URL> library : libraries.entrySet()) {
                try {
                    addLibrary(model, library.getKey(), new File(library.getValue().toURI()));
                } catch (Exception e) {
                    LOG.debug("Could not add the dependency " + library.getKey());
                }
            }
        });
    }

    /**
     * Add the given jar file to the dependencies of the given module.
     *
     * @param model   the model to modify.
     * @param artifact the artifact corresponding to the jar file to add.
     * @param jarFile  the jar file corresponding to the dependency to add.
     */
    private void addLibrary(@NotNull ModifiableRootModel model, ArtifactCoordinates artifact, File jarFile) {
        final String clzUrlString = VirtualFileManager.constructUrl(JarFileSystem.PROTOCOL, jarFile.getAbsolutePath())
            + JarFileSystem.JAR_SEPARATOR;
        final VirtualFile jarVirtualFile = VirtualFileManager.getInstance().findFileByUrl(clzUrlString);
        String libName = artifact.toString();
        LibraryTable table = model.getModuleLibraryTable();
        Library library = table.getLibraryByName(libName);
        if (library == null && jarVirtualFile != null) {
            library = table.createLibrary(libName);
            Library.ModifiableModel libraryModel = library.getModifiableModel();
            libraryModel.addRoot(jarVirtualFile, OrderRootType.CLASSES);
            libraryModel.commit();
        }
    }

    /**
     * Download the given artifacts using the Maven API.
     *
     * @param artifacts the artifacts to download.
     * @return the coordinates and the URL corresponding to the local path of all the artifacts that could be downloaded.
     * @throws IOException if an error occurs while downloading the artifacts.
     */
    private Map<ArtifactCoordinates, URL> downloadArtifacts(@NotNull List<ArtifactCoordinates> artifacts) throws IOException {
        try (MavenArtifactRetrieverContext context = new MavenArtifactRetrieverContext()) {
            for (ArtifactCoordinates artifact : artifacts) {
                context.add(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
            }
            return context.getArtifacts();
        }
    }

    /**
     * Extracts the dependencies that could be parsed from the output of the given process.
     *
     * @param process the process used to extract the dependencies
     * @return the list of artifacts that could be parsed from the output of the process if the exit value is {@code 0}.
     * an empty list otherwise.
     * @throws IOException if the output of the process could not be read.
     */
    private List<ArtifactCoordinates> extractDependencies(@NotNull Process process) throws IOException {
        int exitCode;
        try {
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return List.of();
        }
        if (exitCode == 0) {
            try (BufferedReader reader = process.inputReader()) {
                List<ArtifactCoordinates> result = new ArrayList<>();
                reader.lines().forEach(
                    line -> {
                        Matcher matcher = DEPENDENCY_FORMAT.matcher(line);
                        if (matcher.find()) {
                            result.add(ArtifactCoordinates.of(matcher.group(1), matcher.group(2), matcher.group(3)));
                        }
                    }
                );
                return result;
            }
        } else {
            try (BufferedReader reader = process.errorReader()) {
                reader.lines().forEach(LOG::warn);
            }
        }
        return List.of();
    }

    /**
     * Executes the command {@code camel dependency list} to retrieve all the main dependencies of the underlying project.
     *
     * @return the process corresponding to the command
     * @throws IOException if the command could not be executed
     */
    private Process executeCommand() throws IOException {
        var targetEnvRequest = new LocalTargetEnvironmentRequest();
        var targetEnvironment = new LocalTargetEnvironment(new LocalTargetEnvironmentRequest());

        var builder = new TargetedCommandLineBuilder(targetEnvRequest);
        builder.setExePath("jbang");
        String version = getCamelJBangVersion();
        if (version != null) {
            builder.addParameter(String.format("-Dcamel.jbang.version=%s", version));
        }
        builder.addParameter("camel@apache/camel");
        builder.addParameter("dependency");
        builder.addParameter("list");
        String basePath = project.getBasePath();
        if (basePath != null) {
            builder.setWorkingDirectory(basePath);
        }

        var targetedCommandLine = builder.build();

        try {
            Process process = targetEnvironment.createProcess(targetedCommandLine, new EmptyProgressIndicator());

            OSProcessHandler handler = new KillableColoredProcessHandler.Silent(process,
                targetedCommandLine.getCommandPresentation(targetEnvironment),
                targetedCommandLine.getCharset(),
                builder.getFilesToDeleteOnTermination());
            ProcessTerminatedListener.attach(handler);
            return process;
        } catch (ExecutionException e) {
            throw e.toIOException();
        }
    }

    @Override
    public void dispose() {
        if (notification != null) {
            notification.expire();
            notification = null;
        }
    }
}
