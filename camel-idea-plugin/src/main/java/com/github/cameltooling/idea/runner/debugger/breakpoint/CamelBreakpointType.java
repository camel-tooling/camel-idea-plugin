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
import com.github.cameltooling.idea.util.IdeaUtils;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
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

import java.util.Arrays;
import java.util.List;

public class CamelBreakpointType extends XLineBreakpointType<XBreakpointProperties> {
    private static List<String> parentTagNames = Arrays.asList(new String[] {"setProperty", "setBody", "setHeader", "choice"});
    private static List<String> canBreakpointAt = Arrays.asList(
        new String[] {
            "setProperty",
            "setBody",
            "setHeader",
            "log",
            "to",
            "toD",
            "choice",
            "when",
            "throwException",
            "filter",
            "enrich",
            "wireTap",
            "removeHeader",
            "removeProperty"
        }
    );

    //private static List<String> nonFunctionalJavaNames = Arrays.asList(new String[] {"routeConfigurationId","setBody","setProperty","choice"});

    protected CamelBreakpointType() {
        super("camel", "Camel Breakpoints");
    }

    @Override
    public boolean canPutAt(@NotNull VirtualFile file, int line, @NotNull Project project) {
        final Document document = FileDocumentManager.getInstance().getDocument(file);
        if (document != null) {
            final PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
            if (psiFile != null) {
                final boolean isXml = IdeaUtils.getService().isXmlLanguage(psiFile);
                final boolean isJava = IdeaUtils.getService().isJavaLanguage(psiFile);
                XSourcePosition position = XDebuggerUtil.getInstance().createPosition(file, line);

                if (isXml) {
                    XmlTag tag = IdeaUtils.getService().getXmlTagAt(project, position);
                    if (tag != null) {
                        String parentName = tag.getParentTag() != null ? tag.getParentTag().getLocalName() : tag.getLocalName(); //NPE here
                        //The tag must be inside "route"
                        // and cannot be "from"
                        // and cannot be a child of "setBody", "setHeader", "setProperty", or "choice"
                        return canBreakpointAt.contains(tag.getLocalName()) && !parentTagNames.contains(parentName);
/*
                        return CamelIdeaUtils.getService().isInsideCamelRoute(tag, true)
                                && !"from".equalsIgnoreCase(tag.getLocalName())
                                && !parentTagNames.contains(parentName);
*/
                    }
                } else if (isJava) {
                    //Get PsiClass
                    PsiElement psiElement = XDebuggerUtil.getInstance().findContextElement(file, position.getOffset(), project, false);
                    /*return CamelIdeaUtils.getService().isInsideCamelRoute(psiElement, true)
                            && !psiElement.getText().startsWith("from");*/
                    return canBreakpointAt.contains(psiElement.getText());
                }
            }
        }
        return false;
    }

    @Override
    public XDebuggerEditorsProvider getEditorsProvider(@NotNull XLineBreakpoint<XBreakpointProperties> breakpoint, @NotNull Project project) {
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
    public XBreakpointProperties createBreakpointProperties(@NotNull VirtualFile virtualFile, int line) {
        return null;

    }
/*
        final PsiFile psiFile = PsiDocumentManager.getInstance(getP).getPsiFile(document);
        if (psiFile != null) {
            final boolean isXml = IdeaUtils.getService().isXmlLanguage(psiFile);
            final boolean isJava = IdeaUtils.getService().isJavaLanguage(psiFile);
        return null;
    }
*/


}
