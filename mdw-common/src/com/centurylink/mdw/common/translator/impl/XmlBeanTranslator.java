/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.translator.impl;

import java.lang.reflect.Method;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.exception.TranslationException;
import com.centurylink.mdw.common.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.common.translator.XmlDocumentTranslator;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyManager;

public class XmlBeanTranslator extends DocumentReferenceTranslator implements XmlDocumentTranslator {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    // Note that, due to static loading, the logger should be defined before xmlOptions
    // Note also using a static method instead of static block just for code niceness
    private static XmlOptions xmlOptions = getXmlOptions();

    /**
     * Moved this to a static block/method so it's done only once
     * @return XmlOptions
     */
    private static XmlOptions getXmlOptions() {
        if (System.getProperty("runtimeEnv") == null)
            return new XmlOptions().setSavePrettyPrint().setSavePrettyPrintIndent(2); // avoid errors when running in Designer

        String[] xmlOptionsProperties = new String[] { PropertyNames.MDW_TRANSLATOR_XMLBEANS_LOAD_OPTIONS,
                PropertyNames.MDW_TRANSLATOR_XMLBEANS_SAVE_OPTIONS };
        /**
         * First set it up with default compatibility options
         */
        xmlOptions = new XmlOptions();

        /**
         * Next get the options that the user needs from the properties file.
         */
        Class<?> xmlOptionsClass = xmlOptions.getClass();

        for (int i = 0; i < xmlOptionsProperties.length; i++) {
            String property = xmlOptionsProperties[i];
            String opt = PropertyManager.getProperty(property);
            /**
             * Only do reflection if we need to
             */
            if (opt != null && !"".equals(opt)) {
                try {
                    String[] propTable = opt.split(",");
                    for (int j = 0; j < propTable.length; j++) {
                        String prop = propTable[j];
                        String[] optTable = prop.split("=");
                        String option = optTable[0];
                        String value = null;

                        Class<?>[] classArray = new Class<?>[] { Object.class };
                        if (optTable.length > 1) {
                            // Check for int
                            value = optTable[1];
                            classArray = new Class<?>[] { Object.class, Object.class };
                            boolean gotInteger = true;
                            try {
                                Integer.parseInt(value);
                            }
                            catch (NumberFormatException e) {
                                // It's not an integer
                                gotInteger = false;
                            }

                            Method method = xmlOptionsClass.getMethod("put", classArray);
                            method.invoke(xmlOptions, new Object[] { option,
                                    gotInteger ? Integer.valueOf(value) : value });

                        }
                        else {

                            Method method = xmlOptionsClass.getMethod("put", classArray);
                            method.invoke(xmlOptions, new Object[] { option });

                        }
                    }
                }
                catch (Exception ex) {
                    // Just log it
                    logger.severeException("Unable to set XMLOption " + opt + " due to " + ex.getMessage(), ex);

                }

            }
        }
        return xmlOptions;

    }

    public Object realToObject(String string) throws TranslationException {
        return realToObject(string, ApplicationContext.isOsgi());
    }

    @Override
    protected Object realToObject(String str, boolean tryProviders) throws TranslationException {
        try {
            if (tryProviders)
                return providerDeserialize(str);

            XmlObject xmlBean = XmlObject.Factory.parse(str, xmlOptions);
            return xmlBean;
        }
        catch (XmlException e) {
            throw new TranslationException(e.getMessage(), e);
        }
    }

    public String realToString(Object object) throws TranslationException {
        XmlObject xmlBean = (XmlObject) object;
        XmlOptions tempOptions = new XmlOptions(xmlOptions);
        tempOptions.setSavePrettyPrint();
        return xmlBean.xmlText(tempOptions);
    }

    public Document toDomDocument(Object obj) throws TranslationException {
        return (Document) ((XmlObject) obj).getDomNode();
    }

    public Object fromDomNode(Node domNode) throws TranslationException {
        try {
            return XmlObject.Factory.parse(domNode);
        }
        catch (XmlException ex) {
            throw new TranslationException(ex.getMessage(), ex);
        }
    }

}
