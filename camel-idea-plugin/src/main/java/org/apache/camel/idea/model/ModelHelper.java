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
package org.apache.camel.idea.model;

import java.util.List;
import java.util.Map;
import org.apache.camel.catalog.JSonSchemaHelper;
import static org.apache.camel.idea.util.StringUtils.getSafeValue;

public final class ModelHelper {

    private ModelHelper() {
        // utility class
    }

    public static ComponentModel generateComponentModel(String json, boolean includeOptions) {
        List<Map<String, String>> rows = JSonSchemaHelper.parseJsonSchema("component", json, false);

        ComponentModel component = new ComponentModel();
        component.setScheme(getSafeValue("scheme", rows));
        component.setSyntax(getSafeValue("syntax", rows));
        component.setAlternativeSyntax(getSafeValue("alternativeSyntax", rows));
        component.setAlternativeSchemes(getSafeValue("alternativeSchemes", rows));
        component.setTitle(getSafeValue("title", rows));
        component.setDescription(getSafeValue("description", rows));
        component.setLabel(getSafeValue("label", rows));
        component.setDeprecated(getSafeValue("deprecated", rows));
        component.setConsumerOnly(getSafeValue("consumerOnly", rows));
        component.setProducerOnly(getSafeValue("producerOnly", rows));
        component.setJavaType(getSafeValue("javaType", rows));
        component.setGroupId(getSafeValue("groupId", rows));
        component.setArtifactId(getSafeValue("artifactId", rows));
        component.setVersion(getSafeValue("version", rows));

        if (includeOptions) {
            rows = JSonSchemaHelper.parseJsonSchema("componentProperties", json, true);
            for (Map<String, String> row : rows) {
                ComponentOptionModel option = new ComponentOptionModel();
                option.setName(getSafeValue("name", row));
                option.setKind(getSafeValue("kind", row));
                option.setGroup(getSafeValue("group", row));
                option.setRequired(getSafeValue("required", row));
                option.setType(getSafeValue("type", row));
                option.setJavaType(getSafeValue("javaType", row));
                option.setEnums(getSafeValue("enum", row));
                option.setDeprecated(getSafeValue("deprecated", row));
                option.setSecret(getSafeValue("secret", row));
                option.setDefaultValue(getSafeValue("defaultValue", row));
                option.setDescription(getSafeValue("description", row));
                component.addComponentOption(option);
            }

            rows = JSonSchemaHelper.parseJsonSchema("properties", json, true);
            for (Map<String, String> row : rows) {
                EndpointOptionModel option = new EndpointOptionModel();
                option.setName(getSafeValue("name", row));
                option.setKind(getSafeValue("kind", row));
                option.setGroup(getSafeValue("group", row));
                option.setLabel(getSafeValue("label", row));
                option.setRequired(getSafeValue("required", row));
                option.setType(getSafeValue("type", row));
                option.setJavaType(getSafeValue("javaType", row));
                option.setEnums(getSafeValue("enum", row));
                option.setPrefix(getSafeValue("prefix", row));
                option.setMultiValue(getSafeValue("multiValue", row));
                option.setDeprecated(getSafeValue("deprecated", row));
                option.setSecret(getSafeValue("secret", row));
                option.setDefaultValue(getSafeValue("defaultValue", row));
                option.setDescription(getSafeValue("description", row));
                component.addEndpointOption(option);
            }
        }

        return component;
    }
}
