/**
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
package org.apache.camel.idea.gutter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.swing.*;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.impl.source.tree.java.PsiLiteralExpressionImpl;
import com.intellij.psi.impl.source.xml.XmlTagImpl;
import com.intellij.psi.search.PsiSearchHelper;
import com.intellij.psi.search.UsageSearchContext;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlElementType;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlToken;
import org.apache.camel.idea.service.CamelPreferenceService;
import org.apache.camel.idea.service.CamelService;
import org.apache.camel.idea.util.CamelIdeaUtils;
import org.apache.camel.idea.util.CamelRouteSearchScope;
import org.jetbrains.annotations.NotNull;

import static org.apache.camel.idea.util.IdeaUtils.isFromFileType;

/**
 * Provider that adds the Camel icon in the gutter when it detects a Camel route.
 */
public class CamelRouteLineMarkerProvider extends RelatedItemLineMarkerProvider {


    @Override
    protected void collectNavigationMarkers(@NotNull PsiElement element,
                                            Collection<? super RelatedItemLineMarkerInfo> result) {
        //TODO: remove this when IdeaUtils.isFromJavaMethodCall will be fixed
        if (element.getLanguage().equals(JavaLanguage.INSTANCE) && !(element instanceof PsiLiteralExpression)) {
            return;
        }
        boolean showIcon = getCamelPreferenceService().isShowCamelIconInGutter();
        boolean camelPresent = ServiceManager.getService(element.getProject(), CamelService.class).isCamelPresent();

        if (!showIcon || !camelPresent) {
            return;
        }

        // must be in valid file
        boolean validFile = isFromFileType(element, CamelIdeaUtils.CAMEL_FILE_EXTENSIONS);
        if (!validFile) {
            return;
        }

        Icon icon = getCamelPreferenceService().getCamelIcon();

        if (CamelIdeaUtils.isCamelRouteStart(element)) {
            NavigationGutterIconBuilder<PsiElement> builder =
                    NavigationGutterIconBuilder.create(icon)
                            .setTargets(findRouteDestinationForPsiElement(element))
                            .setTooltipText("Camel route")
                            .setPopupTitle("Navigate to " + findRouteFromElement(element))
                            .setAlignment(GutterIconRenderer.Alignment.RIGHT)
                            .setCellRenderer(new GutterPsiElementListCellRenderer());
            result.add(builder.createLineMarkerInfo(element));
        }
    }

    private CamelPreferenceService getCamelPreferenceService() {
        return ServiceManager.getService(CamelPreferenceService.class);
    }

    /**
     * Returns the Camel route from a PsiElement
     * @param element
     * @return the String route or null if there nothing can be found
     */
    private String findRouteFromElement(PsiElement element) {
        XmlTag xml = PsiTreeUtil.getParentOfType(element, XmlTag.class);
        if (xml != null) {
            return ((XmlTagImpl) element.getParent()).getAttributeValue("uri");
        }

        if (element instanceof PsiLiteralExpressionImpl) {
            return ((PsiLiteralExpressionImpl) element).getValue() == null ? null : ((PsiLiteralExpressionImpl) element).getValue().toString();
        }

        return null;
    }

    /**
     * Searches in the project all the route destinations for the given {@link PsiElement}.
     * Example for Java routes: for 'from("file:inbox")' returns all elements that matches 'to("file:inbox")'
     * <p>
     * Since Intellij API supports only searches with one keyword the search is made using just the Camel component name and then further refined.
     * </p>
     *
     * @param startElement the {@link PsiElement} that contains the definition for a route start
     * @return a list of {@link PsiElement} with all the route ends.
     */
    private List<PsiElement> findRouteDestinationForPsiElement(PsiElement startElement) {
        List<PsiElement> psiElements = new ArrayList<>();
        String route = findRouteFromElement(startElement);

        if (route == null || route.isEmpty()) {
            return psiElements;
        }
        PsiSearchHelper helper = PsiSearchHelper.SERVICE.getInstance(startElement.getProject());
        //get the component name and search only using that
        String componentName = route.split(":")[0];

        helper.processElementsWithWord((psiElement, offsetInElement) -> {
            if (psiElement instanceof XmlToken) {
                PsiElement xmlElement = findXMLElement(route, (XmlToken) psiElement);
                if (xmlElement != null) {
                    psiElements.add(xmlElement);
                }
            }
            if (psiElement instanceof PsiLiteralExpression) {
                PsiElement javaElement = findJavaElement(route, (PsiLiteralExpression) psiElement);
                if (javaElement != null) {
                    psiElements.add(javaElement);
                }
            }
            return true;
        }, new CamelRouteSearchScope(), componentName, UsageSearchContext.ANY, false);

        return psiElements;
    }

    /**
     * Further refine search in order to match the exact Java Camel route.
     * Checks if the given {@link PsiElement} contains a 'to' method that points to the give route.
     *
     * @param route      the complete Camel route to search for
     * @param psiElement the {@link PsiLiteralExpression} that might contain the complete route definition
     * @return the {@link PsiElement} that contains the exact match of the Camel route, null if there is no exact match
     */
    private PsiElement findJavaElement(String route, PsiLiteralExpression psiElement) {
        if (route.equals(psiElement.getValue())) {
            //the method 'to' is a PsiIdentifier not a PsiMethodCallExpression because it's part of method invocation chain
            PsiMethodCallExpression methodCall = PsiTreeUtil.getParentOfType(psiElement, PsiMethodCallExpression.class);
            if (methodCall != null && "to".equals(methodCall.getMethodExpression().getReferenceName())) {
                return psiElement;
            }
        }
        return null;
    }

    /**
     * Further refine search in order to match the exact XML Camel route.
     *
     * @param route      the complete Camel route to search for
     * @param psiElement the {@link PsiElement} that might contain the complete route definition
     * @return the {@link PsiElement} that contains the exact match of the Camel route
     */
    private PsiElement findXMLElement(String route, XmlToken psiElement) {
        if (psiElement.getTokenType() == XmlElementType.XML_ATTRIBUTE_VALUE_TOKEN) {
            if ("to".equals(PsiTreeUtil.getParentOfType(psiElement, XmlTag.class).getLocalName())) {
                if (psiElement.getText().equals(route)) {
                    return psiElement;
                }
            }
        }
        return null;
    }

}
