/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.provider;

import com.centurylink.mdw.model.value.process.PackageVO;

public interface PackageAwareProvider<T> extends Provider<T>, VersionAwareProvider<T> {
    /**
     * Locates an activity instance based on the workflow package.
     * 
     * @param workflowPackage the design-time package of the process where the activity is used
     * @param type fully-qualified class name of the activity implementor
     * @return an instance
     */
    public T getInstance(PackageVO workflowPackage, String type)
    throws ClassNotFoundException, IllegalAccessException, InstantiationException;
}
