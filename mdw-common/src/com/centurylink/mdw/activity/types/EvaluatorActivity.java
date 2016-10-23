/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.activity.types;

import com.centurylink.mdw.activity.ActivityException;

public interface EvaluatorActivity {
    public Object evaluate() throws ActivityException;
}
