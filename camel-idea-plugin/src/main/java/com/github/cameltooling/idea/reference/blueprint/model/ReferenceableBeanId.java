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
package com.github.cameltooling.idea.reference.blueprint.model;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an id of a bean, which can be referenced from other places. Mainly <bean id="[THIS]">
 */
public class ReferenceableBeanId {

    private final PsiElement element;
    private final String id;
    private final ReferencedClass referencedClass;

    public ReferenceableBeanId(@NotNull PsiElement element, @NotNull String id, @Nullable ReferencedClass referencedClass) {
        this.element = element;
        this.id = id;
        this.referencedClass = referencedClass;
    }

    @NotNull
    public PsiElement getElement() {
        return element;
    }

    @NotNull
    public String getId() {
        return id;
    }

    @Nullable
    public ReferencedClass getReferencedClass() {
        return referencedClass;
    }

}
