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
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.execution.MavenRunConfiguration;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import javax.swing.Icon;
import java.util.List;

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

    @NotNull
    public RunConfiguration createConfiguration(@Nullable String name, @NotNull RunConfiguration template) {
        MavenRunConfiguration cfg = (MavenRunConfiguration) createConfiguration(name, template);

        if (!StringUtil.isEmptyOrSpaces(cfg.getRunnerParameters().getWorkingDirPath())) {
            return cfg;
        }

        Project project = cfg.getProject();
        MavenProjectsManager projectsManager = MavenProjectsManager.getInstance(project);

        List<MavenProject> projects = projectsManager.getProjects();
        if (projects.size() != 1) {
            return cfg;
        }

        VirtualFile directory = projects.get(0).getDirectoryFile();

        cfg.getRunnerParameters().setWorkingDirPath(directory.getPath());

        return cfg;
    }

    @Override
    @NonNls
    @NotNull
    public String getId() {
        return id;
    }
}
