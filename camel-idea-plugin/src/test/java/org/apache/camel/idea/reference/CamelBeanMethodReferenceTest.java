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
package org.apache.camel.idea.reference;

import java.util.ArrayList;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.ResolveResult;
import com.intellij.usageView.UsageInfo;
import org.apache.camel.idea.CamelLightCodeInsightFixtureTestCaseIT;
import org.apache.camel.idea.refereance.CamelBeanMethodReference;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;

/**
 * Test method reference link between the Java Camel DSL bean reference method @{code bean(MyClass.class,"myMethodName")}
 */
public class CamelBeanMethodReferenceTest extends CamelLightCodeInsightFixtureTestCaseIT {

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/testData/completion/method";
    }

    public void testCamelMethodReference() {
        myFixture.configureByFiles("CompleteJavaBeanRoute3TestData.java", "CompleteJavaBeanTestData.java",
            "CompleteJavaBeanSuperClassTestData.java", "CompleteJavaBeanMethodPropertyTestData.properties");
        PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset()).getParent();
        final ResolveResult[] resolveResults = ((CamelBeanMethodReference) element.getReferences()[0]).multiResolve(false);
        assertEquals(1, resolveResults.length);
        assertEquals("public void letsDoThis() {}",  resolveResults[0].getElement().getText());
    }

    public void testCamelMultipleMethodReference() {
        myFixture.configureByFiles("CompleteJavaBeanRoute4TestData.java", "CompleteJavaBeanMultipleMethodTestData.java", "CompleteJavaBeanSuperClassTestData.java");
        PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset()).getParent();
        final ResolveResult[] resolveResults = ((CamelBeanMethodReference) element.getReferences()[0]).multiResolve(false);
        assertEquals(2, resolveResults.length);
        assertEquals("public void multipleMethodsWithSameName() { }",  resolveResults[0].getElement().getText());
        assertEquals("public void multipleMethodsWithSameName(int data) { }",  resolveResults[1].getElement().getText());
    }

    public void testCamelNoMethodReference() {
        myFixture.configureByFiles("CompleteJavaBeanRoute4TestData.java");
        PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset()).getParent();
        assertEquals(0, element.getReferences().length);
    }

    public void testFindUsageFromMethodToBeanDSL() {
        ArrayList<UsageInfo> usageInfos = (ArrayList<UsageInfo>) myFixture.testFindUsages("CompleteJavaBeanTestData.java", "CompleteJavaBeanRoute1TestData.java");
        assertEquals(1, usageInfos.size());

        final UsageInfo usageInfo = usageInfos.get(0);
        final PsiElement referenceElement = usageInfo.getElement();
        assertThat(referenceElement, instanceOf(PsiLiteralExpression.class));
        assertEquals("(beanTestData, \"anotherBeanMethod\")", referenceElement.getParent().getText());
    }

    public void testFindUsageFromWithOverloadedMethodToBeanDSL() {
        ArrayList<UsageInfo> usageInfos = (ArrayList<UsageInfo>) myFixture.testFindUsages("CompleteJavaBeanTest2Data.java", "CompleteJavaBeanRoute7TestData.java");
        assertEquals(1, usageInfos.size());

        final UsageInfo usageInfo = usageInfos.get(0);
        final PsiElement referenceElement = usageInfo.getElement();
        assertThat(referenceElement, instanceOf(PsiLiteralExpression.class));
        assertEquals("(beanTestData, \"myOverLoadedBean\")", referenceElement.getParent().getText());
    }

    /**
     * Test if the find usage is working with camel DSL bean method call with parameters
     */
    public void testFindUsageFromWithAmbiguousToBeanDSLWithParameters() {
        ArrayList<UsageInfo> usageInfos = (ArrayList<UsageInfo>) myFixture.testFindUsages("CompleteJavaBeanTest3Data.java", "CompleteJavaBeanRoute8TestData.java");
        assertEquals(1, usageInfos.size());

        final UsageInfo usageInfo = usageInfos.get(0);
        final PsiElement referenceElement = usageInfo.getElement();
        assertThat(referenceElement, instanceOf(PsiLiteralExpression.class));
        assertEquals("(beanTestData, \"myAmbiguousMethod(${body})\")", referenceElement.getParent().getText());
    }
}
