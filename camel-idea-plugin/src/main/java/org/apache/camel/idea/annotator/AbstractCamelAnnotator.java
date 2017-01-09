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
package org.apache.camel.idea.annotator;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.psi.PsiElement;
import org.apache.camel.idea.service.CamelPreferenceService;
import org.apache.camel.idea.service.CamelService;
import org.apache.camel.idea.util.IdeaUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Validate if the URI contains a know Camel component and call the validateEndpoint method
 */
abstract class AbstractCamelAnnotator implements Annotator {

    /**
     * Whether or not the annotator is enabled.
     * <p/>
     * The user can turn this on or off in the plugin preference.
     */
    boolean isEnabled() {
        return ServiceManager.getService(CamelPreferenceService.class).isRealTimeValidation();
    }

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (ServiceManager.getService(element.getProject(), CamelService.class).isCamelPresent() && isEnabled()) {
            String fromElement = IdeaUtils.extractTextFromElement(element, false);
            if (IdeaUtils.isQueryContainingCamelComponent(element.getProject(), fromElement)) {
                validateEndpoint(element, holder, fromElement);
            }
        }
    }

    /**
     * Validate the Camel Endpoint and create error messaged from the validation result.
     *
     * @param element - Element to parse
     * @param holder - Container for the different error messages and it's test range
     * @param uri - String to validate
     */
    abstract void validateEndpoint(@NotNull PsiElement element, @NotNull AnnotationHolder holder, String uri);

}
