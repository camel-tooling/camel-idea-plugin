/**
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
package org.apache.camel.idea;

import com.intellij.ProjectTopics;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.ModuleAdapter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import org.apache.camel.idea.util.CamelService;
import org.jetbrains.annotations.NotNull;

/**
 * Listen for changes to Modules ad update the library cached and isCamelPresent
 * <p>
 *     If changes is made to the module settings it will clear the cache do re-scan
 *     of all camel dependencies and update the camel is present status
 * </p>
 * <p>
 *     When the editor is open for the first time and module is added it will
 *     scan for all camel dependencies and update the camel is present status
 * </p>
 */
public class CamelProjectComponent implements ProjectComponent {

    private final Project project;

    CamelProjectComponent(Project project) {
        this.project = project;
    }

    @NotNull
    @Override
    public String getComponentName() {
        return CamelProjectComponent.class.getName();
    }

    @Override
    public void projectOpened() {
    }

    @Override
    public void projectClosed() {
    }

    @Override
    public void initComponent() {
        project.getMessageBus().connect(project).subscribe(ProjectTopics.PROJECT_ROOTS, new ModuleRootListener() {
            @Override
            public void rootsChanged(ModuleRootEvent event) {
                Project project = (Project) event.getSource();
                getCamelIdeaService(project).setCamelPresent(false);
                getCamelIdeaService(project).clearLibraries();

                for (Module module : ModuleManager.getInstance(project).getModules()) {
                    getCamelIdeaService(project).scanForCamelDependencies(module);
                }
            }
        });

        project.getMessageBus().connect(project).subscribe(ProjectTopics.MODULES, new ModuleAdapter() {
            @Override
            public void moduleAdded(@NotNull Project project, @NotNull Module module) {
                getCamelIdeaService(project).scanForCamelDependencies(module);
            }
        });

    }

    @Override
    public void disposeComponent() {

    }

    private CamelService getCamelIdeaService(Project project) {
        return ServiceManager.getService(project, CamelService.class);
    }
}
