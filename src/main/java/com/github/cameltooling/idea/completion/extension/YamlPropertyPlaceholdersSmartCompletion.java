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

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import com.github.cameltooling.idea.util.CamelIdeaUtils;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLFile;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import static com.github.cameltooling.idea.completion.extension.CamelPropertyPlaceholderSmartCompletionExtension.PROP_PLACEHOLDER_START_TAG;

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
        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        Map<String, Object> result = new HashMap<>();
        try (InputStream is = virtualFile.getInputStream()){
            // Parse the YAML file and return the output as a series of Maps and Lists
            result = yaml.load(is); //TODO: this is a class cast exception when the yaml contains a list at root level, or a scalar
        } catch (Exception e) {
            LOG.warn("Error loading yaml file: " + virtualFile, e);
        }
        return result;
    }

    @Override
    public boolean isValidFile(@NotNull PsiFile file) {
        return !isExcludedFile(file) && file instanceof YAMLFile;
    }

    @Override
    public void buildResultSet(@NotNull ProcessingContext context, CompletionResultSet resultSet, CompletionQuery query, PsiFile file) {
        if (CamelIdeaUtils.getService().isCamelFile(file)) { //do not extract properties from camel route files
            return;
        }
        VirtualFile virtualFile = file.getVirtualFile();
        String startTag = context.get(PROP_PLACEHOLDER_START_TAG);
        String prefix = getPrefix(query, startTag == null ? "" : startTag);
        CompletionContext ctx = new CompletionContext(prefix, virtualFile, query, resultSet);
        getProperties(virtualFile).forEach((key, value) -> {
            if (!isIgnored(key) && haveCommonStart(key, prefix)) {
                if (value instanceof List) {
                    buildResultSetForList(ctx, key, (List<?>) value);
                } else if (value instanceof LinkedHashMap) {
                    buildResultSetForLinkedHashMap(ctx, key, Collections.singletonList(value));
                } else {
                    buildResultSetForElement(ctx, key, String.valueOf(value));
                }
            }
        });
    }

    private boolean haveCommonStart(String s1, String s2) {
        if (s1.length() > s2.length()) {
            return s1.startsWith(s2);
        } else {
            return s2.startsWith(s1);
        }
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
                if (haveCommonStart(flatKeyStr, ctx.prefix())) {
                    if (entry.getValue() instanceof List) {
                        buildResultSetForList(ctx, flatKeyStr, (List<?>) entry.getValue());
                    } else if (entry.getValue() instanceof LinkedHashMap) {
                        buildResultSetForLinkedHashMap(ctx, flatKeyStr, Collections.singletonList(entry.getValue()));
                    } else {
                        buildResultSetForElement(ctx, flatKeyStr, String.valueOf(entry.getValue()));
                    }
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
