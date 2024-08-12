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

import com.github.cameltooling.idea.service.KameletService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

/**
 * {@code CamelKameletOptionCompletion} is a base class for all Camel Kamelet option completion providers.
 */
abstract class CamelKameletOptionCompletion extends CamelKameletCompletion {

    @Override
    protected boolean isConsumerOnly(Project project, PsiElement element) {
        String name = getKameletName(element);
        return name != null && project.getService(KameletService.class).isConsumer(name);
    }

    /**
     * Extracts the name of the Camel Kamelet from the given element.
     *
     * @param element the element
     * @return the name of the Camel Kamelet or {@code null} if the element is not part of the configuration of Camel
     * Kamelet
     */
    protected abstract @Nullable String getKameletName(PsiElement element);
}
