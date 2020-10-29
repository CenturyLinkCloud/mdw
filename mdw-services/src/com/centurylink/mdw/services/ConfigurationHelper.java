package com.centurylink.mdw.services;

import com.centurylink.mdw.bpm.ApplicationCacheDocument;
import com.centurylink.mdw.services.cache.CacheRegistration;
import com.centurylink.mdw.util.log.LoggerUtil;
import org.apache.xmlbeans.XmlOptions;

import java.io.File;
import java.io.FileWriter;
import java.io.Serializable;

public class ConfigurationHelper implements Serializable {

    private static final String APPLICATION_CACHE = "application-cache.xml";

    private ConfigurationHelper() {
    }

    public static boolean applyConfigChange(String fileName, String contents, boolean react) throws Exception {
        if (APPLICATION_CACHE.equals(fileName)) {
            ApplicationCacheDocument.Factory.parse(contents, new XmlOptions());
        }

        String filepath = System.getProperty("mdw.config.location") + "/" + fileName;

        try (FileWriter wr = new FileWriter(new File(filepath))) {
            wr.write(contents);
            wr.flush();
        }
        if (!react)
            return true;
        else
            return reactToConfigChange(fileName);
    }

    public static boolean reactToConfigChange(String fileName) throws Exception {

        if (APPLICATION_CACHE.equals(fileName)) {
            CacheRegistration.getInstance().refreshCaches();
        }
        else {
            LoggerUtil.getStandardLogger().refreshWatcher();
        }

        return true;
    }
}
