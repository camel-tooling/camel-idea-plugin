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
package org.apache.camel.idea.gutter;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestCase;

import java.io.IOException;

public class CamelRouteSearchScopeTest extends PlatformTestCase {

    private CamelRouteSearchScope camelRouteSearchScope = new CamelRouteSearchScope();

    public void test_search_scope_should_contain_java_file() throws IOException {
        VirtualFile javaFile = getVirtualFile(createTempFile("Test.java", ""));
        assertTrue(camelRouteSearchScope.contains(javaFile));
    }

    public void test_search_scope_should_contain_xml_file() throws IOException {
        VirtualFile xmlFile = getVirtualFile(createTempFile("test.xml", ""));
        assertTrue(camelRouteSearchScope.contains(xmlFile));
    }

    public void test_search_scope_should_contain_pom_xml() throws IOException {
        VirtualFile mavenPomFile = getVirtualFile(createTempFile("pom.xml", ""));
        assertFalse(camelRouteSearchScope.contains(mavenPomFile));
    }

    public void test_search_scope_should_contain_xsd_file() throws IOException {
        VirtualFile xsdFile = getVirtualFile(createTempFile("test.xsd", ""));
        assertFalse(camelRouteSearchScope.contains(xsdFile));
    }
}