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
package com.centurylink.mdw.plugin.server;

import java.io.File;

import org.eclipse.core.runtime.IPath;

public class FuseRuntime extends ServiceMixRuntime {
    @Override
    protected String validateRuntimeLoc() {
        IPath location = getRuntimeWorkingCopy().getLocation();
        File locFile = location.toFile();
        if (!locFile.exists() || !locFile.isDirectory())
            return getType() + " Home must be an existing directory";
        else if (!new File(locFile + "/bin/fuse.bat").exists()
                && !new File(locFile + "/bin/fuse").exists())
            return getType() + " Home must contain bin/fuse.bat or bin/fuse";
        else
            return null;
    }
}
