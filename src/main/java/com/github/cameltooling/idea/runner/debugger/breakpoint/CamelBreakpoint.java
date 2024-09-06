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

import com.intellij.psi.PsiElement;
import com.intellij.xdebugger.XSourcePosition;

public class CamelBreakpoint {
    private final String breakpointId;
    private final PsiElement breakpointTag;
    private final XSourcePosition position;

    public CamelBreakpoint(String breakpointId, PsiElement breakpointTag, XSourcePosition position) {
        this.breakpointId = breakpointId;
        this.breakpointTag = breakpointTag;
        this.position = position;
    }

    public String getBreakpointId() {
        return breakpointId;
    }

    public PsiElement getBreakpointTag() {
        return breakpointTag;
    }

    public XSourcePosition getXSourcePosition() {
        return position;
    }

}
