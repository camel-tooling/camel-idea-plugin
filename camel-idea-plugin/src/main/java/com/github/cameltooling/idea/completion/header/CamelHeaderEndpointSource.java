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
package com.github.cameltooling.idea.completion.header;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.cameltooling.idea.util.CamelIdeaUtils;
import com.github.cameltooling.idea.util.StringUtils;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * Defines all the supported source of endpoints.
 */
public enum CamelHeaderEndpointSource {
    /**
     * Represents the source that provides all the producer endpoints.
     */
    PRODUCER_ONLY {
        @Override
        Collection<CamelHeaderEndpoint> getEndpoints(@NotNull PsiElement element) {
            final CamelIdeaUtils utils = getCamelIdeaUtils();
            return utils.findEndpointUsages(
                ModuleUtilCore.findModuleForPsiElement(element), e -> e.indexOf(':') != -1
            )
                .stream()
                .filter(utils::isProducerEndpoint)
                .map(PsiElement::getText)
                .map(StringUtil::unquoteString)
                .map(StringUtils::asComponentName)
                .map(CamelHeaderEndpoint::producerOnly)
                .collect(Collectors.toSet());
        }
    },
    /**
     * Represents the source that provides all the consumer endpoints.
     */
    CONSUMER_ONLY {
        @Override
        Collection<CamelHeaderEndpoint> getEndpoints(@NotNull PsiElement element) {
            final CamelIdeaUtils utils = getCamelIdeaUtils();
            return utils.findEndpointDeclarations(
                ModuleUtilCore.findModuleForPsiElement(element), e -> e.indexOf(':') != -1
            )
                .stream()
                .filter(utils::isConsumerEndpoint)
                .map(PsiElement::getText)
                .map(StringUtil::unquoteString)
                .map(StringUtils::asComponentName)
                .map(CamelHeaderEndpoint::consumerOnly)
                .collect(Collectors.toSet());
        }
    },
    /**
     * Represents the source that provides all the existing endpoints.
     */
    ALL {
        @Override
        Collection<CamelHeaderEndpoint> getEndpoints(@NotNull PsiElement element) {
            return Stream.concat(
                CONSUMER_ONLY.getEndpoints(element).stream(), PRODUCER_ONLY.getEndpoints(element).stream()
            )
                .map(CamelHeaderEndpoint::getComponentName)
                .map(CamelHeaderEndpoint::both)
                .collect(Collectors.toSet());
        }
    };

    /**
     * @param element the element from which we retrieve the module for which we want the endpoints.
     * @return a collection of all matching endpoints in the module corresponding to the given element.
     */
    abstract Collection<CamelHeaderEndpoint> getEndpoints(@NotNull PsiElement element);

    private static CamelIdeaUtils getCamelIdeaUtils() {
        return CamelIdeaUtils.getService();
    }
}
