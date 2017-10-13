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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class StringUtilsTest {

    @Test
    public void hasQuestionMark() {
        assertTrue(StringUtils.hasQuestionMark("seda:foo?size=123"));
        assertFalse(StringUtils.hasQuestionMark("seda:foo"));
        assertFalse(StringUtils.hasQuestionMark(""));
        assertFalse(StringUtils.hasQuestionMark(null));
    }

    @Test
    public void asComponentName() {
        assertEquals("seda", StringUtils.asComponentName("seda:foo?size=123"));
        assertEquals("seda", StringUtils.asComponentName("seda:foo"));
        assertEquals(null, StringUtils.asComponentName("seda"));
        assertNull(StringUtils.asComponentName(null));
    }

    @Test
    public void asLanguageName() {
        assertEquals("simple", StringUtils.asLanguageName("simple"));
        assertEquals("header", StringUtils.asLanguageName("header"));
        assertEquals("tokenize", StringUtils.asLanguageName("tokenize"));
        assertEquals("tokenize", StringUtils.asLanguageName("tokenizeXml"));
        assertEquals("javaScript", StringUtils.asLanguageName("js"));
        assertEquals("javaScript", StringUtils.asLanguageName("javascript"));
        assertNull(StringUtils.asLanguageName(null));
    }

    @Test
    public void getSafeValue() {
        Map<String, String> row = new HashMap<>();
        row.put("foo", "123");

        assertEquals("123", StringUtils.getSafeValue("foo", row));
        assertEquals("", StringUtils.getSafeValue("bar", row));

        Map<String, String> row2 = new HashMap<>();
        row2.put("bar", "true");

        List<Map<String, String>> rows = new ArrayList<>();
        rows.add(row);
        rows.add(row2);

        assertEquals("123", StringUtils.getSafeValue("foo", rows));
        assertEquals("true", StringUtils.getSafeValue("bar", rows));
        assertEquals("", StringUtils.getSafeValue("baz", rows));
    }

    @Test
    public void wrapSeparator() {
        String url = "seda:foo?size=1234";

        assertEquals(url, StringUtils.wrapSeparator(url, "&", "\n", 80));

        String longUrl = "jms:queue:cheese?acknowledgementModeName=SESSION_TRANSACTED&asyncConsumer=true&cacheLevelName=CACHE_CONSUMER"
            + "&deliveryMode=2&errorHandlerLoggingLevel=DEBUG&explicitQosEnabled=true&jmsMessageType=Bytes";

        String wrapped = StringUtils.wrapSeparator(longUrl, "&", "\n", 120);

        String line1 = "jms:queue:cheese?acknowledgementModeName=SESSION_TRANSACTED&asyncConsumer=true&cacheLevelName=CACHE_CONSUMER&deliveryMode=2";
        String line2 = "&errorHandlerLoggingLevel=DEBUG&explicitQosEnabled=true&jmsMessageType=Bytes";

        String[] parts = wrapped.split("\n");
        assertEquals(2, parts.length);
        assertEquals(line1, parts[0]);
        assertEquals(line2, parts[1]);
    }

    @Test
    public void wrapWords() {
        assertNull(StringUtils.wrapWords(null, "\n", 80, true));

        String words = "Plugin for Intellij IDEA to provide a set of small Camel related capabilities to IDEA editor."
            + " When the plugin becomes more complete and stable then the intention is to donate the source code"
            + " to Apache Software Foundation to be included out of the box at Apache Camel.";

        String wrapped = StringUtils.wrapWords(words, "\n", 80, true);

        String[] parts = wrapped.split("\n");
        assertEquals(4, parts.length);

        assertTrue(parts[0].length() <= 80);
        assertTrue(parts[1].length() <= 80);
        assertTrue(parts[2].length() <= 80);
        assertTrue(parts[3].length() <= 80);

        assertTrue(parts[0].startsWith("Plugin for Intellij"));
        assertTrue(parts[1].startsWith("IDEA editor"));
        assertTrue(parts[2].startsWith("is to donate"));
        assertTrue(parts[3].startsWith("the box at Apache Camel."));
    }

    @Test
    public void wrapLongWords() {
        String longWord = "lalalalalalalalalala";
        String expectedWrappedWord = "l\n" + "a\n" + "l\n" + "a\n" + "l\n" + "a\n" + "l\n" + "a\n" + "l\n" + "a\n" + "l\n" + "a\n" + "l\n" + "a\n" + "l\n" + "a\n";
        StringUtils.wrapWords(longWord, null, 0, true);
    }

    @Test
    public void dontWrapLongWords() {
        String longWord = "lalalalalalalalalala";
        String expectedWrappedWord = "l\n" + "a\n" + "l\n" + "a\n" + "l\n" + "a\n" + "l\n" + "a\n" + "l\n" + "a\n" + "l\n" + "a\n" + "l\n" + "a\n" + "l\n" + "a\n";
        StringUtils.wrapWords(longWord, null, 0, false);
    }

    @Test
    public void wrapLongWordsContainingSpaces() {
        String longWord = " lalalalalala lalalala";
        String expectedWrappedWord = "l\n" + "a\n" + "l\n" + "a\n" + "l\n" + "a\n" + "l\n" + "a\n" + "l\n" + "a\n" + "l\n" + "a\n" + "l\n" + "a\n" + "l\n" + "a\n";
        StringUtils.wrapWords(longWord, null, 0, false);
    }

    @Test
    public void wrapNothin() {
        assertNull(StringUtils.wrapWords(null, "\n", 80, true));
    }

    @Test
    public void removeLastNewLineBreak() {
        assertEquals("l\n" + "a\n" + "l\n" + "a\n" + "\n", StringUtils.wrapSeparator("lala\n", "", "\n", 0));
    }

    @Test
    public void isEmpty() {
        assertTrue(StringUtils.isEmpty(null));
        assertTrue(StringUtils.isEmpty(""));
    }

    @Test
    public void isNotEmpty() {
        assertTrue(StringUtils.isNotEmpty("test"));
    }
}
