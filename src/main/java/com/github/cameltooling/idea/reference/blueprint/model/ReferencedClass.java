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

import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReference;
import com.intellij.psi.util.ClassUtil;
import com.intellij.rt.coverage.util.ClassNameUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a java class which is being referenced to. A wrapper for {@link JavaClassReference}.
 */
public class ReferencedClass {

    private final String className;
    private final JavaClassReference reference;

    public ReferencedClass(@NotNull String className, @NotNull JavaClassReference reference) {
        this.className = className;
        this.reference = reference;
    }

    @NotNull
    public JavaClassReference getReference() {
        return reference;
    }

    @NotNull
    public String getClassName() {
        return className;
    }

    @NotNull
    public String getClassSimpleName() {
        return ClassUtil.extractClassName(className);
    }

}
