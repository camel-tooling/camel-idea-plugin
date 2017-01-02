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
package org.apache.camel.idea;

import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.psi.PsiClass;
import org.apache.camel.idea.completion.extension.CamelPropertiesSmartCompletionExtension;

import static com.intellij.patterns.PlatformPatterns.psiElement;

/**
 * Plugin to hook into the IDEA completion system, to setup Camel smart completion for editing Property files.
 */
public class CamelPropertyReferenceContributor extends CamelContributor {

    public CamelPropertyReferenceContributor() {
        addCompletionExtension(new CamelPropertiesSmartCompletionExtension());
        extend(CompletionType.BASIC,
                psiElement().andNot(psiElement().inside(PsiClass.class)), // TODO: wonder if we can do this filter better
                new EndpointCompletion(getCamelCompletionExtensions())
        );
    }

}
