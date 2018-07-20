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
package com.centurylink.mdw.tests.workflow;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.model.workflow.ActivityRuntimeContext;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.workflow.activity.DefaultActivityImpl;

/**
 * MDW general activity.
 */
@Tracked(LogLevel.TRACE)
public class PopulateVar extends DefaultActivityImpl {

    /**
     * Here's where the main processing for the activity is performed.
     *
     * @return the activity result (aka completion code)
     */
    @Override
    public Object execute(ActivityRuntimeContext runtimeContext) throws ActivityException {
        System.out.println("Setting String List variable:");
        ArrayList<String> stingList = new ArrayList<String>(3);
        stingList.add("string1");
        stingList.add("string2");
        stingList.add("string3");
        this.setParameterValueAsDocument("stringListVar","java.util.List<String>", stingList) ;
        System.out.println("Setting  Integer List variable:");
        ArrayList<Integer> intList = new ArrayList<Integer>(3);
        intList.add(1);
        intList.add(2);
        intList.add(3);
        this.setParameterValueAsDocument("intListVar","java.util.List<Integer>", intList) ;
        System.out.println("Setting Long List variable:");
        ArrayList<Long> longList = new ArrayList<Long>(3);
        longList.add(1L);
        longList.add(2L);
        longList.add(3L);
        this.setParameterValueAsDocument("longListVar","java.util.List<Long>", longList) ;
        System.out.println("Setting String Map variable:");
        Map<String, String> stringMap = new HashMap<String, String>(3);
        stringMap.put("name1", "'string1");
        stringMap.put("name2", "{string2");
        stringMap.put("name3", "string3}");
        this.setParameterValueAsDocument("stringMapVar","java.util.Map<String,String>", stringMap) ;
        return null;
    }
}