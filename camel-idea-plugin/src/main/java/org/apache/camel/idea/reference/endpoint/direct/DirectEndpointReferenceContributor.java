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
package org.apache.camel.idea.reference.endpoint.direct;

import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.util.ProcessingContext;
import org.apache.camel.idea.reference.endpoint.CamelEndpoint;
import org.apache.camel.idea.reference.endpoint.CamelEndpointPsiReferenceProvider;
import org.apache.camel.idea.util.CamelIdeaUtils;
import org.jetbrains.annotations.NotNull;

public class DirectEndpointReferenceContributor extends PsiReferenceContributor {

    public static final String DIRECT_ENDPOINT_PREFIX = "direct:";

    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        CamelEndpointPsiReferenceProvider provider = createProvider();
        registrar.registerReferenceProvider(PsiJavaPatterns.literalExpression(), provider);
        registrar.registerReferenceProvider(XmlPatterns.xmlAttributeValue("uri"), provider);
    }

    @NotNull
    private CamelEndpointPsiReferenceProvider createProvider() {
        return new CamelEndpointPsiReferenceProvider() {
                @Override
                protected PsiReference[] getEndpointReferencesByElement(String endpointUri, PsiElement element, ProcessingContext context) {
                    CamelEndpoint endpoint = new CamelEndpoint(endpointUri);
                    PsiReference reference;
                    if (CamelIdeaUtils.getService().isCamelRouteStart(element)) {
                        reference = new DirectEndpointStartSelfReference(element, endpoint);
                    } else {
                        reference = new DirectEndpointReference(element, endpoint);
                    }
                    return new PsiReference[] {reference};
                }

                @Override
                protected boolean isEndpoint(String endpointUri) {
                    return endpointUri.startsWith(DIRECT_ENDPOINT_PREFIX);
                }
            };
    }

}
