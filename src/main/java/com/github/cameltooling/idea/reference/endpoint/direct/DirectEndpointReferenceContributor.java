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
package com.github.cameltooling.idea.reference.endpoint.direct;

import com.github.cameltooling.idea.reference.endpoint.CamelEndpoint;
import com.github.cameltooling.idea.reference.endpoint.CamelEndpointPsiReferenceProvider;
import com.github.cameltooling.idea.util.CamelIdeaUtils;
import com.github.cameltooling.idea.util.StringUtils;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.patterns.ElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

/**
 * Create a reference from the usage of a 'referencable' endpoint (e.g. <to uri="direct:abc"/>)
 * to its declaration (e.g. <from uri="direct:abc"/>).
 *
 * Referencable endpoints are those that behave like 'direct' endpoints, meaning they have a clear
 * connection between their usage in 'from' and 'to' statements, and we want to connect them via IDEA's reference system.
 */
//TODO: maybe rename 'Direct...' to 'Referencable...'
//TODO: for some (like file, or direct-vm) it should not underline and report that the opposing endpoint is not found
public class DirectEndpointReferenceContributor extends PsiReferenceContributor {

    private static final Logger LOG = Logger.getInstance(DirectEndpointReferenceContributor.class);

    private static final Set<String> REFERENCABLE_COMPONENTS = Set.of(
            "direct", "direct-vm", "seda", "file"
    );

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
                    CamelEndpoint endpoint = new CamelEndpoint(endpointUri);
                    if (endpoint.getBaseUri().contains(CamelIdeaUtils.PROPERTY_PLACEHOLDER_START_TAG)) {
                        return PsiReference.EMPTY_ARRAY;
                    }
                    PsiReference reference;
                    if (CamelIdeaUtils.getService().isCamelRouteStart(element)) {
                        reference = new DirectEndpointStartSelfReference(element, endpoint);
                    } else {
                        reference = new DirectEndpointReference(element, endpoint);
                    }
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Creating reference " + reference + " for element " + element + " with parent " + element.getParent().getParent().getText());
                    }
                    return new PsiReference[] {reference};
                }

                @Override
                protected boolean isEndpoint(String endpointUri) {
                    String component = StringUtils.asComponentName(endpointUri);
                    return component != null && REFERENCABLE_COMPONENTS.contains(component);
                }
            };
    }

}
