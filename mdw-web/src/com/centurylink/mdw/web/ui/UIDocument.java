/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.ui;

import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.common.translator.VariableTranslator;
import com.centurylink.mdw.common.translator.XmlDocumentTranslator;
import com.centurylink.mdw.common.translator.impl.DomDocumentTranslator;
import com.centurylink.mdw.common.utilities.UniversalNamespaceCache;
import com.centurylink.mdw.model.StringDocument;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.model.value.variable.DocumentVO;
import com.centurylink.mdw.xml.DomHelper;

/**
 * Facilitates xpath-based reads and updates of a document variable
 * via expressions within JSF facelet pages.  For java.lang.Object type
 * documents, the de-serialized content is simply stored in memory;
 */
public class UIDocument
{
  private DocumentVO documentVO;
  public DocumentVO getDocumentVO() { return documentVO; }
  public String getType()
  {
    return documentVO.getDocumentType();
  }

  public Object getObject()
  {
    return documentVO.getObject(getType(), packageVO);
  }

  private PackageVO packageVO;
  public PackageVO getPackageVO() { return packageVO; }

  public UIDocument(DocumentVO documentVO, PackageVO packageVO)
  {
    this.documentVO = documentVO;
    this.packageVO = packageVO;
  }

  public UIDocument(DocumentVO documentVO, UIDocument parent)
  {
    this(documentVO, parent.getPackageVO());
    this.parent = parent;
  }

  private UIDocument parent;
  public UIDocument getParent() { return parent; }

  public boolean isJavaObject()
  {
    return documentVO.getDocumentType().equals(Object.class.getName());
  }

  public boolean isJaxbElement()
  {
    return documentVO.getDocumentType().equals("javax.xml.bind.JAXBElement");
  }

  public boolean isXml()
  {
    return DocumentReferenceTranslator.isXmlDocumentTranslator(getType());
  }

  public boolean isDomDocument()
  {
    return documentVO.getDocumentType().equals(Document.class.getName());
  }

  public boolean isXmlBean()
  {
    return documentVO.getDocumentType().equals(XmlObject.class.getName());
  }

  public boolean isStringDocument()
  {
    return documentVO.getDocumentType().equals(StringDocument.class.getName());
  }

  // variable name for the document
  private String name;
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }


  /**
   * Retrieves the value of a document element or attribute as a String.
   * If multiple matches are found or the match has children, will return a List<UIDocument>.
   * @param xpath the expression indicating the document location
   * @return a String, List<UIDocument>, or null depending on how many matches are found
   */
  public Object peek(String xpath) throws Exception
  {
    if (ApplicationContext.isOsgi())
      return peekDom(xpath);
    else
      return peekXmlBean(xpath);
  }

  protected Object peekXmlBean(String xpath) throws XmlException
  {
    if (xpath.charAt(0) == '/')
      xpath = xpath.substring(1);

    XmlObject[] matches = null;

    if (isXmlBean())
    {
      XmlObject xmlBean = (XmlObject) getObject();
      matches = xmlBean.selectPath(xpath);
    }
    else if (isDomDocument())
    {
      matches = getDomDocAsXmlBean().selectPath(xpath);
    }
    else
    {
      throw new UnsupportedOperationException("Document type not supported: " + getType());
    }

    if (matches == null || matches.length == 0)
      return null;

    // if only one match and no children, return string value
    if (matches.length == 1)
    {
      String stringValue = null;
      XmlCursor xmlCursor = matches[0].newCursor();
      if (!xmlCursor.toChild(0))
        stringValue = xmlCursor.getTextValue();
      xmlCursor.dispose();
      if (stringValue != null)
        return stringValue;
    }

    List<UIDocument> childDocs = new ArrayList<UIDocument>(matches.length);
    for (XmlObject match : matches)
    {
      DocumentVO docVO = new DocumentVO();
      if (isXmlBean())
      {
        docVO.setObject(match);
        docVO.setDocumentType(XmlObject.class.getName());
        childDocs.add(new UIDocument(docVO, this));
      }
      else if (isDomDocument())
      {
        docVO.setObject(match.getDomNode());
        docVO.setDocumentType(Document.class.getName());
        childDocs.add(new UIDocument(docVO, this));
      }
    }
    return childDocs;
  }

  /**
   * TODO: cache DOM document
   */
  protected Object peekDom(String xpathExpr) throws Exception
  {
    String docType = getDocumentVO().getDocumentType();
    XmlDocumentTranslator translator = (XmlDocumentTranslator)VariableTranslator.getTranslator(docType);
    Document doc = translator.toDomDocument(getDocumentVO().getObject(docType, packageVO));

    XPathFactory xPathfactory = XPathFactory.newInstance();
    XPath xpath = xPathfactory.newXPath();
    xpath.setNamespaceContext(new UniversalNamespaceCache(doc, false));
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
      List<UIDocument> childDocs = new ArrayList<UIDocument>();
      for (int i = 0; i < nodeList.getLength(); i++)
      {
        Node node = nodeList.item(i);
        DocumentVO docVO = new DocumentVO();
        docVO.setObject(DomHelper.toDomDocument(node));
        docVO.setDocumentType(Document.class.getName());
        childDocs.add(new UIDocument(docVO, this));
      }
      return childDocs;
    }
  }

  private XmlObject domDocAsXmlBean;  // cached to avoid multiple parsings
  private XmlObject getDomDocAsXmlBean() throws XmlException
  {
    if (domDocAsXmlBean == null)
    {
      Document domDoc = (Document) getObject();
      domDocAsXmlBean = XmlObject.Factory.parse(domDoc);
    }
    return domDocAsXmlBean;
  }

  /**
   * Updates the first matching element or attribute in a document.
   * @param xpath the expression indicating the document location
   * @param value the new value to substitute with
   */
  public void poke(String xpath, String value) throws Exception
  {
    if (ApplicationContext.isOsgi())
      pokeDom(xpath, value);
    else
      pokeXmlBean(xpath, value);
  }

  protected void pokeXmlBean(String xpath, String value) throws XmlException
  {
    if (xpath.charAt(0) == '/')
      xpath = xpath.substring(1);

    XmlObject[] matches = null;
    if (isXmlBean())
    {
      XmlObject xmlBean = (XmlObject)getObject();
      matches = xmlBean.selectPath(xpath);
    }
    else if (isDomDocument())
    {
      matches = getDomDocAsXmlBean().selectPath(xpath);
    }
    else
    {
      throw new UnsupportedOperationException("Document type not supported: " + getType());
    }


    if (matches == null || matches.length == 0)
      throw new XmlException("No matches found for XPath expression: " + xpath);

    // update the first match found
    XmlCursor xmlCursor = matches[0].newCursor();
    xmlCursor.setTextValue(value);
    xmlCursor.dispose();

    if (isDomDocument())
    {
      documentVO.setObject(getDomDocAsXmlBean().getDomNode());
    }
  }

  /**
   * TODO: cache DOM document
   */
  protected void pokeDom(String xpathExpr, String value) throws XPathExpressionException
  {
    String docType = getDocumentVO().getDocumentType();
    XmlDocumentTranslator translator = (XmlDocumentTranslator)VariableTranslator.getTranslator(docType);
    Document doc = translator.toDomDocument(getDocumentVO().getObject(docType, packageVO));

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

  @Override
  public boolean equals(Object other)
  {
    if (other instanceof UIDocument)
    {
      UIDocument otherDoc = (UIDocument)other;
      if (otherDoc.getType().equals(this.getType()))
      {
        if (otherDoc.isJavaObject())
          return otherDoc.getObject().equals(getObject());
        else
          return otherDoc.toString().equals(this.toString());
      }
    }

    return false;
  }

  public String toString()
  {
    if (isXmlBean())
      return ((XmlObject)getObject()).xmlText();
    else if (isDomDocument())
      return new DomDocumentTranslator().realToString(getObject());
    else
      return getObject().toString();
  }

  /**
   * Backward compatibility.
   */
  public String getXmlText()
  {
    return toString();
  }
}
