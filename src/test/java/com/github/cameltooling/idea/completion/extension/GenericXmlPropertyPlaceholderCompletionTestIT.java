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

import com.github.cameltooling.idea.Constants;
import com.github.cameltooling.idea.preference.propertyplaceholder.PropertyPlaceholderSettingsEntry;
import com.github.cameltooling.idea.service.CamelPreferenceService;

import java.util.List;

public class GenericXmlPropertyPlaceholderCompletionTestIT extends AbstractPropertyPlaceholderIT {

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        CamelPreferenceService.getService().setDefaultXmlPropertyPlaceholders();
    }

    public void testCompletionInBlueprintXml() {
        myFixture.configureByFiles("xml/blueprint.xml", "CompleteJavaPropertyTestData.properties");
        runCompletionTest("xml/blueprint_after.xml",
                List.of("ftp.client", "ftp.server"));
    }

    public void testCompletionInBlueprintXmlWithNoConfiguredPlaceholders() {
        CamelPreferenceService.getService().setXmlPropertyPlaceholders(List.of());

        myFixture.configureByFiles("xml/blueprint.xml", "CompleteJavaPropertyTestData.properties");
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertEquals(List.of(), strings);
    }

    public void testCompletionInBlueprintXmlWithDisabledConfiguredPlaceholders() {
        CamelPreferenceService.getService().setXmlPropertyPlaceholders(List.of(
                new PropertyPlaceholderSettingsEntry("${", "}", List.of(Constants.OSGI_BLUEPRINT_NAMESPACE), false)
        ));

        myFixture.configureByFiles("xml/blueprint.xml", "CompleteJavaPropertyTestData.properties");
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertEquals(List.of(), strings);
    }

    public void testDifferentPlaceholders() {
        CamelPreferenceService.getService().setXmlPropertyPlaceholders(List.of(
                new PropertyPlaceholderSettingsEntry("XX{", "}", List.of(Constants.OSGI_BLUEPRINT_NAMESPACE), true)
        ));
        myFixture.configureByFiles("xml/blueprint_different_placeholders.xml", "CompleteJavaPropertyTestData.properties");
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertSameElements(strings, "ftp.client", "ftp.server");
    }

    public void testDifferentNamespace() {
        CamelPreferenceService.getService().setXmlPropertyPlaceholders(List.of(
                new PropertyPlaceholderSettingsEntry("${", "}", List.of("https://my.test.namespace.com/"), true)
        ));
        myFixture.configureByFiles("xml/my_test_namespace.xml", "CompleteJavaPropertyTestData.properties");
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertNotNull(strings);
        assertSameElements(strings, "ftp.client", "ftp.server");
    }

    public void testWrongNamespace() {
        myFixture.configureByFiles("xml/my_test_namespace.xml", "CompleteJavaPropertyTestData.properties");
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        assertEquals(List.of(), strings);
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
