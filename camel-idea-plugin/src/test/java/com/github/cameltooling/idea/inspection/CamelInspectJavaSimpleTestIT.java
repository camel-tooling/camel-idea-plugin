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
package com.github.cameltooling.idea.inspection;

import com.intellij.codeInspection.ex.LocalInspectionToolWrapper;
import org.jetbrains.annotations.Nullable;


public class CamelInspectJavaSimpleTestIT extends CamelInspectionTestHelper {

    @Nullable
    @Override
    protected String[] getMavenDependencies() {
        return new String[]{CAMEL_CORE_MAVEN_ARTIFACT};
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
