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

import java.util.List;
import java.util.Map;

/**
 * Various utility methods.
 */
public final class StringUtils {

    private StringUtils() {
    }

    public static boolean hasQuestionMark(String val) {
        return val.indexOf('?') > 0;
    }

    public static String asComponentName(String val) {
        if (val == null) {
            return null;
        }

        int pos = val.indexOf(':');
        if (pos > 0) {
            return val.substring(0, pos);
        }
        return null;
    }

    public static String asLanguageName(String val) {
        if (val == null) {
            return null;
        }

        if (val.startsWith("tokenize")) {
            return val;
        } else if (val.equals("js") || val.equals("javascript")) {
            return "javaScript";
        }

        return val;
    }

    /**
     * Gets the value with the key in a safe way, eg returning an empty string if there was no value for the key.
     */
    public static String getSafeValue(String key, List<Map<String, String>> rows) {
        for (Map<String, String> row : rows) {
            String value = row.get(key);
            if (value != null) {
                return value;
            }
        }
        return "";
    }

    /**
     * Gets the value with the key in a safe way, eg returning an empty string if there was no value for the key.
     */
    public static String getSafeValue(String key, Map<String, String> rows) {
        String value = rows.get(key);
        if (value != null) {
            return value;
        }
        return "";
    }

}
