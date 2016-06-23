/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.report.birt;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.faces.component.UIComponent;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.utilities.FileHelper;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.user.AuthenticatedUser;
import com.centurylink.mdw.hub.jsf.FacesUtil;
import com.centurylink.mdw.web.jsf.components.Filter;
import com.centurylink.mdw.web.jsf.components.FilterActionEvent;
import com.centurylink.mdw.web.jsf.components.FilterActionListener;

public class BirtFilterListener implements FilterActionListener {
    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public void processFilterAction(FilterActionEvent event)
    {
        UIComponent component = event.getComponent();
        if (!(component instanceof Filter))
            throw new IllegalStateException("Action source is not a Filter: " + component);

        if (event.getAction().equals(FilterActionEvent.ACTION_RESET)) {
            BirtReport birtReport = (BirtReport) FacesUtil.getValue("mdwReport");
            String reportName = birtReport.getName();
            String filterId = FileHelper.stripDisallowedFilenameChars(reportName) + "_Filter";

            AuthenticatedUser user = FacesUtil.getCurrentUser();

            Map<String,String> userPrefs = user.getAttributes();

            if (userPrefs != null) {
                Map<String, String> newPrefs = new HashMap<String, String>();
                for (String key : userPrefs.keySet()) {
                    if (!key.startsWith(filterId + ":")) {
                        newPrefs.put(key, userPrefs.get(key));
                    }
                }
                user.setAttributes(newPrefs);
                try {
                    // TODO try to accomplish using AJAX without redirect
                    String url = ApplicationContext.getReportsUrl().trim()
                            .replaceAll("/reportsList.jsf", "")
                            + "/reports/birt.jsf?mdwReport="
                            + birtReport.getName().replaceAll(" ", "%20");
                    FacesUtil.navigate(new URL(url));
                }
                catch (IOException ex) {
                    logger.severeException(ex.getMessage(), ex);
                }
            }
        }
    }
}
