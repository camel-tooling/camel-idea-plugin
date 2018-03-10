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

import java.io.File;
import java.util.stream.Stream;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.testFramework.PsiTestUtil;
import org.apache.camel.idea.CamelLightCodeInsightFixtureTestCaseIT;
import org.apache.camel.idea.service.CamelService;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;

public class IdeaUtilsSkipEndpointValidationTestIT extends CamelLightCodeInsightFixtureTestCaseIT {

    private static final String ACTIVEMQ_ARTIFACT = "org.apache.activemq:activemq-camel:5.14.3";
    private static final String QPID_ARTIFACT = "org.apache.qpid:qpid-jms-client:0.29.0";

    private static final String CODE = "import org.apache.camel.builder.RouteBuilder;\n"
        + "import org.apache.activemq.camel.component.ActiveMQComponent;\n"
        + "\n"
        + "public class DummyTestData extends RouteBuilder{\n"
        + "\n"
        + "    @Override\n"
        + "    public void configure() throws Exception {\n"
        + "        new org.apache.activemq.ActiveMQConnectionFactory(\"vm://broker\");\n"
        + "        new org.apache.activemq.spring.ActiveMQConnectionFactory(\"vm://spring\");\n"
        + "        new org.apache.activemq.ActiveMQXAConnectionFactory(\"vm://broker-xa\");\n"
        + "        new org.apache.activemq.spring.ActiveMQXAConnectionFactory(\"vm://spring-xa\");\n"
        + "        new org.apache.qpid.jms.JmsConnectionFactory(\"amqp://localhost:5672\");\n"
        + "        ActiveMQComponent aq = ActiveMQComponent.activeMQComponent(\"vm://localhost\");\n"
        + "        aq.setBrokerURL(\"tcp://evilhost:666\");\n"
        + "        getContext().addComponent(\"queue\", aq);"
        + "    }\n"
        + "\n"
        + ""
        + "}\n";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ServiceManager.getService(myFixture.getProject(), CamelService.class).setCamelPresent(true);
        Stream.of(
            Maven.resolver().resolve(ACTIVEMQ_ARTIFACT, QPID_ARTIFACT)
                .withTransitivity().asFile()
        ).forEach(f -> PsiTestUtil.addLibrary(myFixture.getProjectDisposable(), myFixture.getModule(), f.getName(), f.getParentFile().getAbsolutePath(), f.getName()));
    }

    public void testShouldSkipActiveMQComponentFactoryMethod() {
        myFixture.configureByText("DummyTestData.java", CODE);
        PsiElement element = myFixture.findElementByText("\"vm://localhost\"", PsiLiteralExpression.class);
        assertTrue("ActiveMQComponent.activeMQComponent should be skipped", getCamelIdeaUtils().skipEndpointValidation(element));
    }

    public void testShouldSkipSetBrokerUrlMethod() {
        myFixture.configureByText("DummyTestData.java", CODE);
        PsiElement element = myFixture.findElementByText("\"tcp://evilhost:666\"", PsiLiteralExpression.class);
        assertTrue("setBrokerURL method should be skipped", getCamelIdeaUtils().skipEndpointValidation(element));
    }

    public void testShouldSkipActiveMQConnectionFactoryConstructor() {
        myFixture.configureByText("DummyTestData.java", CODE);
        PsiElement element = myFixture.findElementByText("\"vm://broker\"", PsiLiteralExpression.class);
        assertTrue("ActiveMQConnectionFactory constructor should be skipped", getCamelIdeaUtils().skipEndpointValidation(element));
        element = myFixture.findElementByText("\"vm://spring\"", PsiLiteralExpression.class);
        assertTrue("spring ActiveMQConnectionFactory constructor should be skipped", getCamelIdeaUtils().skipEndpointValidation(element));
    }

    public void testShouldSkipActiveMQXAConnectionFactoryConstructor() {
        myFixture.configureByText("DummyTestData.java", CODE);
        PsiElement element = myFixture.findElementByText("\"vm://broker-xa\"", PsiLiteralExpression.class);
        assertTrue("ActiveMQXAConnectionFactory constructor should be skipped", getCamelIdeaUtils().skipEndpointValidation(element));
        element = myFixture.findElementByText("\"vm://spring-xa\"", PsiLiteralExpression.class);
        assertTrue("spring ActiveMQXAConnectionFactory constructor should be skipped", getCamelIdeaUtils().skipEndpointValidation(element));
    }

    public void testShouldSkipQpidJmsConnectionFactoryConstructor() {
        myFixture.configureByText("DummyTestData.java", CODE);
        PsiElement element = myFixture.findElementByText("\"amqp://localhost:5672\"", PsiLiteralExpression.class);
        assertTrue("JmsConnectionFactory constructor should be skipped", getCamelIdeaUtils().skipEndpointValidation(element));
    }

    private CamelIdeaUtils getCamelIdeaUtils() {
        return ServiceManager.getService(CamelIdeaUtils.class);
    }

}
