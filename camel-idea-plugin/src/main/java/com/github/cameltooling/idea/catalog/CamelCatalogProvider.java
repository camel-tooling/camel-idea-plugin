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

import java.util.function.Supplier;

import com.github.cameltooling.idea.service.CamelService;
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

/**
 * {@code CamelCatalogProvider} defines all the Camel Runtime supported by the plugin.
 */
public enum CamelCatalogProvider {
    /**
     * The mode that automatically detects the right {@code CamelCatalogProvider} according to the libraries found
     * in the project.
     */
    AUTO("Auto detect", null) {
        @Override
        public CamelCatalogProvider getActualProvider(final Project project) {
            LOG.debug("Trying to automatically detect the Camel Runtime");
            final CamelService camelService = project.getService(CamelService.class);
            if (camelService.containsLibrary("camel-spring-boot", false)) {
                LOG.info("The Camel Spring Boot Runtime has been detected");
                return SPRING_BOOT;
            } else if (camelService.containsLibrary("camel-quarkus-core", false)) {
                LOG.info("The Camel Quarkus Runtime has been detected");
                return QUARKUS;
            } else if (camelService.containsLibrary("camel-core-osgi", false)) {
                LOG.info("The Camel Karaf Runtime has been detected");
                return KARAF;
            }
            LOG.info("No specific Camel Runtime has been detected, the default runtime is used");
            return DEFAULT;
        }
    },
    /**
     * The default mode corresponding to the legacy way to retrieve the catalog.
     */
    DEFAULT("Default", DefaultRuntimeProvider::new),
    /**
     * The {@code CamelCatalogProvider} for Quarkus.
     */
    QUARKUS("Quarkus", QuarkusRuntimeProvider::new),
    /**
     * The {@code CamelCatalogProvider} for Karaf with an empty main model.
     */
    KARAF("Karaf", KarafRuntimeProvider::new) {
        @Override
        protected CamelCatalog createCatalog(final Project project) {
            return new DefaultCamelCatalog(true) {
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
    SPRING_BOOT("Spring Boot", SpringBootRuntimeProvider::new) {
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
     * The supplier of {@code RuntimeProvider} to use to retrieve the metadata at the right location according to
     * the runtime.
     */
    private final Supplier<RuntimeProvider> runtimeProviderSupplier;

    /**
     * Constructs a {@code CamelCatalogProvider} with the given parameters.
     * @param name the display name of the {@code CamelCatalogProvider}.
     * @param runtimeProviderSupplier the supplier of {@code RuntimeProvider} to use to retrieve the metadata at the
     *                                right location according to the runtime.
     */
    CamelCatalogProvider(String name, Supplier<RuntimeProvider> runtimeProviderSupplier) {
        this.name = name;
        this.runtimeProviderSupplier = runtimeProviderSupplier;
    }

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
     */
    protected CamelCatalog createCatalog(final Project project) {
        return new DefaultCamelCatalog(true);
    }

    /**
     * @param project the project for which the {@code CamelCatalog} is expected.
     * @return a new instance of {@code CamelCatalog} corresponding to the current {@code CamelCatalogProvider}.
     */
    public CamelCatalog get(final Project project) {
        final CamelCatalogProvider provider = getActualProvider(project);
        final CamelCatalog catalog = provider.createCatalog(project);
        final RuntimeProvider runtimeProvider = provider.createRuntimeProvider();
        runtimeProvider.setCamelCatalog(catalog);
        catalog.setRuntimeProvider(runtimeProvider);
        return catalog;
    }
}
