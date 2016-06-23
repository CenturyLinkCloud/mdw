/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services;

import java.io.File;
import java.io.FileWriter;
import java.io.Serializable;

import com.centurylink.mdw.bpm.ApplicationCacheDocument;
import com.centurylink.mdw.bpm.ApplicationPropertiesDocument;
import com.centurylink.mdw.common.Compatibility;
import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.services.cache.CacheRegistration;

public class ConfigurationHelper implements Serializable{

    private static final String APPLICATION_PROPERTIES = "ApplicationProperties.xml";
    private static final String APPLICATION_CACHE = "ApplicationCache.xml";

    private ConfigurationHelper() {
    }

    public static boolean applyConfigChange(String pFileName, String pContents, boolean pReact)
    throws Exception {
     boolean validContents = true;
     if (APPLICATION_PROPERTIES.equals(pFileName)) {
        ApplicationPropertiesDocument.Factory.parse(pContents, Compatibility.namespaceOptions());

     } else if (APPLICATION_CACHE.equals(pFileName)) {
        ApplicationCacheDocument.Factory.parse(pContents, Compatibility.namespaceOptions());

     }
     if (!validContents) {
       return false;
     }

     String filepath = PropertyManager.getProperty(PropertyNames.MDW_CONFIG_DIRECOTRY) + "/" + pFileName;

     FileWriter wr = new FileWriter(new File(filepath));
     wr.write(pContents);
     wr.flush();
     wr.close();
     if (!pReact)
       return true;
     else
       return reactToConfigChange(pFileName, pContents);
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
