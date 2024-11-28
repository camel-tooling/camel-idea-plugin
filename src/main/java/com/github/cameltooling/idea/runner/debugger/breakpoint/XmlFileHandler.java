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

import com.github.cameltooling.idea.util.IdeaUtils;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.xdebugger.XSourcePosition;

public class XmlFileHandler implements FileTypeHandler {
    @Override
    public String getEipName(Project project, XSourcePosition position, VirtualFile file) {
        XmlTag tag = IdeaUtils.getXmlTagAt(project, position);
        return tag != null ? tag.getLocalName() : null;
    }
}