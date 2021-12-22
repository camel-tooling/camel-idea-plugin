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
package com.github.cameltooling.idea.runner.debugger.evaluator;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.xdebugger.impl.DebuggerSupport;
import com.intellij.xdebugger.impl.actions.DebuggerActionHandler;
import com.intellij.xdebugger.impl.actions.XDebuggerActionBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.model.MavenArtifact;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.utils.actions.MavenActionUtil;

import java.util.List;

public class CamelEvaluateAction extends XDebuggerActionBase {
    private CamelEvaluateActionHandler evaluateActionHandler = new CamelEvaluateActionHandler();

    CamelEvaluateAction() {
        super(true);
    }

    @Override
    @NotNull
    protected DebuggerActionHandler getHandler(@NotNull final DebuggerSupport debuggerSupport) {
        return evaluateActionHandler;
    }

    @Override
    public void update(@NotNull final AnActionEvent event) {
        super.update(event);
        if (event.getPresentation().isEnabledAndVisible()) {
            //If we are debugging Camel project, this should be enabled - but how do we know it's a Camel project?
            //Find Maven project and see if there's a Camel dependency
            MavenProject mavenProject = MavenActionUtil.getMavenProject(event.getDataContext());
            if (mavenProject != null) {
                List<MavenArtifact> dependencies = mavenProject.getDependencies();

                boolean isCamel = dependencies.stream().anyMatch(mavenArtifact -> mavenArtifact.getArtifactId().equals("camel-main") || mavenArtifact.getArtifactId().equals("camel-spring-boot"));

                event.getPresentation().setEnabled(isCamel);
                event.getPresentation().setVisible(isCamel);
            }
        }
    }
}

