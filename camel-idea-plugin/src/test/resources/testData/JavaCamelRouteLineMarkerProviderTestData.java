package org.apache.camel.idea.gutter; /**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.main.Main;

public class JavaCamelRouteLineMarkerProviderTestData extends RouteBuilder {
    @Override
    public void configure() throws Exception {
        rest("/say")
                .get("/hello").to("direct:hello")
                .get("/bye").consumes("application/json").to("direct:bye")
                .post("/bye").to("mock:update");
        from("file:inbox")
                .to("file:outbox");
        from("file:outbox")
                .to("file:inbox");
        rest()
            .get("/hello").to("direct:hello")
            .get("/bye").consumes("application/json").to("direct:bye")
            .post("/bye").to("mock:update");
    }
}
