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
package com.github.cameltooling.idea.runner.debugger.stack;

import com.github.cameltooling.idea.util.StringUtils;
import com.intellij.psi.PsiElement;
import com.intellij.util.ArrayUtil;
import com.intellij.xdebugger.XSourcePosition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CamelMessageInfo {

    private Map<String, Value[]> headers;
    private Map<String, Value[]> properties;

    private Value body;
    private String exchangeId;

    private final String messageInfoAsXML;
    private final DocumentBuilder documentBuilder;

    private XSourcePosition position;
    private PsiElement tag;

    private String routeId;
    private String processorId;
    private String processor;

    private List<CamelMessageInfo> stack;

    public CamelMessageInfo(@NotNull String messageInfoAsXML,
                            XSourcePosition position,
                            PsiElement tag,
                            String routeId,
                            String processorId,
                            String processor,
                            List<CamelMessageInfo> stack) throws Exception {
        this.messageInfoAsXML = messageInfoAsXML;
        this.documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        this.position = position;
        this.tag = tag;
        this.routeId = routeId;
        this.processorId = processorId;
        this.processor = processor;
        this.stack = stack;
        init();
    }

    private void init() throws Exception {
        InputStream targetStream = new ByteArrayInputStream(messageInfoAsXML.getBytes());
        Document document = documentBuilder.parse(targetStream);

        headers = new HashMap<>();

        //parse headers
        NodeList headersNodeList = document.getElementsByTagName("header");
        for (int i = 0; i < headersNodeList.getLength(); i++) {
            Element nextHeader = (Element) headersNodeList.item(i);
            String key = nextHeader.getAttribute("key");
            String type = nextHeader.getAttribute("type");
            String value = nextHeader.getTextContent();

            if (StringUtils.isEmpty(type)) {
                type = "java.lang.String";
            }
            if (StringUtils.isEmpty(value)) {
                value = "";
            }
            Value newValue = new Value(type, value);
            if (headers.containsKey(key)) {
                headers.put(key, ArrayUtil.append(headers.get(key), newValue));
            } else {
                headers.put(key, new Value[]{newValue});
            }
        }
        //Get Exchange ID
        Element exchangeElement = (Element) (document.getElementsByTagName("exchangeId").item(0));
        exchangeId = exchangeElement.getTextContent();
        //Get Body
        Element bodyElement = (Element) (document.getElementsByTagName("body").item(0));
        body = new Value(bodyElement.getAttribute("type"), bodyElement.getTextContent());

        NodeList propertiesNodeList = document.getElementsByTagName("exchangeProperty");
        if (propertiesNodeList.getLength() > 0) {
            properties = new HashMap<>();
        }
        for (int i = 0; i < propertiesNodeList.getLength(); i++) {
            Element nextProp = (Element) propertiesNodeList.item(i);
            String key = nextProp.getAttribute("name");
            String type = nextProp.getAttribute("type");
            String value = nextProp.getTextContent();

            if (StringUtils.isEmpty(type)) {
                type = "java.lang.String";
            }
            if (StringUtils.isEmpty(value)) {
                value = "";
            }
            Value newValue = new Value(type, value);
            properties.put(key, new Value[]{newValue});
        }
    }

    public Map<String, Value[]> getHeaders() {
        return headers;
    }

    @Nullable
    public Map<String, Value[]> getProperties() {
        return properties;
    }

    public Value getBody() {
        return body;
    }

    public String getExchangeId() {
        return exchangeId;
    }

    public XSourcePosition getXSourcePosition() {
        return this.position;
    }

    public PsiElement getTag() {
        return tag;
    }

    public List<CamelMessageInfo> getStack() {
        return stack;
    }

    public void setStack(List<CamelMessageInfo> stack) {
        this.stack = stack;
    }

    public String getRouteId() {
        return routeId;
    }

    public String getProcessorId() {
        return processorId;
    }

    public String getProcessor() {
        return processor;
    }

    public static class Value {
        private String type;
        private Object value;

        public Value(String type, Object value) {
            this.type = type;
            this.value = value;
        }

        public String getType() {
            return type;
        }

        public Object getValue() {
            return value;
        }
    }


}
