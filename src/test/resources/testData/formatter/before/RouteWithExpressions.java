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

import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.RouteBuilder;

import static org.apache.camel.builder.Builder.constant;

public class RouteWithExpressions extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        from("timer:foo")
            .setBody(simple("xxx"))
            .setBody().simple("xxx", String.class)
            .setBody().constant("xxx");

        from("disruptor:foo").transform(constant("Bye World")).to("mock:result");

        from("direct:a").split(
            expression()
                .tokenize()
                .token("\n")
                .end()
        ).to("mock:a");

        from("direct:b").setBody().expression(expression().simple().expression("Hello World Out").end()).to("mock:b");
        from("direct:c").setBody(expression().simple().expression("Hello World In").end()).to("mock:c");
        from("direct:d").filter(expression(expression().header().expression("foo").end()).isEqualTo("bar")).to("mock:d");
        from("direct:e").choice().when(expression(expression().header().expression("foo").end()).isEqualTo("bar")).to("mock:e1").otherwise().to("mock:e2");

        AdviceWith.adviceWith(context, "advice-with-on-exception-transacted-test-route", a -> {
            a.weaveAddFirst().transform(constant("Bye World"));
        });
    }
}
