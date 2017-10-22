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
package org.apache.camel.idea.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.stream.Collectors;
import javax.swing.Icon;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.idea.util.IdeaUtils;
import org.jetbrains.annotations.NotNull;
import static org.apache.camel.catalog.CatalogHelper.loadText;
import static org.apache.camel.idea.service.XmlUtils.getChildNodeByTagName;
import static org.apache.camel.idea.service.XmlUtils.loadDocument;

/**
 * Service access for Camel libraries
 */
public class CamelService implements Disposable {

    private static final NotificationGroup CAMEL_NOTIFICATION_GROUP = NotificationGroup.balloonGroup("Apache Camel");

    private static final Logger LOG = Logger.getInstance(CamelService.class);

    private static final String MISSING_JSON_SCHEMA_LINK = "https://github.com/camel-idea-plugin/camel-idea-plugin/tree/master/custom-components/beverage-component";

    private static final int MIN_MAJOR_VERSION = 2;
    private static final int MIN_MINOR_VERSION = 16;

    private Library camelCoreLibrary;
    private Library slf4japiLibrary;
    private ClassLoader camelCoreClassloader;
    private Set<String> processedLibraries = new HashSet<>();
    private volatile boolean camelPresent;
    private Notification camelVersionNotification;
    private Notification camelMissingJSonSchemaNotification;

    public IdeaUtils getIdeaUtils() {
        return ServiceManager.getService(IdeaUtils.class);
    }

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

        camelCoreClassloader = null;
        camelCoreLibrary = null;
        slf4japiLibrary = null;
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
     * Gets the classloader that can load classes from camel-core which is present on the project classpath
     */
    public ClassLoader getCamelCoreClassloader() {
        if (camelCoreClassloader == null) {
            try {
                camelCoreClassloader = getIdeaUtils().newURLClassLoaderForLibrary(camelCoreLibrary, slf4japiLibrary);
            } catch (Throwable e) {
                LOG.warn("Error creating URLClassLoader for loading classes from camel-core", e);
            }
        }
        return camelCoreClassloader;
    }

    /**
     * Scan for Camel project present and setup {@link CamelCatalog} to use same version of Camel as the project does.
     * These two version needs to be aligned to offer the best tooling support on the given project.
     */
    public void scanForCamelProject(@NotNull Project project, @NotNull Module module) {
        for (OrderEntry entry : ModuleRootManager.getInstance(module).getOrderEntries()) {
            if (!(entry instanceof LibraryOrderEntry)) {
                continue;
            }
            LibraryOrderEntry libraryOrderEntry = (LibraryOrderEntry) entry;

            String name = libraryOrderEntry.getPresentableName().toLowerCase();
            if (!libraryOrderEntry.getScope().isForProductionCompile() && !libraryOrderEntry.getScope().isForProductionRuntime()) {
                continue;
            }
            final Library library = libraryOrderEntry.getLibrary();
            if (library == null) {
                continue;
            }
            String[] split = name.split(":");
            if (split.length < 3) {
                continue;
            }
            int startIdx = 0;
            if (split[0].equalsIgnoreCase("maven")
                    || split[0].equalsIgnoreCase("gradle")
                    || split[0].equalsIgnoreCase("sbt")) {
                startIdx = 1;
            }
            boolean hasVersion = split.length > (startIdx + 2);

            String groupId = split[startIdx++].trim();
            String artifactId = split[startIdx++].trim();
            String version = null;
            if (hasVersion) {
                version = split[startIdx].trim();
                // adjust snapshot which must be in uppercase
                version = version.replace("snapshot", "SNAPSHOT");
            }

            if (isSlf4jMavenDependency(groupId, artifactId)) {
                slf4japiLibrary = library;
            } else if (isCamelMavenDependency(groupId, artifactId)) {
                camelCoreLibrary = library;

                // okay its a camel project
                setCamelPresent(true);

                String currentVersion = getCamelCatalogService(project).get().getLoadedVersion();
                if (currentVersion == null) {
                    // okay no special version was loaded so its the catalog version we are using
                    currentVersion = getCamelCatalogService(project).get().getCatalogVersion();
                }
                if (isThereDifferentVersionToBeLoaded(version, currentVersion)) {
                    boolean notifyNewCamelCatalogVersionLoaded = false;

                    boolean downloadAllowed = getCamelPreferenceService().isDownloadCatalog();
                    if (downloadAllowed) {
                        notifyNewCamelCatalogVersionLoaded = downloadNewCamelCatalogVersion(project, module, version, notifyNewCamelCatalogVersionLoaded);
                    }

                    if (notifyNewCamelCatalogVersionLoaded(notifyNewCamelCatalogVersionLoaded)) {
                        expireOldCamelCatalogVersion();
                    }
                }

                // only notify this once on startup (or if a new version was successfully loaded)
                if (camelVersionNotification == null) {
                    currentVersion = getCamelCatalogService(project).get().getLoadedVersion();
                    if (currentVersion == null) {
                        // okay no special version was loaded so its the catalog version we are using
                        currentVersion = getCamelCatalogService(project).get().getCatalogVersion();
                    }
                    showCamelCatalogVersionAtPluginStart(project, currentVersion);
                }
            }
        }
    }

    private void showCamelCatalogVersionAtPluginStart(@NotNull Project project, String currentVersion) {
        camelVersionNotification = CAMEL_NOTIFICATION_GROUP.createNotification("Camel IDEA plugin is using camel-catalog version "
                + currentVersion, NotificationType.INFORMATION);
        camelVersionNotification.notify(project);
    }

    private boolean isSlf4jMavenDependency(String groupId, String artifactId) {
        return "org.slf4j".equals(groupId) && "slf4j-api".equals(artifactId);
    }

    private boolean isCamelMavenDependency(String groupId, String artifactId) {
        return "org.apache.camel".equals(groupId) && "camel-core".equals(artifactId);
    }

    private void expireOldCamelCatalogVersion() {
        camelVersionNotification.expire();
        camelVersionNotification = null;
    }

    private boolean notifyNewCamelCatalogVersionLoaded(boolean notifyLoaded) {
        return notifyLoaded && camelVersionNotification != null;
    }

    /**
     * attempt to load new version of camel-catalog to match the version from the project
     * use catalog service to load version (which takes care of switching catalog as well)
     */
    private boolean downloadNewCamelCatalogVersion(@NotNull Project project, @NotNull Module module, String version, boolean notifyLoaded) {
        // find out the third party maven repositories
        Map<String, String> repos = scanThirdPartyMavenRepositories(module);

        boolean loaded = getCamelCatalogService(project).loadVersion(version, repos);
        if (!loaded) {
            // always notify if download was not possible
            camelVersionNotification = CAMEL_NOTIFICATION_GROUP.createNotification("Camel IDEA plugin cannot download camel-catalog with version " + version
                    + ". Will fallback and use version " + getCamelCatalogService(project).get().getCatalogVersion(), NotificationType.WARNING);
            camelVersionNotification.notify(project);
        } else {
            // new version loaded so notify
            notifyLoaded = true;
        }
        return notifyLoaded;
    }

    private boolean isThereDifferentVersionToBeLoaded(String version, String currentVersion) {
        return version != null && !version.equalsIgnoreCase(currentVersion) && acceptedVersion(version);
    }

    /**
     * Scan for Camel component (both from Apache Camel and 3rd party components)
     */
    public void scanForCamelDependencies(@NotNull Project project, @NotNull Module module) {
        boolean thirdParty = getCamelPreferenceService().isScanThirdPartyComponents();

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
                    if (split.length < 3) {
                        continue;
                    }
                    int startIdx = 0;
                    if (split[0].equalsIgnoreCase("maven")
                            || split[0].equalsIgnoreCase("gradle")
                            || split[0].equalsIgnoreCase("sbt")) {
                        startIdx = 1;
                    }
                    String groupId = split[startIdx++].trim();
                    String artifactId = split[startIdx].trim();

                    // is it a known library then continue
                    if (containsLibrary(artifactId)) {
                        continue;
                    }

                    if ("org.apache.camel".equals(groupId)) {
                        addLibrary(artifactId);
                    } else if (thirdParty) {
                        addCustomCamelComponentsFromDependency(camelCatalog, library, artifactId, missingJSonSchemas);
                    }
                }
            }
        }

        if (!missingJSonSchemas.isEmpty()) {
            String components = missingJSonSchemas.stream().collect(Collectors.joining(","));
            String message = "The following Camel components with artifactId [" + components
                    + "] does not include component JSon schema metadata which is required for the Camel IDEA plugin to support these components."
                    + "\nSee more details at: " + MISSING_JSON_SCHEMA_LINK;

            Icon icon = getCamelPreferenceService().getCamelIcon();
            camelMissingJSonSchemaNotification = CAMEL_NOTIFICATION_GROUP.createNotification(message, NotificationType.WARNING).setImportant(true).setIcon(icon);
            camelMissingJSonSchemaNotification.notify(project);
        }
    }

    /**
     * Scans for third party maven repositories in the root pom.xml file of the module.
     *
     * @param module the module
     * @return a map with repo id and url for each found repository. The map may be empty if no third party repository is defined in the pom.xml file
     */
    private @NotNull Map<String, String> scanThirdPartyMavenRepositories(@NotNull Module module) {
        Map<String, String> answer = new LinkedHashMap<>();

        VirtualFile vf = module.getProject().getBaseDir().findFileByRelativePath("pom.xml");
        if (vf != null) {
            try {
                InputStream is = vf.getInputStream();
                Document dom = loadDocument(is, false);
                NodeList list = dom.getElementsByTagName("repositories");
                if (list != null && list.getLength() == 1) {
                    Node repos = list.item(0);
                    if (repos instanceof Element) {
                        Element element = (Element) repos;
                        list = element.getElementsByTagName("repository");
                        if (list != null && list.getLength() > 0) {
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
                }
            } catch (Throwable e) {
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
    private void addCustomCamelComponentsFromDependency(CamelCatalog camelCatalog, Library library, String artifactId, List<String> missingJSonSchemas) {
        boolean legacyScan = getCamelPreferenceService().isScanThirdPartyLegacyComponents();
        boolean added = false;

        try (URLClassLoader classLoader = getIdeaUtils().newURLClassLoaderForLibrary(library)) {
            if (classLoader != null) {
                // is there any custom Camel components in this library?
                Properties properties = loadComponentProperties(classLoader, legacyScan);
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
            major = Integer.valueOf(parts[0]);
            minor = Integer.valueOf(parts[1]);
        }

        if (major > MIN_MAJOR_VERSION) {
            return true;
        }
        if (major < MIN_MAJOR_VERSION) {
            return false;
        }

        // okay its the same major versiom, then the minor must be equal or higher
        return minor >= MIN_MINOR_VERSION;
    }

    private static Properties loadComponentProperties(URLClassLoader classLoader, boolean legacyScan) {
        Properties answer = new Properties();
        try {
            // load the component files using the recommended way by a component.properties file
            InputStream is = classLoader.getResourceAsStream("META-INF/services/org/apache/camel/component.properties");
            if (is != null) {
                answer.load(is);
            } else if (legacyScan) {
                // okay then try to load using a fallback using legacy classpath scanning
                loadComponentPropertiesClasspathScan(classLoader, answer);
            }
        } catch (Throwable e) {
            LOG.warn("Error loading META-INF/services/org/apache/camel/component.properties file", e);
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
            LOG.warn("Error finding Camel components in JAR", e);
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
                LOG.warn("Error loading " + path + " file", e);
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
            LOG.warn("Error loading META-INF/services/org/apache/camel/component/" + scheme + " file", e);
        }

        return null;
    }

    private CamelCatalogService getCamelCatalogService(Project project) {
        return ServiceManager.getService(project, CamelCatalogService.class);
    }

    private CamelPreferenceService getCamelPreferenceService() {
        return ServiceManager.getService(CamelPreferenceService.class);
    }

}
