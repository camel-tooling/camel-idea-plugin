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
package com.github.cameltooling.idea;

import com.github.cameltooling.idea.service.CamelService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Main entry point of the Camel IDEA plugin which is a project listener,
 * to detect if Camel is present when a project is loaded.
 */
public class CamelPluginStartup implements ProjectManagerListener, ModuleRootListener, ProjectActivity {

    @Override
    public void projectClosed(@NotNull Project project) {
        getCamelIdeaService(project).reset();
    }

    @Override
    public void rootsChanged(@NotNull ModuleRootEvent event) {
        // A roots change has been detected, Camel could have been removed, added or modified thus a new project scan
        // is needed
        scanForCamelProject(event.getProject());
    }

    /**
     * Scan the given project to know whether it is a Camel project or not and if it is, initialize it consequently.
     * @param project the project to scan
     */
    private static void scanForCamelProject(@NotNull Project project) {
        // rebuild list of libraries because the dependencies may have changed
        getCamelIdeaService(project).scanForCamelProject();
    }

    private static CamelService getCamelIdeaService(Project project) {
        return project.getService(CamelService.class);
    }

    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        scanForCamelProject(project);
        return null;
    }
}
