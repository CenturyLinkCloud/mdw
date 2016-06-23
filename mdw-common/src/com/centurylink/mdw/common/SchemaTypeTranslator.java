/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common;

import org.apache.xmlbeans.XmlException;

import com.centurylink.mdw.bpm.ProcessDefinitionDocument;
import com.centurylink.mdw.service.ActionRequestDocument;

public interface SchemaTypeTranslator {
    public String getOldActionRequest(ActionRequestDocument actionRequestDoc) throws XmlException;
    public String getOldProcessDefinition(ProcessDefinitionDocument procDefDoc) throws XmlException;
    public String getOldProcessDefinition(String pkgDefXML) throws XmlException;
}
