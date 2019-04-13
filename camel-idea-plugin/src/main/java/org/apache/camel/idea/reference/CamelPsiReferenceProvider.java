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
package org.apache.camel.idea.reference;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.util.ProcessingContext;
import org.apache.camel.idea.service.CamelService;
import org.jetbrains.annotations.NotNull;

/**
 * A parent class for {@link PsiReferenceProvider}s which should provide references only when camel support is active.
 */
public abstract class CamelPsiReferenceProvider extends PsiReferenceProvider {

    @NotNull
    @Override
    public PsiReference[] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
        if (!ServiceManager.getService(element.getProject(), CamelService.class).isCamelPresent()) {
            return PsiReference.EMPTY_ARRAY;
        } else {
            return getCamelReferencesByElement(element, context);
        }
    }

    protected abstract PsiReference[] getCamelReferencesByElement(PsiElement element, ProcessingContext context);

}
