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
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.main.Main;
import testData.CompleteJavaSpringServiceBeanTestData.*;

/**
 * Test route for testing find usage from Route bean DSL to the bean method where the
 * bean name is a static string reference with the logic bean name.
 */
public final class CompleteJavaBeanRoute9TestData extends RouteBuilder {

    @Override
    public void configure() {
        from("file:inbox")
            .bean(JAVA_TEST_BEAN, "lets<caret>DoThis")
            .to("log:out");
    }
}
