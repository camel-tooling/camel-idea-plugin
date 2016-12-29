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

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionUtil;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.util.ProcessingContext;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.catalog.JSonSchemaHelper;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.apache.camel.idea.CamelSmartCompletionEndpointOptions.addSmartCompletionSuggestions;
import static org.apache.camel.idea.CamelSmartCompletionEndpointValue.addSmartCompletionForSingleValue;

/**
 * Hook into the IDEA language completion system, to setup Camel smart completion.
 * Extend this class to define what it should re-act on when using smart completion
 */
public class CamelContributor extends CompletionContributor {

    private static final CamelCatalog camelCatalog = new DefaultCamelCatalog(true);

    static protected class PropertyCompletion extends CompletionProvider<CompletionParameters> {

        public void addCompletions(@NotNull CompletionParameters parameters,
                ProcessingContext context,
                @NotNull CompletionResultSet resultSet) {
            // is this a possible Camel endpoint uri which we know
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
                val = val.substring(0,val.length()-1);
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

}
