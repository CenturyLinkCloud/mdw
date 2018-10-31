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
package com.centurylink.mdw.model.event;

import java.util.HashMap;
import java.util.Map;

public class EventType  {

    public static final Integer START = new Integer(1);
    public static final Integer FINISH = new Integer(2);
    public static final Integer DELAY = new Integer(3);
    public static final Integer ERROR = new Integer(4);
    public static final Integer ABORT = new Integer(5);
    public static final Integer RESUME = new Integer(6);
    public static final Integer HOLD = new Integer(7);
    public static final Integer CORRECT = new Integer(8);

    public static final String EVENTNAME_START = "START";
    public static final String EVENTNAME_FINISH = "FINISH";
    public static final String EVENTNAME_DELAY = "DELAY";
    public static final String EVENTNAME_ERROR = "ERROR";
    public static final String EVENTNAME_ABORT ="ABORT";
    public static final String EVENTNAME_RESUME = "RESUME";
    public static final String EVENTNAME_HOLD = "HOLD";
    public static final String EVENTNAME_CORRECT = "CORRECT";


    public static final Integer[] allEventTypes =
    { START, FINISH, DELAY, ERROR, ABORT, RESUME, HOLD, CORRECT};

    public static final String[] allEventTypeNames =
    { EVENTNAME_START, EVENTNAME_FINISH, EVENTNAME_DELAY, EVENTNAME_ERROR,
        EVENTNAME_ABORT, EVENTNAME_RESUME, EVENTNAME_HOLD, EVENTNAME_CORRECT};

    private static Map<Integer,String> eventTypes = new HashMap<Integer,String>();

    private static Map<String,Integer> eventTypeNames = new HashMap<String,Integer>();

    public static Map<Integer,String> getEventTypes()
    {
        return eventTypes;
    }

    public static Integer getEventTypeFromName(String eventTypeName) {
        return eventTypeNames.get(eventTypeName);
    }

    public static String getEventTypeName(Integer eventType) {
        return eventTypes.get(eventType);
    }

    static
    {
        for (int i = 0; i < allEventTypes.length; i++) {
            eventTypes.put(allEventTypes[i], allEventTypeNames[i]);
            eventTypeNames.put(allEventTypeNames[i], allEventTypes[i]);
        }
    }

    /**
     * Default package-level handler process for this type of event.
     */
    public static String getHandlerName(Integer event) {

        switch (event) {
          case 4:
              return "Error";
          case 3:
              return "Delay";
          default:
              return null;
        }
    }

}
