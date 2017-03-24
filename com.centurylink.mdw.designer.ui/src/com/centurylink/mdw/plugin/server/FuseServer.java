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

public class FuseServer extends ServiceMixServer {
    public static final String ID_PREFIX = "com.centurylink.server.jbossfuse";

    public String validateServerLoc() {
        String msg = null;
        String location = getLocation();
        if (location == null || location.isEmpty())
            msg = "";
        else {
            File locationFile = new File(location);
            if (!locationFile.exists() || !locationFile.isDirectory())
                msg = "Location must be an existing directory";
            else if (!new File(locationFile + "/bin/karaf.bat").exists()
                    && !new File(locationFile + "/bin/karaf").exists())
                msg = "Location must contain bin/karaf.bat or bin/karaf";
        }
        return msg;
    }

}
