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
package com.github.cameltooling.idea.reference.blueprint;

import com.github.cameltooling.idea.CamelLightCodeInsightFixtureTestCaseIT;
import com.github.cameltooling.idea.reference.TestReferenceUtil;
import com.intellij.lang.properties.psi.Property;
import com.intellij.lang.properties.references.PropertyReference;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.github.cameltooling.idea.reference.TestReferenceUtil.getPropertyReferences;

public class ConfigPropertyPlaceholderReferenceContributorIT extends CamelLightCodeInsightFixtureTestCaseIT {

    @Override
    protected @Nullable String[] getMavenDependencies() {
        return new String[] { CAMEL_CORE_MODEL_MAVEN_ARTIFACT, "org.apache.aries.blueprint:blueprint-maven-plugin-annotation:1.3.0" };
    }

    public void testNotAppliedWithIncorrectAnnotation() {
        myFixture.configureByFiles("reference/blueprint/config-property/IncorrectConfigPropertyAnnotation.java", "CompleteJavaPropertyTestData.properties");

        PsiElement property = getPropertyRefAtCaret().resolve();
        assertNull(property);
    }

    public void testNormalBehavior() {
        myFixture.configureByFiles("reference/blueprint/config-property/TestClass1.java", "CompleteJavaPropertyTestData.properties");

        PropertyReference ref = getPropertyRefAtCaret();
        assertEquals(new TextRange(3, 13), ref.getRangeInElement());
        Property property = (Property) ref.resolve();
        assertNotNull(property);
        assertEquals("ftp.client", property.getKey());
    }

    public void testDefaultPropertyReferenceNotAppliedForRawValue() {
        myFixture.configureByFiles("reference/blueprint/config-property/DefaultPropertyReferenceNotAppliedForRawValue.java", "CompleteJavaPropertyTestData.properties");
        PsiElement element = TestReferenceUtil.getParentElementAtCaret(myFixture);
        List<PropertyReference> refs = getPropertyReferences(element);
        assertTrue(refs.isEmpty());
    }

    private PropertyReference getPropertyRefAtCaret() {
        PsiElement element = TestReferenceUtil.getParentElementAtCaret(myFixture);
        List<PropertyReference> refs = getPropertyReferences(element);
        assertFalse(refs.isEmpty());
        return refs.getFirst();
    }

}