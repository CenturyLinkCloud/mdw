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
package com.centurylink.mdw.services.task;

import com.centurylink.mdw.activity.types.TaskActivity;
import com.centurylink.mdw.constant.TaskAttributeConstant;
import com.centurylink.mdw.model.task.TaskRuntimeContext;
import com.centurylink.mdw.model.workflow.ProcessRuntimeContext;
import com.centurylink.mdw.observer.task.TaskIndexProvider;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Collects index values based on a simple toString() evaluation of the configured variables.
 * For more complex situations and document-type variables, an @RegisteredService can be
 * created for the TaskIndexProvider interface.
 */
public class AutoFormTaskIndexProvider implements TaskIndexProvider {

    public Map<String,String> collect(TaskRuntimeContext runtimeContext) {

        Map<String,String> indexes = null;
        if (runtimeContext.getTaskAttributes().containsKey(TaskAttributeConstant.VARIABLES)) {
            List<String[]> parsed = runtimeContext.getTaskAttributes().getTable(TaskAttributeConstant.VARIABLES, ',', ';', 5);
            for (String[] one : parsed) {
                String varName = one[0];
                String displayOption = one[2];
                String indexKey = one[4];
                if (!displayOption.equals(TaskActivity.VARIABLE_DISPLAY_NOTDISPLAYED) && !StringUtils.isBlank(indexKey)) {
                    if (indexes == null)
                        indexes = new HashMap<String,String>();
                    Object value = null;
                    if (ProcessRuntimeContext.isExpression(varName))
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
