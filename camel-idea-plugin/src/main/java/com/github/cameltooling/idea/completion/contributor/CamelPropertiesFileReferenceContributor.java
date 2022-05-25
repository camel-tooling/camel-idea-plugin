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
package com.github.cameltooling.idea.completion.contributor;

import com.github.cameltooling.idea.completion.extension.CamelEndpointSmartCompletionExtension;
import com.github.cameltooling.idea.completion.property.CamelPropertiesPropertyKeyCompletion;
import com.github.cameltooling.idea.completion.property.CamelPropertiesPropertyValueCompletion;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.lang.properties.parsing.PropertiesTokenTypes;
import com.intellij.patterns.PsiFilePattern;
import com.intellij.psi.PsiFile;

import static com.intellij.patterns.PlatformPatterns.psiElement;

/**
 * Plugin to hook into the IDEA completion system, to set up Camel smart completion for editing properties.
 */
public class CamelPropertiesFileReferenceContributor extends CamelContributor {

    public CamelPropertiesFileReferenceContributor() {
        // also allow to set up camel endpoints in properties files
        addCompletionExtension(new CamelEndpointSmartCompletionExtension(false));
        // To match with file with "properties" as extension
        final PsiFilePattern.Capture<PsiFile> filePattern = matchFileType("properties");
        extend(CompletionType.BASIC,
                psiElement().and(psiElement().inside(PsiFile.class).inFile(filePattern)),
                new EndpointCompletion(getCamelCompletionExtensions())
        );
        // The key of the property corresponding to key characters in a properties file
        extend(CompletionType.BASIC,
            psiElement(PropertiesTokenTypes.KEY_CHARACTERS)
                .inside(PsiFile.class)
                .inFile(filePattern),
            new CamelPropertiesPropertyKeyCompletion()
        );
        // The value of the property corresponding to value characters in a properties file
        extend(CompletionType.BASIC,
            psiElement(PropertiesTokenTypes.VALUE_CHARACTERS)
                .inside(PsiFile.class)
                .inFile(filePattern),
            new CamelPropertiesPropertyValueCompletion()
        );
    }

}
