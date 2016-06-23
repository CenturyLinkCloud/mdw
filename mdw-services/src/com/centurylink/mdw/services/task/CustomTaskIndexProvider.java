/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.task;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.constant.TaskAttributeConstant;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.model.value.task.TaskRuntimeContext;
import com.centurylink.mdw.observer.task.TaskIndexProvider;

/**
 * Standard index provider for custom manual tasks.
 * Collects indexes from runtimeContext by evaluating expressions.
 */
public class CustomTaskIndexProvider implements TaskIndexProvider {

    public Map<String,String> collect(TaskRuntimeContext runtimeContext) {

        Map<String,String> indexes = null;
        String indicesAttr = runtimeContext.getTaskAttribute(TaskAttributeConstant.INDICES);
        if (!StringHelper.isEmpty(indicesAttr)) {
            indexes = new HashMap<String,String>();
            List<String[]> rows = StringHelper.parseTable(indicesAttr, ',', ';', 2);
            for (String[] row : rows) {
                if (!StringHelper.isEmpty(row[0]) && !StringHelper.isEmpty(row[1])) {
                    String value = runtimeContext.evaluateToString(row[1]);
                    if (value != null)
                        indexes.put(row[0], value);
                }
            }
        }

        return indexes;
    }

}
