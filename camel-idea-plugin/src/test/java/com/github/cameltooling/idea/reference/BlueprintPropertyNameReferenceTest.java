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
package com.github.cameltooling.idea.reference;

import java.util.List;
import com.github.cameltooling.idea.CamelLightCodeInsightFixtureTestCaseIT;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;

public class BlueprintPropertyNameReferenceTest extends CamelLightCodeInsightFixtureTestCaseIT {

    private static final String TEST_CLASS_PATH = "TestClass1.java";

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/testData/reference/blueprint";
    }

    public void testFieldWithSetter() {
        assertMethodReference("property-name/field-with-setter.xml", "setFieldWithSetter");
    }

    public void testSetterWithoutField() {
        assertMethodReference("property-name/setter-without-field.xml", "setSetterWithoutField");
    }

    public void testNoReferenceOnUnknownClass() {
        myFixture.configureByFiles("property-name/field-with-setter.xml");
        PsiElement element = TestReferenceUtil.getParentElementAtCaret(myFixture);
        assertEmpty(element.getReferences());
    }

    public void testFieldWithoutSetter() {
        assertNoReference("property-name/field-without-setter.xml");
    }

    public void testInvalidSetter() {
        assertNoReference("property-name/invalid-setter.xml");
    }

    public void testIncompatibleField() {
        assertMethodReference("property-name/setter-with-incompatible-field.xml", "setIncompatibleField");
    }

    private void assertMethodReference(String path, String methodName) {
        myFixture.configureByFiles(path, TEST_CLASS_PATH);
        PsiElement element = TestReferenceUtil.getParentElementAtCaret(myFixture);
        assertEquals(1, element.getReferences().length);
        List<PsiMethod> methods = TestReferenceUtil.resolveReference(element, PsiMethod.class);
        assertEquals(1, methods.size());
        PsiMethod method = methods.get(0);
        assertEquals(methodName, method.getName());
    }

    private void assertNoReference(String path) {
        myFixture.configureByFiles(path, TEST_CLASS_PATH);
        PsiElement element = TestReferenceUtil.getParentElementAtCaret(myFixture);
        List<PsiElement> elements = TestReferenceUtil.resolveReference(element);
        assertEmpty(elements);
    }

}
