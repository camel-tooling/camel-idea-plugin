/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.idea;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionUtil;
import com.intellij.codeInsight.completion.PlainPrefixMatcher;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.util.ProcessingContext;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.catalog.JSonSchemaHelper;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.apache.camel.idea.CamelSmartCompletionEndpointOptions.addSmartCompletionSuggestions;
import static org.apache.camel.idea.CamelSmartCompletionEndpointValue.addSmartCompletionForSingleValue;

/**
 * Hook into the IDEA language completion system, to setup Camel smart completion.
 * Extend this class to define what it should re-act on when using smart completion
 */
public class CamelContributor extends CompletionContributor {

    private static final CamelCatalog camelCatalog = new DefaultCamelCatalog(true);
    //TODO Allow this to be configurable
    private static final List<String> IGNORE_PROPERTIES = Arrays.asList("java.", "Logger.", "logger", "appender.", "rootLogger.");

    /**
     * Smart completion for Camel endpoints.
     */
    static protected class EndpointCompletion extends CompletionProvider<CompletionParameters> {

        public void addCompletions(@NotNull CompletionParameters parameters,
                                   ProcessingContext context,
                                   @NotNull CompletionResultSet resultSet) {
            // is this a possible Camel endpoint uri which we know
            String val = parsePsiElement(parameters);

            if (val.endsWith("{{")) {
                propertyPlaceholdersSmartCompletion(parameters, resultSet);
                return; //we are done
            }

            String componentName = StringUtils.asComponentName(val);
            if (componentName != null && camelCatalog.findComponentNames().contains(componentName)) {

                // it is a known Camel component
                String json = camelCatalog.componentJSonSchema(componentName);

                // gather list of names
                List<Map<String, String>> rows = JSonSchemaHelper.parseJsonSchema("properties", json, true);

                // grab all existing parameters
                String query = val;
                // strip up ending incomplete parameter
                if (query.endsWith("&") || query.endsWith("?")) {
                    query = query.substring(0, query.length() - 1);
                }

                Map<String, String> existing = null;
                try {
                    existing = camelCatalog.endpointProperties(query);
                } catch (Exception e) {
                    // ignore
                }

                // are we editing an existing parameter value
                // or are we having a list of suggested parameters to choose among
                boolean editSingle = val.endsWith("=");
                ArrayList<LookupElement> answer;
                if (editSingle) {
                    // parameter name is before = and & or ?
                    int pos = Math.max(val.lastIndexOf('&'), val.lastIndexOf('?'));
                    String name = val.substring(pos + 1);
                    name = name.substring(0, name.length() - 1); // remove =
                    answer = (ArrayList<LookupElement>) addSmartCompletionForSingleValue(val, rows, name);
                } else {
                    answer = (ArrayList<LookupElement>) addSmartCompletionSuggestions(val, rows, existing);
                }
                if (!answer.isEmpty()) {
                    resultSet.withPrefixMatcher(val).addAllElements(answer);
                }
            }
        }
    }

    /**
     * Parse the PSI text {@link CompletionUtil#DUMMY_IDENTIFIER} and " character and remove them
     * @param parameters - completion parameter to parse
     * @return new string stripped for any {@link CompletionUtil#DUMMY_IDENTIFIER} and " character
     */
    @NotNull
    private static String parsePsiElement(@NotNull CompletionParameters parameters) {
        String val = parameters.getPosition().getText();
        int hackIndex = val.indexOf(CompletionUtil.DUMMY_IDENTIFIER);
        if (hackIndex == -1) {
            hackIndex = val.indexOf(CompletionUtil.DUMMY_IDENTIFIER_TRIMMED);
        }
        if (hackIndex > -1) {
            val = val.substring(0, hackIndex);
        }

        if (val.startsWith("\"")) {
            val = val.substring(1);
        }
        if (val.endsWith("\"")) {
            val = val.substring(0, val.length() - 1);
        }
        return val;
    }

    private static void propertyPlaceholdersSmartCompletion(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet resultSet) {
        VfsUtil.processFilesRecursively(parameters.getOriginalFile().getManager().getProject().getBaseDir(), virtualFile -> {
            if (virtualFile.getName().endsWith(".properties")) {
                File file = new File(virtualFile.getPath());
                Properties properties = new Properties();
                try {
                    properties.load(Files.newInputStream(file.toPath()));
                } catch (IOException e) {
                }//TODO : log a warning, but for now we ignore it and continue.

                properties.forEach((key, value) -> {
                            String keyStr = (String) key;
                            boolean noneMatch = IGNORE_PROPERTIES.stream().noneMatch(s -> keyStr.startsWith(s));
                            if (noneMatch) {
                                LookupElementBuilder builder = LookupElementBuilder.create(keyStr+"}}")
                                        .appendTailText((String) value, true)
                                        .withPresentableText(keyStr + " = ");
                                resultSet.withPrefixMatcher(new PlainPrefixMatcher("")).addElement(builder);
                            }
                        }
                );
            }
            return true;
        });
    }

}
