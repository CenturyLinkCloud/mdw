/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.xml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class DomHelper {

    public static Document toDomDocument(String xml)
    throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setNamespaceAware(true);
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        // wrap with InputSource to help with utf-16
        Document domDoc = docBuilder.parse(new InputSource(new InputStreamReader(new ByteArrayInputStream(xml
                .getBytes()))));
        domDoc.getDocumentElement().normalize();
        return domDoc;
    }

    public static Node toDomNode(String xml) throws TransformerException {
        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer transformer = tFactory.newTransformer();
        StreamSource source = new StreamSource(new ByteArrayInputStream(xml.getBytes()));
        DOMResult result = new DOMResult();
        transformer.transform(source, result);
        return result.getNode();
    }

    public static String toXml(Document domDoc) throws TransformerException {
        DOMImplementation domImplementation = domDoc.getImplementation();
        if (domImplementation.hasFeature("LS", "3.0") && domImplementation.hasFeature("Core", "2.0")) {
            DOMImplementationLS domImplementationLS = (DOMImplementationLS) domImplementation.getFeature("LS", "3.0");
            LSSerializer lsSerializer = domImplementationLS.createLSSerializer();
            DOMConfiguration domConfiguration = lsSerializer.getDomConfig();
            if (domConfiguration.canSetParameter("xml-declaration", Boolean.TRUE))
                lsSerializer.getDomConfig().setParameter("xml-declaration", Boolean.FALSE);
            if (domConfiguration.canSetParameter("format-pretty-print", Boolean.TRUE)) {
                lsSerializer.getDomConfig().setParameter("format-pretty-print", Boolean.TRUE);
                LSOutput lsOutput = domImplementationLS.createLSOutput();
                lsOutput.setEncoding("UTF-8");
                StringWriter stringWriter = new StringWriter();
                lsOutput.setCharacterStream(stringWriter);
                lsSerializer.write(domDoc, lsOutput);
                return stringWriter.toString();
            }
        }
        return toXml((Node)domDoc);
    }

    public static String toXml(Node domNode) throws TransformerException {
        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer transformer = tFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        DOMSource source = new DOMSource(domNode);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StreamResult result = new StreamResult(baos);
        transformer.transform(source, result);
        return baos.toString();
    }

    public static String toXmlNoWhiteSpace(Node domNode) throws TransformerException {
        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer transformer = tFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        DOMSource source = new DOMSource(domNode);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StreamResult result = new StreamResult(baos);
        transformer.transform(source, result);
        return baos.toString();
    }
    public static Document toDomDocument(Node domNode)
    throws ParserConfigurationException, SAXException, IOException, TransformerException {
        // TODO other way than to reparse?
        return toDomDocument(toXml(domNode));
    }

    public static InputStream toInputStream(Node domNode) throws TransformerException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DOMSource xmlSource = new DOMSource(domNode);
        Result outputTarget = new StreamResult(outputStream);
        TransformerFactory.newInstance().newTransformer().transform(xmlSource, outputTarget);
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    public static Document copyDomDocument(Document originalDomDoc)
    throws ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Node originalRoot = originalDomDoc.getDocumentElement();

        Document copiedDocument = db.newDocument();
        Node copiedRoot = copiedDocument.importNode(originalRoot, true);
        copiedDocument.appendChild(copiedRoot);
        return copiedDocument;
    }

    public static void poke(Document doc, String xpathExpr, String value) throws XPathExpressionException
    {
      XPathFactory xPathfactory = XPathFactory.newInstance();
      XPath xpath = xPathfactory.newXPath();
      XPathExpression expr = xpath.compile(xpathExpr);
      NodeList nodeSet = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
      if (nodeSet != null && nodeSet.getLength() > 0)
      {
        Node node = nodeSet.item(0); // first match
        if (node instanceof Attr)
          ((Attr)node).setValue(value);
        else
          node.getChildNodes().item(0).setNodeValue(value);
      }
    }

    public static Object peek(Document doc, String xpathExpr) throws XPathExpressionException
    {
      XPathFactory xPathfactory = XPathFactory.newInstance();
      XPath xpath = xPathfactory.newXPath();
      XPathExpression expr = xpath.compile(xpathExpr);
      NodeList nodeList = (NodeList)expr.evaluate(doc, XPathConstants.NODESET);
      if (nodeList == null || nodeList.getLength() == 0)
      {
        return null;
      }
      else if (nodeList.getLength() == 1)
      {
        Node node = nodeList.item(0);
        if (node instanceof Attr)
          return ((Attr)node).getValue();
        else
          return node.getChildNodes().item(0).getNodeValue();
      }
      else
      {
        return nodeList;
      }
    }

}
