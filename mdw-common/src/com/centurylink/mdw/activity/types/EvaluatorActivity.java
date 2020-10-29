package com.centurylink.mdw.activity.types;

import com.centurylink.mdw.activity.ActivityException;

public interface EvaluatorActivity extends ActivityCategory {
    public Object evaluate() throws ActivityException;
}
