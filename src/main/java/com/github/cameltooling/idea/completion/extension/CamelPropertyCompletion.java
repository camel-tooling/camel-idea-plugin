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

import com.github.cameltooling.idea.service.CamelPreferenceService;
import com.github.cameltooling.idea.util.CamelIdeaUtils;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.completion.PlainPrefixMatcher;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.PlatformIcons;
import com.intellij.util.ProcessingContext;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

/**
 * Completion handler for building property result set. Hook into the process when
 * scanning for property files and building the completion list in the {@link CamelPropertyPlaceholderSmartCompletionExtension}
 */
public interface CamelPropertyCompletion {

    /**
     * @return true if it matches the property file it should process
     */
    boolean isValidFile(@NotNull PsiFile file);

    default boolean isExcludedFile(@NotNull PsiFile file) {
        String path = file.getVirtualFile().getCanonicalPath();
        if (path == null) {
            return true;
        }
        final CamelPreferenceService preferenceService = CamelPreferenceService.getService();
        return preferenceService.getExcludePropertyFiles()
                .stream()
                .anyMatch(s -> !s.isEmpty() && FilenameUtils.wildcardMatch(path, s));
    }

    /**
     * Build the property completion result set to be shown in the completion dialog
     */
    void buildResultSet(@NotNull ProcessingContext context, CompletionResultSet resultSet, CompletionQuery query, PsiFile file);

    default void addResult(CompletionResultSet resultSet, String prefix, LookupElement element) {
        resultSet.withPrefixMatcher(new PlainPrefixMatcher(prefix))
                .addElement(element);
    }

    default LookupElement createLookupElement(CompletionQuery query, String key, String value, VirtualFile file) {
        return LookupElementBuilder.create(key)
                .withTailText("=" + value, true)
                .withTypeText(file.getNameWithoutExtension(), getFileIcon(file), true)
                .withPresentableText(key)
                .withInsertHandler(new PropertyPlaceholderFinishingInsertHandler(query))
                .withIcon(PlatformIcons.PROPERTY_ICON);
    }

    default Icon getFileIcon(VirtualFile vf) {
        String ext = vf.getExtension() == null ? "" : vf.getExtension().toLowerCase();
        return switch (ext) {
            case "yaml", "yml" -> AllIcons.FileTypes.Yaml;
            default -> AllIcons.FileTypes.Properties;
        };
    }

    default String getPrefix(CompletionQuery query, @NotNull String placeholderStartToken) {
        PsiAnnotation annotation = PsiTreeUtil.getParentOfType(query.element(), PsiAnnotation.class);
        if (annotation != null && CamelIdeaUtils.PROPERTY_INJECT_ANNOTATION.equals(annotation.getQualifiedName())) {
            return query.valueAtPosition();
        }

        String prefix;
        int beginIndex = query.valueAtPosition().lastIndexOf(placeholderStartToken);
        if (beginIndex >= 0) {
            prefix = query.valueAtPosition().substring(beginIndex + placeholderStartToken.length());
        } else {
            prefix = "";
        }
        return prefix;
    }

    /**
     * Test if the property is on the ignore list
     */
    default boolean isIgnored(String key) {
        for (String ignore : CamelPreferenceService.getService().getIgnorePropertyList()) {
            if (key.startsWith(ignore)) {
                return true;
            }
        }
        return false;
    }

    class PropertyPlaceholderFinishingInsertHandler implements InsertHandler<LookupElement> {

        private final CompletionQuery query;

        public PropertyPlaceholderFinishingInsertHandler(CompletionQuery query) {
            this.query = query;
        }

        @Override
        public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement item) {
            Document doc = context.getDocument();
            int pos = context.getEditor().getCaretModel().getOffset();
            boolean insidePropertyPlaceholder = CamelIdeaUtils.getService().hasUnclosedPropertyPlaceholder(query.valueAtPosition());
            boolean suffixClosesPlaceholder = CamelIdeaUtils.getService().closesPropertyPlaceholder(query.suffix());
            String docSuffix = doc.getText(new TextRange(pos, doc.getTextLength()));
            if (insidePropertyPlaceholder && !suffixClosesPlaceholder && !docSuffix.startsWith(CamelIdeaUtils.PROPERTY_PLACEHOLDER_END_TOKEN)) {
                doc.insertString(pos, CamelIdeaUtils.PROPERTY_PLACEHOLDER_END_TOKEN);
            }
        }

    }
}
