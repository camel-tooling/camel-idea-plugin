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
package com.github.cameltooling.idea.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * The test class for {@link JavaClassUtils}.
 */
public class JavaClassUtilsTest {

    /**
     * {@link JavaClassUtils#toSimpleType(String)} has no effect on specific types.
     */
    @Test
    public void noEffectOnSpecificTypes() {
        JavaClassUtils utils = new JavaClassUtils();
        assertEquals("Foo", utils.toSimpleType("Foo"));
        assertEquals("com.foo.Bar", utils.toSimpleType("com.foo.Bar"));
        assertEquals("com.foo.bar.Integer", utils.toSimpleType("com.foo.bar.Integer"));
    }

    /**
     * {@link JavaClassUtils#toSimpleType(String)} has no effect on primitive types.
     */
    @Test
    public void noEffectOnPrimitiveTypes() {
        JavaClassUtils utils = new JavaClassUtils();
        assertEquals("byte", utils.toSimpleType("byte"));
        assertEquals("short", utils.toSimpleType("short"));
        assertEquals("int", utils.toSimpleType("int"));
        assertEquals("long", utils.toSimpleType("long"));
        assertEquals("float", utils.toSimpleType("float"));
        assertEquals("double", utils.toSimpleType("double"));
        assertEquals("boolean", utils.toSimpleType("boolean"));
        assertEquals("char", utils.toSimpleType("char"));
    }

    /**
     * {@link JavaClassUtils#toSimpleType(String)} can simplify wrapper types.
     */
    @Test
    public void simplifyWrapperTypes() {
        JavaClassUtils utils = new JavaClassUtils();
        assertEquals("byte", utils.toSimpleType("Byte"));
        assertEquals("short", utils.toSimpleType("Short"));
        assertEquals("int", utils.toSimpleType("Integer"));
        assertEquals("int", utils.toSimpleType("integer"));
        assertEquals("long", utils.toSimpleType("Long"));
        assertEquals("float", utils.toSimpleType("Float"));
        assertEquals("double", utils.toSimpleType("Double"));
        assertEquals("boolean", utils.toSimpleType("Boolean"));
        assertEquals("char", utils.toSimpleType("Character"));
        assertEquals("char", utils.toSimpleType("character"));
        assertEquals("byte", utils.toSimpleType("java.lang.Byte"));
        assertEquals("short", utils.toSimpleType("java.lang.Short"));
        assertEquals("int", utils.toSimpleType("java.lang.Integer"));
        assertEquals("long", utils.toSimpleType("java.lang.Long"));
        assertEquals("float", utils.toSimpleType("java.lang.Float"));
        assertEquals("double", utils.toSimpleType("java.lang.Double"));
        assertEquals("boolean", utils.toSimpleType("java.lang.Boolean"));
        assertEquals("char", utils.toSimpleType("java.lang.Character"));
    }

    /**
     * {@link JavaClassUtils#toSimpleType(String)} can simplify {@code String}.
     */
    @Test
    public void simplifyString() {
        JavaClassUtils utils = new JavaClassUtils();
        assertEquals("string", utils.toSimpleType("String"));
        assertEquals("string", utils.toSimpleType("java.lang.String"));
    }
}
