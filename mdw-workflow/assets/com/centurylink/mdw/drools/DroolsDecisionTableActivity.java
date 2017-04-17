/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.drools;

import org.drools.KnowledgeBase;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.config.PropertyException;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;

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