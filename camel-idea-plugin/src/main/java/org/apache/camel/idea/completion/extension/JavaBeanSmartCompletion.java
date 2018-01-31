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
package org.apache.camel.idea.completion.extension;

import java.util.ArrayList;
import java.util.List;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionUtil;
import com.intellij.codeInsight.lookup.AutoCompletionPolicy;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.lang.jvm.JvmModifier;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.util.ProcessingContext;
import org.apache.camel.idea.util.CamelIdeaUtils;
import org.jetbrains.annotations.NotNull;


public class JavaBeanSmartCompletion extends CompletionProvider<CompletionParameters> {


    @Override
    protected void addCompletions(@NotNull CompletionParameters completionParameters, ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {
        PsiElement element = completionParameters.getPosition();

        PsiClass psiClass = getCamleIdeaUtils().getCamelBean(element);
        if (psiClass == null) {
            return;
        }

        List<LookupElement> answer = new ArrayList<>();

        final PsiMethod[] methods = psiClass.getMethods();
        for (int m = 0; m < methods.length; m++) {
            final PsiMethod method = methods[m];

            boolean isPrivate = false;
            final JvmModifier[] modifiers = method.getModifiers();
            for (int i = 0; i < modifiers.length; i++) {
                if (modifiers[i].equals(JvmModifier.PRIVATE) || modifiers[i].equals(JvmModifier.ABSTRACT)) {
                    isPrivate = true;
                    break;
                }
            }

            if (!method.isConstructor() && !isPrivate) {
                LookupElementBuilder builder = LookupElementBuilder.create(method);
                builder = builder.withPresentableText(method.getNameIdentifier().getText());
                builder = builder.withTypeText(psiClass.getName(), true);
                builder = builder.withIcon(AllIcons.Nodes.Method);
                if (method.isDeprecated()) {
                    // mark as deprecated
                    builder = builder.withStrikeoutness(true);
                }
                // we don't want to highlight the advanced options which should be more seldom in use
                answer.add(builder.withAutoCompletionPolicy(AutoCompletionPolicy.GIVE_CHANCE_TO_OVERWRITE));
            }
        }
        // are there any results then add them
        if (answer != null && !answer.isEmpty()) {
            String hackVal = element.getText();
            hackVal = hackVal.replace(CompletionUtil.DUMMY_IDENTIFIER_TRIMMED, "");
            hackVal = hackVal.substring(1, hackVal.length() - 1);

            completionResultSet.withPrefixMatcher(hackVal).addAllElements(answer);
            completionResultSet.stopHere();
        }
    }

    private CamelIdeaUtils getCamleIdeaUtils() {
        return ServiceManager.getService(CamelIdeaUtils.class);
    }

}
