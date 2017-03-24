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

import com.centurylink.mdw.model.workflow.Package;

public interface PackageAwareProvider<T> extends Provider<T>, VersionAwareProvider<T> {
    /**
     * Locates an activity instance based on the workflow package.
     * 
     * @param workflowPackage the design-time package of the process where the activity is used
     * @param type fully-qualified class name of the activity implementor
     * @return an instance
     */
    public T getInstance(Package workflowPackage, String type)
    throws ClassNotFoundException, IllegalAccessException, InstantiationException;
}
