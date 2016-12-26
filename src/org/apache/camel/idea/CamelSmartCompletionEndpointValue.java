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
import java.util.Map;

import com.intellij.codeInsight.lookup.AutoCompletionPolicy;
import com.intellij.codeInsight.lookup.LookupElementBuilder;

public class CamelSmartCompletionEndpointValue {

    public static List<Object> addSmartCompletionForSingleValue(String val, List<Map<String, String>> rows, String name) {
        List<Object> answer = new ArrayList<Object>();

        Map<String, String> found = null;
        for (Map<String, String> row : rows) {
            if (name.equals(row.get("name"))) {
                found = row;
                break;
            }
        }
        if (found != null) {
            String javaType = found.get("javaType");
            String deprecated = found.get("deprecated");
            String enums = found.get("enum");
            String defaultValue = found.get("defaultValue");

            if (enums != null) {
                addEnumSuggestions(val, answer, deprecated, enums, defaultValue);
            } else if ("java.lang.Boolean".equals(javaType) || "boolean".equals(javaType)) {
                addBooleanSuggestions(val, answer, deprecated, defaultValue);
            } else if (defaultValue != null) {
                // for any other kind of type and if there is a default value then add that as a suggestion
                // so its easy to see what the default value is
                addDefaultValueSuggestions(val, answer, deprecated, defaultValue);
            }
        }

        return answer;
    }

    private static void addEnumSuggestions(String val, List<Object> answer, String deprecated, String enums, String defaultValue) {
        String[] parts = enums.split(",");
        for (String part : parts) {
            String lookup = val + part;
            LookupElementBuilder builder = LookupElementBuilder.create(lookup);
            // only show the option in the UI
            builder = builder.withPresentableText(part);
            builder = builder.withBoldness(true);
            if ("true".equals(deprecated)) {
                // mark as deprecated
                builder = builder.withStrikeoutness(true);
            }
            boolean isDefaultValue = defaultValue != null && part.equals(defaultValue);
            if (isDefaultValue) {
                builder = builder.withTailText(" (default value)");
                // add default value first in the list
                answer.add(0, builder.withAutoCompletionPolicy(AutoCompletionPolicy.GIVE_CHANCE_TO_OVERWRITE));
            } else {
                answer.add(builder.withAutoCompletionPolicy(AutoCompletionPolicy.GIVE_CHANCE_TO_OVERWRITE));
            }
        }
    }

    private static void addBooleanSuggestions(String val, List<Object> answer, String deprecated, String defaultValue) {
        // for boolean types then give a choice between true|false
        String lookup = val + "true";
        LookupElementBuilder builder = LookupElementBuilder.create(lookup);
        // only show the option in the UI
        builder = builder.withPresentableText("true");
        if ("true".equals(deprecated)) {
            // mark as deprecated
            builder = builder.withStrikeoutness(true);
        }
        boolean isDefaultValue = defaultValue != null && "true".equals(defaultValue);
        if (isDefaultValue) {
            builder = builder.withTailText(" (default value)");
            // add default value first in the list
            answer.add(0, builder.withAutoCompletionPolicy(AutoCompletionPolicy.GIVE_CHANCE_TO_OVERWRITE));
        } else {
            answer.add(builder.withAutoCompletionPolicy(AutoCompletionPolicy.GIVE_CHANCE_TO_OVERWRITE));
        }

        lookup = val + "false";
        builder = LookupElementBuilder.create(lookup);
        // only show the option in the UI
        builder = builder.withPresentableText("false");
        if ("true".equals(deprecated)) {
            // mark as deprecated
            builder = builder.withStrikeoutness(true);
        }
        isDefaultValue = defaultValue != null && "false".equals(defaultValue);
        if (isDefaultValue) {
            builder = builder.withTailText(" (default value)");
            // add default value first in the list
            answer.add(0, builder.withAutoCompletionPolicy(AutoCompletionPolicy.GIVE_CHANCE_TO_OVERWRITE));
        } else {
            answer.add(builder.withAutoCompletionPolicy(AutoCompletionPolicy.GIVE_CHANCE_TO_OVERWRITE));
        }
    }

    private static void addDefaultValueSuggestions(String val, List<Object> answer, String deprecated, String defaultValue) {
        String lookup = val + defaultValue;
        LookupElementBuilder builder = LookupElementBuilder.create(lookup);
        // only show the option in the UI
        builder = builder.withPresentableText(defaultValue);
        if ("true".equals(deprecated)) {
            // mark as deprecated
            builder = builder.withStrikeoutness(true);
        }
        builder = builder.withTailText(" (default value)");
        // there is only one value in the list and its the default value, so never auto complete it but show as suggestion
        answer.add(0, builder.withAutoCompletionPolicy(AutoCompletionPolicy.NEVER_AUTOCOMPLETE));
    }

}
