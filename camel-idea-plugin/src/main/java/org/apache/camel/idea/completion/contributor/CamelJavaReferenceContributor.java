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
import com.intellij.codeInsight.completion.JavaKeywordCompletion;
import com.intellij.patterns.ObjectPattern;
import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiTypeElement;
import org.apache.camel.idea.completion.extension.CamelEndpointSmartCompletionExtension;
import org.apache.camel.idea.completion.extension.JavaBeanSmartCompletion;

import static com.intellij.patterns.PlatformPatterns.psiElement;

/**
 * Plugin to hook into the IDEA Java language, to setup Camel smart completion for editing Java source code.
 */
public class CamelJavaReferenceContributor extends CamelContributor {


    private static final ObjectPattern IN_METHOD_RETURN_TYPE;

    static {
        IN_METHOD_RETURN_TYPE = PsiJavaPatterns.psiElement().withParents(new Class[]{PsiJavaCodeReferenceElement.class, PsiTypeElement.class, PsiMethod.class}).andNot(JavaKeywordCompletion.AFTER_DOT);
    }
    public CamelJavaReferenceContributor() {
        addCompletionExtension(new CamelEndpointSmartCompletionExtension(false));
        extend(CompletionType.BASIC,
            psiElement().and(psiElement().inside(PsiFile.class).inFile(matchFileType("java"))), new JavaBeanSmartCompletion());
        extend(CompletionType.BASIC,
                psiElement().and(psiElement().inside(PsiFile.class).inFile(matchFileType("java"))),
                new EndpointCompletion(getCamelCompletionExtensions())
        );
    }

}
