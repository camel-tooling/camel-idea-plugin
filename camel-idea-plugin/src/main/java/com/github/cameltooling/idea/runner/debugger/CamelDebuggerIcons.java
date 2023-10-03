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
package com.github.cameltooling.idea.runner.debugger;

import com.intellij.openapi.util.IconLoader;

import javax.swing.Icon;

public final class CamelDebuggerIcons {
    public static final Icon EVALUATE_EXPRESSION_ICON = IconLoader.getIcon("/META-INF/evaluateCamelExpression.png", CamelDebuggerIcons.class);
    public static final Icon SET_VALUE_ICON = IconLoader.getIcon("/META-INF/setValue.png", CamelDebuggerIcons.class);
    public static final Icon CAMEL_ICON = IconLoader.getIcon("/META-INF/camelPluginIcon.svg", CamelDebuggerIcons.class);


    private CamelDebuggerIcons() {
    }
}
