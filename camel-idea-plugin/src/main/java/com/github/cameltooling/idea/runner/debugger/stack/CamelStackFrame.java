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


import com.github.cameltooling.idea.runner.debugger.CamelDebuggerSession;
import com.github.cameltooling.idea.runner.debugger.evaluator.CamelExpressionEvaluator;
import com.intellij.debugger.engine.JavaStackFrame;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ColoredTextContainer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator;
import com.intellij.xdebugger.frame.XCompositeNode;
import com.intellij.xdebugger.frame.XStackFrame;
import com.intellij.xdebugger.frame.XValueChildrenList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CamelStackFrame extends XStackFrame {

    private final CamelDebuggerSession session;
    private final CamelMessageInfo camelMessageInfo;


    public CamelStackFrame(@NotNull CamelDebuggerSession session, CamelMessageInfo camelMessageInfo) {
        this.session = session;
        this.camelMessageInfo = camelMessageInfo;
    }

    public CamelMessageInfo getCamelMessageInfo() {
        return camelMessageInfo;
    }

    @Nullable
    @Override
    public XSourcePosition getSourcePosition() {
        return this.camelMessageInfo.getXSourcePosition();
    }

    public void customizePresentation(@NotNull ColoredTextContainer component) {
        StringBuilder nameBuilder = new StringBuilder();

        nameBuilder.append(this.camelMessageInfo.getProcessor());
        nameBuilder.append(" [");
        nameBuilder.append("route=").append(this.camelMessageInfo.getRouteId()).append(",");
        nameBuilder.append("id=").append(this.camelMessageInfo.getProcessorId()).append("]");

        component.append(nameBuilder.toString(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
    }

    @Nullable
    @Override
    public XDebuggerEvaluator getEvaluator() {
        return new CamelExpressionEvaluator(session);
    }

    @Override
    public Object getEqualityObject() {
        return CamelStackFrame.class;
    }


    @Override
    public void computeChildren(@NotNull XCompositeNode node) {
        final XValueChildrenList children = new XValueChildrenList();
        children.add("Body", new ObjectFieldDefinitionValue(this.session, this.camelMessageInfo.getBody(), AllIcons.Debugger.Value));
        children.add("Headers", new MapOfObjectFieldDefinitionValue(this.session, this.camelMessageInfo.getHeaders(), AllIcons.Debugger.Value));

        if (this.camelMessageInfo.getProperties() != null) {
            children.add("Exchange Properties", new MapOfObjectFieldDefinitionValue(this.session, this.camelMessageInfo.getProperties(), AllIcons.Debugger.Value));
        } else {
            children.add("WARNING: ", JavaStackFrame.createMessageNode("Exchange Properties in Debugger are only available in Camel version 3.15 or later", AllIcons.Nodes.WarningMark));
        }
        node.addChildren(children, true);
    }
}

