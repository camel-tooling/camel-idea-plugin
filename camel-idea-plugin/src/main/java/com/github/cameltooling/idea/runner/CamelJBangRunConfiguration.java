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
package com.github.cameltooling.idea.runner;

import com.github.cameltooling.idea.runner.ui.CamelJBangSettingsEditor;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.target.LanguageRuntimeType;
import com.intellij.execution.target.TargetEnvironmentAwareRunProfile;
import com.intellij.execution.target.TargetEnvironmentConfiguration;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CamelJBangRunConfiguration extends RunConfigurationBase<CamelJBangRunProfileState> implements TargetEnvironmentAwareRunProfile {

    CamelJBangRunConfiguration(Project project, ConfigurationFactory factory, String name) {
        super(project, factory, name);
    }

    @Override
    public @NotNull SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new CamelJBangSettingsEditor(this);
    }

    @Override
    public @Nullable RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) {
        return new CamelJBangRunProfileState(executor, environment, this);
    }

    @NotNull
    @Override
    public CamelJBangRunConfigurationOptions getOptions() {
        return (CamelJBangRunConfigurationOptions)super.getOptions();
    }

    @NotNull
    @Override
    protected Class<? extends CamelJBangRunConfigurationOptions> getDefaultOptionsClass() {
        return CamelJBangRunConfigurationOptions.class;
    }

    @Override
    public boolean canRunOn(@NotNull TargetEnvironmentConfiguration target) {
        return false;
    }

    @Override
    public @Nullable LanguageRuntimeType<?> getDefaultLanguageRuntimeType() {
        return null;
    }

    @Override
    public @Nullable String getDefaultTargetName() {
        return null;
    }

    @Override
    public void setDefaultTargetName(@Nullable String targetName) {

    }
}
