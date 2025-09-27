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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import com.github.cameltooling.idea.reference.endpoint.CamelEndpoint;
import com.github.cameltooling.idea.util.CamelIdeaUtils;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.ElementManipulator;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.util.IncorrectOperationException;
import org.apache.commons.lang3.time.StopWatch;
import org.jetbrains.annotations.NotNull;

/**
 * A reference from usage of a direct endpoint (e.g. <to uri="direct:abc"/>) to its declaration (e.g. <from uri="direct:abc"/>)
 */
public class DirectEndpointReference extends PsiPolyVariantReferenceBase<PsiElement> {

    private static final Logger LOG = Logger.getInstance(DirectEndpointReference.class);

    private final CamelEndpoint endpoint;

    public DirectEndpointReference(PsiElement element, CamelEndpoint endpoint) {
        super(element, TextRange.from(getStartOffset(element), endpoint.getBaseUri().length()));
        this.endpoint = endpoint;
    }

    private static int getStartOffset(PsiElement element) {
        return element.getText().startsWith("\"") ? 1 : 0;
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        return CachedValuesManager.getCachedValue(myElement, () -> {
            StopWatch sw = StopWatch.createStarted();
            var result = Optional.ofNullable(ModuleUtilCore.findModuleForPsiElement(myElement))
                    .map(module -> CamelIdeaUtils.getService().findEndpointDeclarations(module, endpoint))
                    .map(this::wrapAsDirectEndpointPsiElements)
                    .map(PsiElementResolveResult::createResults)
                    .orElse(ResolveResult.EMPTY_ARRAY);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Resolving direct endpoint " + endpoint.getUri() + " references took " + sw.formatTime());
            }
            return CachedValueProvider.Result.create(result, PsiModificationTracker.MODIFICATION_COUNT);
        });
    }

    @NotNull
    private List<DirectEndpointPsiElement> wrapAsDirectEndpointPsiElements(List<PsiElement> endpointDeclarations) {
        return endpointDeclarations.stream()
            .map(e -> new DirectEndpointPsiElement(e, endpoint))
            .collect(Collectors.toList());
    }

    @Override
    public PsiElement handleElementRename(@NotNull String newElementName) throws IncorrectOperationException {
        ElementManipulator<PsiElement> manipulator = ElementManipulators.getManipulator(myElement);
        return manipulator.handleContentChange(myElement, endpoint.getNameTextRange().shiftRight(getStartOffset(myElement)), newElementName);
    }

}
