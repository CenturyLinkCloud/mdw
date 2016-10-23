/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.provider;

import com.centurylink.mdw.activity.types.GeneralActivity;

/**
 * Provides instances of GeneralActivity implementor to the MDW runtime engine.
 */
public interface ActivityProvider extends Provider<GeneralActivity> {

    public GeneralActivity getInstance(String className)
    throws ClassNotFoundException, IllegalAccessException, InstantiationException;
}
