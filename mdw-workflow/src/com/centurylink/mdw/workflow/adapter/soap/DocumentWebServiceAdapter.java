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
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.connector.adapter.AdapterException;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.translator.VariableTranslator;
import com.centurylink.mdw.translator.XmlDocumentTranslator;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.xml.DomHelper;

@Tracked(LogLevel.TRACE)
public class DocumentWebServiceAdapter extends SoapWebServiceAdapter {

    /**
     * Create the SOAP request object based on the document variable value.
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
                soapBody.addDocument(requestDoc);
            }
            else {
                Variable reqVar = getProcessDefinition().getVariable(getAttributeValue(REQUEST_VARIABLE));
                XmlDocumentTranslator docRefTrans = (XmlDocumentTranslator)VariableTranslator.getTranslator(getPackage(), reqVar.getType());
                requestDoc = docRefTrans.toDomDocument(requestObj);
                Document copiedDocument = DomHelper.copyDomDocument(requestDoc);
                soapBody.addDocument(copiedDocument);
            }

            return soapMessage;
        }
        catch (Exception ex) {
            throw new ActivityException(ex.getMessage(), ex);
        }
    }

    protected Node unwrapSoapResponse(SOAPMessage soapResponse)
    throws ActivityException, AdapterException {
        try {
            SOAPBody soapBody = soapResponse.getSOAPBody();
            Node childElem = null;
            Iterator<?> it = soapBody.getChildElements();
            while (it.hasNext()) {
                Node node = (Node) it.next();
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    childElem = node;
                    break;
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
        String contentType = "application/soap+xml; charset=utf-8";
        if (SOAP_VERSION_12.equals(getSoapVersion())) {
            String soapAction = getSoapAction();
            if (soapAction != null)
                contentType += ";action=\"" + soapAction + "\"";
        }
        headers.put("Content-Type", contentType);
        return headers;
    }
}
