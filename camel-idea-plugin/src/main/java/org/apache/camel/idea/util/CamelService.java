/**
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
package org.apache.camel.idea.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.idea.catalog.CamelCatalogService;
import org.jetbrains.annotations.NotNull;

import static org.apache.camel.catalog.CatalogHelper.loadText;
import static org.apache.camel.idea.CamelContributor.CAMEL_NOTIFICATION_GROUP;

/**
 * Service access for Camel libraries
 */
public class CamelService implements Disposable {

    private Set<String> processedLibraries = new HashSet<>();
    private volatile boolean camelPresent;
    private Notification camelVersionNotification;

    @Override
    public void dispose() {
        processedLibraries.clear();
        if (camelVersionNotification != null) {
            camelVersionNotification.expire();
            camelVersionNotification = null;
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
    public void addLibrary(String lib) {
        processedLibraries.add(lib);
    }

    /**
     * @return all cached library names
     */
    public Set<String> getLibraries() {
        return processedLibraries;
    }

    /**
     * Clean the library cache
     */
    public void clearLibraries() {
        processedLibraries.clear();
    }

    /**
     * @return true if the library name is cached
     */
    public boolean containsLibrary(String lib) {
        return processedLibraries.contains(lib);
    }

    /**
     * Scan for Camel project present and setup {@link CamelCatalog} to use same version of Camel as the project does.
     * These two version needs to be aligned to offer the best tooling support on the given project.
     */
    public void scanForCamelProject(@NotNull Project project, @NotNull Module module) {
        for (OrderEntry entry : ModuleRootManager.getInstance(module).getOrderEntries()) {
            if (entry instanceof LibraryOrderEntry) {
                LibraryOrderEntry libraryOrderEntry = (LibraryOrderEntry) entry;

                String name = libraryOrderEntry.getPresentableName().toLowerCase();
                if (libraryOrderEntry.getScope().isForProductionCompile() || libraryOrderEntry.getScope().isForProductionRuntime()) {
                    final Library library = libraryOrderEntry.getLibrary();
                    if (library == null) {
                        continue;
                    }
                    String[] split = name.split(":");
                    String groupId = split[1].trim();
                    String artifactId = split[2].trim();
                    String version = null;
                    if (split.length > 2) {
                        version = split[3].trim();
                    }

                    if ("org.apache.camel".equals(groupId) && "camel-core".equals(artifactId)) {

                        // okay its a camel project
                        setCamelPresent(true);

                        String currentVersion = getCamelCatalogService(project).get().getLoadedVersion();
                        if (currentVersion == null) {
                            // okay no special version was loaded so its the catalog version we are using
                            currentVersion = getCamelCatalogService(project).get().getCatalogVersion();
                        }
                        if (version != null && !version.equalsIgnoreCase(currentVersion)) {
                            // there is a different version to be loaded, so expire old notification
                            if (camelVersionNotification != null) {
                                camelVersionNotification.expire();
                                camelVersionNotification = null;
                            }

                            // attempt to load new version of camel-catalog to match the version from the project
                            // use catalog service to load version (which takes care of switching catalog as well)
                            boolean loaded = getCamelCatalogService(project).loadVersion(version);
                            if (!loaded) {
                                camelVersionNotification = CAMEL_NOTIFICATION_GROUP.createNotification("Camel IDEA plugin cannot download camel-catalog with version " + version
                                    + ". Will fallback and use version " + getCamelCatalogService(project).get().getCatalogVersion(), NotificationType.WARNING);
                                camelVersionNotification.notify(project);
                            }
                        }

                        // only notify this once on startup (or if a new version was successfully loaded)
                        if (camelVersionNotification == null) {
                            currentVersion = getCamelCatalogService(project).get().getLoadedVersion();
                            if (currentVersion == null) {
                                // okay no special version was loaded so its the catalog version we are using
                                currentVersion = getCamelCatalogService(project).get().getCatalogVersion();
                            }

                            camelVersionNotification = CAMEL_NOTIFICATION_GROUP.createNotification("Camel IDEA plugin is using camel-catalog version "
                                + currentVersion, NotificationType.INFORMATION);
                            camelVersionNotification.notify(project);
                        }

                        // okay we found camel-core and have setup the project version for it
                        // then we should return early
                        return;
                    }
                }
            }
        }
    }

    /**
     * Scan for Camel component (both from Apache Camel and 3rd party components)
     */
    public void scanForCamelDependencies(@NotNull Project project, @NotNull Module module) {
        CamelCatalog camelCatalog = getCamelCatalogService(project).get();

        for (OrderEntry entry : ModuleRootManager.getInstance(module).getOrderEntries()) {
            if (entry instanceof LibraryOrderEntry) {
                LibraryOrderEntry libraryOrderEntry = (LibraryOrderEntry) entry;

                String name = libraryOrderEntry.getPresentableName().toLowerCase();
                if (libraryOrderEntry.getScope().isForProductionCompile() || libraryOrderEntry.getScope().isForProductionRuntime()) {
                    final Library library = libraryOrderEntry.getLibrary();
                    if (library == null) {
                        continue;
                    }
                    String[] split = name.split(":");
                    String groupId = split[1].trim();
                    String artifactId = split[2].trim();

                    // is it a known library then continue
                    if (containsLibrary(artifactId)) {
                        continue;
                    }

                    if ("org.apache.camel".equals(groupId)) {
                        addLibrary(artifactId);
                    } else {
                        addCustomCamelComponentsFromDependency(camelCatalog, library, artifactId);
                    }
                }
            }
        }
    }

    /**
     * Adds any discovered custom Camel components from the dependency.
     *
     * @param camelCatalog the Camel catalog to add the found custom components
     * @param library      the dependency
     * @param artifactId   the artifact id of the dependency
     */
    private void addCustomCamelComponentsFromDependency(CamelCatalog camelCatalog, Library library, String artifactId) {
        boolean added = false;

        try (URLClassLoader classLoader = newURLClassLoaderForLibrary(library)) {
            if (classLoader != null) {
                // is there any custom Camel components in this library?
                Properties properties = loadComponentProperties(classLoader);
                if (properties != null) {
                    String components = (String) properties.get("components");
                    if (components != null) {
                        String[] part = components.split("\\s");
                        for (String scheme : part) {
                            if (!camelCatalog.findComponentNames().contains(scheme)) {
                                // find the class name
                                String javaType = extractComponentJavaType(classLoader, scheme);
                                if (javaType != null) {
                                    String json = loadComponentJSonSchema(classLoader, scheme);
                                    if (json != null) {
                                        // okay a new Camel component was added
                                        camelCatalog.addComponent(scheme, javaType, json);
                                        added = true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            // ignore
        }

        if (added) {
            addLibrary(artifactId);
        }
    }

    private static URLClassLoader newURLClassLoaderForLibrary(Library library) throws MalformedURLException {
        VirtualFile[] files = library.getFiles(OrderRootType.CLASSES);
        if (files.length == 1) {
            VirtualFile vf = files[0];
            if (vf.getName().toLowerCase().endsWith(".jar")) {
                String path = vf.getPath();
                if (path.endsWith("!/")) {
                    path = path.substring(0, path.length() - 2);
                }
                URL url = new URL("file:" + path);
                return new URLClassLoader(new URL[] {url});
            }
        }
        return null;
    }

    private static Properties loadComponentProperties(URLClassLoader classLoader) {
        Properties answer = new Properties();
        try {
            InputStream is = classLoader.getResourceAsStream("META-INF/services/org/apache/camel/component.properties");
            if (is != null) {
                answer.load(is);
            }
        } catch (Throwable e) {
            // ignore
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
            try {
                InputStream is = classLoader.getResourceAsStream(path);
                if (is != null) {
                    answer = loadText(is);
                }
            } catch (Throwable e) {
                // ignore
            }
        }

        return answer;
    }

    private static String extractComponentJavaType(URLClassLoader classLoader, String scheme) {
        try {
            InputStream is = classLoader.getResourceAsStream("META-INF/services/org/apache/camel/component/" + scheme);
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                return (String) props.get("class");
            }
        } catch (Throwable e) {
            // ignore
        }

        return null;
    }

    private CamelCatalogService getCamelCatalogService(Project project) {
        return ServiceManager.getService(project, CamelCatalogService.class);
    }

}
