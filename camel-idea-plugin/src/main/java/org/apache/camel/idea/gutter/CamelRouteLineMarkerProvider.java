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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.swing.*;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiLiteralValue;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiPolyadicExpression;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiVariable;
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
import org.apache.camel.idea.util.IdeaUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Provider that adds the Camel icon in the gutter when it detects a Camel route.
 */
public class CamelRouteLineMarkerProvider extends RelatedItemLineMarkerProvider {

    private static final Logger LOG = Logger.getInstance(CamelRouteLineMarkerProvider.class);

    private static final String[] JAVA_ROUTE_CALL = new String[]{"to", "toF", "toD", "enrich", "wireTap"};
    private static final String[] XML_ROUTE_CALL = new String[]{"to", "toD", "enrich", "wireTap"};

    public IdeaUtils getIdeaUtils() {
        return ServiceManager.getService(IdeaUtils.class);
    }

    @Override
    protected void collectNavigationMarkers(@NotNull PsiElement element,
                                            Collection<? super RelatedItemLineMarkerInfo> result) {
        //TODO: remove this when IdeaUtils.isFromJavaMethodCall will be fixed
        if (element.getLanguage().equals(JavaLanguage.INSTANCE)
            && !(element instanceof PsiLiteralExpression || isCamelRouteStartIdentifierExpression(element))) {
            return;
        }
        boolean showIcon = getCamelPreferenceService().isShowCamelIconInGutter();
        boolean camelPresent = ServiceManager.getService(element.getProject(), CamelService.class).isCamelPresent();

        if (!showIcon || !camelPresent) {
            return;
        }

        // must be in valid file
        boolean validFile = getIdeaUtils().isFromFileType(element, CamelIdeaUtils.CAMEL_FILE_EXTENSIONS);
        if (!validFile) {
            return;
        }

        //skip the PsiLiteralExpression that are not the first operand of PsiPolyadicExpression to avoid having multiple gutter icons
        // on the same PsiPolyadicExpression
        if (element instanceof PsiLiteralExpression) {
            if (isPartOfPolyadicExpression((PsiLiteralExpression) element)) {
                if (!element.isEquivalentTo(getFirstExpressionFromPolyadicExpression((PsiLiteralExpression) element))) {
                    return;
                }
            }
        }

        Icon icon = getCamelPreferenceService().getCamelIcon();

        if (getCamelIdeaUtils().isCamelRouteStartExpression(element)) {

            // evaluate the targets lazy
            NotNullLazyValue<Collection<? extends PsiElement>> targets = new NotNullLazyValue<Collection<? extends PsiElement>>() {
                @NotNull
                @Override
                protected Collection<PsiElement> compute() {
                    List<PsiElement> routeDestinationForPsiElement = findRouteDestinationForPsiElement(element);
                    // Add identifier references as navigation target
                    resolvedIdentifier(element)
                        .map(PsiElement::getNavigationElement)
                        .ifPresent(routeDestinationForPsiElement::add);
                    return routeDestinationForPsiElement;
                }
            };

            NavigationGutterIconBuilder<PsiElement> builder =
                NavigationGutterIconBuilder.create(icon)
                    .setTargets(targets)
                    .setTooltipText("Camel route")
                    .setPopupTitle("Navigate to " + findRouteFromElement(element))
                    .setAlignment(GutterIconRenderer.Alignment.RIGHT)
                    .setCellRenderer(new GutterPsiElementListCellRenderer());
            result.add(builder.createLineMarkerInfo(element));
        }
    }

    /**
     * Returns true it the give element is an identifier inside a route start expression.
     *
     * @param element the element to evaluate
     * @return true it the give element is an identifier inside a route start expression
     */
    private boolean isCamelRouteStartIdentifierExpression(@NotNull PsiElement element) {
        return resolvedIdentifier(element)
            .filter(resolved -> isRouteStartIdentifier((PsiIdentifier) element, resolved))
            .isPresent();
    }

    private CamelPreferenceService getCamelPreferenceService() {
        return ServiceManager.getService(CamelPreferenceService.class);
    }

    /**
     * Return the resolved reference to a {@link PsiVariable} or {@link PsiMethod}
     * for the given element if it is a {@link PsiIdentifier}.
     *
     * @param element the element to resolve
     * @return an {@link Optional} representing the resolved reference or empty
     *         if reference could not be resolved.
     */
    private Optional<PsiElement> resolvedIdentifier(PsiElement element) {
        if (element instanceof PsiIdentifier) {
            return Optional.ofNullable(element.getParent())
                .map(PsiElement::getReference)
                .map(PsiReference::resolve)
                .filter(resolved -> PsiVariable.class.isInstance(resolved) || PsiMethod.class.isInstance(resolved));
        }
        return Optional.empty();
    }

    private boolean isRouteStartIdentifier(PsiIdentifier identifier, PsiElement resolvedIdentifier) {
        // Eval methods from parent PsiMethodCallExpression to exclude start route method (from)
        PsiElement element = identifier;
        if (resolvedIdentifier instanceof PsiMethod) {
            element = PsiTreeUtil.getParentOfType(element, PsiMethodCallExpression.class);
        }
        if (element == null) {
            return false;
        }
        return getCamelIdeaUtils().isCamelRouteStartExpression(element);
    }

    /**
     * Returns the Camel route from a PsiElement
     *
     * @param element the element
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

        if (element instanceof PsiIdentifier) {
            PsiIdentifier id = (PsiIdentifier) element;
            String text = id.getText();
            if (text != null) {
                return text;
            }
        }

        // Only variables can be resolved?
        Optional<PsiVariable> variable = resolvedIdentifier(element)
            .filter(PsiVariable.class::isInstance)
            .map(PsiVariable.class::cast);
        if (variable.isPresent()) {
            // Try to resolve variable and recursive search route
            return variable.map(PsiVariable::getInitializer)
                .map(this::findRouteFromElement)
                .orElse(null);
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
        LOG.debug("Finding Camel routes which is calling me: " + startElement);

        List<PsiElement> psiElements = new ArrayList<>();
        String route = findRouteFromElement(startElement);

        if (route == null || route.isEmpty()) {
            return psiElements;
        }

        PsiSearchHelper helper = PsiSearchHelper.SERVICE.getInstance(startElement.getProject());
        //get the component name and search only using that
        String componentName = route.split(":")[0];

        helper.processElementsWithWord((psiElement, offsetInElement) -> {
            LOG.debug("processElementsWithWord: " + psiElement + " with offset: " + offsetInElement);
            if (psiElement instanceof XmlToken) {
                PsiElement xmlElement = findXMLElement(route, (XmlToken) psiElement);
                if (xmlElement != null) {
                    psiElements.add(xmlElement);
                }
            } else if (psiElement instanceof PsiLiteralExpression) {
                PsiElement javaElement = findJavaElement(route, psiElement);
                if (javaElement != null) {
                    psiElements.add(javaElement);
                }
            } else if (psiElement instanceof PsiIdentifier) {
                PsiElement javaElement = findJavaElement(route, psiElement);
                if (javaElement != null) {
                    psiElements.add(javaElement);
                } else {
                    // use alternative lookup for identifier
                    resolvedIdentifier(psiElement).ifPresent(psiElements::add);
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
     * @param psiElement the {@link PsiElement} that might contain the complete route definition
     * @return the {@link PsiElement} that contains the exact match of the Camel route, null if there is no exact match
     */
    private PsiElement findJavaElement(String route, PsiElement psiElement) {
        Object value;
        if (psiElement instanceof PsiLiteralValue) {
            value = ((PsiLiteralValue) psiElement).getValue();
        } else {
            value = psiElement.getText();
        }
        if (route.equals(value)) {
            //the method 'to' is a PsiIdentifier not a PsiMethodCallExpression because it's part of method invocation chain
            PsiMethodCallExpression methodCall = PsiTreeUtil.getParentOfType(psiElement, PsiMethodCallExpression.class);
            if (methodCall != null) {
                if (Arrays.stream(JAVA_ROUTE_CALL).anyMatch(s -> s.equals(methodCall.getMethodExpression().getReferenceName()))) {
                    return psiElement;
                }
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
            if (Arrays.stream(XML_ROUTE_CALL).anyMatch(s -> s.equals(PsiTreeUtil.getParentOfType(psiElement, XmlTag.class).getLocalName()))) {
                if (psiElement.getText().equals(route)) {
                    return psiElement;
                }
            }
        }
        return null;
    }

    /**
     * Determines if the given {@link PsiLiteralExpression} is part of a {@link PsiPolyadicExpression}
     *
     * @param psiLiteralExpression the {@link PsiLiteralExpression} to be checked
     * @return true if it's part of {@link PsiPolyadicExpression}, false otherwise
     */
    private static boolean isPartOfPolyadicExpression(PsiLiteralExpression psiLiteralExpression) {
        return PsiTreeUtil.getParentOfType(psiLiteralExpression, PsiPolyadicExpression.class) != null;
    }

    /**
     * Returns the first operand from a {@link PsiPolyadicExpression}
     *
     * @param psiLiteralExpression the {@link PsiLiteralExpression} that is part of a {@link PsiPolyadicExpression}
     * @return the first {@link PsiExpression} if the given {@link PsiLiteralExpression} is part of a {@link PsiPolyadicExpression}, null otherwise
     */
    @Nullable
    private static PsiExpression getFirstExpressionFromPolyadicExpression(PsiLiteralExpression psiLiteralExpression) {
        if (isPartOfPolyadicExpression(psiLiteralExpression)) {
            PsiPolyadicExpression psiPolyadicExpression = PsiTreeUtil.getParentOfType(psiLiteralExpression, PsiPolyadicExpression.class);
            if (psiPolyadicExpression != null) {
                return psiPolyadicExpression.getOperands()[0];
            }
        }
        return null;
    }

    private static CamelIdeaUtils getCamelIdeaUtils() {
        return ServiceManager.getService(CamelIdeaUtils.class);
    }
}
