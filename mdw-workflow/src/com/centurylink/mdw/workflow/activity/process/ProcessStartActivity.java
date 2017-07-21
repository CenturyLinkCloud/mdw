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
package com.centurylink.mdw.workflow.activity.process;

import java.util.Map;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.activity.types.StartActivity;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.workflow.activity.DefaultActivityImpl;

/**
 * Base class for all the ProcessStart Controlled Activity
 * This class will be extended by the custom ProcessStart activity
 */
@Tracked(LogLevel.TRACE)
public class ProcessStartActivity extends DefaultActivityImpl implements StartActivity {

    private static final String PARAMETERS = "Parameters";

    /**
     * Default constructor with params
     */
    public ProcessStartActivity(){
        super();
    }

    @Override
    public void execute() throws ActivityException {
        String parameters_spec = this.getAttributeValue(PARAMETERS);
        try {
            if (parameters_spec != null) {
                Map<String, String> parameters = StringHelper.parseMap(parameters_spec);
                for (String key : parameters.keySet()) {
                    String one = parameters.get(key);
                    if (valueIsJavaExpression(one)) {
                        Object request = getVariableValue("request");
                        if (request != null) {
                            Object value = getRuntimeContext().evaluate(one);
                            // do not override input values set explicitly with
                            // null ones from xpath (eg: HandleOrder demo)
                            if (value != null)
                                setVariableValue(key, value);
                        }
                    }
                }
            }
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
        }
    }
}
