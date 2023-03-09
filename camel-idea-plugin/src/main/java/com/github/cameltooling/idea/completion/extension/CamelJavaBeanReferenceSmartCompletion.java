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

import com.github.cameltooling.idea.util.CamelIdeaUtils;
import com.github.cameltooling.idea.util.JavaMethodUtils;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionUtil;
import com.intellij.codeInsight.lookup.AutoCompletionPolicy;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * Camel Java bean reference smart completion for lookup bean methods.
 * <pre>
 *     Caret inside {@code bean(MyClass.class,"<caret>")} will look up all public methods for the
 *     class and it's super classes. If the a bean method is marked with @Handle it will bold and
 *     for deprecated methods will be with strike through.
 * </pre>
 */
public class CamelJavaBeanReferenceSmartCompletion extends CompletionProvider<CompletionParameters> {

    @Override
    protected void addCompletions(@NotNull CompletionParameters completionParameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {
        final PsiElement element = completionParameters.getPosition();
        final PsiClass psiClass = CamelIdeaUtils.getService().getBean(element);
        final JavaMethodUtils javaMethodUtils = JavaMethodUtils.getService();

        if (psiClass != null) {
            Collection<PsiMethod> methods = javaMethodUtils.getMethods(psiClass);

            List<LookupElement> answer = javaMethodUtils.getBeanAccessibleMethods(methods)
                .stream()
                .map(method -> buildLookupElement(method, javaMethodUtils.getPresentableMethodWithParameters(method)))
                .collect(toList());

            // are there any results then add them
            if (!answer.isEmpty()) {
                String hackVal = element.getText();
                hackVal = hackVal.substring(1, hackVal.indexOf(CompletionUtil.DUMMY_IDENTIFIER_TRIMMED));
                completionResultSet.withPrefixMatcher(hackVal).addAllElements(answer);
                completionResultSet.stopHere();
            }
        }
    }

    @NotNull
    private LookupElement buildLookupElement(PsiMethod method, String presentableMethod) {
        LookupElementBuilder builder = LookupElementBuilder.create(method);
        builder = builder.withPresentableText(presentableMethod);
        builder = builder.withTypeText(method.getContainingClass().getName(), true);
        builder = builder.withIcon(AllIcons.Nodes.Method);
        if (CamelIdeaUtils.getService().isAnnotatedWithHandler(method)) {
            //@Handle methods are marked with
            builder = builder.withBoldness(true);
        }
        if (method.isDeprecated()) {
            // mark as deprecated
            builder = builder.withStrikeoutness(true);
        }
        return builder.withAutoCompletionPolicy(AutoCompletionPolicy.GIVE_CHANCE_TO_OVERWRITE);
    }

}
