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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import static com.intellij.lang.properties.references.PropertiesCompletionContributor.LOOKUP_ELEMENT_RENDERER;

/**
 * To support smart completion where properties are loaded from <tt>.properties</tt> files,
 * or files that the user associated as properties files, e.g. cfg files
 * <p/>
 * Smart completion for editing a Camel endpoint uri, to show a list of property holders can be added.
 * For example editing <tt>jms:queue?{{_CURSOR_HERE_</tt>. Which presents the user
 * with a list of possible properties.
 */
public class PropertiesPropertyPlaceholdersSmartCompletion implements CamelPropertyCompletion {

    private static final Logger LOG = Logger.getInstance(PropertiesPropertyPlaceholdersSmartCompletion.class);

    @Override
    public boolean isValidFile(@NotNull PsiFile file) {
        return !isExcludedFile(file) && file instanceof PropertiesFile;
    }

    @Override
    public void buildResultSet(CompletionResultSet resultSet, CompletionQuery query, PsiFile file) {
        String prefix = getPrefix(query);
        if (file instanceof PropertiesFile pf) {
            addPropertyResults(pf.getProperties(),
                    IProperty::getKey,
                    p -> LookupElementBuilder.create(p, p.getKey() == null ? p.getName() : p.getKey())
                            .withRenderer(LOOKUP_ELEMENT_RENDERER)
                            .withInsertHandler(new PropertyPlaceholderFinishingInsertHandler(query)),
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
                    LOG.debug("Adding property lookup element with key " + key);
                    addResult(resultSet, prefix, lookupElement);
                });

    }

}
