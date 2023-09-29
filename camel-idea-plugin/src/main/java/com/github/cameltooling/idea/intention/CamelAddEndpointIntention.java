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
package com.github.cameltooling.idea.intention;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.swing.*;

import com.github.cameltooling.idea.service.CamelCatalogService;
import com.github.cameltooling.idea.service.CamelPreferenceService;
import com.github.cameltooling.idea.service.CamelService;
import com.github.cameltooling.idea.util.CamelIdeaUtils;
import com.github.cameltooling.idea.util.IdeaUtils;
import com.intellij.codeInsight.intention.LowPriorityAction;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.impl.ImaginaryEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.IPopupChooserBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Iconable;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaToken;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.IncorrectOperationException;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.tooling.model.ComponentModel;
import org.apache.camel.tooling.model.JsonMapper;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;


/**
 * Popup intention (ctrl + enter) to add action to add a Camel endpoint by choosing among all the known
 * Camel components available via the classpath.
 */
public class CamelAddEndpointIntention extends PsiElementBaseIntentionAction implements Iconable, LowPriorityAction {

    /**
     * The logger.
     */
    private static final Logger LOG = Logger.getInstance(CamelAddEndpointIntention.class);

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
        if (editor == null || editor instanceof ImaginaryEditor) {
            return;
        }
        // filter libraries to only be Camel libraries
        Set<String> artifacts = project.getService(CamelService.class).getLibraries();

        // find the camel component from those libraries
        boolean consumerOnly = CamelIdeaUtils.getService().isConsumerEndpoint(element);
        List<String> names = findCamelComponentNamesInArtifact(artifacts, consumerOnly, project);

        // no camel endpoints then exit
        if (names.isEmpty()) {
            return;
        }

        // show popup to choose the component
        JBPopupFactory.getInstance().createPopupChooserBuilder(names)
            .setAdText(names.size() + " components")
            .setTitle("Add Camel Endpoint")
            .setItemChosenCallback(line -> {
                int pos = editor.getCaretModel().getCurrentCaret().getOffset();
                if (pos > 0) {
                    // must run this as write action because we change the source code
                    WriteCommandAction.writeCommandAction(project, element.getContainingFile()).run(() -> {
                        String text = line + ":";
                        editor.getDocument().insertString(pos, text);
                        editor.getCaretModel().moveToOffset(pos + text.length());
                    });
                }
            })
            .createPopup()
            .showInBestPositionFor(editor);
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        if (project.getService(CamelService.class).isCamelProject()) {
            // special for xml
            XmlTag xml = PsiTreeUtil.getParentOfType(element, XmlTag.class);
            if (xml != null) {
                // special check for poll enrich where we add the endpoint on a child node (camel expression)
                XmlTag parent = xml.getParentTag();
                if (parent != null && parent.getLocalName().equals("pollEnrich")) {
                    return true;
                }
            }

            String text = null;

            final IdeaUtils ideaUtils = IdeaUtils.getService();
            // special for java token
            if (element instanceof PsiJavaToken) {
                // if its a string literal
                PsiJavaToken token = (PsiJavaToken) element;
                if (token.getTokenType() == JavaTokenType.STRING_LITERAL) {
                    text = ideaUtils.getInnerText(token.getText());
                }
            } else {
                // should be a literal element and therefore dont fallback to generic
                text = ideaUtils.extractTextFromElement(element, false, false, true);
            }

            return text != null && text.trim().isEmpty();
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

    @Override
    public Icon getIcon(@IconFlags int flags) {
        return CamelPreferenceService.getService().getCamelIcon();
    }

    private static List<String> findCamelComponentNamesInArtifact(Set<String> artifactIds, boolean consumerOnly, Project project) {
        List<String> names = new ArrayList<>();

        CamelCatalog camelCatalog = project.getService(CamelCatalogService.class).get();
        for (String name : camelCatalog.findComponentNames()) {
            String json = camelCatalog.componentJSonSchema(name);
            if (json == null) {
                LOG.debug(String.format("The JSon schema metadata of the component %s could not be found", name));
                continue;
            }
            ComponentModel model = JsonMapper.generateComponentModel(json);
            if (artifactIds.contains(model.getArtifactId())) {
                boolean onlyConsume = model.isConsumerOnly();
                boolean onlyProduce = model.isProducerOnly();
                boolean both = !onlyConsume && !onlyProduce;

                if (both || consumerOnly && onlyConsume || !consumerOnly && onlyProduce) {
                    names.add(name);
                }
            }
        }

        // sort
        Collections.sort(names);

        return names;
    }

}
