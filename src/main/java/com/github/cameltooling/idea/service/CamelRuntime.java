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

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.github.cameltooling.idea.util.ArtifactCoordinates;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.github.cameltooling.idea.Constants.CAMEL_GROUP_ID;
import static java.util.stream.Collectors.toUnmodifiableMap;

/**
 * {@code CamelRuntime} defines all the Camel runtimes supported by the plugin.
 */
public enum CamelRuntime {
    /**
     * The default Camel Runtime.
     */
    DEFAULT(List.of(CAMEL_GROUP_ID), null, null, "camel-debug", "camel-management", "camel:run", null) {
        @Override
        @Nullable
        public String getVersion(final Project project) {
            return project.getService(CamelCatalogService.class).get().getLoadedVersion();
        }
    },
    /**
     * The Quarkus Runtime.
     */
    QUARKUS(
        List.of("org.apache.camel.quarkus"), "camel-quarkus-core", "camel-quarkus-catalog", "camel-quarkus-debug",
        "camel-quarkus-management", "quarkus:dev", null
    ),
    /**
     * The Karaf Runtime.
     */
    KARAF(
        List.of(CAMEL_GROUP_ID, "org.apache.camel.karaf"), "camel-core-osgi", "camel-catalog-provider-karaf",
        "camel-debug", Map.of(VersionRange.of("3.15.0", "4.7.0"), CAMEL_GROUP_ID), "camel-management", null, null
    ),
    /**
     * The SpringBoot Runtime
     */
    SPRING_BOOT(
        List.of(CAMEL_GROUP_ID, "org.apache.camel.springboot"), "camel-spring-boot",
        "camel-catalog-provider-springboot", "camel-debug-starter", "camel-management-starter", "spring-boot:run",
        ArtifactCoordinates.of(CAMEL_GROUP_ID, "camel-spring-xml", null)
    );
    /**
     * The logger.
     */
    private static final Logger LOG = Logger.getInstance(CamelRuntime.class);
    /**
     * The potential group ids of the artifacts.
     */
    @NotNull
    private final List<String> groupIds;
    /**
     * The id of the core artifact.
     */
    @Nullable
    private final String coreArtifactId;
    /**
     * The id of the artifact containing the catalog.
     */
    @Nullable
    private final String catalogArtifactId;
    /**
     * The artifact containing Camel Debug.
     */
    @NotNull
    private final ArtifactCoordinates debugArtifact;
    @NotNull
    private final Map<VersionRange, ArtifactCoordinates> debugArtifactOverrides;
    /**
     * The artifact containing Camel Management.
     */
    @NotNull
    private final ArtifactCoordinates managementArtifact;
    /**
     * The artifact of the additional debug dependency.
     */
    @Nullable
    private final ArtifactCoordinates additionalArtifact;
    /**
     * The pair {@code maven-plugin-name:goal-name} to call to launch the Camel runtime when applicable, {@code null}
     * otherwise.
     */
    @Nullable
    private final String pluginGoal;

    CamelRuntime(@NotNull List<String> groupIds, @Nullable String coreArtifactId, @Nullable String catalogArtifactId,
                 @NotNull String debugArtifactId, @NotNull String managementArtifactId, @Nullable String pluginGoal,
                 @Nullable ArtifactCoordinates additionalArtifact) {
        this(groupIds, coreArtifactId, catalogArtifactId, debugArtifactId, Map.of(), managementArtifactId, pluginGoal, additionalArtifact);
    }

    /**
     * Constructs a {@code CamelRuntime} with the given parameters.
     *
     * @param groupIds             the potential group ids of the artifacts.
     * @param coreArtifactId       the id of the core artifact.
     * @param catalogArtifactId    the id of the artifact containing the catalog.
     * @param debugArtifactId      the id of the artifact containing Camel Debug.
     * @param managementArtifactId the id of the artifact containing Camel Management.
     * @param pluginGoal           the pair {@code maven-plugin-name:goal-name} to call to launch the Camel runtime when applicable, {@code null}
     *                             otherwise.
     * @param additionalArtifact the artifact of the additional debug dependency.
     */
    CamelRuntime(@NotNull List<String> groupIds, @Nullable String coreArtifactId, @Nullable String catalogArtifactId,
                 @NotNull String debugArtifactId, Map<VersionRange, String> debugGroupIdOverrides, @NotNull String managementArtifactId, @Nullable String pluginGoal,
                 @Nullable ArtifactCoordinates additionalArtifact) {
        this.groupIds = groupIds;
        this.coreArtifactId = coreArtifactId;
        this.catalogArtifactId = catalogArtifactId;
        // Use the last group id as it is only supported in recent versions
        String groupId = groupIds.getLast();
        this.debugArtifact = ArtifactCoordinates.of(groupId, debugArtifactId, null);
        this.debugArtifactOverrides = debugGroupIdOverrides.entrySet().stream()
                .collect(toUnmodifiableMap(Map.Entry::getKey,
                        e -> ArtifactCoordinates.of(e.getValue(), debugArtifactId, null)));
        this.managementArtifact = ArtifactCoordinates.of(groupId, managementArtifactId, null);
        this.pluginGoal = pluginGoal;
        this.additionalArtifact = additionalArtifact;
    }

    @NotNull
    public List<String> getGroupIds() {
        return groupIds;
    }

    @Nullable
    public String getCoreArtifactId() {
        return coreArtifactId;
    }

    @Nullable
    public String getCatalogArtifactId() {
        return catalogArtifactId;
    }

    @NotNull
    public ArtifactCoordinates getDebugArtifact(String version) {
        Version parsedVersion = Version.parse(version);
        if (parsedVersion != null) {
            return debugArtifactOverrides.entrySet().stream()
                    .filter(e -> e.getKey().contains(parsedVersion))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse(debugArtifact);
        } else {
            return debugArtifact;
        }
    }

    @NotNull
    public ArtifactCoordinates getManagementArtifact() {
        return managementArtifact;
    }

    @Nullable
    public ArtifactCoordinates getAdditionalArtifact() {
        return additionalArtifact;
    }

    @Nullable
    public String getPluginGoal() {
        return pluginGoal;
    }

    /**
     * Gives the Camel runtime that could be detected from the dependencies of the given project.
     *
     * @param project the project against which the dependencies are tested.
     * @return the Camel runtime that could be detected, {@code DEFAULT} by default.
     */
    public static CamelRuntime getCamelRuntime(final Project project) {
        LOG.debug("Trying to automatically detect the Camel Runtime");
        final CamelService camelService = project.getService(CamelService.class);
        for (CamelRuntime runtime : CamelRuntime.values()) {
            if (runtime.coreArtifactId == null) {
                continue;
            }
            if (camelService.containsLibrary(runtime.coreArtifactId, false)) {
                LOG.info(String.format("The Camel %s Runtime has been detected", runtime));
                return runtime;
            }
        }
        LOG.info("No specific Camel Runtime has been detected, the default runtime is used");
        return DEFAULT;
    }

    /**
     * @param project the project from which the core artifact is extracted.
     * @return an instance of {@link ArtifactCoordinates} if the core artifact could be found in the dependency of the
     * given project, {@code null} otherwise.
     */
    @Nullable
    public ArtifactCoordinates getCoreArtifactCoordinates(final Project project) {
        final CamelService camelService = project.getService(CamelService.class);
        if (coreArtifactId == null) {
            return null;
        }
        for (String groupId : groupIds) {
            ArtifactCoordinates coordinates = camelService.getProjectLibraryCoordinates(groupId, coreArtifactId);
            if (coordinates != null) {
                return coordinates;
            }
        }
        return null;
    }

    /**
     * Provides the version of the core artifact of the Camel Runtime.
     *
     * @param project the project from which the version of the Camel Runtime is retrieved.
     * @return the version of the Camel Runtime.
     */
    @Nullable
    public String getVersion(final Project project) {
        final ArtifactCoordinates coordinates = getCoreArtifactCoordinates(project);
        if (coordinates == null) {
            LOG.debug("No core artifact could be found in the project");
            return null;
        }
        String version = coordinates.getVersion();
        if (version == null) {
            LOG.debug("No runtime version could be found");
        }
        return version;
    }

    private record Version(int major, int minor) implements Comparable<Version> {

        public static Version parse(@NotNull String version) {
            String[] parts = version.split("\\.");
            if (parts.length < 2) {
                return null;
            } else {
                try {
                    return new Version(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }

        @Override
        public int compareTo(@NotNull CamelRuntime.Version o) {
            int majorComparison = Integer.compare(this.major, o.major);
            if (majorComparison != 0) {
                return majorComparison;
            }
            return Integer.compare(this.minor, o.minor);
        }

    }

    private record VersionRange(@NotNull Version from, @NotNull Version to) {

        public static VersionRange of(String from, String to) {
            return new VersionRange(
                    Objects.requireNonNull(Version.parse(from)),
                    Objects.requireNonNull(Version.parse(to))
            );
        }

        public boolean contains(Version version) {
            return version.compareTo(from) >= 0 && version.compareTo(to) < 0;
        }

    }

}
