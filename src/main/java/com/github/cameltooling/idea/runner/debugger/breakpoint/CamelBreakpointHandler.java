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
package com.github.cameltooling.idea.runner.debugger.breakpoint;

import com.github.cameltooling.idea.runner.debugger.CamelDebuggerSession;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.breakpoints.XBreakpointHandler;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import org.jetbrains.annotations.NotNull;

public class CamelBreakpointHandler extends XBreakpointHandler<XLineBreakpoint<XBreakpointProperties<?>>> {
    private final CamelDebuggerSession debuggerSession;

    public CamelBreakpointHandler(CamelDebuggerSession debuggerSession) {
        super(CamelBreakpointType.class);
        this.debuggerSession = debuggerSession;
    }

    @Override
    public void registerBreakpoint(@NotNull XLineBreakpoint<XBreakpointProperties<?>> xBreakpoint) {
        debuggerSession.addBreakpoint(xBreakpoint);
    }

    @Override
    public void unregisterBreakpoint(@NotNull XLineBreakpoint<XBreakpointProperties<?>> xBreakpoint, boolean temporary) {
        debuggerSession.removeBreakpoint(xBreakpoint);
    }

    /**
     * Indicates whether the given project has Camel breakpoints.
     *
     * @param project the project to check
     * @return {@code true} if at least one Camel breakpoint exists, {@code false} otherwise or
     * the method is called .
     */
    public static boolean hasBreakpoints(Project project) {
        return ApplicationManager.getApplication().runReadAction(
            (Computable<Boolean>) () -> !XDebuggerManager.getInstance(project)
                .getBreakpointManager()
                .getBreakpoints(CamelBreakpointType.class)
                .isEmpty()
        );
    }
}
