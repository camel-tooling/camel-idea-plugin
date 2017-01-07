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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.stream.Collectors;

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
import static org.apache.camel.idea.CamelContributor.CAMEL_ICON;
import static org.apache.camel.idea.CamelContributor.CAMEL_NOTIFICATION_GROUP;

/**
 * Service access for Camel libraries
 */
public class CamelService implements Disposable {

    // TODO: should be moved to some other package than util, eg service

    private Set<String> processedLibraries = new HashSet<>();
    private volatile boolean camelPresent;
    private Notification camelVersionNotification;
    private Notification camelMissingJSonSchemaNotification;

    @Override
    public void dispose() {
        processedLibraries.clear();

        if (camelVersionNotification != null) {
            camelVersionNotification.expire();
            camelVersionNotification = null;
        }
        if (camelMissingJSonSchemaNotification != null) {
            camelMissingJSonSchemaNotification.expire();
            camelMissingJSonSchemaNotification = null;
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

        List<String> missingJSonSchemas = new ArrayList<>();

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
                        addCustomCamelComponentsFromDependency(camelCatalog, library, artifactId, missingJSonSchemas);
                    }
                }
            }
        }

        if (!missingJSonSchemas.isEmpty()) {
            String components = missingJSonSchemas.stream().collect(Collectors.joining(","));
            String message = "The following Camel components with artifactId [" + components
                + "] does not include component JSon schema metadata which is required for the Camel IDEA plugin to support these components.";

            camelMissingJSonSchemaNotification = CAMEL_NOTIFICATION_GROUP.createNotification(message, NotificationType.WARNING).setImportant(true).setIcon(CAMEL_ICON);
            camelMissingJSonSchemaNotification.notify(project);
        }
    }

    /**
     * Adds any discovered custom Camel components from the dependency.
     *
     * @param camelCatalog the Camel catalog to add the found custom components
     * @param library      the dependency
     * @param artifactId   the artifact id of the dependency
     */
    private void addCustomCamelComponentsFromDependency(CamelCatalog camelCatalog, Library library, String artifactId, List<String> missingJSonSchemas) {
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
            // load the component files using the recommended way by a component.properties file
            InputStream is = classLoader.getResourceAsStream("META-INF/services/org/apache/camel/component.properties");
            if (is != null) {
                answer.load(is);
            } else {
                // okay then try to load using a fallback using classpath scanning
                loadComponentPropertiesClasspathScan(classLoader, answer);
            }
        } catch (Throwable e) {
            // ignore
        }
        return answer;
    }

    private static void loadComponentPropertiesClasspathScan(URLClassLoader classLoader, Properties answer) throws IOException {
        Enumeration<URL> e = classLoader.getResources("META-INF/services/org/apache/camel/component/");
        if (e != null) {
            final List<String> names = new ArrayList<>();
            while (e.hasMoreElements()) {
                URL url = e.nextElement();
                String urlPath = url.getFile();

                urlPath = URLDecoder.decode(urlPath, "UTF-8");

                // If it's a file in a directory, trim the stupid file: spec
                if (urlPath.startsWith("file:")) {
                    // file path can be temporary folder which uses characters that the URLDecoder decodes wrong
                    // for example + being decoded to something else (+ can be used in temp folders on Mac OS)
                    // to remedy this then create new path without using the URLDecoder
                    try {
                        urlPath = new URI(url.getFile()).getPath();
                    } catch (URISyntaxException ignore) {
                        // fallback to use as it was given from the URLDecoder
                        // this allows us to work on Windows if users have spaces in paths
                    }
                    if (urlPath.startsWith("file:")) {
                        urlPath = urlPath.substring(5);
                    }
                }
                // Else it's in a JAR, grab the path to the jar
                if (urlPath.indexOf('!') > 0) {
                    urlPath = urlPath.substring(0, urlPath.indexOf('!'));
                }

                FileInputStream stream = new FileInputStream(urlPath);
                List<String> found = findCamelComponentNamesInJar(stream, "META-INF/services/org/apache/camel/component/");
                names.addAll(found);
            }
            if (!names.isEmpty()) {
                // join the names using a space
                String line = names.stream().collect(Collectors.joining(" "));
                answer.put("components", line);
            }
        }
    }

    private static List<String> findCamelComponentNamesInJar(InputStream stream, String urlPath) {
        List<String> entries = new ArrayList<>();

        JarInputStream jarStream;
        try {
            jarStream = new JarInputStream(stream);

            JarEntry entry;
            while ((entry = jarStream.getNextJarEntry()) != null) {
                String name = entry.getName();
                if (name != null) {
                    name = name.trim();
                    if (name.startsWith(urlPath)) {
                        if (!entry.isDirectory() && !name.endsWith(".class")) {
                            name = name.substring(urlPath.length());
                            entries.add(name);
                        }
                    }
                }
            }
        } catch (Throwable e) {
            // ignore
        }

        return entries;
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
