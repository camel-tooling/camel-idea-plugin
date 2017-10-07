package org.apache.camel.idea.util;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import static org.junit.Assert.*;

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