/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;

import com.centurylink.mdw.bpm.ProcessDefinitionDocument;
import com.centurylink.mdw.common.Compatibility;
import com.centurylink.mdw.common.SchemaTypeTranslator;
import com.centurylink.mdw.service.ActionRequestDocument;


public class DesignerCompatibility implements SchemaTypeTranslator {

    private static DesignerCompatibility instance;
    public static synchronized DesignerCompatibility getInstance() {
        if (instance == null)
            instance = new DesignerCompatibility();
        return instance;
    }

    private DesignerCompatibility() {}

    /**
     * Generates an action request string using the old namespace.
     * @param actionRequestDoc - New-namespace request doc
     * @return a request string compatible with older MDW servers
     */
    public String getOldActionRequest(ActionRequestDocument actionRequestDoc) throws XmlException {
        // reparse with reverse namespaces
        XmlOptions reverseOpts = Compatibility.getReverseNamespaceOptions();
        com.qwest.mdw.services.ActionRequestDocument oldDoc =
                com.qwest.mdw.services.ActionRequestDocument.Factory.parse(actionRequestDoc.xmlText(), reverseOpts);
        XmlOptions printOpts = new XmlOptions().setSavePrettyPrint().setSavePrettyPrintIndent(2);
        return oldDoc.xmlText(printOpts);
    }

    public String getOldProcessDefinition(ProcessDefinitionDocument procDefDoc) throws XmlException {
        // reparse with reverse namespaces
        XmlOptions reverseOpts = Compatibility.getReverseNamespaceOptions();
        com.qwest.mdw.xmlSchema.ProcessDefinitionDocument oldDoc =
                com.qwest.mdw.xmlSchema.ProcessDefinitionDocument.Factory.parse(procDefDoc.xmlText(), reverseOpts);
        XmlOptions printOpts = new XmlOptions().setSavePrettyPrint().setSavePrettyPrintIndent(2);
        return oldDoc.xmlText(printOpts);
    }

    public String getOldProcessDefinition(String pkgDefXML) throws XmlException {
        // reparse with reverse namespaces
        XmlOptions reverseOpts = Compatibility.getReverseNamespaceOptions();
        com.qwest.mdw.xmlSchema.ProcessDefinitionDocument oldDoc =
                com.qwest.mdw.xmlSchema.ProcessDefinitionDocument.Factory.parse(pkgDefXML, reverseOpts);
        XmlOptions printOpts = new XmlOptions().setSavePrettyPrint().setSavePrettyPrintIndent(2);
        return oldDoc.xmlText(printOpts);
    }

}
