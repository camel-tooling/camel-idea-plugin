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

import java.io.IOException;
import java.util.Properties;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.PlainPrefixMatcher;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.camel.idea.service.CamelPreferenceService;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;

/**
 * To support smart completion where properties are loaded from <tt>.properties</tt> files.
 * <p/>
 * Smart completion for editing a Camel endpoint uri, to show a list of property holders can be added.
 * For example editing <tt>jms:queue?{{_CURSOR_HERE_</tt>. Which presents the user
 * with a list of possible properties.
 */
public class PropertiesPropertyPlaceholdersSmartCompletion implements CamelPropertyCompletion {

    private static final Logger LOG = Logger.getInstance(PropertiesPropertyPlaceholdersSmartCompletion.class);

    @NotNull
    private Properties getProperties(VirtualFile virtualFile) {
        Properties properties = new Properties();
        try {
            properties.load(virtualFile.getInputStream());
        } catch (IOException e) {
            LOG.warn("Error loading properties file: " + virtualFile, e);
        }

        return properties;
    }

    @Override
    public boolean isValidExtension(String filename) {
        final CamelPreferenceService preferenceService = ServiceManager.getService(CamelPreferenceService.class);
        final boolean present = preferenceService.getExcludePropertyFiles()
            .stream()
            .filter(s -> !s.isEmpty() && FilenameUtils.wildcardMatch(filename, s))
            .findFirst()
            .isPresent();
        return (!present) && (filename.endsWith(".properties"));
    }

    @Override
    public void buildResultSet(CompletionResultSet resultSet, VirtualFile virtualFile) {
        getProperties(virtualFile).forEach((key, value) -> {
            String keyStr = (String) key;
            if (!isIgnored(keyStr)) {
                LookupElementBuilder builder = LookupElementBuilder.create(keyStr + "}}")
                        .appendTailText(String.valueOf(value), true)
                        .withTypeText("[" + virtualFile.getPresentableName() + "]", true)
                        .withPresentableText(keyStr + " = ");
                resultSet.withPrefixMatcher(new PlainPrefixMatcher("")).addElement(builder);
            }
        });
    }
}
