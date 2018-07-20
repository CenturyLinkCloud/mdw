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
package com.centurylink.mdw.services;

import java.io.File;
import java.io.FileWriter;
import java.io.Serializable;

import com.centurylink.mdw.app.Compatibility;
import com.centurylink.mdw.bpm.ApplicationCacheDocument;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.services.cache.CacheRegistration;
import com.centurylink.mdw.util.log.LoggerUtil;

public class ConfigurationHelper implements Serializable{

    private static final String APPLICATION_CACHE = "application-cache.xml";

    private ConfigurationHelper() {
    }

    public static boolean applyConfigChange(String fileName, String contents, boolean react)
    throws Exception {
     boolean validContents = true;
     if (APPLICATION_CACHE.equals(fileName)) {
        ApplicationCacheDocument.Factory.parse(contents, Compatibility.namespaceOptions());
     }
     if (!validContents) {
       return false;
     }

     String filepath = System.getProperty("mdw.config.location") + "/" + fileName;

     FileWriter wr = new FileWriter(new File(filepath));
     wr.write(contents);
     wr.flush();
     wr.close();
     if (!react)
       return true;
     else
       return reactToConfigChange(fileName, contents);
    }

    public static boolean reactToConfigChange(String pFileName, String pContents)
    throws Exception {

     if (APPLICATION_CACHE.equals(pFileName)) {
        CacheRegistration.getInstance().refreshCaches();
     }
     else {
//       PropertyManager.getInstance().refreshCache();
       LoggerUtil.getStandardLogger().refreshCache();  // in case log props have changed
     }

     return true;

    }
}
