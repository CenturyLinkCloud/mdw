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
package com.centurylink.mdw.tests.code;

import com.centurylink.mdw.dataaccess.task.MdwTaskRefData;
import com.centurylink.mdw.model.task.TaskCategory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Custom TaskRefData implementation.
 */
public class MyTaskRefData extends MdwTaskRefData {
    private List<TaskCategory> myTaskCategories;

    public MyTaskRefData() {
        myTaskCategories = new ArrayList<>();
        myTaskCategories.add(new TaskCategory(101L, "PLN", "Planning"));
        myTaskCategories.add(new TaskCategory(102L, "CON", "Construction"));
    }

    private Map<Integer,TaskCategory> taskCategories;
    @Override
    public Map<Integer,TaskCategory> getCategories() {
        if (taskCategories == null) {
            taskCategories = super.getCategories();
            for (TaskCategory myTaskCategory : myTaskCategories)
                taskCategories.put(myTaskCategory.getId().intValue(), myTaskCategory);
        }
        return taskCategories;
    }

    private Map<Integer,String> taskCategoryCodes;
    @Override
    public Map<Integer,String> getCategoryCodes() {
        if (taskCategoryCodes == null) {
            taskCategoryCodes = super.getCategoryCodes();
            for (TaskCategory myTaskCategory : myTaskCategories)
                taskCategoryCodes.put(myTaskCategory.getId().intValue(), myTaskCategory.getCode());
        }
        return taskCategoryCodes;
    }
}
