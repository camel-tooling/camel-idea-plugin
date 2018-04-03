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
package testData.annotator.method;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.main.Main;
import testData.annotator.method.AnnotatorJavaBeanTestData;

public final class AnnotatorJavaBeanRoute1TestData extends RouteBuilder {

    @Override
    public void configure() {
        from("file:inbox")
            .bean(AnnotatorJavaBeanTestData.class, <error descr="'thisIsVeryPrivate' has private access in bean 'testData.annotator.method.AnnotatorJavaBeanTestData'">"thisIsVeryPrivate"</error>)
            .bean(AnnotatorJavaBeanTestData.class, <error descr="Can not resolve method 'methodDoesNotExist' in bean 'testData.annotator.method.AnnotatorJavaBeanTestData'">"methodDoesNotExist"</error>)
            .bean(AnnotatorJavaBeanTestData.class, "letsDoThis")
            .to("log:out");

    }
}
