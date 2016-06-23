/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model;

import org.apache.xmlbeans.XmlException;

import com.centurylink.mdw.common.service.types.ActionRequestMessage;
import com.centurylink.mdw.service.ActionRequestDocument;

/**
 * @deprecated use {@link com.centurylink.mdw.common.service.types.ActionRequestMessage}.
 */
@Deprecated
public class ActionRequestXmlBeanWrapper extends ActionRequestMessage {
    public ActionRequestXmlBeanWrapper() {
        super();
    }

    public ActionRequestXmlBeanWrapper(ActionRequestDocument actionRequestDoc) {
        super(actionRequestDoc);
    }

    public ActionRequestXmlBeanWrapper(String actionRequest) throws XmlException {
        super(actionRequest);
    }
}
