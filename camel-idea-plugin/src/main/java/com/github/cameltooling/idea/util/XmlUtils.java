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
package com.github.cameltooling.idea.util;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.lang.xml.XMLLanguage;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleFileIndex;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xml.util.XmlUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Service
public final class XmlUtils {

    public static XmlUtils getService() {
        return ApplicationManager.getApplication().getService(XmlUtils.class);
    }

    @Nullable
    public static XmlTag getXmlTagAt(Project project, XSourcePosition sourcePosition) {
        final VirtualFile file = sourcePosition.getFile();
        final XmlFile xmlFile = (XmlFile) PsiManager.getInstance(project).findFile(file);
        final XmlTag rootTag = xmlFile.getRootTag();
        return findXmlTag(sourcePosition, rootTag);
    }

    private static XmlTag findXmlTag(XSourcePosition sourcePosition, XmlTag rootTag) {
        final XmlTag[] subTags = rootTag.getSubTags();
        for (int i = 0; i < subTags.length; i++) {
            XmlTag subTag = subTags[i];
            final int subTagLineNumber = getLineNumber(sourcePosition.getFile(), subTag);
            if (subTagLineNumber == sourcePosition.getLine()) {
                return subTag;
            } else if (subTagLineNumber > sourcePosition.getLine() && i > 0 && subTags[i - 1].getSubTags().length > 0) {
                return findXmlTag(sourcePosition, subTags[i - 1]);
            }
        }
        if (subTags.length > 0) {
            final XmlTag lastElement = subTags[subTags.length - 1];
            return findXmlTag(sourcePosition, lastElement);
        } else {
            return null;
        }
    }

    public static int getLineNumber(VirtualFile file, XmlTag tag) {
        final int offset = tag.getTextOffset();
        final Document document = FileDocumentManager.getInstance().getDocument(file);
        return offset < document.getTextLength() ? document.getLineNumber(offset) : -1;
    }

    public void iterateXmlDocumentRoots(Module module, Consumer<XmlTag> rootTag) {
        final GlobalSearchScope moduleScope = module.getModuleContentScope();
        final GlobalSearchScope xmlFiles = GlobalSearchScope.getScopeRestrictedByFileTypes(moduleScope, XmlFileType.INSTANCE);

        ModuleFileIndex fileIndex = ModuleRootManager.getInstance(module).getFileIndex();
        fileIndex.iterateContent(f -> {
            if (xmlFiles.contains(f)) {
                PsiFile file = PsiManager.getInstance(module.getProject()).findFile(f);
                if (file instanceof XmlFile xmlFile) {
                    XmlTag root = xmlFile.getRootTag();
                    if (root != null) {
                        rootTag.accept(xmlFile.getRootTag());
                    }
                }
            }
            return true;
        });
    }

    @SuppressWarnings("unchecked")
    public <T> void iterateXmlNodes(XmlTag root, Class<T> nodeClass, Predicate<T> nodeProcessor) {
        XmlUtil.processXmlElementChildren(root, element -> {
            if (nodeClass.isAssignableFrom(element.getClass())) {
                return nodeProcessor.test((T) element);
            }
            return true;
        }, true);
    }

    /**
     * Is the given element from an XML tag with any of the given tag names?
     *
     * @param xml  the xml tag
     * @param methods  xml tag names
     * @return <tt>true</tt> if matched, <tt>false</tt> otherwise
     */
    public boolean isFromXmlTag(@NotNull XmlTag xml, @NotNull String... methods) {
        String name = xml.getLocalName();
        return Arrays.asList(methods).contains(name);
    }

    /**
     * Is the given element from an XML tag with any of the given tag names
     *
     * @param xml  the xml tag
     * @param parentTag a special parent tag name to match first
     * @return <tt>true</tt> if matched, <tt>false</tt> otherwise
     */
    public boolean hasParentXmlTag(@NotNull XmlTag xml, @NotNull String parentTag) {
        XmlTag parent = xml.getParentTag();
        return parent != null && parent.getLocalName().equals(parentTag);
    }

    /**
     * Is the element from XML language
     */
    public boolean isXmlLanguage(PsiElement element) {
        return element != null && PsiUtilCore.getNotAnyLanguage(element.getNode()).is(XMLLanguage.INSTANCE);
    }

//    public boolean hasParentAndFromXmlTag(@NotNull XmlTag xml, @NotNull String parentTag, @NotNull String... methods) {
//        return hasParentXmlTag(xml, parentTag) && IdeaUtils.getService().isFromFileType(xml, methods);
//    }

}
