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

import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionUtil;
import com.intellij.openapi.util.IconLoader;
import com.intellij.patterns.InitialPatternCondition;
import com.intellij.patterns.PsiFilePattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.util.ProcessingContext;
import org.apache.camel.idea.completion.extension.CamelCompletionExtension;
import org.apache.camel.idea.util.IdeaUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.apache.camel.idea.util.IdeaUtils.getInnerText;

/**
 * Hook into the IDEA language completion system, to setup Camel smart completion.
 * Extend this class to define what it should re-act on when using smart completion
 */
public abstract class CamelContributor extends CompletionContributor {

    public static final Icon CAMEL_ICON = IconLoader.getIcon("/icons/camel.png");

    private final List<CamelCompletionExtension> camelCompletionExtensions = new ArrayList<>();

    CamelContributor() {
    }

    /**
     * Smart completion for Camel endpoints.
     */
    protected static class EndpointCompletion extends CompletionProvider<CompletionParameters> {

        private final List<CamelCompletionExtension> camelCompletionExtensions;

        public EndpointCompletion(List<CamelCompletionExtension> camelCompletionExtensions) {
            this.camelCompletionExtensions = camelCompletionExtensions;
        }

        public void addCompletions(@NotNull CompletionParameters parameters,
                                   ProcessingContext context,
                                   @NotNull CompletionResultSet resultSet) {
            String[] tuple = parsePsiElement(parameters);
            camelCompletionExtensions.stream()
                .filter(p -> p.isValid(parameters, context, tuple))
                .forEach(p -> p.addCompletions(parameters, context, resultSet, tuple));
        }
    }

    /**
     * Parse the PSI text {@link CompletionUtil#DUMMY_IDENTIFIER} and " character and remove them.
     * <p/>
     * This implementation support Java literal expressions and XML attributes where you can define Camel endpoints.
     *
     * @param parameters - completion parameter to parse
     * @return new string stripped for any {@link CompletionUtil#DUMMY_IDENTIFIER} and " character
     */
    @NotNull
    private static String[] parsePsiElement(@NotNull CompletionParameters parameters) {
        PsiElement element = parameters.getPosition();

        String val = null;

        // need the entire line so find the literal expression that would hold the entire string (java)
        PsiLiteralExpression literal = PsiTreeUtil.getParentOfType(element, PsiLiteralExpression.class);
        if (literal != null) {
            Object o = literal.getValue();
            val = o != null ? o.toString() : null;
        }

        // maybe its xml then try that
        if (val == null) {
            XmlAttributeValue xml = PsiTreeUtil.getParentOfType(element, XmlAttributeValue.class);
            val = xml != null ? xml.getValue() : null;
        }

        // maybe its groovy
        if (val == null && element instanceof LeafPsiElement) {
            IElementType type = ((LeafPsiElement) element).getElementType();
            if (type.getLanguage().isKindOf("Groovy")) {
                String text = element.getText();
                // unwrap groovy gstring
                val = getInnerText(text);
            }
        }

        if (val == null) {
            // fallback to generic
            val = element.getText();
        }

        String suffix = "";

        // okay IDEA folks its not nice, in groovy the dummy identifier is using lower case i in intellij
        // so we need to lower case it all
        String hackVal = val.toLowerCase();
        int len = CompletionUtil.DUMMY_IDENTIFIER.length();
        int hackIndex = hackVal.indexOf(CompletionUtil.DUMMY_IDENTIFIER.toLowerCase());
        if (hackIndex == -1) {
            hackIndex = hackVal.indexOf(CompletionUtil.DUMMY_IDENTIFIER_TRIMMED.toLowerCase());
            len = CompletionUtil.DUMMY_IDENTIFIER_TRIMMED.length();
        }

        if (hackIndex > -1) {
            suffix = val.substring(hackIndex + len);
            val = val.substring(0, hackIndex);
        }

        if (val.startsWith("\"")) {
            val = val.substring(1);
        }
        if (val.endsWith("\"")) {
            val = val.substring(0, val.length() - 1);
        }
        if (suffix.endsWith("\"")) {
            suffix = suffix.substring(0, suffix.length() - 1);
        }
        return new String[]{val, suffix};
    }

    /**
     * Add additional completion extension to process when the
     * {@link CompletionProvider#addCompletions(CompletionParameters, ProcessingContext, CompletionResultSet)} is called
     */
    public void addCompletionExtension(CamelCompletionExtension provider) {
        camelCompletionExtensions.add(provider);
    }

    public List<CamelCompletionExtension> getCamelCompletionExtensions() {
        return camelCompletionExtensions;
    }

    /**
     * Checks if its a file of expect type
     */
    static PsiFilePattern.Capture<PsiFile> matchFileType(final String... extensions) {
        return new PsiFilePattern.Capture<>(new InitialPatternCondition<PsiFile>(PsiFile.class) {
            @Override
            public boolean accepts(@Nullable Object o, ProcessingContext context) {
                if (o instanceof PsiFile) {
                    String ext = ((PsiFile) o).getFileType().getName();
                    for (String match : extensions) {
                        if (match.equalsIgnoreCase(ext)) {
                            return true;
                        }
                    }
                    return false;
                }
                return false;
            }
        });
    }


}
