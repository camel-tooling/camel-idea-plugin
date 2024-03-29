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
package com.github.cameltooling.idea.catalog;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.camel.catalog.JSonSchemaResolver;
import org.apache.camel.tooling.model.ComponentModel;
import org.apache.camel.tooling.model.JsonMapper;
import org.apache.camel.tooling.model.MainModel;
import org.junit.Test;

import static com.intellij.testFramework.UsefulTestCase.assertContainsElements;
import static com.intellij.testFramework.UsefulTestCase.assertDoesntContain;
import static com.intellij.testFramework.UsefulTestCase.assertNotEmpty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * The test class for {@link SpringBootJSonSchemaResolver}.
 */
public class SpringBootJSonSchemaResolverTest {

    /**
     * Ensure that the content of a Main schema can be retrieved from the classpath.
     */
    @Test
    public void retrieveContentMainSchema() {
        SpringBootJSonSchemaResolver resolver = new SpringBootJSonSchemaResolver(
            () -> this.getClass().getClassLoader(), new MockJSonSchemaResolver()
        );
        String mainJsonSchema = resolver.getMainJsonSchema();
        assertNotNull(mainJsonSchema);
        MainModel model = JsonMapper.generateMainModel(mainJsonSchema);
        assertNotEmpty(model.getGroups());
        List<String> groupNames = model.getGroups().stream().map(MainModel.MainGroupModel::getName)
            .collect(Collectors.toList());
        assertContainsElements(groupNames, "camel.springboot", "camel.cloud", "camel.cluster.file");
        assertNotEmpty(model.getOptions());
        List<String> optionNames = model.getOptions().stream().map(MainModel.MainOptionModel::getName)
            .collect(Collectors.toList());
        assertContainsElements(optionNames, "camel.springboot.auto-startup", "camel.cluster.file.enabled");
    }

    /**
     * Ensure that the content of a Component schema can be retrieved from the classpath when it is accessible.
     */
    @Test
    public void retrieveExistingContentComponentSchema() {
        SpringBootJSonSchemaResolver resolver = new SpringBootJSonSchemaResolver(
            () -> this.getClass().getClassLoader(), new MockJSonSchemaResolver()
        );
        String componentJsonSchema = resolver.getComponentJSonSchema("file");
        assertNotNull(componentJsonSchema);
        final ComponentModel model = JsonMapper.generateComponentModel(componentJsonSchema);
        assertEquals("file", model.getName());
        assertNotEmpty(model.getEndpointHeaders());
        assertEquals(1, model.getEndpointHeaders().size());
        assertEquals("someHeader", model.getEndpointHeaders().get(0).getName());
        assertNotEmpty(model.getEndpointOptions());
        assertEquals(1, model.getEndpointOptions().size());
        assertEquals("someProperty", model.getEndpointOptions().get(0).getName());
        assertNotEmpty(model.getComponentOptions());
        List<String> optionNames = model.getOptions().stream().map(ComponentModel.ComponentOptionModel::getName)
            .collect(Collectors.toList());
        assertDoesntContain(optionNames, "customizer.enabled", "camel.cluster.file.enabled", "file.enabled", "someComponentProperty");
        assertContainsElements(optionNames, "enabled");
    }

    /**
     * Ensure that the content of a Component schema can be retrieved even when not accessible from the classpath.
     */
    @Test
    public void retrieveNonExistingContentComponentSchema() {
        SpringBootJSonSchemaResolver resolver = new SpringBootJSonSchemaResolver(
            () -> this.getClass().getClassLoader(), new MockJSonSchemaResolver()
        );
        String componentJsonSchema = resolver.getComponentJSonSchema("ftp");
        assertNotNull(componentJsonSchema);
        final ComponentModel model = JsonMapper.generateComponentModel(componentJsonSchema);
        assertEquals("ftp", model.getName());
        assertNotEmpty(model.getEndpointHeaders());
        assertEquals(1, model.getEndpointHeaders().size());
        assertEquals("someHeader", model.getEndpointHeaders().get(0).getName());
        assertNotEmpty(model.getEndpointOptions());
        assertEquals(1, model.getEndpointOptions().size());
        assertEquals("someProperty", model.getEndpointOptions().get(0).getName());
        assertNotEmpty(model.getComponentOptions());
        assertEquals(1, model.getComponentOptions().size());
        assertEquals("someComponentProperty", model.getComponentOptions().get(0).getName());
    }


    public static class MockJSonSchemaResolver implements JSonSchemaResolver {

        @Override
        public void setClassLoader(ClassLoader classLoader) {
        }

        @Override
        public String getComponentJSonSchema(String name) {
            return String.format("{\n" +
                "  \"component\": {\n" +
                "    \"kind\": \"component\",\n" +
                "    \"name\": \"%s\",\n" +
                "  },\n" +
                "  \"componentProperties\": {\n" +
                "    \"someComponentProperty\": { \n" +
                "      \"kind\": \"property\", \"displayName\": \"Some Component Property\", \"group\": \"consumer\", \"label\": \"consumer\", \"required\": false, \"type\": \"string\", \"javaType\": \"string\", \n" +
                "      \"deprecated\": false, \"autowired\": false, \"secret\": false, \"defaultValue\": false, \"description\": \"Some Component Property Description.\" \n" +
                "    }\n" +
                "  },\n" +
                "  \"headers\": {\n" +
                "    \"someHeader\": { \n" +
                "      \"kind\": \"header\", \"displayName\": \"\", \"group\": \"consumer\", \"label\": \"consumer\", \"required\": false, \"javaType\": \"long\", \"deprecated\": false, \n" +
                "      \"deprecationNote\": \"\", \"autowired\": false, \"secret\": false, \"description\": \"Some Header Descrption\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"properties\": {\n" +
                "    \"someProperty\": { \n" +
                "      \"kind\": \"parameter\", \"displayName\": \"Some Property\", \"group\": \"common\", \"label\": \"\", \"required\": false, \"type\": \"string\", \"javaType\": \"java.lang.String\", \n" +
                "      \"deprecated\": false, \"autowired\": false, \"secret\": false, \"description\": \"Some Property Description.\" \n" +
                "    }\n" +
                "  }\n" +
                "}", name);
        }

        @Override
        public String getDataFormatJSonSchema(String name) {
            return null;
        }

        @Override
        public String getLanguageJSonSchema(String name) {
            return null;
        }

        @Override
        public String getOtherJSonSchema(String name) {
            return null;
        }

        @Override
        public String getModelJSonSchema(String name) {
            return null;
        }

        @Override
        public String getMainJsonSchema() {
            return null;
        }

        @Override
        public String getTransformerJSonSchema(String name) {
            return null;
        }

        @Override
        public String getDevConsoleJSonSchema(String name) {
            return null;
        }

        @Override
        public String getPojoBeanJSonSchema(String name) {
            return null;
        }
    }
}
