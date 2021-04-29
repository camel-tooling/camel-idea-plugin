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
package com.github.cameltooling.idea.completion.extension;

import java.util.Arrays;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

/**
 * Code completion extension which is valid only on an element which has a reference of the given type
 */
public abstract class ReferenceBasedCompletionExtension<T extends PsiReference> extends SimpleCompletionExtension {

    private final Class<T> referenceClass;

    public ReferenceBasedCompletionExtension(Class<T> referenceClass) {
        this.referenceClass = referenceClass;
    }

    @Override
    public boolean isValid(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull String query) {
        PsiElement element = parameters.getPosition().getParent();
        return Arrays.stream(element.getReferences()).anyMatch(r -> referenceClass.isAssignableFrom(r.getClass()));
    }

}
