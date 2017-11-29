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
package org.apache.camel.idea.extension;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.psi.PsiElement;


/**
 * Extension point for CamelIdeaUtils for handling specific plugin language elements
 */
public interface CamelIdeaUtilsExtension {

    ExtensionPointName<CamelIdeaUtilsExtension> EP_NAME = ExtensionPointName.create("org.apache.camel.CamelIdeaUtilsSupport");

    /**
     * Is the given element from the start of a Camel route, eg <tt>from</tt>, ot &lt;from&gt;.
     */
    boolean isCamelRouteStart(PsiElement element);

    /**
     * Is the given element a simple of a Camel DSL, eg <tt>simple</tt> or &lt;simple&gt;, <tt>log</tt> or &lt;log&gt;.
     */
    boolean isCamelSimpleExpression(PsiElement element);

    /**
     * Is the given element a simple of a Camel route, eg <tt>simple</tt>, ot &lt;simple&gt;
     */
    boolean isCameSimpleExpressionUsedAsPredicate(PsiElement element);

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

}
