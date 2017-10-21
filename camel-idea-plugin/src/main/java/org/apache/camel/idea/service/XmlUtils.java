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
package org.apache.camel.idea.service;

import java.io.InputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.jetbrains.annotations.NotNull;

/**
 * XML and DOM utilities.
 */
final class XmlUtils {

    private XmlUtils() {
    }

    /**
     * Loads the input stream into a DOM
     *
     * @param is the input stream
     * @param validating whether to validate or not
     * @return the DOM
     */
    static @NotNull Document loadDocument(@NotNull InputStream is, boolean validating) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = dbf.newDocumentBuilder();
        if (validating) {
            documentBuilder.setErrorHandler(new XmlErrorHandler());
        }
        dbf.setValidating(validating);
        return documentBuilder.parse(is);
    }

    /**
     * Gets a child node by the given name.
     *
     * @param node the node
     * @param name the name of the child node to find and return
     * @return the child node, or <tt>null</tt> if none found
     */
    static Node getChildNodeByTagName(Node node, String name) {
        NodeList children = node.getChildNodes();
        if (children != null && children.getLength() > 0) {
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child instanceof Element) {
                    Element element = (Element) child;
                    if (name.equals(element.getTagName())) {
                        return element;
                    }
                }
            }
        }
        return null;
    }
}
