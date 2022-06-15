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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.*;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.ui.scale.ScaleContext;
import com.intellij.util.SVGLoader;
import io.fabric8.kubernetes.api.model.apiextensions.v1.JSONSchemaProps;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.Resource;
import io.github.classgraph.ScanResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * {@code KameletService} is service responsible for detecting, loading and providing the Kamelets that can be found
 * within the context of the project.
 * It loads the custom Kamelets that can be found as dependencies of the project and the catalog of Kamelets. If a catalog
 * of Kamelets can be found as a dependency of the project, this specific catalog is loaded otherwise it loads the catalog
 * that is embedded into the plugin.
 */
public class KameletService implements Disposable {

    /**
     * The logger.
     */
    private static final Logger LOG = Logger.getInstance(KameletService.class);
    /**
     * The name of the directory in which the Kamelets can be found.
     */
    private static final String KAMELETS_DIR = "kamelets";
    /**
     * The suffix of a file that defines a Kamelet.
     */
    private static final String KAMELETS_FILE_SUFFIX = ".kamelet.yaml";
    /**
     * The name of the label corresponding to the type of Kamelet.
     */
    private static final String KAMELET_LABEL_TYPE = "camel.apache.org/kamelet.type";
    /**
     * The name of the annotation corresponding to the icon of the Kamelet encoded in Base 64.
     */
    private static final String KAMELET_ANNOTATION_ICON = "camel.apache.org/kamelet.icon";
    /**
     * The prefix of the content of the annotation corresponding to the icon of the Kamelet.
     * It assumes that all icons are encoded in Base64 and are in SVG format.
     */
    private static final String KAMELET_ANNOTATION_ICON_PREFIX = "data:image/svg+xml;base64,";
    /**
     * The type of Kamelet corresponding to a source.
     */
    private static final String KAMELET_SOURCE_TYPE = "source";
    /**
     * The expected size of the icon.
     */
    private static final int ICON_SIZE = 16;
    /**
     * The mapper used to deserialize the definition of a Kamelet.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    /**
     * The Kamelets that could be found indexed by name.
     */
    private volatile Map<String, Kamelet> kamelets;

    /**
     * The project in which the service is registered.
     */
    private final Project project;

    /**
     * Construct a {@code KameletService} with the given project.
     *
     * @param project the project in which the service is registered.
     */
    public KameletService(Project project) {
        this.project = project;
        project.getMessageBus()
            .connect(this)
            .subscribe(CamelService.CamelCatalogListener.TOPIC, this::onCamelCatalogReady);
        project.getMessageBus()
            .connect(this)
            .subscribe(
                VirtualFileManager.VFS_CHANGES,
                new BulkFileListener() {
                    @Override
                    public void after(@NotNull List<? extends VFileEvent> events) {
                        // handle the events
                        checkForReload(events);
                    }
                }
            );
    }

    /**
     * Gives the name of the Kamelets that can be used in a consumer endpoint.
     *
     * @return a list of name of the Kamelets whose type is {@code source}.
     */
    @NotNull
    public List<String> getConsumerNames() {
        return getKamelets().entrySet().stream()
            .filter(entry -> KAMELET_SOURCE_TYPE.equals(entry.getValue().getType()))
            .map(Map.Entry::getKey)
            .sorted(Comparator.naturalOrder())
            .collect(Collectors.toList());
    }

    /**
     * Gives the name of the Kamelets that can be used in a producer endpoint.
     *
     * @return a list of name of the Kamelets whose type is not {@code source}.
     */
    @NotNull
    public List<String> getProducerNames() {
        return getKamelets().entrySet().stream()
            .filter(entry -> !KAMELET_SOURCE_TYPE.equals(entry.getValue().getType()))
            .map(Map.Entry::getKey)
            .sorted(Comparator.naturalOrder())
            .collect(Collectors.toList());
    }

    /**
     * Gives the definition of a Kamelet whose name is the given name.
     *
     * @param name the name of the Kamelet for which the definition is expected.
     * @return the corresponding definition of Kamelet if it exists, {@code null} otherwise.
     */
    @Nullable
    public JSONSchemaProps getDefinition(String name) {
        final Kamelet kamelet = getKamelets().get(name);
        return kamelet == null ? null : kamelet.getDefinition();
    }

    /**
     * Gives the icon of a Kamelet whose name is the given name.
     *
     * @param name the name of the Kamelet for which the icon is expected.
     * @return the corresponding icon of Kamelet if it exists, {@code null} otherwise.
     */
    @Nullable
    public Icon getIcon(String name) {
        final Kamelet kamelet = getKamelets().get(name);
        return kamelet == null ? null : kamelet.getIcon();
    }

    /**
     * Check if the received events are events on Kamelets and if so force the reloading of the Kamelets.
     *
     * @param events the events received to check.
     */
    private void checkForReload(@NotNull List<? extends VFileEvent> events) {
        LOG.debug("Checking received events for a possible reloading");
        boolean toReload = false;
        for (VFileEvent event : events) {
            final VirtualFile file = event.getFile();
            if (LOG.isTraceEnabled()) {
                LOG.trace("Checking the file " + file);
            }
            if (file != null && file.getName().endsWith(KAMELETS_FILE_SUFFIX)) {
                String canonicalPath = file.getCanonicalPath();
                if (canonicalPath == null || canonicalPath.contains("/" + KAMELETS_DIR + "/")) {
                    LOG.debug("An event on a potential Kamelet has been detected");
                    toReload = true;
                }
            }
        }
        if (toReload) {
            LOG.debug("At least one event on a Kamelet has been detected, the Kamelets will be reloaded");
            this.kamelets = null;
        }
    }

    /**
     * Called once the catalog is ready to use.
     */
    private void onCamelCatalogReady() {
        // Force to reload the catalog
        this.kamelets = null;
    }

    /**
     * Gives the loaded Kamelets as {@code Map}. If they have not been loaded yet, it lazily loads them.
     *
     * @return the loaded Kamelets as {@code Map}.
     */
    private Map<String, Kamelet> getKamelets() {
        Map<String, Kamelet> result = kamelets;
        if (result == null) {
            synchronized (this) {
                result = kamelets;
                if (result == null) {
                    result = loadKamelets();
                    kamelets = result;
                }
            }
        }
        return result;
    }

    /**
     * Loads all the Kamelets that can be found in the {@code KAMELETS_DIR} of any jar files defined as dependencies of
     * the project. Loads also the catalog embedded into the plugin if and only if no catalog {@code camel-kamelets} has
     * been added as dependency of the project otherwise the catalog of the project is loaded instead.
     *
     * @return the Kamelets that could be found as {@code Map}.
     */
    private Map<String, Kamelet> loadKamelets() {
        final ClassGraph classGraph = new ClassGraph()
            .acceptPaths("/" + KAMELETS_DIR + "/");
        final CamelService service = project.getService(CamelService.class);
        final ClassLoader projectClassloader = service.getProjectCompleteClassloader();
        if (projectClassloader != null) {
            if (service.containsLibrary("camel-kamelets", false)) {
                // The project has a specific version of the Kamelets catalog, so we use it by default
                classGraph.overrideClassLoaders(projectClassloader);
            } else {
                classGraph.addClassLoader(projectClassloader);
            }
        }
        final Map<String, Kamelet> result = new HashMap<>();
        final Map<String, Icon> icons = new HashMap<>();
        try (ScanResult scanResult = classGraph.scan()) {
            for (Resource resource : scanResult.getAllResources()) {
                try (InputStream is = resource.open()) {
                    final String name = sanitizeFileName(resource.getPath());
                    final JsonNode source = MAPPER.readTree(is);
                    LOG.debug(String.format("Loading kamelet from: %s, path: %s, name: %s",
                        resource.getClasspathElementFile(),
                        resource.getPath(),
                        name));
                    final Kamelet kamelet = toKamelet(icons, resource, source);
                    if (kamelet == null) {
                        continue;
                    }
                    result.put(name, kamelet);
                } catch (IOException | IllegalArgumentException e) {
                    LOG.warn("Cannot init Kamelet Catalog with content of " + resource.getPath(), e);
                }
            }
        }

        return Collections.unmodifiableMap(result);
    }

    /**
     * Convert the given source of Kamelet to an instance of {@link Kamelet}.
     *
     * @param icons The icons that have already been loaded.
     * @param resource the resource that contains the Kamelet definition
     * @param source   the Kamelet definition to convert.
     * @return An instance of {@link Kamelet} with the type of Kamelet and its definition.
     * @throws IOException if the definition of the Kamelet could not be deserialized.
     */
    private static Kamelet toKamelet(Map<String, Icon> icons, Resource resource, JsonNode source) throws IOException {
        final JsonNode spec = source.get("spec");
        if (spec == null) {
            LOG.debug("No spec defined in " + resource.getPath());
            return null;
        }
        final JsonNode definition = spec.get("definition");
        if (definition == null) {
            LOG.debug("No definition defined in " + resource.getPath());
            return null;
        }
        final JsonNode metadata = source.get("metadata");
        if (metadata == null) {
            LOG.debug("No metadata defined in " + resource.getPath());
            return null;
        }
        final JsonNode labels = metadata.get("labels");
        if (labels == null) {
            LOG.debug("No labels defined in " + resource.getPath());
            return null;
        }
        final JsonNode type = labels.get(KAMELET_LABEL_TYPE);
        if (type == null) {
            LOG.debug("No type defined in " + resource.getPath());
            return null;
        }
        final JsonNode annotations = metadata.get("annotations");
        if (annotations == null) {
            LOG.debug("No annotations defined in " + resource.getPath());
            return null;
        }
        final JsonNode annotationIcon = annotations.get(KAMELET_ANNOTATION_ICON);
        Icon icon = null;
        if (annotationIcon == null) {
            LOG.debug("No icon defined in " + resource.getPath());
        } else {
            final String iconContent = annotationIcon.asText();
            if (iconContent == null || !iconContent.startsWith(KAMELET_ANNOTATION_ICON_PREFIX)) {
                LOG.debug("The icon defined in " + resource.getPath() + " is not in the expected format");
            } else {
                icon = toIcon(
                    icons, resource, Base64.getDecoder().decode(iconContent.substring(KAMELET_ANNOTATION_ICON_PREFIX.length()))
                );
            }
        }
        return new Kamelet(type.asText(), MAPPER.treeToValue(definition, JSONSchemaProps.class), icon);
    }

    /**
     * Converts the given content of icon into an instance of {@link Icon}. If the original icon doesn't have the
     * expected size, it is automatically resized. To limit the impact of the icons on the heap as an icon can be heavy
     * and can be used several times, it first tries to see if the icon has already been loaded based on its content, if
     * so it reuses the same instance otherwise it converts it into a {@code Icon} and puts the result into the map {
     * @code icons} for the next potential calls.
     *
     * @param icons The icons that have already been loaded.
     * @param resource the resource that contains the content of the icon
     * @param content the content of the icon as bytes.
     * @return the icon in the expected size. {@code null} in case of an error.
     */
    private static Icon toIcon(Map<String, Icon> icons, Resource resource, byte[] content) {
        try {
            return icons.computeIfAbsent(
                Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(content)),
                key -> {
                    try {
                        return new ImageIcon(
                            SVGLoader.load(null, new ByteArrayInputStream(content), ScaleContext.create(), ICON_SIZE, ICON_SIZE)
                        );
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            );
        } catch (Exception e) {
            LOG.warn("The icon embedded into " + resource.getPath() + " could not be loaded", e);
        }
        return null;
    }

    /**
     * @param fileName the name of the file that contains the definition of the Kamelet.
     * @return the name of the Kamelet based on the given file name.
     */
    private static String sanitizeFileName(String fileName) {
        int index = fileName.lastIndexOf(KAMELETS_FILE_SUFFIX);
        if (index > 0) {
            fileName = fileName.substring(0, index);
        }
        return fileName.substring(9);
    }

    @Override
    public void dispose() {
        this.kamelets = null;
    }

    /**
     * An inner class holding only the information related to a given Kamelet that is needed by the plugin.
     * It is intentionally not the same object as the catalog to avoid dealing with breaking changes between catalog
     * versions.
     */
    private static class Kamelet {

        /**
         * The type of Kamelet.
         */
        private final String type;
        /**
         * The definition of the Kamelet.
         */
        private final JSONSchemaProps definition;
        /**
         * The icon of the Kamelet.
         */
        private final Icon icon;

        /**
         * Construct a {@code Kamelet} with the given parameters.
         *
         * @param type       the type of Kamelet.
         * @param definition the definition of the Kamelet.
         * @param icon       the icon of the Kamelet.
         */
        Kamelet(String type, JSONSchemaProps definition, Icon icon) {
            this.type = type;
            this.definition = definition;
            this.icon = icon;
        }

        String getType() {
            return type;
        }

        JSONSchemaProps getDefinition() {
            return definition;
        }

        Icon getIcon() {
            return icon;
        }
    }
}
