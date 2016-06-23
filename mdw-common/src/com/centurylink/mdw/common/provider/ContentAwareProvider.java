/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.provider;

import java.util.Map;


public interface ContentAwareProvider<T> extends Provider<T>, VersionAwareProvider<T> {
    /**
     * Locates an event handler instance based on request info.
     * 
     * @param metaInfo typically request path, parameters and header values
     * @param content usually a string
     * @param type fully-qualified class name of the event handler impl
     * @return an instance
     */
    public T getInstance(Map<String,String> metaInfo, Object content, String type)
    throws ClassNotFoundException, IllegalAccessException, InstantiationException;
}
