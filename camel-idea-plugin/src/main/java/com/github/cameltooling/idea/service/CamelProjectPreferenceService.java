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

import com.github.cameltooling.idea.catalog.CamelCatalogProvider;
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
         * The flag indicating whether the project is a Camel JBang project. A {@code null} value indicates
         * that it needs to be auto-detected.
         */
        public Boolean isJBangProject;
        /**
         * The version of the catalog to use. A {@code null} value indicates that it needs to be auto-detected.
         */
        public String camelVersion;
        public boolean enableCamelDebugger = true;
        /**
         * The flag indicating whether the Camel Debugger should be automatically setup.
         */
        public boolean camelDebuggerAutoSetup = true;
        /**
         * Flag indicating whether only the options of the Kamelet should be proposed.
         */
        public boolean onlyShowKameletOptions = true;
        /**
         * The {@link CamelCatalogProvider} set in the preferences.
         */
        public CamelCatalogProvider camelCatalogProvider;
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

    public Boolean isJBangProject() {
        return state.isJBangProject;
    }

    public void setJBangProject(Boolean jbangProject) {
        state.isJBangProject = jbangProject;
    }

    public String getCamelVersion() {
        return state.camelVersion;
    }

    public void setCamelVersion(String camelVersion) {
        if (camelVersion != null && camelVersion.isBlank()) {
            camelVersion = null;
        }
        if (!Objects.equals(this.state.camelVersion, camelVersion)) {
            this.state.camelVersion = camelVersion;
            project.getMessageBus().syncPublisher(CamelVersionChangeListener.TOPIC)
                .onCamelVersionChange(camelVersion);
        }
    }

    public boolean isEnableCamelDebugger() {
        return state.enableCamelDebugger;
    }

    public void setEnableCamelDebugger(boolean enableCamelDebugger) {
        this.state.enableCamelDebugger = enableCamelDebugger;
    }

    public boolean isOnlyShowKameletOptions() {
        return state.onlyShowKameletOptions;
    }

    public void setOnlyShowKameletOptions(boolean onlyShowKameletOptions) {
        this.state.onlyShowKameletOptions = onlyShowKameletOptions;
    }

    public void setCamelDebuggerAutoSetup(boolean camelDebuggerAutoSetup) {
        this.state.camelDebuggerAutoSetup = camelDebuggerAutoSetup;
    }

    public boolean isCamelDebuggerAutoSetup() {
        return state.camelDebuggerAutoSetup;
    }

    /**
     * @return the {@link CamelCatalogProvider} defined in the preferences, {@link CamelCatalogProvider#AUTO} by default.
     */
    public CamelCatalogProvider getCamelCatalogProvider() {
        return getCamelCatalogProvider(state.camelCatalogProvider);
    }

    /**
     * Set the {@link CamelCatalogProvider} to use. The change listeners are notified in case the value has changed.
     * @param camelCatalogProvider the new {@link CamelCatalogProvider} to use
     */
    public void setCamelCatalogProvider(CamelCatalogProvider camelCatalogProvider) {
        final boolean hasChanged = getCamelCatalogProvider() != getCamelCatalogProvider(camelCatalogProvider);
        this.state.camelCatalogProvider = camelCatalogProvider;
        if (hasChanged) {
            project.getMessageBus().syncPublisher(CamelCatalogProviderChangeListener.TOPIC)
                .onCamelCatalogProviderChange();
        }
    }

    /**
     * @return {@link CamelCatalogProvider#AUTO} if the given {@link CamelCatalogProvider} is {@code null}, the
     * given {@link CamelCatalogProvider} otherwise.
     */
    private CamelCatalogProvider getCamelCatalogProvider(CamelCatalogProvider camelCatalogProvider) {
        return camelCatalogProvider == null ? CamelCatalogProvider.AUTO : camelCatalogProvider;
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
     * {@code CamelVersionChangeListener} defines a listener to notify in case the version of Camel
     * defined in the preferences has changed.
     */
    public interface CamelVersionChangeListener {

        /**
         * The topic to subscribe to in order to be notified when the version of the Camel version has changed.
         */
        @Topic.ProjectLevel
        Topic<CamelVersionChangeListener> TOPIC = Topic.create("CamelVersionChangeListener", CamelVersionChangeListener.class);

        /**
         * Called when the version of Camel defined in the preferences has changed.
         * @param version the new version of Camel. a {@code null} value indicates that it needs to be
         *                auto-detected
         */
        void onCamelVersionChange(String version);
    }

    /**
     * {@code CamelCatalogProviderChangeListener} defines a listener to notify in case the {@link CamelCatalogProvider}
     * defined in the preferences has changed.
     */
    public interface CamelCatalogProviderChangeListener {

        /**
         * The topic to subscribe to in order to be notified when the {@link CamelCatalogProvider} has changed.
         */
        @Topic.ProjectLevel
        Topic<CamelCatalogProviderChangeListener> TOPIC = Topic.create(
            "CamelCatalogProviderChangeListener", CamelCatalogProviderChangeListener.class
        );

        /**
         * Called when the {@link CamelCatalogProvider} defined in the preferences has changed.
         */
        void onCamelCatalogProviderChange();
    }
}
