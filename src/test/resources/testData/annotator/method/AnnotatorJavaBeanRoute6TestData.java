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

public final class AnnotatorJavaBeanRoute6TestData extends RouteBuilder {

    public void configure() {
        from("file:inbox")
            .bean(AnnotatorJavaBeanTestData.class, <error descr="Can not resolve method 'myOverLoadedBean(java.lang.Long.class' in bean 'AnnotatorJavaBeanTestData'">"myOverLoadedBean(java.lang.Long.class"</error>)
            .bean(AnnotatorJavaBeanTestData.class, <error descr="Can not resolve method 'myOverLoadedBean(java.lang.Long.class)' in bean 'AnnotatorJavaBeanTestData'">"myOverLoadedBean(java.lang.Long.class)"</error>)
            .bean(AnnotatorJavaBeanTestData.class, <error descr="Can not resolve method 'myOverLoadedBean(java.lang.Object.class)' in bean 'AnnotatorJavaBeanTestData'">"myOverLoadedBean(java.lang.Object.class)"</error>)
            .bean(AnnotatorJavaBeanTestData.class, "myOverLoadedBean(java.lang.String.class)")
            .bean(AnnotatorJavaBeanTestData.class, "myOverLoadedBean2(java.lang.String.class, int.class)")
            .bean(AnnotatorJavaBeanTestData.class, "myOverLoadedBean2(java.lang.String.class, ${body})")
            .bean(AnnotatorJavaBeanTestData.class, "myOverLoadedBean2(*, *)")
            .bean(AnnotatorJavaBeanTestData.class, "myOverLoadedBean2(*, int.class)")
            .bean(AnnotatorJavaBeanTestData.class, "myOverLoadedBean2(*, -5)")
            .bean(AnnotatorJavaBeanTestData.class, "myOverLoadedBean2('abc', -5)")
            .bean(AnnotatorJavaBeanTestData.class, <error descr="Can not resolve method 'myOverLoadedBean2('abc', 'abc')' in bean 'AnnotatorJavaBeanTestData'">"myOverLoadedBean2('abc', 'abc')"</error>)
            .bean(AnnotatorJavaBeanTestData.class, "letsDoThis")
            .to("log:out");
    }
}
