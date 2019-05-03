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

import com.centurylink.mdw.constant.TaskAttributeConstant;
import com.centurylink.mdw.model.attribute.Attribute;
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
        String indicesAttr = runtimeContext.getTaskAttribute(TaskAttributeConstant.INDICES);
        if (!StringUtils.isBlank(indicesAttr)) {
            indexes = new HashMap<>();
            List<String[]> rows = Attribute.parseTable(indicesAttr, ',', ';', 2);
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
