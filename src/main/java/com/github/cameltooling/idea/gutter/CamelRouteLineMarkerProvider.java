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
package com.github.cameltooling.idea.gutter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.github.cameltooling.idea.reference.endpoint.direct.DirectEndpointReference;
import com.github.cameltooling.idea.reference.endpoint.direct.DirectEndpointStartSelfReference;
import com.github.cameltooling.idea.service.CamelPreferenceService;
import com.github.cameltooling.idea.service.CamelService;
import com.github.cameltooling.idea.util.CamelIdeaUtils;
import com.github.cameltooling.idea.util.IdeaUtils;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiJavaToken;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiPolyadicExpression;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.search.ProjectScope;
import com.intellij.psi.search.PsiSearchHelper;
import com.intellij.psi.search.UsageSearchContext;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlToken;
import com.intellij.psi.xml.XmlTokenType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;

/**
 * Provider that adds the Camel icon in the gutter when it detects a Camel route.
 */
public class CamelRouteLineMarkerProvider extends RelatedItemLineMarkerProvider {

    private static final Logger LOG = Logger.getInstance(CamelRouteLineMarkerProvider.class);

    private static final String[] JAVA_ROUTE_CALL = {"to", "toF", "toD", "enrich", "wireTap"};
    private static final String[] XML_ROUTE_CALL = {"to", "toD", "enrich", "wireTap"};
    private static final String[] YAML_ROUTE_CALL = {"to", "tod", "toD", "to-d", "enrich", "wireTap", "wire-tap"};

    @Override
    protected void collectNavigationMarkers(@NotNull PsiElement element,
                                            @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {
        CamelPreferenceService preferenceService = CamelPreferenceService.getService();
        if (!preferenceService.isShowCamelIconInGutter() || !element.getProject().getService(CamelService.class).isCamelProject()
            || !isCamelFile(element)) {
            // The Gutter should not be shown, or it is not a Camel project or a Camel file
            return;
        }

        NotNullLazyValue<Collection<PsiElement>> endpointTargets = findEndpointReferences(element);
        NotNullLazyValue<Collection<PsiElement>> manualTargets = findManualReferences(element);

        if (endpointTargets != null || manualTargets != null) {
            NotNullLazyValue<Collection<? extends PsiElement>> combinedTargets = NotNullLazyValue.lazy(() -> {
                List<PsiElement> combined = new ArrayList<>();
                if (endpointTargets != null) {
                    combined.addAll(endpointTargets.get());
                }
                if (manualTargets != null) {
                    combined.addAll(manualTargets.get());
                }
                return combined.stream()
                        .sorted(Comparator.comparing((PsiElement el) -> el.getContainingFile().getName())
                                .thenComparing(IdeaUtils::getLineNumber)
                                .thenComparing(PsiElement::getText))
                        .toList();
            });
            result.add(createLineMarkerInfo(element, combinedTargets));
        }
    }

    private NotNullLazyValue<Collection<PsiElement>> findEndpointReferences(@NotNull PsiElement element) {
        DirectEndpointStartSelfReference startRef = Arrays.stream(element.getReferences())
                .filter(ref -> ref instanceof DirectEndpointStartSelfReference)
                .map(ref -> (DirectEndpointStartSelfReference) ref)
                .findFirst()
                .orElse(null);

        if (startRef != null) {
            return NotNullLazyValue.lazy(() -> {
                PsiElement startElement = startRef.resolve();
                if (startElement == null) {
                    return List.of();
                }
                Collection<PsiReference> references = ReferencesSearch.search(startElement, ProjectScope.getContentScope(element.getProject())).findAll();
                return references.stream()
                        .filter(ref -> ref instanceof DirectEndpointReference)
                        .map(ref -> ref.getElement().getNavigationElement())
                        .toList();
            });
        } else {
            return null;
        }
    }

    private NotNullLazyValue<Collection<PsiElement>> findManualReferences(@NotNull PsiElement element) {
        CamelIdeaUtils camelIdeaUtils = CamelIdeaUtils.getService();
        //TODO: remove this when IdeaUtils.isFromJavaMethodCall will be fixed
        if (camelIdeaUtils.isCamelLineMarker(element) || isCamelRouteStartIdentifierExpression(element)) {

            // let's not duplicate items found via #findEndpointReferences
            if (Arrays.stream(element.getReferences()).anyMatch(ref -> ref instanceof DirectEndpointStartSelfReference)) {
                return null;
            }

            //skip the PsiLiteralExpression that are not the first operand of PsiPolyadicExpression to avoid having multiple gutter icons
            // on the same PsiPolyadicExpression
            if (element instanceof PsiLiteralExpression
                    && isPartOfPolyadicExpression((PsiLiteralExpression) element)
                    && !element.isEquivalentTo(getFirstExpressionFromPolyadicExpression((PsiLiteralExpression) element))) {
                return null;
            }
            // skip if it is a Camel route that is multi-lined, then we only want Camel icon on the first line
            if (element instanceof PsiJavaToken && isPartOfPolyadicExpression((PsiJavaToken) element)) {
                PsiExpression first = getFirstExpressionFromPolyadicExpression((PsiJavaToken) element);
                if (first != null && !element.isEquivalentTo(first.getFirstChild())) {
                    return null;
                }
            }

            if (camelIdeaUtils.isCamelRouteStartExpression(element)) {

                // evaluate the targets lazy
                return NotNullLazyValue.lazy(() -> {
                    List<PsiElement> routeDestinationForPsiElement = findRouteDestinationForPsiElement(element);
                    // Add identifier references as navigation target
                    resolvedIdentifier(element)
                            .map(PsiElement::getNavigationElement)
                            .ifPresent(routeDestinationForPsiElement::add);
                    return routeDestinationForPsiElement;
                });
            }
        }
        return null;
    }

    private @NotNull RelatedItemLineMarkerInfo<PsiElement> createLineMarkerInfo(PsiElement element, NotNullLazyValue<Collection<? extends PsiElement>> targets) {
        return NavigationGutterIconBuilder.create(CamelPreferenceService.getService().getCamelIcon())
                .setTargets(targets)
                .setEmptyPopupText("Could not find any usages of this endpoint")
                .setTooltipText("Camel route")
                .setPopupTitle("Choose Declaration")
                .setAlignment(GutterIconRenderer.Alignment.RIGHT)
                .setTargetRenderer(GutterPsiTargetPresentationRenderer::new)
                .createLineMarkerInfo(CamelIdeaUtils.getService().getLeafElementForLineMarker(element));
    }

    private boolean isCamelFile(@NotNull PsiElement element) {
        return IdeaUtils.getService().isFromFileType(element, CamelIdeaUtils.CAMEL_FILE_EXTENSIONS);
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
                    .filter(resolved -> resolved instanceof PsiVariable || resolved instanceof PsiMethod);
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
        return CamelIdeaUtils.getService().isCamelRouteStartExpression(element);
    }

    /**
     * Returns the Camel route from a PsiElement
     *
     * @param element the element
     * @return the String route or null if nothing could be found
     */
    private String findRouteFromElement(PsiElement element) {
        XmlTag xml = PsiTreeUtil.getParentOfType(element, XmlTag.class);
        if (xml != null) {
            return xml.getAttributeValue("uri");
        }
        // In case of Yaml extract the uri from its child
        YAMLMapping yamlMapping = PsiTreeUtil.getChildOfType(element.getParent(), YAMLMapping.class);
        if (yamlMapping != null) {
            return Optional.ofNullable(yamlMapping.getKeyValueByKey("uri"))
                    .map(YAMLKeyValue::getValueText)
                    .orElse(null);
        }

        if (element instanceof PsiIdentifier) {
            PsiIdentifier id = (PsiIdentifier) element;
            String text = id.getText();
            if (text != null) {
                return text;
            }
        }

        if (element instanceof PsiJavaToken) {
            return element.getText();
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
        String rawRoute = findRouteFromElement(startElement);

        if (rawRoute != null && !rawRoute.isEmpty()) {
            final String route = rawRoute.replace("\"", "");
            PsiSearchHelper helper = PsiSearchHelper.getInstance(startElement.getProject());
            //get the component name and search only using that
            String componentName = route.split(":")[0];

            helper.processElementsWithWord((psiElement, offsetInElement) -> {
                LOG.debug("processElementsWithWord: " + psiElement + " with offset: " + offsetInElement);
                if (psiElement instanceof XmlToken) {
                    PsiElement xmlElement = findXMLElement(route, (XmlToken) psiElement);
                    if (xmlElement != null) {
                        psiElements.add(xmlElement);
                    }
                } else if (psiElement instanceof PsiIdentifier) {
                    PsiElement javaElement = findJavaElement(route, psiElement);
                    if (javaElement != null) {
                        psiElements.add(javaElement);
                    } else {
                        // use alternative lookup for identifier
                        resolvedIdentifier(psiElement).ifPresent(psiElements::add);
                    }
                } else if (psiElement instanceof PsiJavaToken) {
                    PsiElement javaElement = findJavaElement(route, psiElement);
                    if (javaElement != null) {
                        psiElements.add(javaElement);
                    }
                } else if (psiElement instanceof YAMLKeyValue) {
                    PsiElement yamlElement = findYamlElement(route, (YAMLKeyValue) psiElement);
                    if (yamlElement != null) {
                        psiElements.add(psiElement);
                    }
                }
                return true;
            }, new CamelRouteSearchScope(), componentName, UsageSearchContext.ANY, false);
        }
        return psiElements;
    }

    /**
     * Further refine search in order to match the exact YAML Camel route.
     *
     * @param route      the complete Camel route to search for
     * @param keyValue the {@link YAMLKeyValue} that might contain the complete route definition
     * @return the {@link PsiElement} that contains the exact match of the Camel route
     */
    private PsiElement findYamlElement(String route, YAMLKeyValue keyValue) {
        if (Arrays.stream(YAML_ROUTE_CALL).anyMatch(s -> s.equals(keyValue.getKeyText()))) {
            if (route.equals(keyValue.getValueText())) {
                return keyValue;
            }
            final YAMLMapping mapping = PsiTreeUtil.getChildOfType(keyValue, YAMLMapping.class);
            if (mapping != null) {
                final YAMLKeyValue value = mapping.getKeyValueByKey("uri");
                if (value != null && route.equals(value.getValueText())) {
                    return keyValue;
                }
            }
        }
        return null;
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
        Object value = psiElement.getText().replace("\"", "");
        if (route.equals(value)) {
            //the method 'to' is a PsiIdentifier not a PsiMethodCallExpression because it's part of method invocation chain
            PsiMethodCallExpression methodCall = PsiTreeUtil.getParentOfType(psiElement, PsiMethodCallExpression.class);
            if (methodCall != null
                    && Arrays.stream(JAVA_ROUTE_CALL).anyMatch(s -> s.equals(methodCall.getMethodExpression().getReferenceName()))) {
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
        if (psiElement.getTokenType() == XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN
                && Arrays.stream(XML_ROUTE_CALL).anyMatch(s -> s.equals(PsiTreeUtil.getParentOfType(psiElement, XmlTag.class).getLocalName()))
                && psiElement.getText().equals(route)) {
            return psiElement;
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
     * Determines if the given {@link PsiLiteralExpression} is part of a {@link PsiPolyadicExpression}
     *
     * @param psiJavaToken the {@link PsiJavaToken} to be checked
     * @return true if it's part of {@link PsiPolyadicExpression}, false otherwise
     */
    private static boolean isPartOfPolyadicExpression(PsiJavaToken psiJavaToken) {
        return PsiTreeUtil.getParentOfType(psiJavaToken, PsiPolyadicExpression.class) != null;
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

    /**
     * Returns the first operand from a {@link PsiPolyadicExpression}
     *
     * @param psiJavaToken the {@link PsiLiteralExpression} that is part of a {@link PsiPolyadicExpression}
     * @return the first {@link PsiExpression} if the given {@link PsiLiteralExpression} is part of a {@link PsiPolyadicExpression}, null otherwise
     */
    @Nullable
    private static PsiExpression getFirstExpressionFromPolyadicExpression(PsiJavaToken psiJavaToken) {
        if (isPartOfPolyadicExpression(psiJavaToken)) {
            PsiPolyadicExpression psiPolyadicExpression = PsiTreeUtil.getParentOfType(psiJavaToken, PsiPolyadicExpression.class);
            if (psiPolyadicExpression != null) {
                return psiPolyadicExpression.getOperands()[0];
            }
        }
        return null;
    }

}
