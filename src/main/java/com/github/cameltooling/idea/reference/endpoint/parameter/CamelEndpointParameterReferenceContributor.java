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
package com.github.cameltooling.idea.reference.endpoint.parameter;

import com.github.cameltooling.idea.reference.endpoint.CamelEndpointPsiReferenceProvider;
import com.github.cameltooling.idea.service.CamelCatalogService;
import com.github.cameltooling.idea.util.CamelIdeaUtils;
import com.github.cameltooling.idea.util.IdeaUtils;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.patterns.ElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.util.ProcessingContext;
import org.apache.camel.catalog.CamelCatalog;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Contributor for {@link EndpointParameterReference}, which is a reference from endpoint uri query parameter to its setter method in the corresponding camel class
 * E.g. from the 'synchronous' substring in endpoint uri "direct:abc?synchronous=true" to {@link org.apache.camel.component.direct.DirectEndpoint#setSynchronous}
 */
public class CamelEndpointParameterReferenceContributor extends PsiReferenceContributor {

    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        List<ElementPattern<? extends PsiElement>> patterns = CamelIdeaUtils.getService().getAllowedEndpointUriLocations();
        if (!patterns.isEmpty()) {
            CamelEndpointPsiReferenceProvider provider = createProvider();
            patterns.forEach(pattern -> {
                registrar.registerReferenceProvider(pattern, provider);
            });
        }
    }

    @NotNull
    private CamelEndpointPsiReferenceProvider createProvider() {
        return new CamelEndpointPsiReferenceProvider() {
            @Override
            protected PsiReference[] getEndpointReferencesByElement(String endpointUri, PsiElement element, ProcessingContext context) {
                Project project = element.getProject();

                CamelCatalogService catalogService = project.getService(CamelCatalogService.class);
                CamelCatalog catalog = catalogService.get();
                String component = catalog.endpointComponentName(endpointUri);
                if (component == null) {
                    return PsiReference.EMPTY_ARRAY;
                }

                Map<String, String> params;
                try {
                    String unescapedUri = IdeaUtils.getService().isXmlLanguage(element) ? StringUtil.unescapeXmlEntities(endpointUri) : endpointUri;
                    params = catalog.endpointProperties(unescapedUri);
                } catch (Exception e) {
                    return PsiReference.EMPTY_ARRAY;
                }

                return createParameterReferences(endpointUri, element, component, params);
            }

            private PsiReference[] createParameterReferences(String endpointUri, PsiElement endpointElement, String component, Map<String, String> params) {
                return params.keySet().stream()
                        .map(param -> {
                            int paramStartIndex = endpointUri.indexOf(param + "=");
                            if (paramStartIndex < 0) {
                                return null;
                            }
                            if (endpointElement.getText().startsWith("\"")) {
                                paramStartIndex++;
                            }
                            return new EndpointParameterReference(endpointElement, component, param, new TextRange(paramStartIndex, paramStartIndex + param.length()));
                        })
                        .filter(Objects::nonNull)
                        .toArray(PsiReference[]::new);
            }

            @Override
            protected boolean isEndpoint(String endpointUri) {
                return true;
            }
        };
    }

}
