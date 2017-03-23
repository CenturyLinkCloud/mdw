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
package com.centurylink.mdw.plugin.designer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.common.Compatibility;
import com.centurylink.mdw.common.constant.TaskAttributeConstant;
import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.model.value.activity.ActivityVO;
import com.centurylink.mdw.model.value.attribute.AssetVersionSpec;
import com.centurylink.mdw.model.value.attribute.AttributeVO;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.task.TaskVO;

public class ActivityUpgrader {
    public static String INVOKE_SUBPROCESS = "com.centurylink.mdw.workflow.activity.process.InvokeSubProcessActivity";
    public static String INVOKE_HETERO = "com.centurylink.mdw.workflow.activity.process.InvokeHeterogeneousProcessActivity";
    public static String AUTOFORM_TASK = "com.centurylink.mdw.workflow.activity.task.AutoFormManualTaskActivity";
    public static String CUSTOM_TASK = "com.centurylink.mdw.workflow.activity.task.CustomManualTaskActivity";
    public static String VARIABLE_EVAL = "com.qwest.mdw.workflow.activity.impl.evaluator.VariableValueEvaluator";
    public static String SCRIPT_EVAL = "com.centurylink.mdw.workflow.activity.script.ScriptEvaluator";
    public static String OLD_TASK_NOTIFIER = "com.qwest.mdw.workflow.task.observer.TaskEmailNotifier";
    public static String NEW_TASK_NOTIFIER = "com.centurylink.mdw.workflow.task.notifier.TaskEmailNotifier";

    private ActivityVO activity;

    public ActivityUpgrader(ActivityVO activity) {
        this.activity = activity;
    }

    /**
     * @return true if changed
     */
    boolean doUpgrade() throws IOException {
        boolean updated = false;
        String activityImplClassName = activity.getImplementorClassName();
        String updatedClassName = Compatibility.getActivityImplementor(activityImplClassName);
        if (!updatedClassName.equals(activityImplClassName)) {
            activity.setImplementorClassName(updatedClassName);
            updated = true;
        }
        if (INVOKE_SUBPROCESS.equals(activity.getImplementorClassName())) {
            // smart subprocess versioning
            String versionAttr = activity.getAttribute(WorkAttributeConstant.PROCESS_VERSION);
            if (versionAttr != null) {
                String friendly = RuleSetVO.formatVersion(Integer.parseInt(versionAttr));
                activity.setAttribute(WorkAttributeConstant.PROCESS_VERSION,
                        AssetVersionSpec.getDefaultSmartVersionSpec(friendly));
                updated = true;
            }
        }
        else if (INVOKE_HETERO.equals(activity.getImplementorClassName())) {
            // smart subprocess versioning
            String mapAttr = activity.getAttribute(WorkAttributeConstant.PROCESS_MAP);
            if (mapAttr != null && !mapAttr.isEmpty()) {
                List<String[]> rows = StringHelper.parseTable(mapAttr, ',', ';', 3);
                for (String[] row : rows)
                    row[2] = AssetVersionSpec.getDefaultSmartVersionSpec(row[2]);
                activity.setAttribute(WorkAttributeConstant.PROCESS_MAP,
                        StringHelper.serializeTable(rows));
                updated = true;
            }
        }
        else if (AUTOFORM_TASK.equals(activity.getImplementorClassName())
                || CUSTOM_TASK.equals(activity.getImplementorClassName())) {
            // update notices attribute value
            String noticesAttr = activity.getAttribute(TaskAttributeConstant.NOTICES);
            if (noticesAttr == null || noticesAttr.isEmpty()
                    || noticesAttr.equals("$DefaultNotices")
                    || noticesAttr.equals(TaskVO.getDefaultCompatibilityNotices())) {
                activity.setAttribute(TaskAttributeConstant.NOTICES, TaskVO.getDefaultNotices());
            }
            else {
                int colCount = StringHelper.delimiterColumnCount(
                        noticesAttr.substring(0, noticesAttr.indexOf(";")), ",", "\\,") + 1;
                List<String[]> rows = StringHelper.parseTable(noticesAttr, ',', ';', colCount);
                List<String[]> newRows = new ArrayList<String[]>();
                for (String[] row : rows) {
                    String[] newRow = new String[colCount + 1];
                    newRow[0] = row[0];
                    newRow[1] = row[1];
                    newRow[2] = "0"; // we don't know what version to use, so
                                     // use latest
                    if (colCount == 4) // actually 3
                    {
                        String notifier = row[2];
                        if (OLD_TASK_NOTIFIER.equals(notifier))
                            newRow[3] = NEW_TASK_NOTIFIER;
                        else
                            newRow[3] = row[2];
                    }
                    newRows.add(newRow);
                }
                activity.setAttribute(TaskAttributeConstant.NOTICES,
                        StringHelper.serializeTable(newRows));
            }
            updated = true;
        }
        else if (VARIABLE_EVAL.equals(activity.getImplementorClassName())) {
            // switch to ScriptEvaluator
            activity.setImplementorClassName(SCRIPT_EVAL);
            AttributeVO varNameAttr = null;
            for (AttributeVO attr : activity.getAttributes()) {
                if (WorkAttributeConstant.VARIABLE_NAME.equals(attr.getAttributeName())) {
                    varNameAttr = attr;
                    break;
                }
            }
            if (varNameAttr != null) {
                activity.setAttribute("SCRIPT", "Groovy");
                activity.setAttribute("Expression", varNameAttr.getAttributeValue());
                activity.getAttributes().remove(varNameAttr);
                updated = true;
            }
        }

        return updated;
    }
}
