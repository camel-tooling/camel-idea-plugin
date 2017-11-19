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
package org.apache.camel.idea.util;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestCase;

public class CamelRouteSearchScopeTest extends PlatformTestCase {


    public void testSearchScope() throws Exception {
        VirtualFile javaFile = getVirtualFile(createTempFile("Test.java", ""));
        VirtualFile xmlFile = getVirtualFile(createTempFile("test.xml", ""));


        VirtualFile mavenPomFile = getVirtualFile(createTempFile("pom.xml", ""));
        VirtualFile xsdFile = getVirtualFile(createTempFile("test.xsd", ""));

        CamelRouteSearchScope camelRouteSearchScope = new CamelRouteSearchScope();

        assertTrue(camelRouteSearchScope.contains(javaFile));
        assertTrue(camelRouteSearchScope.contains(xmlFile));

        assertFalse(camelRouteSearchScope.contains(mavenPomFile));
        assertFalse(camelRouteSearchScope.contains(xsdFile));
    }
}