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
package com.github.cameltooling.idea.runner.debugger.actions;

import com.github.cameltooling.idea.service.CamelService;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.impl.DebuggerSupport;
import com.intellij.xdebugger.impl.actions.DebuggerActionHandler;
import com.intellij.xdebugger.impl.actions.XDebuggerActionBase;
import org.jetbrains.annotations.NotNull;

public class CamelSetValueAction extends XDebuggerActionBase {
    private final CamelSetValueActionHandler setValueActionHandler = new CamelSetValueActionHandler();

    public CamelSetValueAction() {
        super(true);
    }

    @Override
    @NotNull
    protected DebuggerActionHandler getHandler(@NotNull final DebuggerSupport debuggerSupport) {
        return setValueActionHandler;
    }

    @Override
    public void update(@NotNull final AnActionEvent event) {
        super.update(event);
        if (event.getPresentation().isEnabledAndVisible()) {
            final Project project = event.getProject();
            if (project == null) {
                return;
            }
            final CamelService camelService = project.getService(CamelService.class);
            event.getPresentation().setEnabled(camelService.isCamelPresent());
            event.getPresentation().setVisible(camelService.isCamelPresent());
        }
    }
}

