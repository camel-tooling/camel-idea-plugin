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
package com.github.cameltooling.idea.util;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import com.github.cameltooling.idea.extension.CamelIdeaUtilsExtension;
import com.github.cameltooling.idea.reference.blueprint.BeanReference;
import com.github.cameltooling.idea.reference.blueprint.model.ReferenceableBeanId;
import com.github.cameltooling.idea.reference.blueprint.model.ReferencedClass;
import com.github.cameltooling.idea.reference.endpoint.CamelEndpoint;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiType;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;

/**
 * Utility methods to work with Camel related {@link com.intellij.psi.PsiElement} elements.
 * <p/>
 * This class is only for Camel related IDEA APIs. If you need only IDEA APIs then use {@link IdeaUtils} instead.
 */
public final class CamelIdeaUtils implements Disposable {

    public static final String[] CAMEL_FILE_EXTENSIONS = {"java", "xml"};
    public static final String BEAN_INJECT_ANNOTATION = "org.apache.camel.BeanInject";

    private final List<CamelIdeaUtilsExtension> enabledExtensions;

    private CamelIdeaUtils() {
        enabledExtensions = Arrays.stream(CamelIdeaUtilsExtension.EP_NAME.getExtensions())
            .filter(CamelIdeaUtilsExtension::isExtensionEnabled)
            .collect(Collectors.toList());
    }

    public static CamelIdeaUtils getService() {
        return ServiceManager.getService(CamelIdeaUtils.class);
    }

    public boolean isBeanDeclaration(@NotNull PsiElement element) {
        return enabledExtensions.stream().anyMatch(e -> e.isBeanDeclaration(element));
    }

    public boolean isPartOfCamelContext(@NotNull PsiElement element) {
        return enabledExtensions.stream().anyMatch(e -> e.isPartOfCamelContext(element));
    }

    public Optional<ReferenceableBeanId> findReferenceableBeanId(Module module, String id) {
        return enabledExtensions.stream()
            .map(e -> e.findReferenceableBeanId(module, id))
            .filter(Objects::nonNull)
            .findAny();
    }

    public List<ReferenceableBeanId> findReferenceableBeanIds(Module module, Predicate<String> idCondition) {
        return enabledExtensions.stream()
            .map(e -> e.findReferenceableBeanIds(module, idCondition))
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }

    public List<ReferenceableBeanId> findReferenceableBeanIdsByType(Module module, PsiType expectedBeanType) {
        return findReferenceableBeanIdsByType(module, id -> true, expectedBeanType);
    }

    public List<ReferenceableBeanId> findReferenceableBeanIdsByType(Module module,
                                                                    Predicate<String> idCondition,
                                                                    PsiType expectedBeanType) {
        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(module.getProject());
        return findReferenceableBeanIds(module, idCondition).stream()
                .filter(ref -> {
                    PsiClass psiClass = resolveToPsiClass(ref);
                    if (psiClass != null) {
                        PsiClassType beanType = elementFactory.createType(psiClass);
                        return expectedBeanType.isAssignableFrom(beanType);
                    }
                    return false;
                })
                .collect(Collectors.toList());
    }

    public Optional<PsiClass> findReferencedBeanClass(PsiElement element) {
        return findBeanReference(element)
            .map(this::resolveToPsiClass);
    }

    private PsiClass resolveToPsiClass(ReferenceableBeanId ref) {
        return (PsiClass) Optional.of(ref)
                .map(ReferenceableBeanId::getReferencedClass)
                .map(ReferencedClass::getReference)
                .map(PsiReference::resolve)
                .filter(e -> e instanceof PsiClass)
                .orElse(null);
    }

    private Optional<ReferenceableBeanId> findBeanReference(PsiElement element) {
        PsiReference[] references = element.getReferences();
        return Arrays.stream(references)
            .filter(r -> r instanceof BeanReference)
            .map(r -> ((BeanReference) r).findReferenceableBeanId().orElse(null))
            .filter(Objects::nonNull)
            .findAny();
    }

    public Optional<PsiType> findExpectedBeanTypeAt(PsiElement location) {
        return enabledExtensions.stream()
                .map(e -> e.findExpectedBeanTypeAt(location))
                .filter(Objects::nonNull)
                .findAny();
    }

    public PsiClass getPropertyBeanClass(XmlTag propertyTag) {
        XmlTag beanTag = propertyTag.getParentTag();
        if (beanTag != null && CamelIdeaUtils.getService().isBeanDeclaration(beanTag)) {
            IdeaUtils ideaUtils = IdeaUtils.getService();
            return ideaUtils.findAttributeValue(beanTag, "class")
                .map(ideaUtils::findClassReference)
                .map(Optional::get)
                .map(ideaUtils::resolveJavaClassReference)
                .orElse(null);
        }
        return null;
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
     * Certain elements should be skipped for endpoint validation such as ActiveMQ brokerURL property and others.
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
            .collect(Collectors.toList());
    }

    public List<PsiElement> findEndpointDeclarations(Module module, CamelEndpoint endpoint) {
        return findEndpointDeclarations(module, endpoint::baseUriMatches);
    }

    public List<PsiElement> findEndpointDeclarations(Module module, Predicate<String> uriCondition) {
        return enabledExtensions.stream()
            .map(e -> e.findEndpointDeclarations(module, uriCondition))
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }

    private IdeaUtils getIdeaUtils() {
        return ServiceManager.getService(IdeaUtils.class);
    }

    @Override
    public void dispose() {

    }
}
