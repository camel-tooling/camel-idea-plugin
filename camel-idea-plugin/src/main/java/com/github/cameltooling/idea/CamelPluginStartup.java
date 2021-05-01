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
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import org.jetbrains.annotations.NotNull;

/**
 * Main entry point of the Camel IDEA plugin which is a project listener,
 * to detect if Camel is present when a project is loaded.
 */
public class CamelPluginStartup implements ProjectManagerListener {

    @Override
    public void projectOpened(@NotNull Project project) {
        // rebuild list of libraries because the dependencies may have changed
        getCamelIdeaService(project).setCamelPresent(false);
        getCamelIdeaService(project).clearLibraries();

        for (Module module : ModuleManager.getInstance(project).getModules()) {
            getCamelIdeaService(project).scanForCamelProject(project, module);
            // if its a Camel project then scan for additional Camel components
            if (getCamelIdeaService(project).isCamelPresent()) {
                getCamelIdeaService(project).scanForCamelDependencies(project, module);
            }
        }
    }

    @Override
    public void projectClosed(@NotNull Project project) {
        getCamelIdeaService(project).setCamelPresent(false);
        getCamelIdeaService(project).clearLibraries();
    }

    private CamelService getCamelIdeaService(Project project) {
        return ServiceManager.getService(project, CamelService.class);
    }

}
