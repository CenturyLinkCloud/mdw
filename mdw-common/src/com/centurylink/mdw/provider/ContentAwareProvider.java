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
