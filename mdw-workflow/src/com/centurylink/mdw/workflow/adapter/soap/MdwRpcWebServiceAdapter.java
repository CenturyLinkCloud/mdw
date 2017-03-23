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
package com.centurylink.mdw.workflow.adapter.soap;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPBodyElement;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.connector.adapter.AdapterException;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.translator.VariableTranslator;
import com.centurylink.mdw.translator.XmlDocumentTranslator;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.xml.DomHelper;


/**
 * Old MDW RPC-style web service adapter that simulates a document-style service by specifying
 * a single operation in it's WSDL that accepts and returns a simple string that's expected to contain
 * an XML payload that corresponds to an externally agreed-on XSD (outside the WSDL).
 */
@Tracked(LogLevel.TRACE)
public class MdwRpcWebServiceAdapter extends SoapWebServiceAdapter {

    /**
     * Populate the SOAP request message.
     */
    protected SOAPMessage createSoapRequest(Object requestObj) throws ActivityException {
        try {
            MessageFactory messageFactory = getSoapMessageFactory();
            SOAPMessage soapMessage = messageFactory.createMessage();
            Map<Name,String> soapReqHeaders = getSoapRequestHeaders();
            if (soapReqHeaders != null) {
                SOAPHeader header = soapMessage.getSOAPHeader();
                for (Name name : soapReqHeaders.keySet()) {
                    header.addHeaderElement(name).setTextContent(soapReqHeaders.get(name));
                }
            }

            SOAPBody soapBody = soapMessage.getSOAPBody();

            Document requestDoc = null;
            if (requestObj instanceof String) {
                requestDoc = DomHelper.toDomDocument((String)requestObj);
            }
            else {
                Variable reqVar = getProcessDefinition().getVariable(getAttributeValue(REQUEST_VARIABLE));
                XmlDocumentTranslator docRefTrans = (XmlDocumentTranslator)VariableTranslator.getTranslator(getPackage(), reqVar.getVariableType());
                requestDoc = docRefTrans.toDomDocument(requestObj);
            }

            SOAPBodyElement bodyElem = soapBody.addBodyElement(getOperation());
            String requestLabel = getRequestLabelPartName();
            if (requestLabel != null) {
                SOAPElement serviceNameElem = bodyElem.addChildElement(requestLabel);
                serviceNameElem.addTextNode(getRequestLabel());
            }
            SOAPElement requestDetailsElem = bodyElem.addChildElement(getRequestPartName());
            requestDetailsElem.addTextNode("<![CDATA[" + DomHelper.toXml(requestDoc) + "]]>");

            return soapMessage;
        }
        catch (Exception ex) {
            throw new ActivityException(ex.getMessage(), ex);
        }
    }

    /**
     * Unwrap the SOAP response into a DOM Node.
     */
    protected Node unwrapSoapResponse(SOAPMessage soapResponse) throws ActivityException, AdapterException {
        try {
            // unwrap the soap content from the message
            SOAPBody soapBody = soapResponse.getSOAPBody();
            Node childElem = null;
            Iterator<?> it = soapBody.getChildElements();
            while (it.hasNext()) {
                Node node = (Node) it.next();
                if (node.getNodeType() == Node.ELEMENT_NODE && node.getLocalName().equals(getOutputMessageName())) {
                    NodeList childNodes = node.getChildNodes();
                    for (int i = 0; i < childNodes.getLength(); i++) {
                        if (getResponsePartName().equals(childNodes.item(i).getLocalName())) {
                            String content = childNodes.item(i).getTextContent();
                            childElem = DomHelper.toDomNode(content);
                        }
                    }
                }
            }
            if (childElem == null)
              throw new SOAPException("SOAP body child element not found");

            // extract soap response headers
            SOAPHeader header = soapResponse.getSOAPHeader();
            if (header != null) {
                extractSoapResponseHeaders(header);
            }

            return childElem;
        }
        catch (Exception ex) {
            throw new ActivityException(ex.getMessage(), ex);
        }
    }

    @Override
    protected Map<String,String> getRequestHeaders() {
        Map<String,String> headers = super.getRequestHeaders();
        if (headers == null)
            headers = new HashMap<String,String>();
        headers.put("Content-Type", "text/xml; charset=utf-8");  // WebLogic services need this content-type
        return headers;
    }

    protected static final String PAYLOAD_NAMESPACE = "http://mdw.qwest.com/listener/webservice";
    protected Name getOperation() throws SOAPException {
        SOAPFactory soapFactory = getSoapFactory();
        return soapFactory.createName(getOperationName(), "mdw", PAYLOAD_NAMESPACE);
    }

    protected String getOperationName() {
        return "invokeWebService";
    }

    /**
     * Override this and return null if your input message does not contain a label.
     */
    protected String getRequestLabelPartName() {
        return "ServiceName";
    }

    protected String getRequestLabel() {
        return "";
    }

    protected String getRequestPartName() {
        return "RequestDetails";
    }

    protected String getOutputMessageName() {
        return "invokeWebServiceResponse";
    }

    protected String getResponsePartName() {
        return "Response";
    }
}
