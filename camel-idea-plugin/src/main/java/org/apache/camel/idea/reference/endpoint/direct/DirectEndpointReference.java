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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.ElementManipulator;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.util.IncorrectOperationException;
import org.apache.camel.idea.reference.endpoint.CamelEndpoint;
import org.apache.camel.idea.util.CamelIdeaUtils;
import org.jetbrains.annotations.NotNull;

/**
 * A reference from usage of a direct endpoint (e.g. <to uri="direct:abc"/>) to its declaration (e.g. <from uri="direct:abc"/>)
 */
public class DirectEndpointReference extends PsiPolyVariantReferenceBase<PsiElement> {

    private final CamelEndpoint endpoint;

    public DirectEndpointReference(PsiElement element, CamelEndpoint endpoint) {
        super(element, TextRange.from(1, endpoint.getUri().length()));
        this.endpoint = endpoint;
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        return Optional.ofNullable(ModuleUtilCore.findModuleForPsiElement(myElement))
            .map(module -> CamelIdeaUtils.getService().findEndpointDeclarations(module, endpoint))
            .map(this::wrapAsDirectEndpointPsiElements)
            .map(PsiElementResolveResult::createResults)
            .orElse(ResolveResult.EMPTY_ARRAY);
    }

    @NotNull
    private List<DirectEndpointPsiElement> wrapAsDirectEndpointPsiElements(List<PsiElement> endpointDeclarations) {
        return endpointDeclarations.stream()
            .map(e -> new DirectEndpointPsiElement(e, endpoint))
            .collect(Collectors.toList());
    }

    @NotNull
    @Override
    public String getCanonicalText() {
        return getValue();
    }

    @Override
    public PsiElement handleElementRename(@NotNull String newElementName) throws IncorrectOperationException {
        ElementManipulator<PsiElement> manipulator = ElementManipulators.getManipulator(myElement);
        return manipulator.handleContentChange(myElement, endpoint.getNameTextRange().shiftRight(1), newElementName);
    }

}
