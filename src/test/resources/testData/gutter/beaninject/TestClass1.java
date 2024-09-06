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
import org.apache.camel.BeanInject;

public class TestClass1 {

    @BeanInject("<caret>testClass2Bean")
    private TestClass2 field1;

    private TestClass2 field2;
    private TestClass2 field3;
    private TestClass3 field4;

    @BeanInject
    public TestClass1(TestClass2 field3) {
        this.field3 = field3;
    }

    @BeanInject
    public void setField2(TestClass2 field2) {
        this.field2 = field2;
    }

    @BeanInject
    public void setField4(TestClass3 field4) {
        this.field4 = field4;
    }

}