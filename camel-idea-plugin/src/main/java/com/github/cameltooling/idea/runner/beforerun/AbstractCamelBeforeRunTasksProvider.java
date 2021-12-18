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
package com.github.cameltooling.idea.runner.beforerun;

import com.github.cameltooling.idea.runner.CamelRunConfiguration;
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
import com.intellij.util.concurrency.Semaphore;
import com.intellij.util.execution.ParametersListUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.execution.MavenRunner;
import org.jetbrains.idea.maven.execution.MavenRunnerParameters;
import org.jetbrains.idea.maven.model.MavenExplicitProfiles;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.idea.maven.tasks.TasksBundle;

import javax.swing.Icon;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractCamelBeforeRunTasksProvider extends BeforeRunTaskProvider<CamelBeforeRunTask> {
    @Override
    public Icon getIcon() {
        return CamelPreferenceService.getService().getCamelIcon();
    }

    @Override
    public Icon getTaskIcon(CamelBeforeRunTask task) {
        return CamelPreferenceService.getService().getCamelIcon();
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public boolean configureTask(RunConfiguration runConfiguration, CamelBeforeRunTask beforeRunTask) {
        return runConfiguration instanceof CamelRunConfiguration;
    }

    @Override
    public boolean canExecuteTask(RunConfiguration runConfiguration, CamelBeforeRunTask beforeRunTask) {
        return runConfiguration instanceof CamelRunConfiguration;
    }

    @Override
    public boolean executeTask(DataContext dataContext, RunConfiguration runConfiguration, ExecutionEnvironment executionEnvironment, CamelBeforeRunTask camelBeforeRunTask) {
        final Semaphore targetDone = new Semaphore();
        final List<Boolean> results = new ArrayList<>();

        final Project project = executionEnvironment.getProject();

        CamelRunConfiguration camelConfiguration = (CamelRunConfiguration) runConfiguration;

        Module[] modules = camelConfiguration.getModules();

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
