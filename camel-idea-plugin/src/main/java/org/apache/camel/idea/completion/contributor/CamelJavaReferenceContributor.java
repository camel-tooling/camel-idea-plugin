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
package org.apache.camel.idea.completion.contributor;

import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.patterns.PatternCondition;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaToken;
import com.intellij.util.ProcessingContext;
import org.apache.camel.idea.completion.extension.CamelEndpointNameCompletionExtension;
import org.apache.camel.idea.completion.extension.CamelEndpointSmartCompletionExtension;
import org.apache.camel.idea.completion.extension.CamelJavaBeanReferenceSmartCompletion;
import org.apache.camel.idea.util.CamelIdeaUtils;
import org.jetbrains.annotations.NotNull;
import static com.intellij.patterns.PlatformPatterns.psiElement;
import static org.apache.camel.idea.completion.extension.CamelJavaBeanReferenceSmartCompletion.BEAN_CLASS_KEY;

/**
 * Plugin to hook into the IDEA Java language, to setup Camel smart completion for editing Java source code.
 */
public class CamelJavaReferenceContributor extends CamelContributor {

    public CamelJavaReferenceContributor() {
        addCompletionExtension(new CamelEndpointNameCompletionExtension(false));
        addCompletionExtension(new CamelEndpointSmartCompletionExtension(false));
        extend(CompletionType.BASIC, psiElement().and(psiElement().inside(PsiFile.class).inFile(matchFileType("java"))),
                new EndpointCompletion(getCamelCompletionExtensions())
        );
        extend(CompletionType.BASIC, psiElement(PsiJavaToken.class).with(new PatternCondition<PsiJavaToken>("CamelJavaBeanReferenceSmartCompletion") {
            @Override
            public boolean accepts(@NotNull PsiJavaToken psiJavaToken, ProcessingContext processingContext) {
                final PsiClass beanClass = getCamelIdeaUtils().getBean(psiJavaToken);
                if (beanClass != null) {
                    processingContext.put(BEAN_CLASS_KEY, beanClass);
                }
                return beanClass != null;
            }
        }), new CamelJavaBeanReferenceSmartCompletion());
    }

    private CamelIdeaUtils getCamelIdeaUtils() {
        return ServiceManager.getService(CamelIdeaUtils.class);
    }
}
