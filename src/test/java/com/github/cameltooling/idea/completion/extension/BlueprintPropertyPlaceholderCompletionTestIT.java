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
package com.github.cameltooling.idea.completion.extension;

import java.util.List;

public class BlueprintPropertyPlaceholderCompletionTestIT extends AbstractPropertyPlaceholderIT {

    public void testCompletionInBlueprintXml() {
        myFixture.configureByFiles("xml/blueprint.xml", "CompleteJavaPropertyTestData.properties");
        runCompletionTest("xml/blueprint_after.xml",
                List.of("ftp.client", "ftp.server"));
    }

    public void testCompletionInCamelContextInsideBlueprintXmlNotWorking() {
        myFixture.configureByFiles("xml/blueprint_with_camel.xml", "CompleteJavaPropertyTestData.properties");
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertSameElements(strings, "direct:${ftp.");
    }

    public void testCompletionInCxfSectionOfBlueprintFileWorking() {
        myFixture.configureByFiles("xml/blueprint_with_cxf.xml", "xml/blueprint_with_cxf.properties");
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertSameElements(strings, "myAddress", "mySomethingElse");
    }

}
