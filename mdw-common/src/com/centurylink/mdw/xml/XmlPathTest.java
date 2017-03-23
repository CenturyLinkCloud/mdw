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

import org.apache.xmlbeans.XmlObject;

import junit.framework.TestCase;

public class XmlPathTest extends TestCase {
    
    private static String xml1 = 
        "<Ping xmlns:ps='http:' id='MyId'><G a='8'><ps:H> asf\n</ps:H></G><ps:EventName>START</ps:EventName></Ping>";
    private static String xml2 = "<?xml version='1.0' encoding='utf-8'?>" +
    "<ActivationMessage senderName='Cramer' orderId='100' recipientName='QMOEAT' activationId='567834'" +
    "  xmlns='http://www.qwest.com/XMLSchema'" +
    "  xmlns:qd='http://www.qwest.com/XMLSchema/BIM'" +
    "  xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'" +
    "  xsi:schemaLocation='http://www.qwest.com/XMLSchema" +
    "  C:\\DOCUME~1\\dxvanto\\Desktop\\ngas_activation_request_v7.xsd'>" +
    "   <ReplyTo>" +
    "     <WebServiceAddress targetNamespace='http://www.qwest.com/XMLSchema'" +
    "          serviceName='LimsServiceInterface' " +
    "          portName='3333' " +
    "          wsdlURL='http://10.5.111.220:3333/LimsServiceInterface?WSDL'" +
    "          operationName='callback'/>" +
    "  </ReplyTo>" +
    "</ActivationMessage>";
    
    private void runCase(String xml, String xpath, String expected)
        throws Exception {
        XmlObject xmlBean = XmlObject.Factory.parse(xml);
        XmlPath path = new XmlPath(xpath);
        String actual = path.evaluate(xmlBean);
        assertEquals(expected, actual);
    }

    public void testCase1() throws Exception {
        
        String xpath = "//G[H=asf]";
        String expected = " asf\n";
        runCase(xml1, xpath, expected);
    }
    
    public void testCase2() throws Exception {
        String xpath = "/Ping/@id";
        String expected = "MyId";
        runCase(xml1, xpath, expected);
    }
    
    public void testCase3() throws Exception {
        String xpath = "//@id";
        String expected = "MyId";
        runCase(xml1, xpath, expected);
    }
    
    public void testCase4() throws Exception {
        String xpath = "ActivationMessage/@activationId";
        String expected = "567834";
        runCase(xml2, xpath, expected);
    }
    
    public void testCase5() throws Exception {
        String xpath = "/ActivationMessage/@activationId";
        String expected = "567834";
        runCase(xml2, xpath, expected);
    }
    
    public void testCase6() throws Exception {
        String xpath = "//ActivationMessage/@activationId";
        String expected = "567834";
        runCase(xml2, xpath, expected);
    }
    
}
