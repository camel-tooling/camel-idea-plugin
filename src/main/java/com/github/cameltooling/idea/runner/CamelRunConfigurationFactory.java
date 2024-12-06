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

import java.util.List;
import java.util.Objects;

import com.intellij.compiler.options.CompileStepBeforeRun;
import com.intellij.compiler.options.CompileStepBeforeRunNoErrorCheck;
import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.RunManager;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.execution.MavenRunConfiguration;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

/**
 * The {@link ConfigurationFactory} for all kind of Camel applications that are launched thanks to a maven command.
 */
final class CamelRunConfigurationFactory extends ConfigurationFactory {

    CamelRunConfigurationFactory(@NotNull CamelRunConfigurationType type) {
        super(type);
    }

    @NotNull
    @Override
    public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
        return new CamelRunConfiguration(project, this, getType().getDisplayName());
    }

    @NotNull
    @Override
    public RunConfiguration createTemplateConfiguration(@NotNull Project project, @NotNull RunManager runManager) {
        return new CamelRunConfiguration(project, this, getType().getDisplayName());
    }

    @Override
    public @NotNull String getId() {
        return getType().getId();
    }

    @NotNull
    @Override
    public RunConfiguration createConfiguration(@Nullable String name, @NotNull RunConfiguration template) {
        MavenRunConfiguration cfg = (MavenRunConfiguration) super.createConfiguration(name, template);

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
    public void configureBeforeRunTaskDefaults(Key<? extends BeforeRunTask> providerID, BeforeRunTask task) {
        // Decompose Conditional
        boolean isCompileStep = Objects.equals(providerID, CompileStepBeforeRun.ID);
        boolean isCompileStepNoErrorCheck = Objects.equals(providerID, CompileStepBeforeRunNoErrorCheck.ID);

        if (isCompileStep || isCompileStepNoErrorCheck) {
            task.setEnabled(false);
        }
    }

    @Override
    public boolean isEditableInDumbMode() {
        return true;
    }

}
