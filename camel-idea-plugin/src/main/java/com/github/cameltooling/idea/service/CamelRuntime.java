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

import com.github.cameltooling.idea.util.ArtifactCoordinates;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.github.cameltooling.idea.Constants.CAMEL_GROUP_ID;

/**
 * {@code CamelRuntime} defines all the Camel runtimes supported by the plugin.
 */
public enum CamelRuntime {
    /**
     * The default Camel Runtime.
     */
    DEFAULT(List.of(CAMEL_GROUP_ID), null, null, "camel-debug", "camel-management", "camel:run") {
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
        "camel-quarkus-management", "quarkus:dev"
    ),
    /**
     * The Karaf Runtime.
     */
    KARAF(
        List.of(CAMEL_GROUP_ID, "org.apache.camel.karaf"), "camel-core-osgi", "camel-catalog-provider-karaf",
        "camel-debug", "camel-management", null
    ),
    /**
     * The SpringBoot Runtime
     */
    SPRING_BOOT(
        List.of(CAMEL_GROUP_ID, "org.apache.camel.springboot"), "camel-spring-boot",
        "camel-catalog-provider-springboot", "camel-debug-starter", "camel-management-starter", "spring-boot:run"
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
     * The id of the artifact containing Camel Debug.
     */
    @NotNull
    private final String debugArtifactId;
    /**
     * The id of the artifact containing Camel Management.
     */
    @NotNull
    private final String managementArtifactId;
    /**
     * The pair {@code maven-plugin-name:goal-name} to call to launch the Camel runtime when applicable, {@code null}
     * otherwise.
     */
    @Nullable
    private final String pluginGoal;

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
     */
    CamelRuntime(@NotNull List<String> groupIds, @Nullable String coreArtifactId, @Nullable String catalogArtifactId,
                 @NotNull String debugArtifactId, @NotNull String managementArtifactId, @Nullable String pluginGoal) {
        this.groupIds = groupIds;
        this.coreArtifactId = coreArtifactId;
        this.catalogArtifactId = catalogArtifactId;
        this.debugArtifactId = debugArtifactId;
        this.managementArtifactId = managementArtifactId;
        this.pluginGoal = pluginGoal;
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
    public String getDebugArtifactId() {
        return debugArtifactId;
    }

    @NotNull
    public String getManagementArtifactId() {
        return managementArtifactId;
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
}
