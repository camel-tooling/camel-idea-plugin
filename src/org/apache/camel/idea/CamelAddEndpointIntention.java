/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.idea;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.swing.*;

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupChooserBuilder;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaToken;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.util.IncorrectOperationException;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.idea.model.ComponentModel;
import org.apache.camel.idea.model.ModelHelper;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.xml.CommonXmlStrings.QUOT;

public class CamelAddEndpointIntention extends PsiElementBaseIntentionAction {

    private static final CamelCatalog camelCatalog = new DefaultCamelCatalog(true);

    // TODO: Add Camel icon

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
        // gather all libraries (JARs) from the project/classpath
        Set<Library> processedLibraries = new HashSet<>();

        // TODO: this should be cached/faster maybe?
        Module[] modules = ModuleManager.getInstance(project).getModules();
        for (Module module : modules) {
            ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
            OrderEntry[] orderEntries = moduleRootManager.getOrderEntries();
            for (OrderEntry orderEntry : orderEntries) {
                if (orderEntry instanceof LibraryOrderEntry) {
                    LibraryOrderEntry libraryOrderEntry = (LibraryOrderEntry) orderEntry;
                    // skip test scope
                    if (libraryOrderEntry.getScope().isForProductionCompile() || libraryOrderEntry.getScope().isForProductionRuntime()) {
                        final Library library = libraryOrderEntry.getLibrary();
                        if (library == null) {
                            continue;
                        }
                        if (processedLibraries.contains(library)) {
                            continue;
                        }
                        processedLibraries.add(library);
                    }
                }
            }
        }

        // filter libraries to only be Camel libraries
        Set<String> artifacts = new LinkedHashSet<>();
        for (Library lib : processedLibraries) {
            String name = lib.getName();
            if (name != null && name.startsWith("Maven: org.apache.camel:")) {
                name = name.substring(24);
                String artifactId = name.substring(0, name.indexOf(":"));
                artifacts.add(artifactId);
            }
        }

        // find the camel component from those libraries
        List<String> names = findCamelComponentsInArtifact(artifacts);

        // no camel endpoints then exit
        if (names.isEmpty()) {
            return;
        }

        // show popup to chose the component
        JList list = new JList(names.toArray(new String[names.size()]));
        PopupChooserBuilder builder = JBPopupFactory.getInstance().createListPopupBuilder(list);
        builder.setAdText(names.size() + " components");
        builder.setTitle("Add Camel Endpoint");
        builder.setItemChoosenCallback(() -> {
            String line = (String) list.getSelectedValue();
            int pos = editor.getCaretModel().getCurrentCaret().getOffset();
            if (pos > 0) {
                // must run this as write action because we change the source code
                new WriteCommandAction(project, element.getContainingFile()) {
                    @Override
                    protected void run(@NotNull Result result) throws Throwable {
                        String text = line + ":";
                        editor.getDocument().insertString(pos, text);
                        editor.getCaretModel().moveToOffset(pos + text.length());
                    }
                }.execute();
            }
        });

        JBPopup popup = builder.createPopup();
        popup.showInBestPositionFor(editor);
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        // if its a string literal
        if (IdeaUtils.isStringLiteral(element)) {
            PsiLiteralExpression literal = (PsiLiteralExpression) element;
            String text = (String) literal.getValue();
            // only be available if the string is empty
            return text == null || text.isEmpty();
        }
        if (IdeaUtils.isJavaTokenLiteral(element)) {
            PsiJavaToken token = (PsiJavaToken) element;
            String text = getInnerText(token);
            return text == null || text.isEmpty();
        }
        return false;
    }

    @NotNull
    @Override
    public String getText() {
        return "Add camel endpoint";
    }

    @Nls
    @NotNull
    @Override
    public String getFamilyName() {
        return "Apache Camel";
    }

    private static List<String> findCamelComponentsInArtifact(Set<String> artifactIds) {
        List<String> names = new ArrayList<>();

        for (String name : camelCatalog.findComponentNames()) {
            String json = camelCatalog.componentJSonSchema(name);
            ComponentModel model = ModelHelper.generateComponentModel(json, false);
            if (artifactIds.contains(model.getArtifactId())) {
                names.add(name);
            }
        }

        // sort
        Collections.sort(names);

        return names;
    }

    /**
     * Code from com.intellij.psi.impl.source.tree.java.PsiLiteralExpressionImpl#getInnerText()
     */
    @Nullable
    private String getInnerText(PsiJavaToken token) {
        String text = token.getText();
        int textLength = text.length();
        if (StringUtil.endsWithChar(text, '\"')) {
            if (textLength == 1) return null;
            text = text.substring(1, textLength - 1);
        } else {
            if (text.startsWith(QUOT) && text.endsWith(QUOT) && textLength > QUOT.length()) {
                text = text.substring(QUOT.length(), textLength - QUOT.length());
            } else {
                return null;
            }
        }
        return text;
    }

}
