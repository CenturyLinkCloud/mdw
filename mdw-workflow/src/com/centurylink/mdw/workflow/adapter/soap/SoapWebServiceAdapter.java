/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
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
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Node;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.config.PropertyException;
import com.centurylink.mdw.connector.adapter.AdapterException;
import com.centurylink.mdw.connector.adapter.ConnectionException;
import com.centurylink.mdw.model.StringDocument;
import com.centurylink.mdw.model.event.AdapterStubRequest;
import com.centurylink.mdw.model.variable.DocumentReference;
import com.centurylink.mdw.translator.VariableTranslator;
import com.centurylink.mdw.translator.XmlDocumentTranslator;
import com.centurylink.mdw.util.HttpHelper;
import com.centurylink.mdw.workflow.adapter.http.HttpServiceAdapter;
import com.centurylink.mdw.xml.DomHelper;

abstract public class SoapWebServiceAdapter extends HttpServiceAdapter {

    public static final String PROP_WSDL = "WSDL";
    public static final String SOAP_VERSION = "SoapVersion";
    public static final String SOAP_VERSION_11 = "SOAP 1.1 Protocol";
    public static final String SOAP_VERSION_12 = "SOAP 1.2 Protocol";
    public static final String SOAP_ACTION = "SOAPAction";

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
            return httpHelper.post(request);
        }
        catch (IOException ex) {
            if (httpHelper != null && httpHelper.getResponse() != null)
                logResponse(httpHelper.getResponse());
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

    private MessageFactory messageFactory;
    protected MessageFactory getSoapMessageFactory() throws SOAPException {
        if (messageFactory == null) {
            String soapVersion = getSoapVersion();
            if (soapVersion == null) {
                messageFactory = MessageFactory.newInstance();
            }
            else {
                messageFactory = MessageFactory.newInstance(soapVersion);
            }
        }
        return messageFactory;
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

    @Override
    protected AdapterStubRequest getStubRequest(String requestContent) throws AdapterException {
        AdapterStubRequest stubRequest = super.getStubRequest(requestContent);
        try {
            stubRequest.setUrl(getWsdlUrl());
            stubRequest.setMethod("POST");
            stubRequest.setHeaders(getRequestHeaders());
        }
        catch (Exception ex) {
            throw new AdapterException(500, ex.getMessage(), ex, false);
        }
        return stubRequest;
    }


}
