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
package com.centurylink.mdw.hub.servlet;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.xmlbeans.XmlException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.centurylink.mdw.app.Compatibility;
import com.centurylink.mdw.bpm.MDWStatusMessageDocument;
import com.centurylink.mdw.bpm.MDWStatusMessageDocument.MDWStatusMessage;
import com.centurylink.mdw.cache.impl.AssetCache;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.listener.ListenerHelper;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.util.timer.CodeTimer;
import com.centurylink.mdw.xml.DomHelper;

/**
 * General SOAP listener servlet corresponding to mdw.wsdl. Request content is
 * pulled out of the SOAP envelope and forwarded to the MDW external event
 * handler mechanism.
 */
@WebServlet(urlPatterns={"/SOAP/*"}, loadOnStartup=1)
public class SoapServlet extends ServiceServlet {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private static Pattern tokenPattern = Pattern.compile("([\\$]\\{.*?\\})");
    private static String RPC_SERVICE_PATH = "/MDWWebService";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (logger.isMdwDebugEnabled()) {
            logger.mdwDebug("SOAP Listener GET Request:\n" + request.getRequestURI()
                    + (request.getQueryString() == null ? "" : ("?" + request.getQueryString())));
        }

        if (request.getServletPath().endsWith(RPC_SERVICE_PATH)
                || RPC_SERVICE_PATH.equals(request.getPathInfo())) {
            Asset rpcWsdlAsset = AssetCache.getAsset(Package.MDW + "/MdwRpcWebService.wsdl", Asset.WSDL);
            response.setContentType("text/xml");
            response.getWriter().print(substituteRuntimeWsdl(rpcWsdlAsset.getStringContent()));
        }
        else if (request.getPathInfo() == null || request.getPathInfo().equalsIgnoreCase("mdw.wsdl")) {
            // forward to general wsdl
            RequestDispatcher requestDispatcher = request.getRequestDispatcher("/mdw.wsdl");
            requestDispatcher.forward(request, response);
        }
        else if (request.getPathInfo().toUpperCase().endsWith(Asset.WSDL)) {
            String wsdlAsset = request.getPathInfo().substring(1);
            Asset asset = AssetCache.getAsset(wsdlAsset, Asset.WSDL);
            if (asset == null) {
                // try trimming file extension
                wsdlAsset = wsdlAsset.substring(0, wsdlAsset.length() - 5);
                asset = AssetCache.getAsset(wsdlAsset, Asset.WSDL);
            }
            if (asset == null) {
                // try with lowercase extension
                wsdlAsset = wsdlAsset + ".wsdl";
                asset = AssetCache.getAsset(wsdlAsset, Asset.WSDL);
            }
            if (asset == null) {
                String message = "No WSDL resource found: " + request.getPathInfo().substring(1);
                logger.severe(message);
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().print(message);
            }
            else {
                response.setContentType("text/xml");
                response.getWriter().print(substituteRuntimeWsdl(asset.getStringContent()));
            }
        }
        else {
            ServletException ex = new ServletException("HTTP GET not supported for URL: " + request.getRequestURL());
            logger.severeException(ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        CodeTimer timer = new CodeTimer("SoapServlet.doPost()", true);

        InputStream reqInputStream = request.getInputStream();
        // read the POST request contents
        String requestString = getRequestString(request);
        if (logger.isMdwDebugEnabled()) {
            logger.mdwDebug("SOAP Listener POST Request:\n" + requestString);
        }

        Map<String,String> metaInfo = buildMetaInfo(request);

        String responseString = null;
        MessageFactory factory = null;
        String soapVersion = SOAPConstants.SOAP_1_1_PROTOCOL;
        try {
            SOAPMessage message = null;
            SOAPBody body = null;
            try {
                // Intuitively guess which SOAP version is needed
                // factory = getMessageFactory(requestString, true);
                soapVersion = getSoapVersion(requestString, true);
                factory = getSoapMessageFactory(soapVersion);
                reqInputStream = new ByteArrayInputStream(requestString.getBytes());

                message = factory.createMessage(null, reqInputStream);
                body = message.getSOAPBody();
            }
            catch (SOAPException e) {
                // Unlikely, but just in case the SOAP version guessing
                // has guessed incorrectly, this catches any SOAP exception,
                // in which case try the other version
                if (logger.isMdwDebugEnabled()) {
                    logger.mdwDebug("SOAPListenerServlet failed to find correct Message Factory:"
                            + "\n" + e.getMessage());
                }
                // Try with the other unintuitive MessageFactory
                // factory = getMessageFactory(requestString, false);
                soapVersion = getSoapVersion(requestString, false);
                factory = getSoapMessageFactory(soapVersion);
                reqInputStream = new ByteArrayInputStream(requestString.getBytes());

                message = factory.createMessage(null, reqInputStream);
                body = message.getSOAPBody();
                // Only 2 versions, so let any exceptions bubble up
            }
            Node childElem = null;
            Iterator<?> it = body.getChildElements();
            while (it.hasNext()) {
                Node node = (Node) it.next();
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    childElem = node;
                    break;
                }
            }
            if (childElem == null)
                throw new SOAPException("SOAP body child element not found");

            String requestXml = null;

            boolean oldStyleRpcRequest = false;
            if (request.getServletPath().endsWith(RPC_SERVICE_PATH)
                    || RPC_SERVICE_PATH.equals(request.getPathInfo())) {
                NodeList nodes = childElem.getChildNodes();
                for (int i = 0; i < nodes.getLength(); i++) {
                    if (StringUtils.isNotBlank(nodes.item(i).getNodeName())
                            && nodes.item(i).getNodeName().equals("RequestDetails")) {
                        oldStyleRpcRequest = true;
                        Node requestNode = nodes.item(i).getFirstChild();
                        if (requestNode.getNodeType() == Node.CDATA_SECTION_NODE) {
                            requestXml = requestNode.getTextContent();
                        }
                        else {
                            requestXml = DomHelper.toXml(requestNode);
                            if (requestXml.contains("&lt;"))
                                requestXml = StringEscapeUtils.unescapeXml(requestXml);
                        }
                    }
                }
            }
            else {
                requestXml = DomHelper.toXml(childElem);
            }

            metaInfo = addSoapMetaInfo(metaInfo, message);
            ListenerHelper helper = new ListenerHelper();

            try {
               authenticate(request, metaInfo, requestXml);
               String handlerResponse = helper.processEvent(requestXml, metaInfo);

               try {
                   // standard response indicates a potential problem
                   MDWStatusMessageDocument responseDoc = MDWStatusMessageDocument.Factory
                           .parse(handlerResponse, Compatibility.namespaceOptions());
                   MDWStatusMessage responseMsg = responseDoc.getMDWStatusMessage();
                   if ("SUCCESS".equals(responseMsg.getStatusMessage()))
                       responseString = createSoapResponse(soapVersion, handlerResponse);
                   else
                       responseString = createSoapFaultResponse(soapVersion,
                               String.valueOf(responseMsg.getStatusCode()),
                               responseMsg.getStatusMessage());
               }
               catch (XmlException xex) {
                   if (Listener.METAINFO_ERROR_RESPONSE_VALUE
                           .equalsIgnoreCase(metaInfo.get(Listener.METAINFO_ERROR_RESPONSE))) {
                       // Support for custom error response
                       responseString = handlerResponse;
                   }
                   else {
                       // not parseable as standard response doc (a good thing)
                       if (oldStyleRpcRequest) {
                           responseString = createOldStyleSoapResponse(soapVersion,
                                   "<m:invokeWebServiceResponse xmlns:m=\"http://mdw.qwest.com/listener/webservice\"><Response>"
                                           + StringEscapeUtils.escapeXml(handlerResponse)
                                           + "</Response></m:invokeWebServiceResponse>");
                       }
                       else {
                           responseString = createSoapResponse(soapVersion, handlerResponse);
                       }
                   }
               }
            }
            catch (ServiceException ex) {
                logger.severeException(ex.getMessage(), ex);
                responseString = createSoapFaultResponse(soapVersion, String.valueOf(ex.getCode()), ex.getMessage());
            }
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            try {
                responseString = createSoapFaultResponse(soapVersion, null, ex.getMessage());
            }
            catch (Exception tex) {
                logger.severeException(tex.getMessage(), tex);
            }
        }

        if (logger.isMdwDebugEnabled()) {
            logger.mdwDebug("SOAP Listener Servlet POST Response:\n" + responseString);
        }

        if (metaInfo.get(Listener.METAINFO_CONTENT_TYPE) != null) {
            response.setContentType(metaInfo.get(Listener.METAINFO_CONTENT_TYPE));
        }
        else {
            if (soapVersion.equals(SOAPConstants.SOAP_1_1_PROTOCOL))
                response.setContentType(Listener.CONTENT_TYPE_XML);
            else
                response.setContentType("application/soap+xml");
        }

        response.getOutputStream().print(responseString);

        timer.stopAndLogTiming("");
    }

    /**
     * <p>
     * Gives a hint as to which version of SOAP using the rudimentary check
     * below. If the hint fails, it'll try the other version anyway.
     * </p>
     * <ul>
     * <li>SOAP 1.1 : http://schemas.xmlsoap.org/soap/envelope/</li>
     * <li>SOAP 1.2 : http://www.w3.org/2003/05/soap-envelope</li>
     * </ul>
     * <p>
     * This is on a per-request basis and can't be static since we need to
     * support SOAP 1.1 and 1.2
     * </p>
     *
     * @param requestString
     * @param goodguess
     * @return SOAP version 1 or 2
     * @throws IOException
     * @throws SOAPException
     */
    private String getSoapVersion(String requestString, boolean goodguess) {

        String guessedVersion = SOAPConstants.SOAP_1_1_PROTOCOL;
        String otherVersion = SOAPConstants.SOAP_1_2_PROTOCOL;
        if (requestString.contains(SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE)) {
            guessedVersion = SOAPConstants.SOAP_1_2_PROTOCOL;
            otherVersion = SOAPConstants.SOAP_1_1_PROTOCOL;
        }
        return goodguess ? guessedVersion : otherVersion;

    }

    private String getRequestString(HttpServletRequest request) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream()));
        StringBuffer requestBuffer = new StringBuffer();
        String line = null;
        while ((line = reader.readLine()) != null) {
            requestBuffer.append(line).append('\n');
        }
        return requestBuffer.toString();

    }

    protected String substituteRuntimeWsdl(String wsdl) {
        StringBuffer substituted = new StringBuffer(wsdl.length());
        Matcher matcher = tokenPattern.matcher(wsdl);
        int index = 0;
        while (matcher.find()) {
            String match = matcher.group();
            substituted.append(wsdl.substring(index, matcher.start()));
            String propName = match.substring(2, match.length() - 1);
            String value = PropertyManager.getProperty(propName);
            if (value != null)
                substituted.append(value);
            index = matcher.end();
        }
        substituted.append(wsdl.substring(index));
        return substituted.toString();
    }

    protected Map<String,String> addSoapMetaInfo(Map<String, String> metaInfo, SOAPMessage soapMessage)
            throws SOAPException {
        SOAPHeader soapHeader = soapMessage.getSOAPHeader();
        if (soapHeader == null) {
            return metaInfo;
        }
        else {
            Map<String, String> newMetaInfo = new HashMap<String, String>();
            newMetaInfo.putAll(metaInfo);
            Iterator<?> iter = soapHeader.examineAllHeaderElements();
            while (iter.hasNext()) {
                SOAPHeaderElement headerElem = (SOAPHeaderElement) iter.next();
                if (!Listener.AUTHENTICATED_USER_HEADER.equals(headerElem.getNodeName()))
                    newMetaInfo.put(headerElem.getNodeName(), headerElem.getTextContent());
            }
            return newMetaInfo;
        }
    }

    /**
     * Original API (Defaults to using MessageFactory.newInstance(), i.e. SOAP
     * 1.1)
     *
     * @param message
     * @return Soap fault as string
     * @throws SOAPException
     * @throws TransformerException
     */
    protected String createSoapFaultResponse(String message)
            throws SOAPException, TransformerException {
        return createSoapFaultResponse(SOAPConstants.SOAP_1_1_PROTOCOL, null, message);
    }

    /**
     * Original API (Defaults to using MessageFactory.newInstance(), i.e. SOAP
     * 1.1)
     *
     * @param code
     * @param message
     * @return Soap fault as string
     * @throws SOAPException
     * @throws TransformerException
     */
    protected String createSoapFaultResponse(String code, String message)
            throws SOAPException, TransformerException {
        return createSoapFaultResponse(SOAPConstants.SOAP_1_1_PROTOCOL, code, message);
    }

    /**
     * Allow version specific factory passed in to support SOAP 1.1 and 1.2
     * <b>Important</b> Faults are treated differently for 1.1 and 1.2 For 1.2
     * you can't use the elementName otherwise it throws an exception
     *
     * @see http://docs.oracle.com/cd/E19159-01/819-3669/bnbip/index.html
     *
     * @param factory
     * @param code
     * @param message
     * @return Xml fault string
     * @throws SOAPException
     * @throws TransformerException
     */
    protected String createSoapFaultResponse(String soapVersion, String code, String message)
            throws SOAPException, TransformerException {

        SOAPMessage soapMessage = getSoapMessageFactory(soapVersion).createMessage();
        SOAPBody soapBody = soapMessage.getSOAPBody();
        /**
         * Faults are treated differently for 1.1 and 1.2 For 1.2 you can't use
         * the elementName otherwise it throws an exception
         *
         * @see http://docs.oracle.com/cd/E19159-01/819-3669/bnbip/index.html
         */
        SOAPFault fault = null;
        if (soapVersion.equals(SOAPConstants.SOAP_1_1_PROTOCOL)) {
            // existing 1.1 functionality
            fault = soapBody.addFault(soapMessage.getSOAPHeader().getElementName(), message);
            if (code != null)
                fault.setFaultCode(code);
        }
        else if (soapVersion.equals(SOAPConstants.SOAP_1_2_PROTOCOL)) {
            /**
             * For 1.2 there are only a set number of allowed codes, so we can't
             * just use any one like what we did in 1.1. The recommended one to
             * use is SOAPConstants.SOAP_RECEIVER_FAULT
             */
            fault = soapBody.addFault(SOAPConstants.SOAP_RECEIVER_FAULT,
                    code == null ? message : code + " : " + message);

        }
        return DomHelper.toXml(soapMessage.getSOAPPart().getDocumentElement());

    }

    /**
     * Original API (Defaults to using MessageFactory.newInstance(), i.e. SOAP
     * 1.1)
     *
     * @param xml
     * @return
     * @throws SOAPException
     */
    protected String createSoapResponse(String xml) throws SOAPException {
        return createSoapResponse(SOAPConstants.SOAP_1_1_PROTOCOL, xml);
    }

    /**
     * Allow version specific factory passed in TODO: allow specifying response
     * headers
     */
    protected String createSoapResponse(String soapVersion, String xml) throws SOAPException {
        try {
            SOAPMessage soapMessage = getSoapMessageFactory(soapVersion).createMessage();
            SOAPBody soapBody = soapMessage.getSOAPBody();
            soapBody.addDocument(DomHelper.toDomDocument(xml));
            return DomHelper.toXml(soapMessage.getSOAPPart().getDocumentElement());
        }
        catch (Exception ex) {
            throw new SOAPException(ex.getMessage(), ex);
        }
    }

    protected String createOldStyleSoapResponse(String soapVersion, String xml)
            throws SOAPException {
        try {
            SOAPMessage soapMessage = getSoapMessageFactory(soapVersion).createMessage();
            SOAPBody soapBody = soapMessage.getSOAPBody();
            soapBody.addDocument(DomHelper.toDomDocument(xml));
            return DomHelper.toXmlNoWhiteSpace(soapMessage.getSOAPPart());
        }
        catch (Exception ex) {
            throw new SOAPException(ex.getMessage(), ex);
        }
    }

    protected MessageFactory getSoapMessageFactory(String soapVersion) throws SOAPException {
        return MessageFactory.newInstance(soapVersion);
    }

}
