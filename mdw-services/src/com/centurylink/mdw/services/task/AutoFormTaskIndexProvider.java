/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.task;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.activity.types.TaskActivity;
import com.centurylink.mdw.common.constant.TaskAttributeConstant;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.model.value.task.TaskRuntimeContext;
import com.centurylink.mdw.observer.task.TaskIndexProvider;

/**
 * Collects index values based on a simple toString() evaluation of the configured variables.
 * For more complex situations and document-type variables, an @RegisteredService can be
 * created for the TaskIndexProvider interface.
 */
public class AutoFormTaskIndexProvider implements TaskIndexProvider {

    public Map<String,String> collect(TaskRuntimeContext runtimeContext) {

        Map<String,String> indexes = null;
        String varAttr = runtimeContext.getTaskAttribute(TaskAttributeConstant.VARIABLES);
        if (!StringHelper.isEmpty(varAttr)) {
            List<String[]> parsed = StringHelper.parseTable(varAttr, ',', ';', 5);
            for (String[] one : parsed) {
                String varName = one[0];
                String displayOption = one[2];
                String indexKey = one[4];
                if (!displayOption.equals(TaskActivity.VARIABLE_DISPLAY_NOTDISPLAYED) && !StringHelper.isEmpty(indexKey)) {
                    if (indexes == null)
                        indexes = new HashMap<String,String>();
                    Object value = null;
                    if (runtimeContext.isExpression(varName))
                        value = runtimeContext.evaluateToString(varName);
                    else
                        value = runtimeContext.getVariables().get(varName);
                    if (value != null)
                        indexes.put(indexKey, value.toString());
                }
            }
        }
        return indexes;
    }

}
