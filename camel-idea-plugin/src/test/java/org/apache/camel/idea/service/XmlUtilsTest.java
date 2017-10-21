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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class XmlUtilsTest {

    private Document document;

    public XmlUtilsTest() throws Exception {
        document = XmlUtils.loadDocument(readTestXmlFile(), true);
    }

    @Test
    public void loadDocument() {
        assertEquals("problems", document.getDocumentElement().getTagName());
    }

    @Test
    public void getChildNodeByTagName() {
        NodeList problem = document.getElementsByTagName("problem");
        Node item = problem.item(0);
        Node description = XmlUtils.getChildNodeByTagName(item, "description");
        assertEquals("fileExist is not applicable in consumer only mode", description.getTextContent());

    }

    @Test
    public void returnNullWhenRootDoesNotContainChildren() throws Exception {
        Document document = XmlUtils.loadDocument(readTestXmlFileWithoutChildren(), true);
        Element root = document.getDocumentElement();
        Node nothing = XmlUtils.getChildNodeByTagName(document, "description");
        assertNull(nothing);
    }

    private FileInputStream readTestXmlFile() {
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream("src/test/resources/testData/inspectionxml/expected.xml");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return fileInputStream;
    }

    private FileInputStream readTestXmlFileWithoutChildren() {
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream("src/test/resources/testData/inspectionxml/expected-xml-without-children.xml");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return fileInputStream;
    }
}