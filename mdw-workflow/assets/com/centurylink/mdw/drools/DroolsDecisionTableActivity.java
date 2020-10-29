package com.centurylink.mdw.drools;

import com.centurylink.mdw.activity.types.RuleActivity;
import com.centurylink.mdw.annotations.Activity;
import org.kie.api.KieBase;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.config.PropertyException;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;

@Tracked(LogLevel.TRACE)
@Activity(value="Drools Decision Table", category=RuleActivity.class, icon="com.centurylink.mdw.drools/excel.gif",
        pagelet="com.centurylink.mdw.drools/decisionTable.pagelet")
public class DroolsDecisionTableActivity extends DroolsActivity {

    public static final String DECISION_TABLE_SHEET = "DecisionTableSheet";

    @Override
    protected KieBase getKnowledgeBase(String name, String version) throws ActivityException {
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