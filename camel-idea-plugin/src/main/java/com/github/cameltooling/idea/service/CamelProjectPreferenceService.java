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

import java.util.Objects;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;

/**
 * Service for holding project preferences for this plugin.
 */
@State(
    name = "CamelProjectPreferences",
    storages = {@Storage("apachecamelplugin.xml")})
public class CamelProjectPreferenceService implements PersistentStateComponent<CamelProjectPreferenceService.State>, Disposable {

    public static class State {
        /**
         * The flag indicating whether the project is a Camel project. A {@code null} value indicates
         * that it needs to be auto-detected.
         */
        public Boolean isCamelProject;
        /**
         * The version of the catalog to use. A {@code null} value indicates that it needs to be auto-detected.
         */
        public String catalogVersion;
    }

    private final Project project;

    private State state = new State();

    public CamelProjectPreferenceService(@NotNull Project project) {
        this.project = project;
    }

    public static CamelProjectPreferenceService getService(Project project) {
        return project.getService(CamelProjectPreferenceService.class);
    }

    public Boolean isCamelProject() {
        return state.isCamelProject;
    }

    public void setCamelProject(Boolean camelProject) {
        state.isCamelProject = camelProject;
    }

    public String getCatalogVersion() {
        return state.catalogVersion;
    }

    public void setCatalogVersion(String catalogVersion) {
        if (catalogVersion != null && catalogVersion.isBlank()) {
            catalogVersion = null;
        }
        if (!Objects.equals(this.state.catalogVersion, catalogVersion)) {
            this.state.catalogVersion = catalogVersion;
            project.getMessageBus().syncPublisher(CamelCatalogVersionChangeListener.TOPIC)
                .onCamelCatalogVersionChange(catalogVersion);
        }
    }

    @Override
    public void dispose() {
        // noop
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
    }


    /**
     * {@code CamelCatalogVersionChangeListener} defines a listener to notify in case the version of the Camel catalog
     * defined in the preferences has changed.
     */
    public interface CamelCatalogVersionChangeListener {

        /**
         * The topic to subscribe to in order to be notified when the version of the Camel catalog has changed.
         */
        @Topic.ProjectLevel
        Topic<CamelCatalogVersionChangeListener> TOPIC = Topic.create(
            "CamelCatalogVersionChangeListener", CamelCatalogVersionChangeListener.class
        );

        /**
         * Called when the version of the Camel catalog defined in the preferences has changed.
         * @param version the new version of the Camel catalog. a {@code null} value indicates that it needs to be
         *                auto-detected
         */
        void onCamelCatalogVersionChange(String version);
    }
}
