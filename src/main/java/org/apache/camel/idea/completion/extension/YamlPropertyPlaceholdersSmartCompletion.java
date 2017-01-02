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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.PlainPrefixMatcher;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.Yaml;

/**
 * Smart completion for editing a Camel endpoint uri, to show a list of YAML properties can be added.
 * For example editing <tt>jms:queue?{{_CURSOR_HERE_</tt>. Which presents the user
 * with a list of possible properties.
 */
public class YamlPropertyPlaceholdersSmartCompletion implements CamelPropertyCompletion {

    @NotNull
    private Map<String, Object> getProperties(VirtualFile virtualFile) {
        Map<String, Object> result = new HashMap<>();
        Yaml yaml = new Yaml();
        try {
            // Parse the YAML file and return the output as a series of Maps and Lists
            result = (Map<String, Object>) yaml.load(virtualFile.getInputStream());
        } catch (Exception e) {
        }//TODO : log a warning, but for now we ignore it and continue.
        return result;
    }

    @Override
    public boolean isValidExtension(String filename) {
        return filename.endsWith(".yaml") || filename.endsWith(".yml");
    }

    @Override
    public void buildResultSet(CompletionResultSet resultSet, VirtualFile virtualFile) {
        getProperties(virtualFile).forEach((key, value) -> {
            final String keyStr = key;
            if (!isIgnored(key)) {
                if (value instanceof List) {
                    buildResultSetForList(resultSet, virtualFile, keyStr, (List) value);
                } else if (value instanceof LinkedHashMap) {
                    buildResultSetForLinkedHashMap(resultSet, virtualFile, keyStr, Collections.singletonList(value));
                } else {
                    buildResultSet(resultSet, virtualFile, keyStr, String.valueOf(value));
                }
            }
        });
    }

    /**
     * Flat the {@link LinkedHashMap} to string property names and build the {@link CompletionResultSet}
     */
    private void buildResultSetForLinkedHashMap(CompletionResultSet resultSet, VirtualFile virtualFile, String keyStr, List<?> propertyList) {
        propertyList.stream()
            .filter(l -> l instanceof LinkedHashMap)
            .map(LinkedHashMap.class::cast)
            .flatMap(lhm -> lhm.entrySet().stream())
            .forEach(e -> {
                        Map.Entry<String, Object> entry = (Map.Entry<String, Object>) e;
                        String flatKeyStr = keyStr + "." + String.valueOf(entry.getKey());
                        if (entry.getValue() instanceof List) {
                            buildResultSetForList(resultSet, virtualFile, flatKeyStr, (List<?>) entry.getValue());
                        } else if (entry.getValue() instanceof LinkedHashMap) {
                            buildResultSetForLinkedHashMap(resultSet, virtualFile, flatKeyStr, Collections.singletonList(entry.getValue()));
                        } else {
                            buildResultSet(resultSet, virtualFile, flatKeyStr, String.valueOf(entry.getValue()));
                        }
                    }

            );
    }

    /**
     * Flat the List to string array and build the {@link CompletionResultSet}
     */
    private void buildResultSetForList(CompletionResultSet resultSet, VirtualFile virtualFile, String keyStr, List<?> propertyList) {
        final AtomicInteger count = new AtomicInteger(0);
        propertyList.stream().forEach(e -> {
            if (e instanceof String) {
                String flatKeyStr = String.format("%s[%s]", keyStr, count.getAndIncrement());
                buildResultSet(resultSet, virtualFile, flatKeyStr, String.valueOf(e));
            } else if (e instanceof List) {
                buildResultSetForList(resultSet, virtualFile, keyStr, (List<?>) e);
            } else if (e instanceof LinkedHashMap) {
                buildResultSetForLinkedHashMap(resultSet, virtualFile, keyStr, propertyList);
            }
        });
    }

    private void buildResultSet(CompletionResultSet resultSet, VirtualFile virtualFile, String keyStr, String value) {
        if (!isIgnored(keyStr)) {
            LookupElementBuilder builder = LookupElementBuilder.create(keyStr + "}}")
                .appendTailText(value, true)
                .withTypeText("[" + virtualFile.getPresentableName() + "]", true)
                .withPresentableText(keyStr + " = ");
            resultSet.withPrefixMatcher(new PlainPrefixMatcher("")).addElement(builder);
        }
    }
}
