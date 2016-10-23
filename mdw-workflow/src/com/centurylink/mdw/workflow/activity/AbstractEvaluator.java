/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.activity;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.activity.types.EvaluatorActivity;

public abstract class AbstractEvaluator extends DefaultActivityImpl implements EvaluatorActivity {

    @Override
    public void execute() throws ActivityException {
        Object obj = evaluate();
        if (obj != null)
            setReturnCode(obj.toString());
    }
}
