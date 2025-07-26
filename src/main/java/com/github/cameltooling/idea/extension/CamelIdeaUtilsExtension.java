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
package com.github.cameltooling.idea.extension;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.ElementPattern;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;

import java.util.List;
import java.util.function.Predicate;

/**
 * Extension point for camelIdeaUtils for handling specific plugin language elements
 */
public interface CamelIdeaUtilsExtension {

    ExtensionPointName<CamelIdeaUtilsExtension> EP_NAME = ExtensionPointName.create("org.apache.camel.camelIdeaUtilsSupport");

    /**
     * Is the given file a file containing Camel route or route configuration
     *
     * @param file the file
     */
    boolean isCamelFile(PsiFile file);

    /**
     * Is the given element from the start of a Camel route, eg <tt>from</tt>, ot &lt;from&gt;.
     */
    boolean isCamelRouteStart(PsiElement element);
    boolean isCamelRouteStartExpression(PsiElement element);

    /**
     * Returns a leaf element to be used as a line marker (according to javadoc of {@link com.intellij.codeInsight.daemon.LineMarkerProvider#getLineMarkerInfo(PsiElement)})
     */
    PsiElement getLeafElementForLineMarker(PsiElement element);

    boolean isInsideCamelRoute(PsiElement element, boolean excludeRouteStart);

    /**
     * Indicates whether the given element can be marked with a line marker.
     * @param element the element to check.
     * @return {@code true} if it can be marked, {@code false} otherwise.
     */
    boolean isCamelLineMarker(PsiElement element);

    /**
     * Is the given element a language of a Camel route
     *
     * @param element the element
     * @param language the language such as simple, jsonpath
     */
    boolean isCamelExpression(PsiElement element, String language);

    /**
     * Is the given element a language used in a predicate of a Camel route
     *
     * @param element the element
     * @param language the language such as simple, jsonpath
     */
    boolean isCamelExpressionUsedAsPredicate(PsiElement element, String language);

    /**
     * Is the given element from a consumer endpoint used in a route from a <tt>from</tt>, <tt>fromF</tt>,
     * <tt>interceptFrom</tt>, or <tt>pollEnrich</tt> pattern.
     */
    boolean isConsumerEndpoint(PsiElement element);

    /**
     * Is the given element from a producer endpoint used in a route from a <tt>to</tt>, <tt>toF</tt>,
     * <tt>interceptSendToEndpoint</tt>, <tt>wireTap</tt>, or <tt>enrich</tt> pattern.
     */
    boolean isProducerEndpoint(PsiElement element);

    /**
     * Certain elements should be skipped for endpoint validation such as ActiveMQ brokerURL property and others.
     */
    boolean skipEndpointValidation(PsiElement element);

    /**
     * Is the given element from a method call named <tt>fromF</tt> or <tt>toF</tt>, or <tt>String.format</tt> which supports the
     * {@link String#format(String, Object...)} syntax and therefore we need special handling.
     */
    boolean isFromStringFormatEndpoint(PsiElement element);

    /**
     * Whether the element can be accepted for the annator or inspection.
     * <p/>
     * Some elements are too complex structured which we cannot support such as complex programming structures to concat string values together.
     *
     * @param element the element
     * @return <tt>true</tt> to accept, <tt>false</tt> to skip
     */
    boolean acceptForAnnotatorOrInspection(PsiElement element);

    boolean isExtensionEnabled();

    /**
     * @return Resolve the bean {@link PsiClass} from the specified element or return null
     */
    PsiClass getBeanClass(PsiElement element);

    /**
     * @return the Camel Bean method {@link PsiElement} for the specified element. eg {@code from(..).bean(MyClass.class, "myMethod")}
     */
    PsiElement getPsiElementForCamelBeanMethod(PsiElement element);

    List<PsiElement> findEndpointUsages(Module module, Predicate<String> uriCondition);
    List<PsiElement> findEndpointDeclarations(Module module, Predicate<String> uriCondition);

    /**
     * Could an endpoint uri be present at this location?
     */
    boolean isPlaceForEndpointUri(PsiElement location);

    /**
     * Get psi patterns at which camel's property placeholders ({{propertyName}}) can be present
     */
    List<ElementPattern<? extends PsiElement>> getAllowedPropertyPlaceholderLocations();

    /**
     * Process the source PSI file within the given text range.
     *
     * @param source          The source PSI file.
     * @param rangeToReformat The range within which the changes can be made.
     * @param settings        The root code style settings to use.
     *
     * @return The updated text range after the changes.
     */
    default TextRange processText(PsiFile source, TextRange rangeToReformat, CodeStyleSettings settings) {
        return rangeToReformat;
    }

}
