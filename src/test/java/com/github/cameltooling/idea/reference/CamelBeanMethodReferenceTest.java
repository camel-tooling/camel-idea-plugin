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
package com.github.cameltooling.idea.reference;

import com.github.cameltooling.idea.CamelLightCodeInsightFixtureTestCaseIT;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.usageView.UsageInfo;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;

/**
 * Test method reference link between the Java Camel DSL bean reference method @{code bean(MyClass.class,"myMethodName")}
 */
public class CamelBeanMethodReferenceTest extends CamelLightCodeInsightFixtureTestCaseIT {
    private static final String SPRING_CONTEXT_MAVEN_ARTIFACT = "org.springframework:spring-context:5.1.6.RELEASE";

    @Nullable
    @Override
    protected String[] getMavenDependencies() {
        return new String[]{SPRING_CONTEXT_MAVEN_ARTIFACT, CAMEL_CORE_MODEL_MAVEN_ARTIFACT, CAMEL_API_MAVEN_ARTIFACT};
    }

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
        PsiElement element = PsiTreeUtil.getParentOfType(myFixture.getFile().findElementAt(myFixture.getCaretOffset()), false, PsiLiteralExpression.class);
        assertNotNull(element);
        PsiReference[] references = element.getReferences();
        assertEquals(1, references.length);
        assertTrue(references[0] instanceof CamelBeanMethodReference);
        CamelBeanMethodReference reference = (CamelBeanMethodReference) references[0];
        final ResolveResult[] resolveResults = reference.multiResolve(false);
        assertEquals(2, resolveResults.length);
        assertEquals("public void multipleMethodsWithSameName() { }",  resolveResults[0].getElement().getText());
        assertEquals("public void multipleMethodsWithSameName(int data) { }",  resolveResults[1].getElement().getText());
    }

    public void testCamelNoMethodReference() {
        myFixture.configureByFiles("CompleteJavaBeanRoute4TestData.java");
        PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset()).getParent();
        assertEquals(0, ((PsiPolyVariantReference) element.getReferences()[0]).multiResolve(false).length);
    }

    public void testFindUsageFromMethodToBeanDSL() {
        Collection<UsageInfo> usageInfos = myFixture.testFindUsages("CompleteJavaBeanTestData.java", "CompleteJavaBeanRoute1TestData.java");
        assertEquals(1, usageInfos.size());

        final UsageInfo usageInfo = usageInfos.iterator().next();
        final PsiElement referenceElement = usageInfo.getElement();
        assertThat(referenceElement, instanceOf(PsiLiteralExpression.class));
        assertEquals("(beanTestData, \"anotherBeanMethod\")", referenceElement.getParent().getText());
    }

    /**
     * Test if it can find usage from a Spring Service bean method to it's Camel routes bean method
     */
    public void testFindUsageFromSpringServiceMethodToBeanDSL() {
        Collection<UsageInfo> usageInfos = myFixture.testFindUsages("CompleteJavaSpringServiceBeanTestData.java", "CompleteJavaSpringServiceBeanRouteTestData.java");
        assertEquals(1, usageInfos.size());

        final UsageInfo usageInfo = usageInfos.iterator().next();
        final PsiElement referenceElement = usageInfo.getElement();
        assertThat(referenceElement, instanceOf(PsiLiteralExpression.class));
        assertEquals("(\"myServiceBean\", \"anotherBeanMethod\")", referenceElement.getParent().getText());
    }

    /**
     * Test if it can find usage from a Spring Component bean method to it's Camel routes bean method
     */
    public void testFindUsageFromSpringComponentMethodToBeanDSL() {
        Collection<UsageInfo> usageInfos = myFixture.testFindUsages("CompleteJavaSpringComponentBeanTestData.java", "CompleteJavaSpringComponentBeanRouteTestData.java");
        assertEquals(1, usageInfos.size());

        final UsageInfo usageInfo = usageInfos.iterator().next();
        final PsiElement referenceElement = usageInfo.getElement();
        assertThat(referenceElement, instanceOf(PsiLiteralExpression.class));
        assertEquals("(\"myComponentBean\", \"anotherBeanMethod\")", referenceElement.getParent().getText());
    }

    /**
     * Test if it can find usage from a Spring Repository bean method to it's Camel routes bean method
     */
    public void testFindUsageFromSpringRepositoryMethodToBeanDSL() {
        Collection<UsageInfo> usageInfos = myFixture.testFindUsages("CompleteJavaSpringRepositoryBeanTestData.java", "CompleteJavaSpringRepositoryBeanRouteTestData.java");
        assertEquals(1, usageInfos.size());

        final UsageInfo usageInfo = usageInfos.iterator().next();
        final PsiElement referenceElement = usageInfo.getElement();
        assertThat(referenceElement, instanceOf(PsiLiteralExpression.class));
        assertEquals("(\"myRepositoryBean\", \"anotherBeanMethod\")", referenceElement.getParent().getText());
    }

    public void testFindUsageFromWithOverloadedMethodToBeanDSL() {
        Collection<UsageInfo> usageInfos = myFixture.testFindUsages("CompleteJavaBeanTest2Data.java", "CompleteJavaBeanRoute7TestData.java");
        assertEquals(1, usageInfos.size());

        final UsageInfo usageInfo = usageInfos.iterator().next();
        final PsiElement referenceElement = usageInfo.getElement();
        assertThat(referenceElement, instanceOf(PsiLiteralExpression.class));
        assertEquals("(beanTestData, \"myOverLoadedBean\")", referenceElement.getParent().getText());
    }


    /**
     * Test if resolve the reference to the bean method when using a static String as bean reference name
     */
    public void testFindUsageFromWithStaticBeanNameToBeanDSL() {
        myFixture.configureByFiles("CompleteJavaBeanRoute9TestData.java", "CompleteJavaSpringServiceBeanTestData.java", "CompleteJavaBeanSuperClassTestData.java", "CompleteJavaBeanMethodPropertyTestData.properties");
        PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset()).getParent();
        assertEquals("\"letsDoThis\"",  element.getText());
    }

    /**
     * Test if resolve the reference to the bean repository method when using a static String as bean reference name
     */
    public void testFindUsageFromWithStaticRepositoryNameToBeanDSL() {
        myFixture.configureByFiles("CompleteJavaBeanRoute11TestData.java", "CompleteJavaSpringRepositoryBeanTestData.java","CompleteJavaBeanMethodPropertyTestData.properties");
        PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset()).getParent();
        assertEquals("\"myRepositorySpringBeanMethod\"",  element.getText());
    }

    public void testOverriddenAbstractCamelMethodReference() {
        singleResultMethodReferenceTest("public void mySuperAbstractMethod() {}",
                "CompleteJavaBeanRoute12TestData.java", "CompleteJavaBeanTestData.java",
                "CompleteJavaBeanSuperClassTestData.java", "CompleteJavaBeanMethodPropertyTestData.properties");
    }

    public void testAbstractCamelMethodReference() {
        singleResultMethodReferenceTest("public abstract void mySuperAbstractMethod();",
                "CompleteJavaBeanRoute13TestData.java", "CompleteJavaBeanTestData.java",
                "CompleteJavaBeanSuperClassTestData.java", "CompleteJavaBeanMethodPropertyTestData.properties");
    }

    public void testInterfaceMethod() {
        singleResultMethodReferenceTest("String myInterfaceMethod();",
                "CompleteJavaBeanRoute14TestData.java", "CompleteJavaBeanInterface.java");
    }

    private void singleResultMethodReferenceTest(String resolvedMethodText, String ... testFiles) {
        myFixture.configureByFiles(testFiles);
        PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset()).getParent();
        final ResolveResult[] resolveResults = ((CamelBeanMethodReference) element.getReferences()[0]).multiResolve(false);
        assertEquals(1, resolveResults.length);
        assertEquals(resolvedMethodText,  resolveResults[0].getElement().getText());
    }


    /**
     * Test if the find usage is working with camel DSL bean method call with parameters
     */
    public void testFindUsageFromWithAmbiguousToBeanDSLWithParameters() {
        Collection<UsageInfo> usageInfos = myFixture.testFindUsages("CompleteJavaBeanTest3Data.java", "CompleteJavaBeanRoute8TestData.java");
        assertEquals(1, usageInfos.size());

        final UsageInfo usageInfo = usageInfos.iterator().next();
        final PsiElement referenceElement = usageInfo.getElement();
        assertThat(referenceElement, instanceOf(PsiLiteralExpression.class));
        assertEquals("(beanTestData, \"myAmbiguousMethod(${body})\")", referenceElement.getParent().getText());
    }

    public void testThisKeywordUsedAsBean() {
        myFixture.configureByText("MyRoute.java",
                // language=JAVA
                """
                import org.apache.camel.builder.RouteBuilder;
                public class MyRoute extends RouteBuilder {
                    @Override
                    public void configure() {
                        from("direct:test")
                            .bean(this, "myMet<caret>hod");
                    }
                
                    public String myMethod() { return "Hello World"; }
                
                }
                """);
        PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset()).getParent();
        final ResolveResult[] resolveResults = ((CamelBeanMethodReference) element.getReferences()[0]).multiResolve(false);
        assertEquals(1, resolveResults.length);
        assertEquals("public String myMethod() { return \"Hello World\"; }",  resolveResults[0].getElement().getText());
    }

}
