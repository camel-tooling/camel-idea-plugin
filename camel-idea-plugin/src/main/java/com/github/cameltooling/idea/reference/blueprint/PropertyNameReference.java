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

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.util.PropertyUtilBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Reference from bean property name to its backing field or setter method in the given bean
 */
public class PropertyNameReference extends PsiReferenceBase<PsiElement> {

    private final PsiClass beanClass;
    private final String propertyName;

    public PropertyNameReference(@NotNull PsiElement element, String propertyName, PsiClass beanClass) {
        super(element);
        this.beanClass = beanClass;
        this.propertyName = propertyName;
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        return PropertyUtilBase.findPropertySetter(beanClass, propertyName, false, true);
    }

}
