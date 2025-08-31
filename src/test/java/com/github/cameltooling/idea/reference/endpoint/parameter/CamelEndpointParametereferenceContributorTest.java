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
package com.github.cameltooling.idea.reference.endpoint.parameter;

import com.github.cameltooling.idea.CamelLightCodeInsightFixtureTestCaseIT;
import com.github.cameltooling.idea.reference.TestReferenceUtil;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;

import java.util.List;
import java.util.Objects;

public class CamelEndpointParametereferenceContributorTest extends CamelLightCodeInsightFixtureTestCaseIT {

    // language=JAVA
    private static final String JAVA_ROUTE = """
            import org.apache.camel.builder.RouteBuilder;
            public final class MyRoute extends RouteBuilder {
                @Override
                public void configure() {
                    from("time<caret>r:foo?period=1000&delay=50").to("mock:out");
                }
            }
            """;

    @Override
    protected String getTestDataPath() {
        return super.getTestDataPath() + "/reference";
    }

    @Override
    protected String[] getMavenDependencies() {
        return new String[]{CAMEL_CORE_MAVEN_ARTIFACT, CAMEL_TIMER_MAVEN_ARTIFACT, CAMEL_FTP_MAVEN_ARTIFACT};
    }

    public void testPropertyReferencesAtCorrectIndices() {
        myFixture.configureByText("MyRoute.java", JAVA_ROUTE);
        PsiElement textElement = TestReferenceUtil.getParentElementAtCaret(myFixture);;
        List<EndpointParameterReference> refs = TestReferenceUtil.getReferencesOfType(textElement, EndpointParameterReference.class);

        List<String> propNames = refs.stream().map(PsiReference::getCanonicalText).toList();
        assertSameElements(propNames, List.of("period", "delay"));

        assertParameterTextRange(refs, textElement.getText(), "period");
        assertParameterTextRange(refs, textElement.getText(), "delay");
    }

    private void assertParameterTextRange(List<EndpointParameterReference> refs, String endpointUriText, String param) {
        int index = endpointUriText.indexOf(param + "=");
        assertTrue(index >= 0);
        EndpointParameterReference ref = refs.stream().filter(r -> r.getCanonicalText().equals(param)).findFirst().orElse(null);
        assertNotNull(ref);
        assertEquals(new TextRange(index, index + param.length()), ref.getRangeInElement());
    }

    public void testTimerEndpointPeriodReference() {
        testQueryParameterReference(JAVA_ROUTE, "period", "org.apache.camel.component.timer.TimerEndpoint", "setPeriod");
    }

    public void testFtpConfigurationAccountReference() {
        testQueryParameterReference(
                // language=JAVA
                """
                import org.apache.camel.builder.RouteBuilder;
                public final class MyRoute extends RouteBuilder {
                    @Override
                    public void configure() {
                        from("f<caret>tp:xxxx?account=acc").to("mock:out");
                    }
                }
                """, "account", "org.apache.camel.component.file.remote.FtpConfiguration", "setAccount");
    }

    private void testQueryParameterReference(String route, String param, String resolvedClass, String resolvedMethod) {
        List<EndpointParameterReference> refs = getEndpointPropertyReferences(route);
        assertParameterReferenceResolvesToCorrectMethod(refs, param, resolvedClass, resolvedMethod);
    }

    private static void assertParameterReferenceResolvesToCorrectMethod(List<EndpointParameterReference> refs, String param, String resolvedClass, String resolvedMethod) {
        EndpointParameterReference paramReference = refs.stream().filter(r -> r.getCanonicalText().equals(param)).findFirst().orElse(null);
        assertNotNull(paramReference);
        PsiElement resolved = paramReference.resolve();
        assertTrue(resolved instanceof PsiMethod);
        PsiMethod method = (PsiMethod) resolved;
        assertEquals(resolvedMethod, method.getName());
        assertEquals(resolvedClass, Objects.requireNonNull(method.getContainingClass()).getQualifiedName());
    }

    private List<EndpointParameterReference> getEndpointPropertyReferences(String fileContent) {
        return getEndpointPropertyReferences("MyRoute.java", fileContent);
    }

    private List<EndpointParameterReference> getEndpointPropertyReferences(String fileName, String fileContent) {
        myFixture.configureByText(fileName, fileContent);
        PsiElement textElement = TestReferenceUtil.getParentElementAtCaret(myFixture);
        return TestReferenceUtil.getReferencesOfType(textElement, EndpointParameterReference.class);
    }

    public void testNoReferencesForUnknownComponent() {
        // language=JAVA
        List<EndpointParameterReference> refs = getEndpointPropertyReferences("""
                import org.apache.camel.builder.RouteBuilder;
                public final class MyRoute extends RouteBuilder {
                    @Override
                    public void configure() {
                        from("xxxunknown:abc?foo=1&bar=2");
                    }
                }
                """);
        assertTrue(refs.isEmpty());
    }

    public void testUnknownQueryParamHasReferenceButResolvesToNullInJava() {
        // language=JAVA
        List<EndpointParameterReference> refs = getEndpointPropertyReferences("""
                import org.apache.camel.builder.RouteBuilder;
                public final class MyRoute extends RouteBuilder {
                    @Override
                    public void configure() {
                        from("time<caret>r:bar?foo=1&delay=2");
                    }
                }
                """);
        List<String> names = refs.stream().map(PsiReference::getCanonicalText).toList();
        assertEquals(names, List.of("foo", "delay"));
        PsiReference fooRef = refs.stream().filter(r -> r.getCanonicalText().equals("foo")).findFirst().orElse(null);
        assertNotNull(fooRef);
        assertNull(fooRef.resolve());

        assertParameterReferenceResolvesToCorrectMethod(refs, "delay", "org.apache.camel.component.timer.TimerEndpoint", "setDelay");
    }

    public void testXmlKnownAndUnknownParams() {
        List<EndpointParameterReference> refs = getEndpointPropertyReferences("routes.xml",
                // language=XML
                """
                <camelContext xmlns="http://camel.apache.org/schema/spring">
                    <route>
                        <from uri="tim<caret>er:abc?foo=1&amp;period=1000"/>
                        <to uri="mock:out"/>
                    </route>
                </root>
                """);
        List<String> names = refs.stream().map(PsiReference::getCanonicalText).toList();
        assertSameElements(names, List.of("foo", "period"));

        assertParameterReferenceResolvesToCorrectMethod(refs, "period", "org.apache.camel.component.timer.TimerEndpoint", "setPeriod");
    }

    public void testYamlKnownAndUnknownParams() {
        List<EndpointParameterReference> refs = getEndpointPropertyReferences("routes.yaml",
                // language=YAML
                """
                - route:
                    from:
                      uri: "tim<caret>er:abc?foo=1&delay=5&period=1000"
                    steps:
                      - to: "mock:out"
                """);
        List<String> names = refs.stream().map(PsiReference::getCanonicalText).toList();
        assertSameElements(names, List.of("foo", "delay", "period"));

        assertParameterReferenceResolvesToCorrectMethod(refs, "period", "org.apache.camel.component.timer.TimerEndpoint", "setPeriod");
        assertParameterReferenceResolvesToCorrectMethod(refs, "delay", "org.apache.camel.component.timer.TimerEndpoint", "setDelay");
    }
    
    
}
