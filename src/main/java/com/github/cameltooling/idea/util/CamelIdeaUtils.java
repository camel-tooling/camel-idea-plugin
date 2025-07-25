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

import com.github.cameltooling.idea.extension.CamelIdeaUtilsExtension;
import com.github.cameltooling.idea.reference.endpoint.CamelEndpoint;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.ElementPattern;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Utility methods to work with Camel related {@link com.intellij.psi.PsiElement} elements.
 * <p/>
 * This class is only for Camel related IDEA APIs. If you need only IDEA APIs then use {@link IdeaUtils} instead.
 */
@Service
public final class CamelIdeaUtils implements Disposable {

    public static final String[] CAMEL_FILE_EXTENSIONS = {"java", "xml", "yaml", "yml"};
    public static final String BEAN_INJECT_ANNOTATION = "org.apache.camel.BeanInject";
    public static final String PROPERTY_INJECT_ANNOTATION = "org.apache.camel.PropertyInject";
    public static final String PROPERTY_PLACEHOLDER_START_TAG = "{{";
    public static final String PROPERTY_PLACEHOLDER_END_TAG = "}}";
    public static final Pattern PROPERTY_PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{([^}]*)}}");

    private final List<CamelIdeaUtilsExtension> enabledExtensions;

    private CamelIdeaUtils() {
        enabledExtensions = Arrays.stream(CamelIdeaUtilsExtension.EP_NAME.getExtensions())
            .filter(CamelIdeaUtilsExtension::isExtensionEnabled)
            .toList();
    }

    public static CamelIdeaUtils getService() {
        return ApplicationManager.getApplication().getService(CamelIdeaUtils.class);
    }

    /**
     * Is the given file a file containing Camel route or route configuration
     */
    public boolean isCamelFile(PsiFile file) {
        return enabledExtensions.stream()
                .anyMatch(extension -> extension.isCamelFile(file));
    }


    /**
     * Is the given element from the start of a Camel route, eg <tt>from</tt>, ot &lt;from&gt;.
     */
    public boolean isCamelRouteStart(PsiElement element) {
        return enabledExtensions.stream()
            .anyMatch(extension -> extension.isCamelRouteStart(element));
    }

    /**
     * For java methods tries to find if element is inside a camel route start expression,
     * otherwise delegates to {@link CamelIdeaUtils#isCamelRouteStart(PsiElement)}.
     */
    public boolean isCamelRouteStartExpression(PsiElement element) {
        return enabledExtensions.stream()
            .anyMatch(extension -> extension.isCamelRouteStartExpression(element));
    }

    /**
     * Indicates whether the given element can be marked with a line marker.
     * @param element the element to check.
     * @return {@code true} if it can be marked, {@code false} otherwise.
     */
    public boolean isCamelLineMarker(PsiElement element) {
        return enabledExtensions.stream()
            .anyMatch(extension -> extension.isCamelLineMarker(element));
    }

    public boolean isInsideCamelRoute(PsiElement element, boolean excludeRouteStart) {
        return enabledExtensions.stream()
            .anyMatch(extension -> extension.isInsideCamelRoute(element, excludeRouteStart));
    }

    /**
     * Is the given element a language of a Camel DSL, eg <tt>simple</tt> or &lt;simple&gt;, <tt>log</tt> or &lt;log&gt;.
     *
     * @param element  the element
     * @param language the language such as simple, jsonpath
     */
    public boolean isCamelExpression(@NotNull PsiElement element, @NotNull String language) {
        return enabledExtensions.stream()
            .anyMatch(extension -> extension.isCamelExpression(element, language));
    }

    /**
     * Is the given element a language of a Camel route, eg <tt>simple</tt>, ot &lt;simple&gt;
     *
     * @param element  the element
     * @param language the language such as simple, jsonpath
     */
    public boolean isCamelExpressionUsedAsPredicate(@NotNull PsiElement element, @NotNull String language) {
        return enabledExtensions.stream()
            .anyMatch(extension -> extension.isCamelExpressionUsedAsPredicate(element, language));
    }

    /**
     * Is the given element from a consumer endpoint used in a route from a <tt>from</tt>, <tt>fromF</tt>,
     * <tt>interceptFrom</tt>, or <tt>pollEnrich</tt> pattern.
     */
    public boolean isConsumerEndpoint(PsiElement element) {
        return enabledExtensions.stream()
            .anyMatch(extension -> extension.isConsumerEndpoint(element));
    }

    /**
     * Is the given element from a producer endpoint used in a route from a <tt>to</tt>, <tt>toF</tt>,
     * <tt>interceptSendToEndpoint</tt>, <tt>wireTap</tt>, or <tt>enrich</tt> pattern.
     */
    public boolean isProducerEndpoint(PsiElement element) {
        return enabledExtensions.stream()
            .anyMatch(extension -> extension.isProducerEndpoint(element));
    }

    /**
     * Could an endpoint uri be present at this location?
     */
    public boolean isPlaceForEndpointUri(PsiElement element) {
        return enabledExtensions.stream()
            .anyMatch(extension -> extension.isPlaceForEndpointUri(element));
    }

    /**
     * Is the given element from a method call named <tt>fromF</tt> or <tt>toF</tt>, or <tt>String.format</tt> which supports the
     * {@link String#format(String, Object...)} syntax and therefore we need special handling.
     */
    public boolean isFromStringFormatEndpoint(PsiElement element) {
        return enabledExtensions.stream()
            .anyMatch(extension -> extension.isFromStringFormatEndpoint(element));
    }

    /**
     * Is the class a Camel expression class
     *
     * @param clazz  the class
     * @return <tt>true</tt> if its a Camel expression class, <tt>false</tt> otherwise.
     */
    public boolean isCamelExpressionOrLanguage(PsiClass clazz) {
        if (clazz == null) {
            return false;
        }
        String fqn = clazz.getQualifiedName();
        if ("org.apache.camel.Expression".equals(fqn)
            || "org.apache.camel.Predicate".equals(fqn)
            || "org.apache.camel.model.language.ExpressionDefinition".equals(fqn)
            || "org.apache.camel.builder.ExpressionClause".equals(fqn)) {
            return true;
        }
        // try implements first
        for (PsiClassType ct : clazz.getImplementsListTypes()) {
            PsiClass resolved = ct.resolve();
            if (isCamelExpressionOrLanguage(resolved)) {
                return true;
            }
        }
        // then fallback as extends
        for (PsiClassType ct : clazz.getExtendsListTypes()) {
            PsiClass resolved = ct.resolve();
            if (isCamelExpressionOrLanguage(resolved)) {
                return true;
            }
        }
        // okay then go up and try super
        return isCamelExpressionOrLanguage(clazz.getSuperClass());
    }

    /**
     * Certain elements should be skipped for endpoint validation, such as ActiveMQ brokerURL property and others.
     */
    public boolean skipEndpointValidation(PsiElement element) {
        return enabledExtensions.stream()
            .anyMatch(extension -> extension.skipEndpointValidation(element));
    }

    /**
     * Whether the element can be accepted for the annator or inspection.
     * <p/>
     * Some elements are too complex structured which we cannot support such as complex programming structures to concat string values together.
     *
     * @param element the element
     * @return <tt>true</tt> to accept, <tt>false</tt> to skip
     */
    public boolean acceptForAnnotatorOrInspection(PsiElement element) {
        return enabledExtensions.stream()
            .anyMatch(extension -> extension.acceptForAnnotatorOrInspection(element));
    }

    /**
     * @return Resolve the bean {@link PsiClass} from the specified element or return null
     */
    public PsiClass getBean(PsiElement element) {
        return enabledExtensions.stream()
            .map(c -> c.getBeanClass(element))
            .filter(Objects::nonNull)
            .findFirst().orElse(null);
    }

    /**
     * @return the bean {@link PsiElement} for the specified element
     */
    public PsiElement getBeanPsiElement(PsiElement element) {
        return enabledExtensions.stream()
            .map(c -> c.getPsiElementForCamelBeanMethod(element))
            .filter(Objects::nonNull)
            .findFirst().orElse(null);
    }

    public boolean isAnnotatedWithHandler(PsiMethod psiMethod) {
        return  Arrays.stream(psiMethod.getAnnotations()).anyMatch(a -> "org.apache.camel.Handler".equals(a.getQualifiedName()));
    }

    public boolean isExtendingRouteBuild(PsiClass clazz) {
        final PsiClass[] interfaces = clazz.getSupers();
        return Arrays.stream(interfaces)
            .anyMatch(c -> "org.apache.camel.RoutesBuilder".equals(c.getQualifiedName()));
    }

    public List<PsiElement> findEndpointUsages(Module module, CamelEndpoint endpoint) {
        return findEndpointUsages(module, endpoint::baseUriMatches);
    }

    public List<PsiElement> findEndpointUsages(Module module, Predicate<String> uriCondition) {
        return enabledExtensions.stream()
            .map(e -> e.findEndpointUsages(module, uriCondition))
            .flatMap(List::stream)
            .toList();
    }

    public List<PsiElement> findEndpointDeclarations(Module module, CamelEndpoint endpoint) {
        return findEndpointDeclarations(module, endpoint::baseUriMatches);
    }

    public List<PsiElement> findEndpointDeclarations(Module module, Predicate<String> uriCondition) {
        return enabledExtensions.stream()
            .map(e -> e.findEndpointDeclarations(module, uriCondition))
            .flatMap(List::stream)
            .toList();
    }

    @Override
    public void dispose() {
        //noop
    }

    /**
     * Process the source PSI file within the given text range.
     *
     * @param source          The source PSI file.
     * @param rangeToReformat The range within which the changes can be made.
     * @param settings        The root code style settings to use.
     *
     * @return The updated text range after the changes.
     */
    public TextRange processText(PsiFile source, TextRange rangeToReformat, CodeStyleSettings settings) {
        for (CamelIdeaUtilsExtension extension : enabledExtensions) {
            if (extension.isCamelFile(source)) {
                return extension.processText(source, rangeToReformat, settings);
            }
        }
        return rangeToReformat;
    }

    public @NotNull List<ElementPattern<? extends PsiElement>> getAllowedPropertyPlaceholderPatterns() {
        return enabledExtensions.stream()
            .flatMap(e -> e.getAllowedPropertyPlaceholderPsiPatterns().stream())
            .toList();
    }

}
