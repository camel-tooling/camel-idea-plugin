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
package com.github.cameltooling.idea.completion;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.intellij.openapi.roots.ModifiableRootModel;
import org.codehaus.plexus.util.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The integration test allowing to ensure that a specific legacy version of the catalog can be downloaded in case of
 * the Spring Boot runtime.
 */
public class PropertiesPropertyKeyCompletionSpringBootLegacyTestIT extends PropertiesPropertyKeyCompletionSpringBootTestIT {

    @Nullable
    @Override
    protected String[] getMavenDependencies() {
        return new String[]{
            "com.sun.xml.bind:jaxb-core:2.3.0",
            "com.sun.xml.bind:jaxb-impl:2.3.0",
            "org.apache.camel:camel-core:2.25.4",
            "org.apache.camel:camel-spring-boot:2.25.4",
            "org.apache.camel:camel-sql-starter:2.25.4"
        };
    }

    @Override
    protected void loadDependencies(@NotNull ModifiableRootModel model) {
        // Ugly hack to prevent Grape issues due to the absence of jaxb-core-2.3.0.jar in the local maven repository only the pom exists
        // Here we remove jaxb-core and jaxb-impl from the local maven repository and from the Gradle cache
        // to ensure that the jar file will properly be downloaded along with its pom file
        try {
            File[] files = getMavenArtifacts("com.sun.xml.bind:jaxb-core:2.3.0", "org.apache.camel:camel-core:2.25.4");
            // Remove the folder of jaxb-core and jaxb-impl from the Gradle cache
            FileUtils.deleteDirectory(files[0].getParentFile().getParentFile().getParentFile().getParentFile());
            // Remove the folder of jaxb-core and jaxb-impl from the local maven repository
            FileUtils.deleteDirectory(
                new File(
                    files[1].getParentFile().getParentFile().getParentFile().getParentFile().getParentFile().getParentFile(),
                    "com/sun/xml/bind"
                )
            );
            super.loadDependencies(model);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void assertComponentOptionSuggestion(List<String> strings) {
        assertContainsElements(strings, "camel.component.sql.data-source = ", "camel.component.sql.use-placeholder = ",
            "camel.component.sql.resolve-property-placeholders = ", "camel.component.sql.enabled = ");
    }

    protected void assertDataFormatNameSuggestion(List<String> strings) {
        assertContainsElements(strings, "camel.dataformat.json-jackson.", "camel.dataformat.csv.", "camel.dataformat.bindy-csv.");
    }

    /**
     * Ensures that data format option suggestions can properly be proposed even with an old name.
     */
    public void testDataFormatOptionWithOldName() {
        myFixture.configureByFiles(getFileName("data-format-options-with-old-name"));
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertContainsElements(
            strings, "camel.dataformat.json-jackson.include = ", "camel.dataformat.json-jackson.pretty-print = ",
            "camel.dataformat.json-jackson.json-view = "
        );
        assertDoesntContain(strings, "camel.dataformat.json-jackson.id = ");
    }
}