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
package com.github.cameltooling.idea.runner.debugger;

import java.util.List;

import com.intellij.execution.Executor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.project.Project;
import com.intellij.task.ExecuteRunConfigurationTask;
import com.intellij.task.ProjectTask;
import com.intellij.task.ProjectTaskRunner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * {@code CamelProjectTaskPatcher} is only used to patch the given project task if needed.
 */
public class CamelProjectTaskPatcher extends ProjectTaskRunner {
    @Override
    public boolean canRun(@NotNull ProjectTask projectTask) {
        return projectTask instanceof ExecuteRunConfigurationTask;
    }

    @Override
    public @Nullable ExecutionEnvironment createExecutionEnvironment(@NotNull Project project,
                                                                     @NotNull ExecuteRunConfigurationTask runTask,
                                                                     @Nullable Executor executor) {
        // This an ugly hack to be able to get the executor that is needed to patch the task
        // The idea is to intercept the call to createExecutionEnvironment to get the executor
        // then call the following ProjectTaskRunners with the patched task
        CamelDebuggerPatcher.patchExecuteRunConfigurationTask(executor, runTask);
        List<ProjectTaskRunner> extensionList = ProjectTaskRunner.EP_NAME.getExtensionList();
        int index = extensionList.indexOf(this);
        for (int i = index + 1; i < extensionList.size(); i++) {
            ProjectTaskRunner runner = extensionList.get(i);
            if (runner.canRun(project, runTask)) {
                return runner.createExecutionEnvironment(project, runTask, executor);
            }
        }
        return null;
    }
}
