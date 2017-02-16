/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.wizards;

public class ExportAssetPage extends ImportExportPage {
    public ExportAssetPage() {
        super("Export MDW Asset", "Export workflow asset to file.");
    }

    protected String getDefaultFileName() {
        return getAsset().getName();
    }

    @Override
    protected String getFileExtension() {
        return getAsset().getExtension();
    }
}
