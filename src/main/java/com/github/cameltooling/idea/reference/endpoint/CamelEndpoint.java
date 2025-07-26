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
package com.github.cameltooling.idea.reference.endpoint;

import com.github.cameltooling.idea.util.StringUtils;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a camel endpoint and provides support methods for working with the parts of its uri.
 */
public class CamelEndpoint {

    private final String uri;
    private String baseUri;
    private String prefix;
    private String name;
    private String query;

    public CamelEndpoint(String uri) {
        this.uri = StringUtil.unquoteString(uri);
        processUri();
    }

    private void processUri() {
        int questionMarkIndex = uri.indexOf('?');
        if (questionMarkIndex >= 0) {
            baseUri = uri.substring(0, questionMarkIndex);
            query = (uri.length() > questionMarkIndex + 1) ? uri.substring(questionMarkIndex + 1) : "";
        } else {
            baseUri = uri;
            query = null;
        }

        prefix = StringUtils.asComponentName(baseUri);

        name = prefix == null ? baseUri : baseUri.substring(prefix.length());
    }

    public String getUri() {
        return uri;
    }

    public String getName() {
        return name;
    }

    public String getBaseUri() {
        return baseUri;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getQuery() {
        return query;
    }

    public boolean baseUriMatches(@Nullable String endpointUri) {
        if (endpointUri != null) {
            CamelEndpoint other = new CamelEndpoint(endpointUri);
            return baseUri.equals(other.baseUri);
        }
        return false;
    }

    public TextRange getNameTextRange() {
        return TextRange.from(prefix.length(), name.length());
    }

}
