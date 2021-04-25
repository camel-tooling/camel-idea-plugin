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
package com.github.cameltooling.idea.model;

import java.util.List;
import java.util.Map;

import org.apache.camel.util.json.DeserializationException;
import org.apache.camel.util.json.JsonObject;
import org.apache.camel.util.json.Jsoner;

public final class ModelHelper {

    private ModelHelper() {
        // utility class
    }

    public static ComponentModel generateComponentModel(String json, boolean includeOptions) {
        JsonObject jsonObject;
        try {
            jsonObject = (JsonObject) Jsoner.deserialize(json);
        } catch (DeserializationException e) {
            throw new RuntimeException(e);
        }
        JsonObject componentJsonObject = new JsonObject(jsonObject.getMap("component"));

        ComponentModel component = new ComponentModel();
        component.setScheme(componentJsonObject.getString("scheme"));
        component.setSyntax(componentJsonObject.getString("syntax"));
        component.setAlternativeSyntax(componentJsonObject.getString("alternativeSyntax"));
        component.setAlternativeSchemes(componentJsonObject.getString("alternativeSchemes"));
        component.setTitle(componentJsonObject.getString("title"));
        component.setDescription(componentJsonObject.getString("description"));
        component.setLabel(componentJsonObject.getString("label"));
        component.setDeprecated(componentJsonObject.getString("deprecated"));
        component.setConsumerOnly(componentJsonObject.getString("consumerOnly"));
        component.setProducerOnly(componentJsonObject.getString("producerOnly"));
        component.setJavaType(componentJsonObject.getString("javaType"));
        component.setGroupId(componentJsonObject.getString("groupId"));
        component.setArtifactId(componentJsonObject.getString("artifactId"));
        component.setVersion(componentJsonObject.getString("version"));

        if (includeOptions) {
            Map<String, JsonObject> componentProperties = jsonObject.getMap("componentProperties");
            for (Map.Entry<String, JsonObject> row : componentProperties.entrySet()) {
                JsonObject property = row.getValue();
                ComponentOptionModel option = new ComponentOptionModel();
                option.setName(property.getStringOrDefault("name", row.getKey()));
                option.setKind(property.getStringOrDefault("kind", ""));
                option.setGroup(property.getStringOrDefault("group", ""));
                option.setRequired(property.getStringOrDefault("required", ""));
                option.setType(property.getStringOrDefault("type", ""));
                option.setJavaType(property.getStringOrDefault("javaType", ""));
                option.setEnums(jasonArrayToString(property.getCollection("enum")));
                option.setDeprecated(property.getStringOrDefault("deprecated", ""));
                option.setSecret(property.getStringOrDefault("secret", ""));
                option.setDefaultValue(property.getStringOrDefault("defaultValue", ""));
                option.setDescription(property.getStringOrDefault("description", ""));
                component.addComponentOption(option);
            }

            Map<String, JsonObject> properties = jsonObject.getMap("properties");
            for (Map.Entry<String, JsonObject> row : properties.entrySet()) {
                JsonObject property = row.getValue();
                EndpointOptionModel option = new EndpointOptionModel();
                option.setName(property.getStringOrDefault("name", row.getKey()));
                option.setKind(property.getStringOrDefault("kind", ""));
                option.setGroup(property.getStringOrDefault("group", ""));
                option.setLabel(property.getStringOrDefault("label", ""));
                option.setRequired(property.getStringOrDefault("required", ""));
                option.setType(property.getStringOrDefault("type", ""));
                option.setJavaType(property.getStringOrDefault("javaType", ""));
                option.setEnums(jasonArrayToString(property.getCollection("enum")));
                option.setPrefix(property.getStringOrDefault("prefix", ""));
                option.setMultiValue(property.getStringOrDefault("multiValue", ""));
                option.setDeprecated(property.getStringOrDefault("deprecated", ""));
                option.setSecret(property.getStringOrDefault("secret", ""));
                option.setDefaultValue(property.getStringOrDefault("defaultValue", ""));
                option.setDescription(property.getStringOrDefault("description", ""));
                component.addEndpointOption(option);
            }
        }

        return component;
    }

    private static String jasonArrayToString(List<String> in) {
        if (in != null) {
            return String.join(",", in);
        }
        return "";
    }
}
