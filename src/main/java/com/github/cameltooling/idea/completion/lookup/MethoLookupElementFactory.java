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
package com.github.cameltooling.idea.completion.lookup;

import com.github.cameltooling.idea.util.CamelIdeaUtils;
import com.intellij.codeInsight.lookup.AutoCompletionPolicy;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;

public class MethoLookupElementFactory {

    public static LookupElement create(PsiMethod method, String presentableMethod) {
        PsiClass containingClass = method.getContainingClass();
        return create(method, presentableMethod, containingClass == null ? null : containingClass.getName());
    }
    public static LookupElement create(PsiMethod method, String presentableMethod, String typeText) {
        LookupElementBuilder builder = LookupElementBuilder.create(method);
        builder = builder.withPresentableText(presentableMethod);
        builder = builder.withTypeText(typeText, true);
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
