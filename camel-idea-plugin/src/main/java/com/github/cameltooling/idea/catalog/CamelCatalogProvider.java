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
package com.github.cameltooling.idea.catalog;

import java.util.List;
import java.util.function.Supplier;

import com.github.cameltooling.idea.service.CamelCatalogService;
import com.github.cameltooling.idea.service.CamelService;
import com.github.cameltooling.idea.util.ArtifactCoordinates;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.catalog.DefaultRuntimeProvider;
import org.apache.camel.catalog.RuntimeProvider;
import org.apache.camel.catalog.karaf.KarafRuntimeProvider;
import org.apache.camel.catalog.quarkus.QuarkusRuntimeProvider;
import org.apache.camel.springboot.catalog.SpringBootRuntimeProvider;
import org.apache.camel.tooling.model.MainModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * {@code CamelCatalogProvider} defines all the Camel Runtime supported by the plugin.
 */
public enum CamelCatalogProvider {
    /**
     * The mode that automatically detects the right {@code CamelCatalogProvider} according to the libraries found
     * in the project.
     */
    AUTO("Auto detect", null, null, List.of(), null, null) {
        @Override
        public CamelCatalogProvider getActualProvider(final Project project) {
            LOG.debug("Trying to automatically detect the Camel Runtime");
            final CamelService camelService = project.getService(CamelService.class);
            for (CamelCatalogProvider provider : CamelCatalogProvider.values()) {
                if (provider.coreArtifactId == null) {
                    continue;
                }
                if (camelService.containsLibrary(provider.coreArtifactId, false)) {
                    LOG.info("The Camel " + provider.getName() + " Runtime has been detected");
                    return provider;
                }
            }
            LOG.info("No specific Camel Runtime has been detected, the default runtime is used");
            return DEFAULT;
        }
    },
    /**
     * The default mode corresponding to the legacy way to retrieve the catalog.
     */
    DEFAULT("Default", DefaultRuntimeProvider::new, null, List.of(), null, null) {
        @Override
        public boolean loadRuntimeProviderVersion(Project project) {
            // No specific artifact to load
            return true;
        }
    },
    /**
     * The {@code CamelCatalogProvider} for Quarkus.
     */
    QUARKUS(
        "Quarkus", QuarkusRuntimeProvider::new, null, List.of("org.apache.camel.quarkus"),
        "camel-quarkus-core", "camel-quarkus-catalog"
    ),
    /**
     * The {@code CamelCatalogProvider} for Karaf with an empty main model.
     */
    KARAF(
        "Karaf", KarafRuntimeProvider::new, null, List.of("org.apache.camel", "org.apache.camel.karaf"),
        "camel-core-osgi", "camel-catalog-provider-karaf"

    ) {
        @Override
        protected CamelCatalog createCatalog(final Project project) {
            return new DefaultCamelCatalog() {
                @Override
                public MainModel mainModel() {
                    // Empty Main model in case of Karaf
                    return new MainModel();
                }
            };
        }
    },
    /**
     * The {@code CamelCatalogProvider} for Spring Boot with a specific {@code JSonSchemaResolver} to be able to
     * merge the main and component schemas with the Spring configuration metadata available in the classpath of the
     * project.
     */
    SPRING_BOOT(
        "Spring Boot", SpringBootRuntimeProvider::new, "org.apache.camel.catalog.springboot.SpringBootRuntimeProvider",
        List.of("org.apache.camel", "org.apache.camel.springboot"),
        "camel-spring-boot", "camel-catalog-provider-springboot"
    ) {
        @Override
        protected CamelCatalog createCatalog(final Project project) {
            final CamelCatalog catalog = super.createCatalog(project);
            // Use specific schema resolver for Spring Boot
            catalog.setJSonSchemaResolver(
                new SpringBootJSonSchemaResolver(
                    () -> project.getService(CamelService.class).getProjectClassloader(),
                    catalog.getJSonSchemaResolver()
                )
            );
            return catalog;
        }
    };

    /**
     * The logger.
     */
    private static final Logger LOG = Logger.getInstance(CamelCatalogProvider.class);
    /**
     * The display name of the {@code CamelCatalogProvider}.
     */
    private final String name;
    /**
     * The potential group ids of the artifacts of the Runtime Provider.
     */
    @NotNull
    private final List<String> groupIds;
    /**
     * The id of the core artifact of the Runtime Provider.
     */
    @Nullable
    private final String coreArtifactId;
    /**
     * The id of the artifact containing the catalog of the Runtime Provider.
     */
    private final String catalogArtifactId;
    /**
     * The supplier of {@code RuntimeProvider} to use to retrieve the metadata at the right location according to
     * the runtime.
     */
    private final Supplier<RuntimeProvider> runtimeProviderSupplier;
    /**
     * The fully qualified name of the legacy {@code RuntimeProvider}.
     */
    @Nullable
    private final String runtimeProviderLegacyClass;

    /**
     * Constructs a {@code CamelCatalogProvider} with the given parameters.
     *
     * @param name                    the display name of the {@code CamelCatalogProvider}.
     * @param runtimeProviderSupplier the supplier of {@code RuntimeProvider} to use to retrieve the metadata at the
     *                                right location according to the runtime.
     * @param runtimeProviderLegacyClass the fully qualified name of the legacy {@code RuntimeProvider}.
     * @param groupIds                the potential group ids of the artifacts of the Runtime Provider.
     * @param coreArtifactId          the id of the core artifact of the Runtime Provider.
     * @param catalogArtifactId       the id of the artifact containing the catalog of the Runtime Provider.
     */
    CamelCatalogProvider(String name, Supplier<RuntimeProvider> runtimeProviderSupplier,
                         @Nullable String runtimeProviderLegacyClass, @NotNull List<String> groupIds,
                         @Nullable String coreArtifactId, String catalogArtifactId) {
        this.name = name;
        this.groupIds = groupIds;
        this.coreArtifactId = coreArtifactId;
        this.catalogArtifactId = catalogArtifactId;
        this.runtimeProviderSupplier = runtimeProviderSupplier;
        this.runtimeProviderLegacyClass = runtimeProviderLegacyClass;
    }

    /**
     * @return The display name of the {@code CamelCatalogProvider}.
     */
    public String getName() {
        return name;
    }

    /**
     * @param project the project used to find the actual {@code CamelCatalogProvider}.
     * @return the actual {@code CamelCatalogProvider} in case of an automatic detection mode, {@code this} otherwise.
     */
    public CamelCatalogProvider getActualProvider(final Project project) {
        return this;
    }

    /**
     * @return a new instance of {@code RuntimeProvider} corresponding to the current {@code CamelCatalogProvider}.
     */
    private RuntimeProvider createRuntimeProvider() {
        return runtimeProviderSupplier.get();
    }

    /**
     * @param project the project for which a {@code CamelCatalog} should be created.
     * @return a new instance of {@code CamelCatalog} corresponding to the current {@code CamelCatalogProvider}.
     * with the cache disabled to prevent loading incorrect data into the cache while loading the catalog.
     */
    protected CamelCatalog createCatalog(final Project project) {
        return new DefaultCamelCatalog();
    }

    /**
     * @param project the project for which the {@code CamelCatalog} is expected.
     * @return a new instance of {@code CamelCatalog} corresponding to the current {@code CamelCatalogProvider}
     * with the cache disabled to prevent loading incorrect data into the cache while loading the catalog.
     */
    public CamelCatalog get(final Project project) {
        final CamelCatalogProvider provider = getActualProvider(project);
        final CamelCatalog catalog = provider.createCatalog(project);
        final RuntimeProvider runtimeProvider = provider.createRuntimeProvider();
        runtimeProvider.setCamelCatalog(catalog);
        catalog.setRuntimeProvider(runtimeProvider);
        return catalog;
    }

    /**
     * @param project the project from which the core artifact is extracted.
     * @return an instance of {@link ArtifactCoordinates} if the core artifact could be found in the dependency of the
     * given project, {@code null} otherwise.
     */
    @Nullable
    protected ArtifactCoordinates getCoreArtifactCoordinates(final Project project) {
        final CamelService camelService = project.getService(CamelService.class);
        for (String groupId : groupIds) {
            ArtifactCoordinates coordinates = camelService.getProjectLibraryCoordinates(groupId, coreArtifactId);
            if (coordinates != null) {
                return coordinates;
            }
        }
        return null;
    }

    /**
     * Loads the catalog of the Runtime provider by first finding the core artifact in the dependency of the project to
     * retrieve the group id and version and if they can be found it completes them with the corresponding catalog
     * artifact id and finally load this specific Runtime provider version.
     *
     * @param project the project from which the core artifact is extracted.
     * @return {@code true} if the specific Runtime provider version could be loaded, {@code false} otherwise.
     */
    public boolean loadRuntimeProviderVersion(final Project project) {
        final ArtifactCoordinates coordinates = getCoreArtifactCoordinates(project);
        if (coordinates == null) {
            LOG.debug("No core artifact could be found in the project");
            return false;
        }
        final String version = coordinates.getVersion();
        if (version == null) {
            LOG.debug("No version of the core artifact has been set");
            return false;
        }
        return project.getService(CamelCatalogService.class)
            .loadRuntimeProviderVersion(coordinates.getGroupId(), catalogArtifactId, version);
    }

    /**
     * @param classLoader the class loader from which the legacy class of the {@code RuntimeProvider} is retrieved.
     * @return a new instance of the legacy {@code RuntimeProvider} corresponding to the current
     * {@code CamelCatalogProvider} that could be found in the given class loader, {@code null} otherwise.
     */
    @Nullable
    private RuntimeProvider createLegacyRuntimeProvider(final ClassLoader classLoader) {
        if (runtimeProviderLegacyClass == null) {
            LOG.debug("There is no legacy provider defined");
            return null;
        }
        try {
            LOG.debug("Trying to instantiate the legacy class of the runtime provider");
            Class<?> clazz = classLoader.loadClass(runtimeProviderLegacyClass);
            return (RuntimeProvider) clazz.getConstructor().newInstance();
        } catch (ClassNotFoundException e) {
            LOG.warn("The class " + runtimeProviderLegacyClass + " could not be found");
        } catch (Exception e) {
            LOG.warn("Could not instantiate the class " + runtimeProviderLegacyClass, e);
        }
        return null;
    }

    /**
     * Updates the {@code RuntimeProvider} in the given catalog using the legacy implementation if any that can be found
     * in the given class loader.
     * @param project the project from which the actual {@code RuntimeProvider} is retrieved.
     * @param catalog the catalog in which the {@code RuntimeProvider} is updated if needed.
     * @param classLoader the class loader from which the legacy {@code RuntimeProvider} is retrieved.
     */
    public void updateRuntimeProvider(final Project project, final CamelCatalog catalog, final ClassLoader classLoader) {
        // Retrieve the runtime provider available in the project
        final RuntimeProvider runtimeProvider = getActualProvider(project).createLegacyRuntimeProvider(classLoader);
        if (runtimeProvider == null) {
            LOG.debug("No legacy runtime provider to use");
        } else {
            LOG.debug("Replacing the initial legacy runtime provider with the legacy one");
            runtimeProvider.setCamelCatalog(catalog);
            catalog.setRuntimeProvider(runtimeProvider);
        }
    }
}
