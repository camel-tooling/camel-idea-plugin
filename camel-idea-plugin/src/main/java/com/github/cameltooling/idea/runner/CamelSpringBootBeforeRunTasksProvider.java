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
import com.intellij.execution.BeforeRunTaskProvider;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.util.concurrency.Semaphore;
import com.intellij.util.execution.ParametersListUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.execution.MavenRunner;
import org.jetbrains.idea.maven.execution.MavenRunnerParameters;
import org.jetbrains.idea.maven.model.MavenExplicitProfiles;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.idea.maven.tasks.MavenBeforeRunTask;
import org.jetbrains.idea.maven.tasks.TasksBundle;

import javax.swing.Icon;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CamelSpringBootBeforeRunTasksProvider extends BeforeRunTaskProvider<CamelSpringBootBeforeRunTask> {
    public static final Key<MavenBeforeRunTask> ID = Key.create("Mule.BeforeRunTask");

    @Override
    public Key getId() {
        return ID;
    }

    @Override
    public Icon getIcon() {
        return CamelPreferenceService.getService().getCamelIcon();
    }

    @Override
    public Icon getTaskIcon(CamelSpringBootBeforeRunTask task) {
        return CamelPreferenceService.getService().getCamelIcon();
    }

    @Override
    public String getName() {
        return "Camel SpringBoot Builder";
    }

    @Override
    public String getDescription(CamelSpringBootBeforeRunTask beforeRunTask) {
        return "Build the Camel SpringBoot Application";
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Nullable
    @Override
    public CamelSpringBootBeforeRunTask createTask(RunConfiguration runConfiguration) {
        final CamelSpringBootBeforeRunTask camelSpringBootBeforeRunTask = new CamelSpringBootBeforeRunTask(getId());
        camelSpringBootBeforeRunTask.setEnabled(runConfiguration instanceof CamelSpringBootRunConfiguration);
        return camelSpringBootBeforeRunTask;
    }

    @Override
    public boolean configureTask(RunConfiguration runConfiguration, CamelSpringBootBeforeRunTask beforeRunTask) {
        return runConfiguration instanceof CamelSpringBootRunConfiguration;
    }

    @Override
    public boolean canExecuteTask(RunConfiguration runConfiguration, CamelSpringBootBeforeRunTask beforeRunTask) {
        return runConfiguration instanceof CamelSpringBootRunConfiguration;
    }

    @Override
    public boolean executeTask(DataContext dataContext, RunConfiguration runConfiguration, ExecutionEnvironment executionEnvironment, CamelSpringBootBeforeRunTask camelSpringBootBeforeRunTask) {
        final Semaphore targetDone = new Semaphore();
        final List<Boolean> results = new ArrayList<>();

        final Project project = executionEnvironment.getProject();

        CamelSpringBootRunConfiguration muleConfiguration = (CamelSpringBootRunConfiguration) runConfiguration;

        Module[] modules = muleConfiguration.getModules();

        for (Module nextModule : modules) {
            //final MavenProject mavenProject = getMavenProject(runConfiguration, project);
            final MavenProject mavenProject = getMavenProject(nextModule);
            try {
                ApplicationManager.getApplication().invokeAndWait(new Runnable() {
                    public void run() {
                        if (!project.isDisposed() && mavenProject != null) {
                            FileDocumentManager.getInstance().saveAllDocuments();
                            final MavenExplicitProfiles explicitProfiles = MavenProjectsManager.getInstance(project).getExplicitProfiles();
                            final MavenRunner mavenRunner = MavenRunner.getInstance(project);
                            targetDone.down();
                            (new Task.Backgroundable(project, TasksBundle.message("maven.tasks.executing"), true) {
                                public void run(@NotNull ProgressIndicator indicator) {
                                    try {
                                        MavenRunnerParameters params =
                                                new MavenRunnerParameters(true,
                                                        mavenProject.getDirectory(),
                                                        mavenProject.getFile().getName(),
                                                        ParametersListUtil.parse("clean package"),
                                                        explicitProfiles.getEnabledProfiles(),
                                                        explicitProfiles.getDisabledProfiles());
                                        boolean result = mavenRunner.runBatch(Collections.singletonList(params), null, null, TasksBundle.message("maven.tasks.executing"), indicator);
                                        results.add(result);
                                    } finally {
                                        targetDone.up();
                                    }
                                }

                                public boolean shouldStartInBackground() {
                                    return MavenRunner.getInstance(project).getSettings().isRunMavenInBackground();
                                }

                                public void processSentToBackground() {
                                    MavenRunner.getInstance(project).getSettings().setRunMavenInBackground(true);
                                }
                            }).queue();
                        }
                    }
                }, ModalityState.NON_MODAL);
            } catch (Exception exeception) {
                return false;
            }
            targetDone.waitFor();
        }

        boolean endResult = true;

        for (Boolean nextResult : results) {
            endResult = endResult && nextResult;
        }

        return endResult;
    }

    private MavenProject getMavenProject(Module module) {
        final MavenProjectsManager instance = MavenProjectsManager.getInstance(module.getProject());
        return instance.findProject(module);
    }


}
