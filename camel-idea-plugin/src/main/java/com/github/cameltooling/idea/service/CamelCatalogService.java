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

import java.util.Map;

import com.github.cameltooling.idea.catalog.CamelCatalogProvider;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.DefaultVersionManager;
import org.apache.camel.catalog.VersionManager;
import org.jetbrains.annotations.NotNull;

/**
 * Service which provides the instance to be used when accessing the {@link CamelCatalog}.
 */
public class CamelCatalogService implements Disposable, CamelPreferenceService.CamelCatalogProviderChangeListener {

    private volatile CamelCatalog instance;
    /**
     * The project in which the service is registered.
     */
    private final Project project;

    /**
     * Construct a {@code CamelCatalogService} with the given project.
     * @param project the project in which the service is registered.
     */
    public CamelCatalogService(Project project) {
        this.project = project;
    }

    /**
     * Gets the {@link CamelCatalog} instance to use according to the {@link CamelCatalogProvider} that has been
     * defined in the preferences.
     */
    public CamelCatalog get() {
        if (instance == null) {
            synchronized (this) {
                if (instance == null) {
                    final CamelPreferenceService preferenceService = getPreferenceService();
                    this.instance = preferenceService.getCamelCatalogProvider().get(project);
                    preferenceService.addListener(this);
                }
            }
        }
        return instance;
    }

    boolean isInstantiated() {
        return instance != null;
    }

    /**
     * Called once the catalog is ready to use.
     */
    void onCamelCatalogReady() {
        CamelCatalog catalog = instance;
        if (catalog != null) {
            // Update the runtime provider is needed
            updateRuntimeProvider(catalog);
            // As the catalog is ready to use, the cache can be enabled
            catalog.enableCache();
        }
    }

    /**
     * Updates the Camel Runtime provider if needed.
     * @param catalog the catalog into which the Camel Runtime should be updated.
     */
    private void updateRuntimeProvider(CamelCatalog catalog) {
        final VersionManager versionManager = catalog.getVersionManager();
        if (versionManager instanceof CamelMavenVersionManager) {
            getPreferenceService().getCamelCatalogProvider().updateRuntimeProvider(
                project, catalog, ((CamelMavenVersionManager) versionManager).getClassLoader()
            );
        }
    }

    /**
     * Loads a specific Camel version into the Catalog to use.
     *
     * @param version the version to load
     * @param repos   any third party maven repositories
     */
    boolean loadVersion(@NotNull String version, @NotNull Map<String, String> repos) {
        // we should load a new version of the catalog, and therefore must discard the old version
        dispose();
        // use maven to be able to load the version dynamic
        CamelMavenVersionManager maven = new CamelMavenVersionManager();

        // add support for the maven repos
        repos.forEach(maven::addMavenRepository);

        get().setVersionManager(maven);
        boolean loaded = get().getVersionManager().loadVersion(version);
        if (!loaded) {
            // we could not load it, then fallback to default
            get().setVersionManager(new DefaultVersionManager(get()));
        }
        return loaded;
    }

    /**
     * Attempt to load the runtime provider version to be used by the catalog.
     * <p/>
     * Loading the runtime provider JAR of the given version of choice may require internet access to download the JAR
     * from Maven central. You can pre download the JAR and install in a local Maven repository to avoid internet access
     * for offline environments.
     *
     * @param  groupId    the runtime provider Maven groupId
     * @param  artifactId the runtime provider Maven artifactId
     * @param  version    the runtime provider Maven version
     * @return            <tt>true</tt> if the version was loaded, <tt>false</tt> if not.
     */
    public boolean loadRuntimeProviderVersion(@NotNull String groupId, @NotNull String artifactId,
                                              @NotNull String version) {
        return get().getVersionManager().loadRuntimeProviderVersion(groupId, artifactId, version);
    }

    public void clearLoadedVersion() {
        // this will force re initialization of the catalog
        dispose();
    }

    @Override
    public void dispose() {
        getPreferenceService().removeListener(this);
        instance = null;
    }

    /**
     * @return the preference service
     */
    private CamelPreferenceService getPreferenceService() {
        return ApplicationManager.getApplication().getService(CamelPreferenceService.class);
    }

    /**
     * Force the catalog to be reloaded when the {@link CamelCatalogProvider} defined in the preferences has changed.
     */
    @Override
    public void onChange() {
        // Clear the old catalog
        clearLoadedVersion();
        // Load the new catalog
        project.getService(CamelService.class).loadCamelCatalog(project);
    }
}
