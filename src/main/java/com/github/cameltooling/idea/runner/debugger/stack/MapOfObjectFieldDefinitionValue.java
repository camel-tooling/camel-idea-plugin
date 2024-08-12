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
import com.github.cameltooling.idea.runner.debugger.CamelDebuggerTarget;
import com.intellij.util.PlatformIcons;
import com.intellij.xdebugger.frame.XCompositeNode;
import com.intellij.xdebugger.frame.XValue;
import com.intellij.xdebugger.frame.XValueChildrenList;
import com.intellij.xdebugger.frame.XValueNode;
import com.intellij.xdebugger.frame.XValuePlace;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import java.util.Map;


public class MapOfObjectFieldDefinitionValue extends XValue {

    private final CamelDebuggerTarget target;
    private final CamelDebuggerSession session;
    private final Map<String, CamelMessageInfo.Value[]> values;
    private final Icon icon;

    public MapOfObjectFieldDefinitionValue(CamelDebuggerTarget target, CamelDebuggerSession session, Map<String, CamelMessageInfo.Value[]> values, Icon icon) {
        this.target = target;
        this.session = session;
        this.values = values;
        this.icon = icon;
    }

    @Override
    public void computePresentation(@NotNull XValueNode xValueNode, @NotNull XValuePlace xValuePlace) {
        xValueNode.setPresentation(icon, "", "", !values.isEmpty());
    }

    @Override
    public void computeChildren(@NotNull XCompositeNode node) {
        final XValueChildrenList list = new XValueChildrenList();
        for (Map.Entry<String, CamelMessageInfo.Value[]> entry : values.entrySet()) {
            for (CamelMessageInfo.Value nextValue : entry.getValue()) {
                String key = entry.getKey();
                list.add(
                    key, new ObjectFieldDefinitionValue(target, key, session, nextValue, PlatformIcons.PROPERTY_ICON)
                );
            }
        }
        node.addChildren(list, false);
        super.computeChildren(node);
    }
}
