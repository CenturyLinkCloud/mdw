/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.wizards;

public class ExportTaskTemplatesPage extends ImportExportPage {
    public ExportTaskTemplatesPage() {
        super("Export MDW Tasks", "Export XML file for MDW task templates.");
    }

    protected String getDefaultFileName() {
        return getPackage().getName() + "-" + getPackage().getVersionString() + "_tasks.xml";
    }
}
