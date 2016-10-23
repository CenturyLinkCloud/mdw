/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services;

import java.io.File;
import java.io.FileWriter;
import java.io.Serializable;

import com.centurylink.mdw.app.Compatibility;
import com.centurylink.mdw.bpm.ApplicationCacheDocument;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
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

     String filepath = PropertyManager.getProperty(PropertyNames.MDW_CONFIG_DIRECOTRY) + "/" + fileName;

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
       PropertyManager.getInstance().refreshCache();
       LoggerUtil.getStandardLogger().refreshCache();  // in case log props have changed
     }

     return true;

    }
}
