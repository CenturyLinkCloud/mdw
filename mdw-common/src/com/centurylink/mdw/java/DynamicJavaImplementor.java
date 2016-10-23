/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.java;

import com.centurylink.mdw.activity.ActivityException;

public interface DynamicJavaImplementor {

    public void execute() throws ActivityException;

    public JavaExecutor getExecutorInstance() throws MdwJavaException;

    public void setExecutorClassLoader(ClassLoader loader);
}
