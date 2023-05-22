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

import com.github.cameltooling.idea.service.CamelPreferenceService;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.SimpleConfigurationType;
import com.intellij.openapi.components.BaseState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The {@link ConfigurationType} for Camel JBang applications.
 */
public class CamelJBangRunConfigurationType extends SimpleConfigurationType {

    public static final String ID = "CamelJBangRunConfiguration";
    CamelJBangRunConfigurationType() {
        super(ID, "Camel JBang Application", "Camel JBang application configuration",
            NotNullLazyValue.createValue(() -> CamelPreferenceService.getService().getCamelIcon()));
    }

    @Override
    public @NotNull RunConfiguration createTemplateConfiguration(@NotNull Project project) {
        return new CamelJBangRunConfiguration(project, this, getType().getDisplayName());
    }

    @Override
    public String getHelpTopic() {
        return "Configuration of a Camel JBang Application";
    }

    @Override
    public @Nullable Class<? extends BaseState> getOptionsClass() {
        return CamelJBangRunConfigurationOptions.class;
    }
}
