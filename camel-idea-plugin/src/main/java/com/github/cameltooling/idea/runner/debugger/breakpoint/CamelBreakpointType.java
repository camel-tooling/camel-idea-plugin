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

import com.github.cameltooling.idea.runner.debugger.CamelDebuggerEditorsProvider;
import com.github.cameltooling.idea.util.CamelIdeaUtils;
import com.github.cameltooling.idea.util.IdeaUtils;
import com.github.cameltooling.idea.util.XmlUtils;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlTag;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.intellij.xdebugger.breakpoints.XLineBreakpointType;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import java.util.Arrays;
import java.util.List;

public class CamelBreakpointType extends XLineBreakpointType<XBreakpointProperties<?>> {

    private static final List<String> NO_BREAKPOINTS_AT = Arrays.asList(
        "routes",
        "route",
        "from",
        "routeConfiguration",
        "routeConfigurationId",
        "exception",
        "handled",
        "simple",
        "constant",
        "datasonnet",
        "groovy",
        "steps",
        "name",
        "constant",
        "uri");

    protected CamelBreakpointType() {
        super("camel", "Camel Breakpoints");
    }

    @Override
    public boolean canPutAt(@NotNull VirtualFile file, int line, @NotNull Project project) {
        XSourcePosition position = XDebuggerUtil.getInstance().createPosition(file, line);
        String eipName = "";

        final Document document = FileDocumentManager.getInstance().getDocument(file);
        final PsiFile psiFile = document != null ? PsiDocumentManager.getInstance(project).getPsiFile(document) : null;

        switch (file.getFileType().getName()) {
        case "XML":
            XmlTag tag = XmlUtils.getXmlTagAt(project, position);
            if (tag == null) {
                return false;
            }
            eipName = tag.getLocalName();
            break;
        case "JAVA":
            PsiElement psiElement = XDebuggerUtil.getInstance().findContextElement(file, position.getOffset(), project, false);
            if (psiElement == null) {
                return false;
            }
            eipName = psiElement.getText();
            break;
        case "YAML":
            YAMLKeyValue keyValue = IdeaUtils.getYamlKeyValueAt(project, position);
            if (keyValue != null) {
                eipName = keyValue.getKeyText();
            }
            break;
        default: // noop
        }

        try {
            return !NO_BREAKPOINTS_AT.contains(eipName) && CamelIdeaUtils.getService().isCamelFile(psiFile);
        } catch (IndexNotReadyException e) {
            DumbService.getInstance(project).showDumbModeNotification("Toggling breakpoints is disabled while " + ApplicationNamesInfo.getInstance().getProductName() + " is updating indices");
            return false;
        }
    }

    @Override
    public XDebuggerEditorsProvider getEditorsProvider(@NotNull XLineBreakpoint<XBreakpointProperties<?>> breakpoint,
                                                       @NotNull Project project) {
        final XSourcePosition position = breakpoint.getSourcePosition();
        if (position == null) {
            return null;
        }

        final PsiFile file = PsiManager.getInstance(project).findFile(position.getFile());
        if (file == null) {
            return null;
        }

        return new CamelDebuggerEditorsProvider();
    }

    @Nullable
    @Override
    public XBreakpointProperties<?> createBreakpointProperties(@NotNull VirtualFile virtualFile, int line) {
        return new CamelBreakpointProperties(virtualFile.getFileType());
    }

    static class CamelBreakpointProperties extends XBreakpointProperties<CamelBreakpointProperties> {
        private FileType myFileType;

        CamelBreakpointProperties(FileType fileType) {
            myFileType = fileType;
        }

        @Override
        public @Nullable CamelBreakpointProperties getState() {
            return this;
        }

        @Override
        public void loadState(@NotNull CamelBreakpointProperties state) {
            myFileType = state.myFileType;
        }

        public FileType getFileType() {
            return myFileType;
        }
    }
}
