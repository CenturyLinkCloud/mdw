/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.activity.rules;

import org.drools.KnowledgeBase;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.common.exception.PropertyException;
import com.centurylink.mdw.common.utilities.logger.StandardLogger.LogLevel;
import com.centurylink.mdw.common.utilities.timer.Tracked;

@Tracked(LogLevel.TRACE)
public class DroolsDecisionTableActivity extends DroolsActivity {

    public static final String DECISION_TABLE_SHEET = "DecisionTableSheet";

    @Override
    protected KnowledgeBase getKnowledgeBase(String name, String version) throws ActivityException {
        String decisionTableSheetName = null;
        try {
            decisionTableSheetName = getAttributeValueSmart(DECISION_TABLE_SHEET);
        }
        catch (PropertyException ex) {
            throw new ActivityException(ex.getMessage(), ex);
        }

        return super.getKnowledgeBase(name, version, decisionTableSheetName);
    }
}