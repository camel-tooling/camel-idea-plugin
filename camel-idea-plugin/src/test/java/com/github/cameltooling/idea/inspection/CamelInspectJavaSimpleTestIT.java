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
package com.github.cameltooling.idea.inspection;

import com.intellij.codeInspection.ex.LocalInspectionToolWrapper;
import com.intellij.testFramework.JavaInspectionTestCase;
import com.intellij.testFramework.PsiTestUtil;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;

import java.io.File;


public class CamelInspectJavaSimpleTestIT extends JavaInspectionTestCase {
    public static final String CAMEL_CORE_MAVEN_ARTIFACT = "org.apache.camel:camel-core:2.22.0";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        File[] mavenArtifacts =  Maven.resolver().resolve(CAMEL_CORE_MAVEN_ARTIFACT).withoutTransitivity().asFile();
        PsiTestUtil.addLibrary(myFixture.getProjectDisposable(), myFixture.getModule(), "Maven: " + CAMEL_CORE_MAVEN_ARTIFACT, mavenArtifacts[0].getParent(), mavenArtifacts[0].getName());
    }

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/";
    }

    public void testSimpleInspection() {
        // force Camel enabled so the inspection test can run
        CamelInspection inspection = new CamelInspection(true);

        // must be called fooroute as inspectionsimplejava fails for some odd reason
        doTest("testData/fooroute/", new LocalInspectionToolWrapper(inspection));
    }

}
