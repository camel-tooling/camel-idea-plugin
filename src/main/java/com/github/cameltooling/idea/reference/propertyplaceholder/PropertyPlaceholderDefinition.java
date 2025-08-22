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
package com.github.cameltooling.idea.reference.propertyplaceholder;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

public class PropertyPlaceholderDefinition {

    private final String startToken;
    private final String endToken;
    private final Pattern pattern;

    public PropertyPlaceholderDefinition(@NotNull String startToken, @NotNull String endToken) {
        if (startToken.isEmpty()) {
            throw new IllegalArgumentException("startToken must not be empty");
        }
        if (endToken.isEmpty()) {
            throw new IllegalArgumentException("endToken must not be empty");
        }
        this.startToken = startToken;
        this.endToken = endToken;
        String endTokenStartChar = endToken.substring(0, 1);
        this.pattern = Pattern.compile(Pattern.quote(startToken) + "([^" + Pattern.quote(endTokenStartChar) + "]*)" + Pattern.quote(endToken));
    }

    public String getStartToken() {
        return startToken;
    }

    public String getEndToken() {
        return endToken;
    }

    public Pattern getPattern() {
        return pattern;
    }
}
