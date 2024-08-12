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
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

/**
 * The base {@link ConfigurationType} for all kind of Camel applications that are launched thanks to a maven command.
 */
public abstract class CamelRunConfigurationType implements ConfigurationType {

    @NotNull
    private final String id;
    @NotNull
    private final String name;
    private final ConfigurationFactory myFactory;

    CamelRunConfigurationType(@NotNull String id, @NotNull String name) {
        this.id = id;
        this.name = name;
        this.myFactory = new CamelRunConfigurationFactory(this);
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return name;
    }

    @Override
    public String getConfigurationTypeDescription() {
        return id;
    }

    @Override
    public Icon getIcon() {
        return CamelPreferenceService.getService().getCamelIcon();
    }

    @Override
    public ConfigurationFactory[] getConfigurationFactories() {
        return new ConfigurationFactory[]{myFactory};
    }

    @Override
    public String getHelpTopic() {
        return "reference.dialogs.rundebug.MavenRunConfiguration";
    }

    @Override
    @NonNls
    @NotNull
    public String getId() {
        return id;
    }
}
