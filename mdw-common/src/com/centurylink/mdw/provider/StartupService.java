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
import com.centurylink.mdw.startup.StartupClass;

/**
 * Implemented by workflow bundles that perform startup functionality (especially custom listeners).
 */
public interface StartupService extends StartupClass, RegisteredService {

    public boolean isEnabled();

    @Override
    public void onStartup() throws StartupException;
}
