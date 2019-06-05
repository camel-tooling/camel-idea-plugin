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
package com.github.cameltooling.idea.reference.endpoint.direct;

import com.github.cameltooling.idea.reference.FakeCamelPsiElement;
import com.github.cameltooling.idea.reference.endpoint.CamelEndpoint;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A fake psi element for direct endpoint references.
 */
public class DirectEndpointPsiElement extends FakeCamelPsiElement {

    private final CamelEndpoint endpoint;

    public DirectEndpointPsiElement(@NotNull PsiElement element, @NotNull CamelEndpoint endpoint) {
        super(element);
        this.endpoint = endpoint;
    }

    @Override
    public String getName() {
        return endpoint.getName();
    }

    @Nullable
    @Override
    public String getTypeName() {
        return "direct endpoint";
    }

}
