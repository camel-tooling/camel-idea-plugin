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

/**
 * Service access for Camel libraries
 */
public class CamelService implements Disposable {

    private Set<String> processedLibraries = new HashSet<>();
    private volatile boolean camelPresent;

    @Override
    public void dispose() {
        processedLibraries.clear();
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
     * Scan for Apache Camel Libraries and update the cache and isCamelPresent
     */
    public void scanForCamelDependencies(@NotNull Module module) {
        for (OrderEntry entry : ModuleRootManager.getInstance(module).getOrderEntries()) {
            if (entry instanceof LibraryOrderEntry) {
                LibraryOrderEntry libraryOrderEntry = (LibraryOrderEntry) entry;

                String name = libraryOrderEntry.getPresentableName().toLowerCase();
                if (name.contains("org.apache.camel") && (libraryOrderEntry.getScope().isForProductionCompile() || libraryOrderEntry.getScope().isForProductionRuntime())) {
                    if (!isCamelPresent() && name.contains("camel-core") && !name.contains("camel-core-")
                        && (libraryOrderEntry.getLibrary() != null && libraryOrderEntry.getLibrary().getFiles(OrderRootType.CLASSES).length > 0)) {
                        setCamelPresent(true);
                    }

                    final Library library = libraryOrderEntry.getLibrary();
                    if (library == null) {
                        continue;
                    }
                    String[] split = name.split(":");
                    String groupId = split[1].trim();
                    String artifactId = split[2].trim();

                    if (containsLibrary(artifactId)) {
                        continue;
                    }

                    // must be from vanilla Apache Camel as
                    // we have a another scanner for custom components
                    if ("org.apache.camel".equals(groupId)) {
                        addLibrary(artifactId);
                    }
                }
            }
        }
    }

    /**
     * Scan for Custom Camel Libraries and update the cache and camel catalog with custom components discovered
     */
    public void scanForCustomCamelDependencies(@NotNull Project project, @NotNull Module module) {
        CamelCatalog camelCatalog = ServiceManager.getService(project, CamelCatalogService.class).get();

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

                    // skip reserved from Apache Camel itself
                    if ("org.apache.camel".equals(groupId)) {
                        continue;
                    }

                    addCustomCamelComponentsFromDependency(camelCatalog, library, artifactId);
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
                                        camelCatalog.addComponent(scheme, javaType, json);
                                        // okay a new Camel component was added
                                        if (!containsLibrary(artifactId)) {
                                            addLibrary(artifactId);
                                        }
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
}
