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
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.ElementManipulator;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This is a reference from direct endpoint start to itself - necessary for find usages functionality
 *
 * @author Rastislav Papp (rastislav.papp@gmail.com)
 */
public class DirectEndpointStartSelfReference extends PsiReferenceBase<PsiElement> {

    private final CamelEndpoint endpoint;
    private final DirectEndpointPsiElement resolvedElement;

    public DirectEndpointStartSelfReference(@NotNull PsiElement element, CamelEndpoint endpoint) {
        super(element, TextRange.from(getStartOffset(element), endpoint.getBaseUri().length()));
        this.endpoint = endpoint;
        this.resolvedElement = new DirectEndpointPsiElement(element, endpoint);
    }

    private static int getStartOffset(PsiElement element) {
        return element.getText().startsWith("\"") ? 1 : 0;
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        return resolvedElement;
    }

    public PsiElement handleElementRename(@NotNull String newElementName) throws IncorrectOperationException {
        ElementManipulator<PsiElement> manipulator = ElementManipulators.getManipulator(myElement);
        return manipulator.handleContentChange(myElement, endpoint.getNameTextRange().shiftRight(getStartOffset(myElement)), newElementName);
    }

    public CamelEndpoint getEndpoint() {
        return endpoint;
    }

}
