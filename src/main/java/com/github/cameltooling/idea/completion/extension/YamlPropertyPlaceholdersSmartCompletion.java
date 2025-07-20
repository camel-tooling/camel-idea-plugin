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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import com.github.cameltooling.idea.service.CamelPreferenceService;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * To support smart completion where properties are loaded from <tt>.yaml</tt> files.
 * <p/>
 * Smart completion for editing a Camel endpoint uri, to show a list of YAML properties can be added.
 * For example editing <tt>jms:queue?{{_CURSOR_HERE_</tt>. Which presents the user
 * with a list of possible properties.
 */
public class YamlPropertyPlaceholdersSmartCompletion implements CamelPropertyCompletion {

    private static final Logger LOG = Logger.getInstance(YamlPropertyPlaceholdersSmartCompletion.class);

    @NotNull
    private Map<String, Object> getProperties(VirtualFile virtualFile) {
        Map<String, Object> result = new HashMap<>();
        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        try {
            // Parse the YAML file and return the output as a series of Maps and Lists
            result = yaml.load(virtualFile.getInputStream());
        } catch (Exception e) {
            LOG.warn("Error loading yaml file: " + virtualFile, e);
        }
        return result;
    }

    @Override
    public boolean isValidExtension(String filename) {
        final CamelPreferenceService preferenceService = CamelPreferenceService.getService();
        final boolean present = preferenceService.getExcludePropertyFiles()
            .stream()
            .anyMatch(s -> !s.isEmpty() && FilenameUtils.wildcardMatch(filename, s));
        return !present && (filename.endsWith(".yaml") || filename.endsWith(".yml"));
    }

    @Override
    public void buildResultSet(CompletionResultSet resultSet, CompletionQuery query, PsiFile file) {
        VirtualFile virtualFile = file.getVirtualFile();
        CompletionContext ctx = new CompletionContext(getPrefix(query), virtualFile, query, resultSet);
        getProperties(virtualFile).forEach((key, value) -> {
            final String keyStr = key;
            if (!isIgnored(key)) {
                if (value instanceof List) {
                    buildResultSetForList(ctx, keyStr, (List<?>) value);
                } else if (value instanceof LinkedHashMap) {
                    buildResultSetForLinkedHashMap(ctx, keyStr, Collections.singletonList(value));
                } else {
                    buildResultSetForElement(ctx, keyStr, String.valueOf(value));
                }
            }
        });
    }

    /**
     * Flat the {@link LinkedHashMap} to string property names and build the {@link CompletionResultSet}
     */
    @SuppressWarnings("unchecked")
    private void buildResultSetForLinkedHashMap(CompletionContext ctx, String keyStr, List<?> propertyList) {
        propertyList.stream()
            .filter(l -> l instanceof LinkedHashMap)
            .map(LinkedHashMap.class::cast)
            .flatMap(lhm -> lhm.entrySet().stream())
            .forEach(e -> {
                Map.Entry<String, Object> entry = (Map.Entry<String, Object>) e;
                String flatKeyStr = keyStr + "." + entry.getKey();
                if (entry.getValue() instanceof List) {
                    buildResultSetForList(ctx, flatKeyStr, (List<?>) entry.getValue());
                } else if (entry.getValue() instanceof LinkedHashMap) {
                    buildResultSetForLinkedHashMap(ctx, flatKeyStr, Collections.singletonList(entry.getValue()));
                } else {
                    buildResultSetForElement(ctx, flatKeyStr, String.valueOf(entry.getValue()));
                }
            });
    }

    /**
     * Flat the List to string array and build the {@link CompletionResultSet}
     */
    private void buildResultSetForList(CompletionContext ctx, String keyStr, List<?> propertyList) {
        final AtomicInteger count = new AtomicInteger(0);
        propertyList.forEach(e -> {
            if (e instanceof String) {
                String flatKeyStr = String.format("%s[%s]", keyStr, count.getAndIncrement());
                buildResultSetForElement(ctx, flatKeyStr, String.valueOf(e));
            } else if (e instanceof List) {
                buildResultSetForList(ctx, keyStr, (List<?>) e);
            } else if (e instanceof LinkedHashMap) {
                buildResultSetForLinkedHashMap(ctx, keyStr, propertyList);
            }
        });
    }

    private void buildResultSetForElement(CompletionContext ctx, String key, String value) {
        if (!isIgnored(key) && key.startsWith(ctx.prefix())) {
            LookupElement element = createLookupElement(ctx.query(), key, value, ctx.file());
            addResult(ctx.resultSet(), ctx.prefix(), element);
        }
    }

    private record CompletionContext(
        String prefix,
        VirtualFile file,
        CompletionQuery query,
        CompletionResultSet resultSet
    ) {}

}
