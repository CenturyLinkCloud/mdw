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


