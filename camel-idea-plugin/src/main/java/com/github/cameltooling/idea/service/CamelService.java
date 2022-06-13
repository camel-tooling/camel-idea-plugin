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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.github.cameltooling.idea.catalog.CamelCatalogProvider;
import com.github.cameltooling.idea.util.ArtifactCoordinates;
import com.github.cameltooling.idea.util.IdeaUtils;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.Topic;
import org.apache.camel.catalog.CamelCatalog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.github.cameltooling.idea.service.XmlUtils.getChildNodeByTagName;
import static com.github.cameltooling.idea.service.XmlUtils.loadDocument;
import static org.apache.camel.catalog.impl.CatalogHelper.loadText;

/**
 * Service access for Camel libraries
 */
public class CamelService implements Disposable {

    private static final NotificationGroup CAMEL_NOTIFICATION_GROUP = NotificationGroupManager.getInstance().getNotificationGroup("Apache Camel");

    private static final Logger LOG = Logger.getInstance(CamelService.class);

    private static final int MIN_MAJOR_VERSION = 2;
    private static final int MIN_MINOR_VERSION = 16;

    private final AtomicBoolean downloadInProgress = new AtomicBoolean();
    private Library camel2CoreLibrary;
    private List<Library> camel3CoreLibraries = new ArrayList<>();
    private Library slf4jApiLibrary;
    private URLClassLoader camelCoreClassloader;
    private final Set<String> processedLibraries = new HashSet<>();
    private final Map<Library, ArtifactCoordinates> projectLibraries = new HashMap<>();
    /**
     * The class loader of the Project based only on the libraries defined as dependencies of the project's modules.
     */
    private URLClassLoader projectClassloader;
    /**
     * The class loader of the Project based on the sources of the project but also the libraries defined as
     * dependencies of the project's modules.
     */
    private URLClassLoader projectCompleteClassloader;
    private volatile boolean camelPresent;
    private volatile Notification camelVersionNotification;
    private volatile Notification camelMissingJSonSchemaNotification;
    private volatile Notification camelMissingJSonPathJarNotification;

    /**
     * The project in which the service is registered.
     */
    private final Project project;

    /**
     * Construct a {@code CamelService} with the given project.
     * @param project the project in which the service is registered.
     */
    public CamelService(Project project) {
        this.project = project;
    }

    public IdeaUtils getIdeaUtils() {
        return ApplicationManager.getApplication().getService(IdeaUtils.class);
    }

    @Override
    public synchronized void dispose() {
        processedLibraries.clear();
        projectLibraries.clear();

        if (camelVersionNotification != null) {
            camelVersionNotification.expire();
            camelVersionNotification = null;
        }
        if (camelMissingJSonSchemaNotification != null) {
            camelMissingJSonSchemaNotification.expire();
            camelMissingJSonSchemaNotification = null;
        }
        if (camelMissingJSonPathJarNotification != null) {
            camelMissingJSonPathJarNotification.expire();
            camelMissingJSonPathJarNotification = null;
        }
        if (camelCoreClassloader != null) {
            try {
                camelCoreClassloader.close();
            } catch (IOException e) {
                LOG.warn("Could not close the Camel Core ClassLoader: " + e.getMessage());
                LOG.debug(e);
            } finally {
                camelCoreClassloader = null;
            }
        }
        camel2CoreLibrary = null;
        camel3CoreLibraries = null;
        slf4jApiLibrary = null;
        // Close the child Class Loader first
        if (projectCompleteClassloader != null) {
            try {
                projectCompleteClassloader.close();
            } catch (IOException e) {
                LOG.warn("Could not close the Project Complete ClassLoader: " + e.getMessage());
                LOG.debug(e);
            } finally {
                projectCompleteClassloader = null;
            }
        }
        // Then close the parent Class Loader
        if (projectClassloader != null) {
            try {
                projectClassloader.close();
            } catch (IOException e) {
                LOG.warn("Could not close the Project ClassLoader: " + e.getMessage());
                LOG.debug(e);
            } finally {
                projectClassloader = null;
            }
        }
    }

    /**
     * @return true if Camel is present on the classpath
     */
    public boolean isCamelPresent() {
        return camelPresent;
    }

    /**
     * @param camelPresent - true if camel is present
     */
    public void setCamelPresent(boolean camelPresent) {
        this.camelPresent = camelPresent;
    }

    /**
     * @param lib - Add the of the library
     */
    public synchronized void addLibrary(String lib) {
        processedLibraries.add(lib);
    }

    /**
     * @return all cached library names
     */
    public synchronized Set<String> getLibraries() {
        return new HashSet<>(processedLibraries);
    }

    /**
     * Reset the state of the service.
     */
    public synchronized void reset() {
        processedLibraries.clear();
        projectLibraries.clear();
        // Close the child Class Loader first
        if (projectCompleteClassloader != null) {
            try {
                projectCompleteClassloader.close();
            } catch (IOException e) {
                LOG.warn("Could not close the Project Complete ClassLoader: " + e.getMessage());
                LOG.debug(e);
            } finally {
                projectCompleteClassloader = null;
            }
        }
        // Then close the parent Class Loader
        if (projectClassloader != null) {
            try {
                projectClassloader.close();
            } catch (IOException e) {
                LOG.warn("Could not close the Project ClassLoader: " + e.getMessage());
                LOG.debug(e);
            } finally {
                projectClassloader = null;
            }
        }
        if (camelCoreClassloader != null) {
            try {
                camelCoreClassloader.close();
            } catch (IOException e) {
                LOG.warn("Could not close the Camel Core ClassLoader: " + e.getMessage());
                LOG.debug(e);
            } finally {
                camelCoreClassloader = null;
            }
        }
        camel2CoreLibrary = null;
        slf4jApiLibrary = null;
        camel3CoreLibraries = new ArrayList<>();
        camelPresent = false;
    }

    /**
     * @return true if the library name is cached
     */
    public synchronized boolean containsLibrary(String lib, boolean quickCheck) {
        boolean answer = processedLibraries.contains(lib);
        if (!answer && !quickCheck) {
            for (ArtifactCoordinates coordinates : projectLibraries.values()) {
                if (coordinates.getArtifactId().equals(lib)) {
                    answer = true;
                    break;
                }
            }
        }
        return answer;
    }

    /**
     * Gives the artifact defined as the project library corresponding to the given group id and artifact id.
     * @param groupId the group id of the artifact to find.
     * @param artifactId the artifact id of artifact to find.
     * @return the {@code ArtifactCoordinates} corresponding to the artifact if it could be found, {@code null} otherwise.
     */
    @Nullable
    public synchronized ArtifactCoordinates getProjectLibraryCoordinates(String groupId, String artifactId) {
        for (ArtifactCoordinates coordinates : projectLibraries.values()) {
            if (coordinates.getGroupId().equals(groupId) && coordinates.getArtifactId().equals(artifactId)) {
                return coordinates;
            }
        }
        return null;
    }

    /**
     * Gives an artifact defined as the project library known as a core library of Camel.
     * @return the {@code ArtifactCoordinates} of any matching artifact if at least one could be found, {@code null}
     * otherwise.
     */
    @Nullable
    private synchronized ArtifactCoordinates getProjectCamelCoreCoordinates() {
        for (ArtifactCoordinates coordinates : projectLibraries.values()) {
            if (isCamel2CoreMavenDependency(coordinates) || isCamel3CoreMavenDependency(coordinates)) {
                return coordinates;
            }
        }
        return null;
    }

    /**
     * Gets the classloader that can load classes from camel-core which is present on the project classpath
     */
    public synchronized ClassLoader getCamelCoreClassloader() {
        if (camelCoreClassloader == null) {
            try {
                if (camel2CoreLibrary == null) {
                    // Camel 3 is assumed
                    final List<Library> list = new ArrayList<>(camel3CoreLibraries);
                    list.add(slf4jApiLibrary);
                    camelCoreClassloader = getIdeaUtils().newURLClassLoaderForLibrary(list.toArray(new Library[0]));
                } else {
                    // Camel 2 has been detected
                    camelCoreClassloader = getIdeaUtils().newURLClassLoaderForLibrary(camel2CoreLibrary, slf4jApiLibrary);
                }
            } catch (Exception e) {
                LOG.warn("Error creating URLClassLoader for loading classes from camel-core", e);
            }
        }
        return camelCoreClassloader;
    }

    /**
     * Gets the class loader of the Project based only on the libraries defined as dependencies of the project's modules.
     */
    public synchronized ClassLoader getProjectClassloader() {
        if (projectClassloader == null) {
            try {
                final Library[] libs = projectLibraries.keySet().toArray(new Library[0]);
                projectClassloader = getIdeaUtils().newURLClassLoaderForLibrary(libs);
            } catch (Exception e) {
                LOG.warn("Error creating URLClassLoader for loading classes from the project", e);
            }
        }
        return projectClassloader;
    }

    /**
     * Gets the class loader of the Project based on the sources of the project but also the libraries defined as
     * dependencies of the project's modules.
     */
    synchronized ClassLoader getProjectCompleteClassloader() {
        if (projectCompleteClassloader == null) {
            try {
                final List<URL> sourceURLs = new ArrayList<>();
                for (Module module : ModuleManager.getInstance(project).getModules()) {
                    for (String sourceURL : ModuleRootManager.getInstance(module).getSourceRootUrls(false)) {
                        if (!sourceURL.startsWith("file:")) {
                            continue;
                        }
                        if (!sourceURL.endsWith("/")) {
                            sourceURL += "/";
                        }
                        sourceURLs.add(new URL(sourceURL));
                    }
                }
                projectCompleteClassloader = new URLClassLoader(sourceURLs.toArray(new URL[0]), getProjectClassloader());
            } catch (Exception e) {
                LOG.warn("Error creating URLClassLoader for loading classes and resources from the project", e);
            }
        }
        return projectCompleteClassloader;
    }

    public void showMissingJSonPathJarNotification() {
        if (camelMissingJSonPathJarNotification == null) {
            Icon icon = getCamelPreferenceService().getCamelIcon();
            camelMissingJSonPathJarNotification = CAMEL_NOTIFICATION_GROUP.createNotification("camel-jsonpath is not on classpath. Cannot perform real time JSonPath validation.",
                    NotificationType.WARNING).setImportant(true).setIcon(icon);
            camelMissingJSonPathJarNotification.notify(project);
        }
    }

    /**
     * Scan the given project to know whether it is a Camel project or not and if so, set up the {@link CamelCatalog} to
     * use same version of Camel as the project does.
     * These two version needs to be aligned to offer the best tooling support on the given project.
     */
    public void scanForCamelProject() {
        final List<String> missingJSonSchemas = new ArrayList<>();
        synchronized (this) {
            reset();
            for (Module module : ModuleManager.getInstance(project).getModules()) {
                scanForCamelProject(module);
                // if it is a Camel project then scan for additional Camel components
                if (isCamelPresent()) {
                    missingJSonSchemas.addAll(scanForCamelDependencies(module));
                }
            }
        }
        loadCamelCatalog();
        notifyForMissingJsonSchemas(missingJSonSchemas);
    }

    /**
     * Scan the given module to know whether it contains Camel artifacts indicating that it is a Camel project.
     */
    private void scanForCamelProject(@NotNull Module module) {
        for (OrderEntry entry : ModuleRootManager.getInstance(module).getOrderEntries()) {
            if (!(entry instanceof LibraryOrderEntry)) {
                continue;
            }
            LibraryOrderEntry libraryOrderEntry = (LibraryOrderEntry) entry;

            if (!libraryOrderEntry.getScope().isForProductionCompile() && !libraryOrderEntry.getScope().isForProductionRuntime()) {
                continue;
            }
            final Library library = libraryOrderEntry.getLibrary();
            if (library == null) {
                continue;
            }
            final ArtifactCoordinates coordinates = ArtifactCoordinates.parse(libraryOrderEntry);

            projectLibraries.putIfAbsent(library, coordinates);

            if (isSlf4jMavenDependency(coordinates)) {
                slf4jApiLibrary = library;
            } else if (isCamel2CoreMavenDependency(coordinates)) {
                // okay it is a camel v2 project
                camel2CoreLibrary = library;
                setCamelPresent(true);
            } else if (isCamel3CoreMavenDependency(coordinates)) {
                camel3CoreLibraries.add(library);
                // okay it is a camel v3 project
                setCamelPresent(true);
            }
        }
    }

    /**
     * Load the Camel catalog if Camel is present in the project, the version of Camel in the project is not the same
     * as the one in the plugin and the preference indicating to download the catalog is enabled.
     */
    void loadCamelCatalog() {
        if (!isCamelPresent()) {
            return;
        }
        final ArtifactCoordinates artifactCoordinates = getProjectCamelCoreCoordinates();
        final String version = artifactCoordinates == null ? null : artifactCoordinates.getVersion();
        final CamelCatalogService camelCatalogService = getCamelCatalogService();
        String currentVersion = camelCatalogService.get().getLoadedVersion();
        if (currentVersion == null) {
            // okay no special version was loaded so its the catalog version we are using
            currentVersion = camelCatalogService.get().getCatalogVersion();
        }
        if (isThereDifferentVersionToBeLoaded(version, currentVersion) && getCamelPreferenceService().isDownloadCatalog()) {
            if (downloadInProgress.compareAndSet(false, true)) {
                // execute this work in a background thread
                loadCamelCatalogInBackground(version);
            }
        } else {
            // The catalog is ready to be used
            project.getMessageBus().syncPublisher(CamelCatalogListener.TOPIC).onCamelCatalogReady();
        }
    }

    /**
     * Loads the Camel catalog in background.
     *
     * @param version the version of the Camel catalog to load.
     */
    private void loadCamelCatalogInBackground(@NotNull String version) {
        final CamelCatalogProvider provider = getCamelPreferenceService().getCamelCatalogProvider()
            .getActualProvider(project);
        new Task.Backgroundable(project, "Download the Camel catalog for the " + provider.getName() + " Runtime", true) {
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setText("Downloading camel-catalog version: " + version);
                indicator.setIndeterminate(false);
                indicator.setFraction(0.00);

                // download catalog via maven
                boolean notifyNewCamelCatalogVersionLoaded = downloadNewCamelCatalogVersion(provider, version);
                if (notifyNewCamelCatalogVersionLoaded(notifyNewCamelCatalogVersionLoaded)) {
                    expireOldCamelCatalogVersion();
                }
                final CamelCatalogService camelCatalogService = getCamelCatalogService();
                // The catalog is ready to be used
                project.getMessageBus().syncPublisher(CamelCatalogListener.TOPIC).onCamelCatalogReady();
                // only notify this once on startup (or if a new version was successfully loaded)
                if (camelVersionNotification == null) {
                    String loadedVersion = camelCatalogService.get().getLoadedVersion();
                    if (loadedVersion == null) {
                        // okay no special version was loaded so its the catalog version we are using
                        loadedVersion = camelCatalogService.get().getCatalogVersion();
                    }
                    showCamelCatalogVersionAtPluginStart(provider, loadedVersion);
                }
                indicator.setFraction(1.0);
                downloadInProgress.set(false);
            }
        }.setCancelText("Stop Downloading the Camel catalog for the " + provider.getName() + " Runtime").queue();
    }

    private void showCamelCatalogVersionAtPluginStart(CamelCatalogProvider provider, String currentVersion) {
        camelVersionNotification = CAMEL_NOTIFICATION_GROUP.createNotification(
            "Apache Camel plugin is using the Camel catalog for the " + provider.getName() + " Runtime version "
                + currentVersion, NotificationType.INFORMATION);
        camelVersionNotification.notify(project);
    }

    /**
     * @param artifact the artifact to test
     * @return {@code true} if the given artifact is slf4j, {@code false} otherwise.
     */
    private static boolean isSlf4jMavenDependency(ArtifactCoordinates artifact) {
        return "org.slf4j".equals(artifact.getGroupId()) && "slf4j-api".equals(artifact.getArtifactId());
    }

    /**
     * @param artifact the artifact to test
     * @return {@code true} if the given artifact is the core artifact of Camel v2, {@code false} otherwise.
     */
    private static boolean isCamel2CoreMavenDependency(ArtifactCoordinates artifact) {
        if ("org.apache.camel".equals(artifact.getGroupId()) && "camel-core".equals(artifact.getArtifactId())) {
            String version = artifact.getVersion();
            return version == null || version.startsWith("2");
        }
        return false;
    }

    /**
     * @param artifact the artifact to test
     * @return {@code true} if the given artifact is one of the core artifacts of Camel v3, {@code false} otherwise.
     */
    private static boolean isCamel3CoreMavenDependency(ArtifactCoordinates artifact) {
        if (!"org.apache.camel".equals(artifact.getGroupId())) {
            return false;
        }
        switch (artifact.getArtifactId()) {
        case "camel-api":
        case "camel-base":
        case "camel-core-engine":
        case "camel-base-engine":
        case "camel-util":
        case "camel-core-languages":
        case "camel-management-api":
        case "camel-support":
        case "camel-core-model":
        case "camel-core-processor":
        case "camel-bean":
            String version = artifact.getVersion();
            return version == null || version.startsWith("3");
        default: return false;
        }
    }

    private void expireOldCamelCatalogVersion() {
        camelVersionNotification.expire();
        camelVersionNotification = null;
    }

    private boolean notifyNewCamelCatalogVersionLoaded(boolean notifyLoaded) {
        return notifyLoaded && camelVersionNotification != null;
    }

    /**
     * Attempt to load new version of camel-catalog and Runtime Provider to match the version from the project
     * use catalog service to load version (which takes care of switching catalog as well)
     */
    private boolean downloadNewCamelCatalogVersion(@NotNull CamelCatalogProvider provider, @NotNull String version) {
        // find out the third party maven repositories
        final CamelCatalogService catalogService = getCamelCatalogService();
        boolean loaded = catalogService.loadVersion(version, scanThirdPartyMavenRepositories())
            && provider.loadRuntimeProviderVersion(project);
        if (!loaded) {
            // always notify if download was not possible
            camelVersionNotification = CAMEL_NOTIFICATION_GROUP.createNotification(
                "Camel IDEA plugin cannot download the Camel catalog for the " + provider.getName() + " Runtime with version " + version
                    + ". Will fallback and use the Camel catalog version " + catalogService.get().getCatalogVersion(),
                NotificationType.WARNING);
            camelVersionNotification.notify(project);
        }
        return loaded;
    }

    private static boolean isThereDifferentVersionToBeLoaded(String version, String currentVersion) {
        return version != null && !version.equalsIgnoreCase(currentVersion) && acceptedVersion(version);
    }

    /**
     * Scan for Camel component (both from Apache Camel and 3rd party components)
     *
     * @return the list of missing JSon schemas
     */
    private List<String> scanForCamelDependencies(@NotNull Module module) {
        final boolean thirdParty = getCamelPreferenceService().isScanThirdPartyComponents();
        final CamelCatalog camelCatalog = getCamelCatalogService().get();
        final List<String> missingJSonSchemas = new ArrayList<>();
        for (OrderEntry entry : ModuleRootManager.getInstance(module).getOrderEntries()) {
            if (entry instanceof LibraryOrderEntry) {
                LibraryOrderEntry libraryOrderEntry = (LibraryOrderEntry) entry;

                if (libraryOrderEntry.getScope().isForProductionCompile() || libraryOrderEntry.getScope().isForProductionRuntime()) {
                    final Library library = libraryOrderEntry.getLibrary();
                    if (library == null) {
                        continue;
                    }
                    final ArtifactCoordinates coordinates = ArtifactCoordinates.parse(libraryOrderEntry);
                    final String artifactId = coordinates.getArtifactId();

                    // is it a known library then continue
                    if (containsLibrary(artifactId, true)) {
                        continue;
                    }

                    if ("org.apache.camel".equals(coordinates.getGroupId())) {
                        addLibrary(artifactId);
                    } else if (thirdParty) {
                        addCustomCamelComponentsFromDependency(camelCatalog, library, artifactId, missingJSonSchemas);
                    }
                }
            }
        }
        return missingJSonSchemas;
    }

    /**
     * Notify if at least one component does not include the Json schema metadata.
     *
     * @param missingJSonSchemas the list of components that don't include their corresponding Json schema metadata.
     */
    private void notifyForMissingJsonSchemas(List<String> missingJSonSchemas) {
        if (!missingJSonSchemas.isEmpty()) {
            String components = String.join(",", missingJSonSchemas);
            String message = "The following Camel components with artifactId [" + components
                    + "] does not include component JSon schema metadata which is required for the Camel IDEA plugin to support these components.";

            Icon icon = getCamelPreferenceService().getCamelIcon();
            camelMissingJSonSchemaNotification = CAMEL_NOTIFICATION_GROUP.createNotification(message, NotificationType.WARNING).setImportant(true).setIcon(icon);
            camelMissingJSonSchemaNotification.notify(project);
        }
    }

    /**
     * Scans for third party maven repositories in the root pom.xml file of the project.
     *
     * @return a map with repo id and url for each found repository. The map may be empty if no third party repository
     * is defined in the pom.xml file
     */
    private @NotNull Map<String, String> scanThirdPartyMavenRepositories() {
        Map<String, String> answer = new LinkedHashMap<>();

        VirtualFile vf = ProjectUtil.guessProjectDir(project);
        if (vf != null) {
            vf = vf.findFileByRelativePath("pom.xml");
        }
        if (vf != null) {
            try (InputStream is = vf.getInputStream()) {
                Document dom = loadDocument(is, false);
                NodeList list = dom.getElementsByTagName("repositories");
                if (list != null && list.getLength() == 1) {
                    Node repos = list.item(0);
                    if (repos instanceof Element) {
                        Element element = (Element) repos;
                        list = element.getElementsByTagName("repository");
                        for (int i = 0; i < list.getLength(); i++) {
                            Node node = list.item(i);
                            // grab id and url
                            Node id = getChildNodeByTagName(node, "id");
                            Node url = getChildNodeByTagName(node, "url");
                            if (id != null && url != null) {
                                LOG.info("Found third party Maven repository id: " + id.getTextContent() + " url:" + url.getTextContent());
                                answer.put(id.getTextContent(), url.getTextContent());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOG.warn("Error parsing Maven pon.xml file", e);
            }
        }

        return answer;
    }

    /**
     * Adds any discovered third party Camel components from the dependency.
     *
     * @param camelCatalog the Camel catalog to add the found custom components
     * @param library      the dependency
     * @param artifactId   the artifact id of the dependency
     */
    private void addCustomCamelComponentsFromDependency(CamelCatalog camelCatalog, Library library, String artifactId,
                                                        List<String> missingJSonSchemas) {
        boolean added = false;

        try (URLClassLoader classLoader = getIdeaUtils().newURLClassLoaderForLibrary(library)) {
            if (classLoader != null) {
                // is there any custom Camel components in this library?
                Properties properties = loadComponentProperties(classLoader);
                String components = (String) properties.get("components");
                if (components != null) {
                    String[] part = components.split("\\s");
                    for (String scheme : part) {
                        if (!camelCatalog.findComponentNames().contains(scheme)) {
                            // mark as added to avoid re-scanning the same component again
                            added = true;
                            // find the class name
                            String javaType = extractComponentJavaType(classLoader, scheme);
                            if (javaType != null) {
                                String json = loadComponentJSonSchema(classLoader, scheme);
                                if (json != null) {
                                    // okay a new Camel component was added
                                    camelCatalog.addComponent(scheme, javaType, json);
                                } else {
                                    // the component has no json schema, and hence its not supported by the plugin
                                    missingJSonSchemas.add(artifactId);
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            LOG.warn("Error scanning for custom Camel components", e);
        }

        if (added) {
            addLibrary(artifactId);
        }
    }

    /**
     * Can the version be accepted to use for switching camel-catalog version.
     * <p/>
     * Only newer versions of Camel is accepted, older versions do not have a camel-catalog or the catalog
     * has invalid data.
     *
     * @param version the version from the project
     * @return <tt>true</tt> to allow to switch version, <tt>false</tt> otherwise.
     */
    private static boolean acceptedVersion(String version) {
        version = version.toLowerCase();
        if (version.endsWith("snapshot")) {
            // accept snapshot version which can be Camel team developing on latest Camel source
            return true;
        }

        // special issue with 2.16.0 which does not work
        if ("2.16.0".equals(version)) {
            return false;
        }

        int major = -1;
        int minor = -1;

        // split into major, minor and patch
        String[] parts = version.split("\\.");
        if (parts.length >= 2) {
            major = Integer.parseInt(parts[0]);
            minor = Integer.parseInt(parts[1]);
        }

        if (major > MIN_MAJOR_VERSION) {
            return true;
        }
        if (major < MIN_MAJOR_VERSION) {
            return false;
        }

        // okay its the same major version, then the minor must be equal or higher
        return minor >= MIN_MINOR_VERSION;
    }

    private static Properties loadComponentProperties(URLClassLoader classLoader) {
        Properties answer = new Properties();
        try (InputStream is = classLoader.getResourceAsStream("META-INF/services/org/apache/camel/component.properties")) {
            // load the component files using the recommended way by a component.properties file
            if (is != null) {
                answer.load(is);
            }
        } catch (IOException e) {
            LOG.warn("Error loading META-INF/services/org/apache/camel/component.properties file", e);
        }
        return answer;
    }

    private static String loadComponentJSonSchema(URLClassLoader classLoader, String scheme) {
        String answer = null;

        String path = null;
        String javaType = extractComponentJavaType(classLoader, scheme);
        if (javaType != null) {
            int pos = javaType.lastIndexOf(".");
            path = javaType.substring(0, pos);
            path = path.replace('.', '/');
            path = path + "/" + scheme + ".json";
        }

        if (path != null) {
            try (InputStream is = classLoader.getResourceAsStream(path)) {
                if (is != null) {
                    answer = loadText(is);
                }
            } catch (IOException e) {
                LOG.warn("Error loading " + path + " file", e);
            }
        }

        return answer;
    }

    private static String extractComponentJavaType(URLClassLoader classLoader, String scheme) {
        try (InputStream is = classLoader.getResourceAsStream("META-INF/services/org/apache/camel/component/" + scheme)) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                return (String) props.get("class");
            }
        } catch (IOException e) {
            LOG.warn("Error loading META-INF/services/org/apache/camel/component/" + scheme + " file", e);
        }

        return null;
    }

    private CamelCatalogService getCamelCatalogService() {
        return project.getService(CamelCatalogService.class);
    }

    private static CamelPreferenceService getCamelPreferenceService() {
        return ApplicationManager.getApplication().getService(CamelPreferenceService.class);
    }

    /**
     * {@code CamelCatalogListener} defines a listener to notify in case the Camel catalog is ready to use.
     */
    public interface CamelCatalogListener {

        /**
         * The topic to subscribe to in order to be notified when the Camel catalog is ready to use.
         */
        @Topic.ProjectLevel
        Topic<CamelCatalogListener> TOPIC = Topic.create("CamelCatalogListener", CamelCatalogListener.class);

        /**
         * Called when the Camel catalog is ready to use.
         */
        void onCamelCatalogReady();
    }
}
