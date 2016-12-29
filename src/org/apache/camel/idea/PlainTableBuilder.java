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
import java.util.Arrays;
import java.util.List;

/**
 * To build a table in plain text.
 *
 * @deprecated currently not in use, delete if we don't need it
 */
@Deprecated
public class PlainTableBuilder {

    private final List<Integer> minLength = new ArrayList<>();
    private final List<String> headers = new ArrayList<>();
    private final List<List<String>> rows = new ArrayList<>();

    public PlainTableBuilder withHeader(String header) {
        headers.add(header);
        // use 20 as minimum length by default
        minLength.add(20);
        return this;
    }

    public PlainTableBuilder withRow(String... values) {
        rows.add(Arrays.asList(values));

        // skip min length for last column
        for (int i = 0; i < values.length - 1; i++) {
            String value = values[i];
            int len = Math.max(minLength.get(i), value.length());
            minLength.set(i, len);
        }

        return this;
    }

    public boolean isEmpty() {
        return rows.isEmpty();
    }

    public String build() {
        StringBuilder sb = new StringBuilder();
        for (String header : headers) {
            String line = rightPad(header, 20);
            sb.append(line);
            // tab between each option
            sb.append("\t\t");
        }
        sb.append("\n");
        for (List<String> row : rows) {
            for (int i = 0; i < row.size(); i++) {
                String value = row.get(i);
                String line;
                if (i < row.size() - 1) {
                    int len = minLength.get(i);
                    line = rightPad(value, len);
                } else {
                    // last line can be as long as it want
                    line = value;
                }
                sb.append(line);
                // tab between each option
                sb.append("\t\t");
            }
            sb.append("\n");
        }
        sb.append("\n");
        return sb.toString();
    }

    private String leftPad(String text, int length) {
        // use &nbsp; to force IDEA to show as space (otherwise double spaces are not respected)
        StringBuilder sb = new StringBuilder();
        int len = length - text.length();
        for (int i = 0; i < len; i++) {
            sb.append("&nbsp;");
        }
        sb.append(text);
        return sb.toString();
    }

    private String rightPad(String text, int length) {
        // use &nbsp; to force IDEA to show as space (otherwise double spaces are not respected)
        StringBuilder sb = new StringBuilder();
        sb.append(text);
        int len = length - text.length();
        for (int i = 0; i < len; i++) {
            sb.append("&nbsp;");
        }
        return sb.toString();
    }

}
