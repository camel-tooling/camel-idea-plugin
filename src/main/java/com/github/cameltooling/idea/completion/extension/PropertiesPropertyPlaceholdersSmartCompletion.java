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

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;

import com.github.cameltooling.idea.service.CamelPreferenceService;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;

import static com.intellij.lang.properties.references.PropertiesCompletionContributor.LOOKUP_ELEMENT_RENDERER;

/**
 * To support smart completion where properties are loaded from <tt>.properties</tt> files.
 * <p/>
 * Smart completion for editing a Camel endpoint uri, to show a list of property holders can be added.
 * For example editing <tt>jms:queue?{{_CURSOR_HERE_</tt>. Which presents the user
 * with a list of possible properties.
 */
public class PropertiesPropertyPlaceholdersSmartCompletion implements CamelPropertyCompletion {

    private static final Logger LOG = Logger.getInstance(PropertiesPropertyPlaceholdersSmartCompletion.class);

    private static final Set<String> PROPERTY_EXTENSIONS = Set.of("properties", "cfg");

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
        final CamelPreferenceService preferenceService = CamelPreferenceService.getService();
        final boolean excluded = preferenceService.getExcludePropertyFiles()
            .stream()
            .anyMatch(s -> !s.isEmpty() && FilenameUtils.wildcardMatch(filename, s));
        return !excluded && PROPERTY_EXTENSIONS.stream().anyMatch(ext -> filename.endsWith("." + ext));
    }

    @Override
    public void buildResultSet(CompletionResultSet resultSet, CompletionQuery query, PsiFile file) {
        String prefix = getPrefix(query);
        if (file instanceof PropertiesFile pf) {
            addPropertyResults(pf.getProperties(),
                    IProperty::getKey,
                    p -> LookupElementBuilder.create(p, p.getKey() == null ? p.getName() : p.getKey())
                            .withRenderer(LOOKUP_ELEMENT_RENDERER)
                            .withInsertHandler(new PropertyPlaceholderInsertHandler(query)),
                    resultSet,
                    prefix);
        } else {
            VirtualFile virtualFile = file.getVirtualFile();
            addPropertyResults(getProperties(virtualFile).entrySet().stream()
                            .map(e -> Map.entry((String) e.getKey(), e.getValue()))
                            .toList(),
                    Map.Entry::getKey,
                    e -> createLookupElement(query, e.getKey(), String.valueOf(e.getValue()), virtualFile),
                    resultSet,
                    prefix);
        }
    }

    private <T> void addPropertyResults(List<T> properties,
                                        Function<T, String> keyGetter,
                                        Function<T, LookupElement> lookupElementCreator,
                                        CompletionResultSet resultSet,
                                        String prefix) {
        Set<String> usedProperties = new HashSet<>();
        properties.stream()
                .filter(p -> {
                    String key = keyGetter.apply(p);
                    return key != null && !isIgnored(key) && key.startsWith(prefix);
                })
                .forEach(p -> {
                    String key = keyGetter.apply(p);
                    if (usedProperties.contains(key)) {
                        return;
                    }
                    usedProperties.add(key);
                    LookupElement lookupElement = lookupElementCreator.apply(p);
                    addResult(resultSet, prefix, lookupElement);
                });

    }

}
