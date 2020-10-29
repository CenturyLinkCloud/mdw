package com.centurylink.mdw.services.task;

import com.centurylink.mdw.constant.TaskAttributeConstant;
import com.centurylink.mdw.model.task.TaskRuntimeContext;
import com.centurylink.mdw.observer.task.TaskIndexProvider;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Standard index provider for custom manual tasks.
 * Collects indexes from runtimeContext by evaluating expressions.
 */
public class CustomTaskIndexProvider implements TaskIndexProvider {

    public Map<String,String> collect(TaskRuntimeContext runtimeContext) {

        Map<String,String> indexes = null;
        if (runtimeContext.getTaskAttributes().containsKey(TaskAttributeConstant.INDICES)) {
            indexes = new HashMap<>();
            List<String[]> rows = runtimeContext.getTaskAttributes().getTable(TaskAttributeConstant.INDICES, ',', ';', 2);
            for (String[] row : rows) {
                if (!StringUtils.isBlank(row[0]) && !StringUtils.isBlank(row[1])) {
                    String value = runtimeContext.evaluateToString(row[1]);
                    if (value != null)
                        indexes.put(row[0], value);
                }
            }

        }
        return indexes;
    }

}
