/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.provider;

import com.centurylink.mdw.common.service.RegisteredService;

/**
 * MDW provider interface used by the runtime engine to obtain instances
 * provided dynamically.
 *
 * @param <T> the runtime type to be provided
 */
public interface Provider<T> extends RegisteredService {

    /**
     * Returns an instance of the parameterized type
     * @param type class name of the object to be provided
     * @return an instance of the designated object, implementing the specified type
     */
    public T getInstance(String type)
    throws ClassNotFoundException, IllegalAccessException, InstantiationException;

    /**
     * @return unique alias to identify this provider of the service
     * (displayed on successful registration)
     */
    public String getAlias() throws ProviderException;

    /**
     * Property value associated when the service is registered.
     */
    public String getProperty(String name);
    /**
     * Property value associated when the service is registered.
     */
    public void setProperty(String name, String value);

}


