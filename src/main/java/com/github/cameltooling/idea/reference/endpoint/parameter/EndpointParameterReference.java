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
package com.github.cameltooling.idea.reference.endpoint.parameter;

import com.github.cameltooling.idea.service.CamelCatalogService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PropertyUtilBase;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.tooling.model.ComponentModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Reference from an endpoint uri query parameter to its setter method in the corresponding camel class
 * E.g. from the 'synchronous' substring in endpoint uri "direct:abc?synchronous=true" to {@link org.apache.camel.component.direct.DirectEndpoint#setSynchronous}
 */
public class EndpointParameterReference extends PsiReferenceBase<PsiElement> {

    private static final List<String> CONFIG_CLASS_SUFFIXES = List.of("Endpoint", "Configuration");

    private final String component;
    private final String parameterName;

    public EndpointParameterReference(@NotNull PsiElement element, @NotNull String component, @NotNull String parameterName, TextRange parameterTextRange) {
        super(element, parameterTextRange);
        this.component = component;
        this.parameterName = parameterName;
    }

    @Override
    public @Nullable PsiElement resolve() {
        Project project = getElement().getProject();
        CamelCatalog catalog = project.getService(CamelCatalogService.class).get();
        ComponentModel model = catalog.componentModel(component);
        String componentClassName = model.getJavaType();
        if (componentClassName.endsWith("Component")) {
            String baseClassName = componentClassName.substring(0, componentClassName.length() - "Component".length());
            for (String configClassSuffix : CONFIG_CLASS_SUFFIXES) {
                String configClassName = baseClassName + configClassSuffix;
                PsiClass configClass = JavaPsiFacade.getInstance(project).findClass(configClassName, GlobalSearchScope.allScope(project));
                if (configClass != null) {
                    PsiMethod setter = PropertyUtilBase.findPropertySetter(configClass, parameterName, false, true);
                    if (setter != null) {
                        return setter;
                    }
                }
            }
        }
        return null;
    }

}