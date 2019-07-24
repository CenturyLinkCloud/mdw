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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;

import com.centurylink.mdw.app.Compatibility;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value="XmlBeanWrapper")
public abstract class XmlBeanWrapper implements Serializable {

    private XmlObject _xmlBean;
    private String _validationError;

    /**
     * Public constructor requires an XmlBean.
     *
     * @param xmlBean
     */
    public XmlBeanWrapper(XmlObject xmlBean) {
        _xmlBean = xmlBean;
        _validationError = null;
    }

    public XmlBeanWrapper(String xml) throws XmlException {
        fromXml(xml);
    }

    /**
     * Subclasses can instantiate without an XmlBean.
     */
    protected XmlBeanWrapper() {
        _validationError = null;
    }

    /**
     * @return XmlBean
     */
    @ApiModelProperty(hidden=true)
    public XmlObject getXmlBean() {
        return _xmlBean;
    }

    @ApiModelProperty(hidden=true)
    public String getValidationError() {
        return _validationError;
    }

    /**
     * @param xmlBean
     */
    public void setXmlBean(XmlObject xmlBean) {
        this._xmlBean = xmlBean;
    }

    @ApiModelProperty(hidden=true)
    public XmlOptions getXmlLoadOptions()  throws XmlException {
        return Compatibility.namespaceOptions();
    }

    @ApiModelProperty(hidden=true)
    public XmlOptions getXmlSaveOptions()  {
        XmlOptions options = new XmlOptions();
        options.setSavePrettyPrint();
        options.setSaveAggressiveNamespaces();
        options.setSavePrettyPrintIndent(2);
        return options;
    }

    @ApiModelProperty(hidden=true)
    public String getXml() {
        return getXmlBean().xmlText(getXmlSaveOptions());
    }

    public String toString() {
        return getXml();
    }

    public void fromXml(String xml) throws XmlException {
        _xmlBean = XmlObject.Factory.parse(xml, getXmlLoadOptions());
    }

    /**
     * Performs validation on the XmlBean, populating the error message if failed.
     * @return false if the XmlBean is invalid (error message is available in getValidationError()).
     */
    public boolean validate() {
        _validationError = null;
        List<XmlError> errors = new ArrayList<XmlError>();

        boolean valid = _xmlBean.validate(new XmlOptions().setErrorListener(errors));
        if (!valid) {
            _validationError = "";
            for (int i = 0; i < errors.size(); i++) {
                _validationError += errors.get(i).toString();
                if (i < errors.size() - 1)
                    _validationError += '\n';
            }
        }

        return valid;
    }

    public String transform(String xslt) throws TransformerException {
        @SuppressWarnings("squid:S4435") // false positive
        TransformerFactory tFactory = TransformerFactory.newInstance();
        tFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

        Source xslSource = new StreamSource(new ByteArrayInputStream(xslt.getBytes()));
        Transformer transformer = tFactory.newTransformer(xslSource);

        Source xmlSource = new StreamSource(new ByteArrayInputStream(getXml().getBytes()));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        transformer.transform(xmlSource, new StreamResult(outputStream));

        return new String(outputStream.toByteArray());
    }

    public Document toDomDocument() {
        return (Document)getXmlBean().getDomNode();
    }

    @ApiModelProperty(hidden=true)
    public JSONObject getJson() throws JSONException {
        return org.json.XML.toJSONObject(getXml());
    }

}
