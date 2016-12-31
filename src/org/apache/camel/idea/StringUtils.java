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

    /**
     * To wrap a big line by a separator.
     *
     * @param line the big line
     * @param separator the separator char such as <tt>&</tt>
     * @param newLine the new line to use when breaking into a new line
     * @param watermark a watermark to denote the size to cut after
     */
    public static String wrapSeparator(String line, String separator, String newLine, int watermark) {
        StringBuilder sb = new StringBuilder();
        String[] parts = line.split(separator);

        StringBuilder part = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String word = parts[i];
            part.append(word);
            if (i < parts.length - 1) {
                part.append(separator);
            }
            // did we hit watermark then reset
            if (part.length() >= watermark) {
                // move separator to new line
                String add = part.toString();
                if (add.endsWith(separator)) {
                    add = add.substring(0, add.length() - separator.length());
                }
                sb.append(add);
                sb.append(newLine);
                sb.append(separator);
                part.setLength(0);
            }
        }
        // any leftover
        if (part.length() > 0) {
            sb.append(part.toString());
        }

        String answer = sb.toString();
        if (answer.endsWith(newLine)) {
            answer = answer.substring(0, answer.length() - newLine.length());
        }
        return answer;
    }

}
