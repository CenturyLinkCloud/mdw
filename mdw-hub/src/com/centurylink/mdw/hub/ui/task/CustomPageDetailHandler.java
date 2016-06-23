/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.ui.task;

import java.io.IOException;

import com.centurylink.mdw.taskmgr.ui.tasks.FullTaskInstance;

/**
 * TODO: honor customPageVersion
 */
public class CustomPageDetailHandler extends com.centurylink.mdw.taskmgr.ui.tasks.detail.CustomPageDetailHandler {

    public CustomPageDetailHandler(FullTaskInstance taskInstance) {
        super(taskInstance);
    }

    @Override
    public String getNavOutcome(String customPage, String customPageVersion) {
        return CUSTOM_PAGE_OUTCOME;
    }

    /**
     * TODO: honor customPageVersion (uses smart version syntax)
     */
    @Override
    protected String getPageUrl(String customPage, String customPageVersion) throws IOException {
        return super.getPageUrl(customPage, customPageVersion);
    }
}
