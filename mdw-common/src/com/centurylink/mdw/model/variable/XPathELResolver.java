/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.variable;

import java.beans.FeatureDescriptor;
import java.util.Iterator;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ELResolver;
import javax.el.PropertyNotFoundException;

import org.apache.xmlbeans.XmlObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.centurylink.mdw.util.XPathHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class XPathELResolver extends ELResolver {
    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    @Override
    public Object getValue(ELContext elContext, Object base, Object property) throws NullPointerException,
    PropertyNotFoundException, ELException {
        Object value = null;
        if (((base instanceof XmlObject) || (base instanceof Document)) && property instanceof String) {
            boolean isXpath = false;
            for (int i = 0; i < ((String) property).length(); i++) {
                if (!Character.isJavaIdentifierPart(((String) property).charAt(i))){
                    isXpath = true;
                    break;
                }
            }
            if (isXpath) {
                elContext.setPropertyResolved(true);
                XPathHelper xHelper = new XPathHelper();
                try {
                    if (base instanceof XmlObject){
                        XmlObject xObj = (XmlObject) base;
                        Node domNode = xObj.getDomNode();
                        value = xHelper.peekDom((Document)domNode, (String) property);
                    }
                    else
                        value = xHelper.peekDom((Document)base, (String) property);

                    logger.mdwDebug("value = [" + value + "],property[" + (String) property + "]");
                    return value;
                }
                catch (Exception ex) {
                    logger.severeException(ex.getMessage(), ex);
                    throw new ELException(ex.getMessage(), ex);
                }
            }
        }
        return value;
    }

    @Override
    public Class<?> getCommonPropertyType(ELContext arg0, Object arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext arg0, Object arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Class<?> getType(ELContext arg0, Object arg1, Object arg2) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isReadOnly(ELContext arg0, Object arg1, Object arg2) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setValue(ELContext arg0, Object arg1, Object arg2, Object arg3) {
        // TODO Auto-generated method stub

    }
}