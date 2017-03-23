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

/**
 * Marker interface designating that an MDW provider needs to be passed
 * deployment-specific information (typically workflow package or event content).
 * This allows simultaneous providers for a given type from multiple deployed bundles
 * to be consulted, and passes the information they need to determine whether
 * they really want to provide the instance.
 * 
 * Typical usage: Bundle myworkflow 1.0.0 is deployed in production.  Then for 
 * release 1.1.0 new functionality is added to one of it's custom implementors.
 * With the VersionAware mechanism, version 1.1.0 does not need to worry about
 * backward compatiblity with in-flight workflow processes because it will only
 * provide activity instances when the runtime workflow package meets the expected
 * conditions.
 * 
 * VersionAware providers are always preferred over non-VersionAware.  This allows
 * the initial deployment of a workflow bundle to ignore version specifics.  Then
 * subsequent deployments can expose providers that are VersionAware, and let the non-
 * VersionAware providers respond to processes that fall through their conditional logic. 
 */
public interface VersionAwareProvider<T> extends Provider<T> {

}
