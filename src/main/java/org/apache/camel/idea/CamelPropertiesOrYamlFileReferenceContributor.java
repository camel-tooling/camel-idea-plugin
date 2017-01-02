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
import com.intellij.patterns.InitialPatternCondition;
import com.intellij.patterns.PsiFilePattern;
import com.intellij.psi.PsiFile;
import com.intellij.util.ProcessingContext;
import org.apache.camel.idea.completion.extension.CamelEndpointSmartCompletionExtension;
import org.jetbrains.annotations.Nullable;

import static com.intellij.patterns.PlatformPatterns.psiElement;

/**
 * Plugin to hook into the IDEA completion system, to setup Camel smart completion for editing properties or yaml files.
 */
public class CamelPropertiesOrYamlFileReferenceContributor extends CamelContributor {

    public CamelPropertiesOrYamlFileReferenceContributor() {
        // also allow to setup camel endpoints in properties files
        addCompletionExtension(new CamelEndpointSmartCompletionExtension(false));
        extend(CompletionType.BASIC,
                psiElement().and(psiElement().inside(PsiFile.class).inFile(propertiesFile())),
                new EndpointCompletion(getCamelCompletionExtensions())
        );
    }

    /**
     * Checks if its a (properties or yaml) file or not
     */
    private static PsiFilePattern.Capture<PsiFile> propertiesFile() {
        return new PsiFilePattern.Capture<>(new InitialPatternCondition<PsiFile>(PsiFile.class) {
            @Override
            public boolean accepts(@Nullable Object o, ProcessingContext context) {
                if (o instanceof PsiFile) {
                    String ext = ((PsiFile) o).getFileType().getName();
                    return ext.equalsIgnoreCase("properties")
                        || ext.equalsIgnoreCase("yaml") || ext.equalsIgnoreCase("yml");
                }
                return false;
            }
        });
    }

}
