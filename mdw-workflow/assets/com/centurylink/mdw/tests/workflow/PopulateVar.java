/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */

package com.centurylink.mdw.tests.workflow;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.common.utilities.logger.StandardLogger.LogLevel;
import com.centurylink.mdw.common.utilities.timer.Tracked;
import com.centurylink.mdw.model.value.activity.ActivityRuntimeContext;
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