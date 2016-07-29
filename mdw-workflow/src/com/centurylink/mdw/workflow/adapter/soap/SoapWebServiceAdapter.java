/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.adapter.soap;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Node;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.exception.PropertyException;
import com.centurylink.mdw.common.translator.VariableTranslator;
import com.centurylink.mdw.common.translator.XmlDocumentTranslator;
import com.centurylink.mdw.common.utilities.HttpHelper;
import com.centurylink.mdw.connector.adapter.AdapterException;
import com.centurylink.mdw.connector.adapter.ConnectionException;
import com.centurylink.mdw.model.StringDocument;
import com.centurylink.mdw.model.value.variable.DocumentReference;
import com.centurylink.mdw.workflow.adapter.http.HttpServiceAdapter;
import com.centurylink.mdw.xml.DomHelper;

abstract public class SoapWebServiceAdapter extends HttpServiceAdapter {

    public static final String PROP_WSDL = "WSDL";
    public static final String SOAP_VERSION = "SoapVersion";
    public static final String SOAP_VERSION_11 = "SOAP 1.1 Protocol";
    public static final String SOAP_VERSION_12 = "SOAP 1.2 Protocol";
    public static final String SOAP_ACTION = "SOAPAction";


    protected static final String ARTIS_NAMESPACE = "http://www.qwest.com/artis";
    private static final String ARTIS_NS_PREFIX = "artis";
    private static final String ARTIS_LOCAL_NAME = "ArtisSoapHeader";

    private SOAPMessage soapRequest;
    private SOAPMessage soapResponse;

    @Override
    public Object openConnection() throws ConnectionException {
        try {
            String wsdl = getWsdlUrl();
            if (wsdl == null)
                throw new ConnectionException("Missing attribute: WSDL");

            URL wsdlUrl = new URL(wsdl);
            return wsdlUrl.openConnection();
        }
        catch (Exception ex) {
            throw new ConnectionException(-1, ex.getMessage(), ex);
        }
    }


    /**
     * Builds the request XML.
     */
    protected String getRequestData() throws ActivityException {
        Object request = null;
        String requestVarName = getAttributeValue(REQUEST_VARIABLE);
        if (requestVarName == null)
            throw new ActivityException("Missing attribute: " + REQUEST_VARIABLE);
        String requestVarType = getParameterType(requestVarName);

        request = getParameterValue(requestVarName);
        if (!hasPreScript()) {
            if (request == null)
                throw new ActivityException("Request data is null: " + requestVarName);
            if (!(request instanceof DocumentReference))
                throw new ActivityException("Request data must be a DocumentReference: " + requestVarName);
        }

        try {
            Object requestObj = request == null ? null : getDocument((DocumentReference)request, requestVarType);
            if (hasPreScript()) {
                Object ret = executePreScript(requestObj);
                if (ret == null) {
                    // nothing returned; requestVar may have been assigned by script
                    Object req = getParameterValue(requestVarName);
                    if (req == null)
                      throw new ActivityException("Request data is null: " + requestVarName);
                    requestObj = getDocument((DocumentReference)req, requestVarType);
                }
                else {
                    requestObj = ret;
                    setParameterValueAsDocument(requestVarName, this.getProcessDefinition().getVariable(requestVarName).getVariableType(), requestObj);
                }
            }
            soapRequest = createSoapRequest(requestObj);
            return DomHelper.toXml(soapRequest.getSOAPPart().getDocumentElement());
        }
        catch (TransformerException ex) {
            throw new ActivityException(ex.getMessage(), ex);
        }
    }

    /**
     * Populate the SOAP request message.
     */
    abstract protected SOAPMessage createSoapRequest(Object requestObj) throws ActivityException;

    /**
     * Performs the work of invoking the Web service. The main reason to
     * override this is to control whether error conditions are retryable by
     * throwing either ConnectionException (retry) or AdapterException (no retry).
     *
     * @param connection
     *            the connection object created in openConnection()
     * @param requestData
     *            the requestData populated in getRequestData()
     * @return object representing the service response
     */
    @Override
    public String invoke(Object connection, String request, int timeout, Map<String,String> headers)
    throws ConnectionException, AdapterException {
        HttpHelper httpHelper = null;
        try {
            if (headers != null) {
                for (String key : headers.keySet()) {
                    if (key.startsWith("Artis")) {
                        request = addArtisSoapHeader(request, headers);
                        if (requestDocId != null) {
                            DocumentReference requestDocRef = new DocumentReference(requestDocId, null);
                            updateDocumentContent(requestDocRef, request, String.class.getName());
                        }
                        break;
                    }
                }
            }

            // invoke service over http
            // allow users to override
            httpHelper = getHttpHelper(connection);

            int connectTimeout = getConnectTimeout();
            if (connectTimeout > 0)
                httpHelper.setConnectTimeout(connectTimeout);

            int readTimeout = getReadTimeout();
            if (readTimeout > 0)
                httpHelper.setReadTimeout(readTimeout);

            httpHelper.setHeaders(headers);
            String response = httpHelper.post(request);
            if (response.indexOf(ARTIS_NAMESPACE) > 0) {
                try {
                    String artisHeader = extractArtisHeaders(response);
                    if (artisHeader != null) {
                        Name artisName = soapRequest.getSOAPPart().getEnvelope().createName(ARTIS_LOCAL_NAME, ARTIS_NS_PREFIX, ARTIS_NAMESPACE);
                        if (getSoapResponseHeaders() == null)
                            setSoapResponseHeaders(new HashMap<Name,String>());
                        getSoapResponseHeaders().put(artisName, artisHeader);
                    }
                }
                catch (Exception ex) {
                    // don't interfere with response logging
                    logger.severeException(ex.getMessage(), ex);
                }
            }
            return response;
        }
        catch (IOException ex) {
            if (httpHelper != null && httpHelper.getResponse() != null)
                logMessage(httpHelper.getResponse(), true);
            this.logexception(ex.getMessage(), ex);
            throw new ConnectionException(-1, ex.getMessage(), ex);
        }
        catch (Exception ex) {
            this.logexception(ex.getMessage(), ex);
            throw new AdapterException(-1, ex.getMessage(), ex);
        }
        finally {
            if (httpHelper != null) {
                if (getResponseHeaders() == null)
                    setResponseHeaders(new HashMap<String,String>());
                getResponseHeaders().putAll(httpHelper.getHeaders());
            }
        }
    }


    /**
     * Overriding this method affords the opportunity to parse the response and
     * populate process variables as needed.
     */
    @Override
    public void onSuccess(String response)
    throws ActivityException, ConnectionException, AdapterException {
        try {
            // set the variable value based on the unwrapped soap content
            soapResponse = getSoapResponse(response);
            Node childElem = unwrapSoapResponse(soapResponse);
            String responseXml = DomHelper.toXml(childElem);

            String responseVarName = getAttributeValue(RESPONSE_VARIABLE);
            if (responseVarName == null)
                throw new AdapterException("Missing attribute: " + RESPONSE_VARIABLE);
            String responseVarType = getParameterType(responseVarName);

            if (!VariableTranslator.isDocumentReferenceVariable(responseVarType))
                throw new AdapterException("Response variable must be a DocumentReference: " + responseVarName);

            if (responseVarType.equals(StringDocument.class.getName())) {
                setParameterValueAsDocument(responseVarName, responseVarType, responseXml);
            }
            else {
                com.centurylink.mdw.variable.VariableTranslator varTrans = VariableTranslator.getTranslator(getPackage(), responseVarType);
                if (!(varTrans instanceof XmlDocumentTranslator))
                    throw new AdapterException("Unsupported response variable type: " + responseVarType + " (must implement XmlDocumentTranslator)");
                XmlDocumentTranslator xmlDocTrans = (XmlDocumentTranslator) varTrans;
                Object responseObj = xmlDocTrans.fromDomNode(childElem);
                setParameterValueAsDocument(responseVarName, responseVarType, responseObj);
            }
        }
        catch (Exception ex) {
            throw new ActivityException(ex.getMessage(), ex);
        }
    }

    /**
     * Unwrap the SOAP response into a DOM Node.
     */
    abstract protected Node unwrapSoapResponse(SOAPMessage soapResponse) throws ActivityException, AdapterException;

    protected String getWsdlUrl() throws PropertyException {
        return this.getAttributeValueSmart(PROP_WSDL);
    }

    private Long requestDocId;

    @Override
    protected Long logMessage(String message, boolean isResponse) {
        // store the request doc id so it can be updated if necessary with Artis headers
        Long docId = super.logMessage(message, isResponse);
        if (!isResponse)
            requestDocId = docId;
        return docId;
    }

    /**
     * Override to populate soap request header values
     */
    protected Map<Name,String> getSoapRequestHeaders() {
        return null;
    }

    private Map<Name,String> soapResponseHeaders;
    protected Map<Name,String> getSoapResponseHeaders() { return soapResponseHeaders; }
    protected void setSoapResponseHeaders(Map<Name,String> headers) { this.soapResponseHeaders = headers; }

    protected void extractSoapResponseHeaders(SOAPHeader header) throws TransformerException {
        Map<Name,String> soapHeaders = new HashMap<Name,String>();
        Iterator<?> iter = header.examineAllHeaderElements();
        while (iter.hasNext()) {
            SOAPHeaderElement headerElem = (SOAPHeaderElement) iter.next();
            soapHeaders.put(headerElem.getElementName(), headerElem.getTextContent());
        }
        setSoapResponseHeaders(soapHeaders);
    }

    protected String extractArtisHeaders(String response) throws IOException, SOAPException, TransformerException {

        SOAPHeader soapHeader = getSoapResponse(response).getSOAPHeader();
        if (soapHeader != null) {
            Iterator<?> iter = soapHeader.examineAllHeaderElements();
            while (iter.hasNext()) {
                SOAPHeaderElement headerElem = (SOAPHeaderElement) iter.next();
                if (ARTIS_NAMESPACE.equals(headerElem.getNamespaceURI()) && ARTIS_LOCAL_NAME.equals(headerElem.getLocalName())) {
                    if (getResponseHeaders() == null)
                        setResponseHeaders(new HashMap<String,String>());
                    for (Iterator<?> iter2 = headerElem.getChildElements(); iter2.hasNext(); ) {
                        Node child = (Node) iter2.next();
                        if (child.getNodeType() == Node.ELEMENT_NODE)
                            getResponseHeaders().put(child.getLocalName(), child.getTextContent());
                    }
                    return DomHelper.toXml(headerElem);
                }
            }
        }
        return null;
    }

    protected SOAPMessage getSoapRequest(String request) throws IOException, SOAPException {
        if (soapRequest == null) {
            soapRequest = getSoapMessageFactory().createMessage(null, new ByteArrayInputStream(request.getBytes()));
        }
        return soapRequest;
    }

    protected SOAPMessage getSoapResponse(String response) throws IOException, SOAPException {
        if (soapResponse == null) {
            soapResponse = getSoapMessageFactory().createMessage(null, new ByteArrayInputStream(response.getBytes()));
        }
        return soapResponse;
    }

    protected String addArtisSoapHeader(String request, Map<String,String> headers) throws IOException, SOAPException, TransformerException {

        SOAPMessage soapRequest = getSoapRequest(request);
        Name artisName = soapRequest.getSOAPPart().getEnvelope().createName(ARTIS_LOCAL_NAME, ARTIS_NS_PREFIX, ARTIS_NAMESPACE);
        // extract soap response headers
        SOAPHeader header = soapRequest.getSOAPHeader();
        SOAPHeaderElement element = header.addHeaderElement(artisName);
        element.setMustUnderstand(false);
        for (String key : headers.keySet()) {
            if (key.startsWith("Artis")) {
                SOAPElement child = element.addChildElement(key, ARTIS_NS_PREFIX, ARTIS_NAMESPACE);
                child.addTextNode(headers.get(key));
            }
        }

        return DomHelper.toXml(soapRequest.getSOAPPart().getDocumentElement());
    }

    private MessageFactory messageFactory;
    protected MessageFactory getSoapMessageFactory() throws SOAPException {
        if (messageFactory == null) {
            String soapVersion = getSoapVersion();
            if (soapVersion == null) {
                messageFactory = MessageFactory.newInstance();
            }
            else {
                if (ApplicationContext.isOsgi() && System.getProperty("javax.xml.soap.MetaFactory") == null && System.getProperty("javax.xml.soap.MessageFactory") == null) {
                    try {
                        if (localMetaFactory == null) {
                            localMetaFactory = new LocalMetaFactory();
                        }
                        messageFactory = localMetaFactory.newMessageFactory(soapVersion);
                        return messageFactory;
                    }
                    catch (Throwable t) {
                        logger.severeException(t.getMessage(), t);
                    }
                }
                messageFactory = MessageFactory.newInstance(soapVersion);
            }
        }
        return messageFactory;
    }


    private LocalMetaFactory localMetaFactory;
    /**
     * workaround ServiceMix bug: https://issues.apache.org/jira/browse/SMX4-1089
     */
    private class LocalMetaFactory extends com.sun.xml.messaging.saaj.soap.SAAJMetaFactoryImpl {
        @Override
        public MessageFactory newMessageFactory(String protocol) throws SOAPException {
            return super.newMessageFactory(protocol);
        }
    }

    private SOAPFactory soapFactory;
    protected SOAPFactory getSoapFactory() throws SOAPException {
        if (soapFactory == null) {
            soapFactory = SOAPFactory.newInstance();
        }
        return soapFactory;
    }

    @Override
    protected Map<String,String> getRequestHeaders() {
        Map<String,String> headers = super.getRequestHeaders();
        String soapAction = getSoapAction();
        if (soapAction != null) {
            if (headers == null)
                headers = new HashMap<String,String>();
            headers.put(SOAP_ACTION, soapAction);
        }

         return headers;
    }

    protected String getSoapVersion() {
        return getAttributeValue(SOAP_VERSION);
    }

    /**
     * The SOAPAction HTTP request header value.
     */
    protected String getSoapAction() {
        String soapAction = null;
        try {
            soapAction = getAttributeValueSmart(SOAP_ACTION);
        }
        catch (PropertyException ex) {
            logger.severeException(ex.getMessage(), ex);
        }

        if (soapAction == null) {
            // required by SOAP 1.1 (http://www.w3.org/TR/soap11/#_Toc478383528)
            String soapVersion = getSoapVersion();
            if (soapVersion != null && soapVersion.equals(SOAP_VERSION_11))
                soapAction = "";
        }

        return soapAction;
    }


    @Override
    public String getAdapterInvocationErrorMessage() {

        try {
            return "Adapter Invocation Exception - endpoint = "+getWsdlUrl()+ " : Adapter Interface = "+getClass().getName();
        }
        catch (PropertyException e) {
            return "Adapter Invocation Exception";
        }
    }
}
