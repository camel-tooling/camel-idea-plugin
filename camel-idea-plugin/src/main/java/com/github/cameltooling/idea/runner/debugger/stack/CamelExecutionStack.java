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
package com.github.cameltooling.idea.runner.debugger.stack;

import com.intellij.icons.AllIcons;
import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XStackFrame;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CamelExecutionStack extends XExecutionStack {
    private List<XStackFrame> frames;

    public CamelExecutionStack(@NotNull String displayName, XStackFrame... frame) {
        super(displayName, AllIcons.Debugger.ThreadSuspended);
        this.frames = Arrays.asList(frame);
    }

    @Nullable
    @Override
    public XStackFrame getTopFrame() {
        return frames.get(0);
    }

    @Override
    public void computeStackFrames(int firstFrameIndex, XStackFrameContainer container) {
        if (firstFrameIndex <= frames.size()) {
            container.addStackFrames(frames.subList(firstFrameIndex, frames.size()), true);
        } else {
            container.addStackFrames(Collections.<XStackFrame>emptyList(), true);
        }
    }
}