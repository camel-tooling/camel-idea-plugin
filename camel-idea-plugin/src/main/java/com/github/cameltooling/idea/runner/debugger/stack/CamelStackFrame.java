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
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.psi.xml.XmlTag;
import com.intellij.ui.ColoredTextContainer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator;
import com.intellij.xdebugger.frame.XCompositeNode;
import com.intellij.xdebugger.frame.XStackFrame;
import com.intellij.xdebugger.frame.XValueChildrenList;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CamelStackFrame extends XStackFrame {
    private final Project project;

    private CamelDebuggerSession session;
    private CamelMessageInfo camelMessageInfo;
    //    @Nullable
//    private ObjectFieldDefinition exceptionThrown;

    public CamelStackFrame(@NotNull Project project, @NotNull CamelDebuggerSession session, CamelMessageInfo camelMessageInfo) {
        this.session = session;
        this.camelMessageInfo = camelMessageInfo;
        this.project = project;
    }

/*
    public CamelStackFrame(@NotNull Project project, @NotNull CamelDebuggerSession session, CamelMessageInfo camelMessageInfo)
    {
        this(project, session, camelMessageInfo, null);
    }

    public CamelStackFrame(@NotNull Project project, CamelDebuggerSession session, CamelMessageInfo muleMessageInfo, @Nullable ObjectFieldDefinition exceptionThrown)
    {
        this.session = session;
        this.camelMessageInfo = camelMessageInfo;
        this.exceptionThrown = exceptionThrown;
        this.tag = MuleConfigUtils.getTagAt(project, muleMessageInfo.getMessageProcessorInfo().getPath());
        this.position = MuleConfigUtils.createPositionByElement(tag);
        this.project = project;
    }
*/

    @Nullable
    @Override
    public XSourcePosition getSourcePosition() {
        return this.camelMessageInfo.getXSourcePosition();
    }

    public void customizePresentation(@NotNull ColoredTextContainer component) {
        XmlTag tag = this.camelMessageInfo.getTag();
        final String mp = StringUtils.isNotBlank(tag.getNamespacePrefix()) ? tag.getNamespacePrefix() + ":" + tag.getLocalName() : tag.getLocalName();
        component.append(mp, SimpleTextAttributes.REGULAR_ATTRIBUTES);
    }

    @Nullable
    @Override
    public XDebuggerEvaluator getEvaluator() {
        //return new MuleScriptEvaluator(session);
        return null;
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
/*
        children.add("Message Processor", new MessageProcessorInfoValue(this.session, this.muleMessageInfo.getMessageProcessorInfo()));
        children.add("Payload", new ObjectFieldDefinitionValue(this.session, this.muleMessageInfo.getPayloadDefinition(), AllIcons.Debugger.Value));
        if (exceptionThrown != null) {
            children.add("Exception", new ObjectFieldDefinitionValue(this.session, exceptionThrown, AllIcons.General.Error));
        }
        children.add("Flow Vars", new MapOfObjectFieldDefinitionValue(this.session, this.muleMessageInfo.getInvocationProperties(), AllIcons.Nodes.Parameter));
        children.add("Session Properties", new MapOfObjectFieldDefinitionValue(this.session, this.muleMessageInfo.getSessionProperties(), AllIcons.Nodes.Parameter));
        children.add("Inbound Properties", new MapOfObjectFieldDefinitionValue(this.session, this.muleMessageInfo.getInboundProperties(), AllIcons.Nodes.Parameter));
        children.add("OutboundProperties", new MapOfObjectFieldDefinitionValue(this.session, this.muleMessageInfo.getOutboundProperties(), AllIcons.Nodes.Parameter));
*/
        node.addChildren(children, true);
    }
}

