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
package com.github.cameltooling.idea.preference.propertyplaceholder;

import java.util.ArrayList;
import java.util.List;

public final class PropertyPlaceholderSettingsEntry {

    private String startToken;
    private String endToken;
    private List<String> namespaces = new ArrayList<>();
    private boolean enabled;

    @SuppressWarnings("unused") // serialized to XML by IDEA
    public PropertyPlaceholderSettingsEntry() {

    }

    public PropertyPlaceholderSettingsEntry(String startToken,
                                            String endToken,
                                            List<String> namespaces,
                                            boolean enabled) {
        this.startToken = startToken;
        this.endToken = endToken;
        this.namespaces = namespaces;
        this.enabled = enabled;
    }

    public String getStartToken() {
        return startToken;
    }

    public void setStartToken(String startToken) {
        this.startToken = startToken;
    }

    public String getEndToken() {
        return endToken;
    }

    public void setEndToken(String endToken) {
        this.endToken = endToken;
    }

    public List<String> getNamespaces() {
        return namespaces;
    }

    public void setNamespaces(List<String> namespaces) {
        this.namespaces = namespaces;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}
