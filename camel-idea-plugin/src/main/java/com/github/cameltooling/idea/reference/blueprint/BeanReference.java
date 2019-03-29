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
package com.github.cameltooling.idea.reference.blueprint;

import java.util.Optional;
import com.github.cameltooling.idea.reference.blueprint.model.ReferenceableBeanId;
import com.github.cameltooling.idea.util.CamelIdeaUtils;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Reference from an usage of a bean, to its declaration.
 *
 * For example - when using bean id in a 'ref' attribute - <property name='xxx' ref='myBean'/>, the myBean is a reference
 * to its declaration - <bean id='myBean' .../>
 */
public class BeanReference extends PsiReferenceBase<PsiElement> {

    private final String id;

    public BeanReference(@NotNull PsiElement element, @NotNull String id) {
        super(element);
        this.id = id;
    }

    public String getBeanId() {
        return id;
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        return findReferenceableBeanId()
            .map(ref -> new ReferenceableIdPsiElement(ref.getElement(), id))
            .orElse(null);
    }

    public Optional<ReferenceableBeanId> findReferenceableBeanId() {
        final Module module = ModuleUtilCore.findModuleForPsiElement(getElement());
        if (module != null) {
            return CamelIdeaUtils.getService().findReferenceableBeanId(module, id);
        } else {
            return Optional.empty();
        }
    }

}
