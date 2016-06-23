/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.activity.types;

import com.centurylink.mdw.activity.ActivityException;

public interface EvaluatorActivity {
    public Object evaluate() throws ActivityException;
}
