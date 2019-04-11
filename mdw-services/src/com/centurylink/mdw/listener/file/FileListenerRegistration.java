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
package com.centurylink.mdw.listener.file;

import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import org.apache.xmlbeans.XmlException;

import com.centurylink.mdw.config.PropertyException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.config.PropertyUtil;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.startup.StartupException;
import com.centurylink.mdw.startup.StartupService;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class FileListenerRegistration implements StartupService {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private static Map<String, FileListener> registeredFileListeners = new Hashtable<String, FileListener>();

    /**
     * Startup the file listeners.
     */
    public void onStartup() throws StartupException {
        try {
            Map<String, Properties> fileListeners = getFileListeners();
            for (String listenerName : fileListeners.keySet()) {
                Properties listenerProps = fileListeners.get(listenerName);
                String listenerClassName = listenerProps.getProperty("ClassName");
                logger.info("Registering File Listener: " + listenerName + "  Class: " + listenerClassName);
                FileListener listener = getFileListenerInstance(listenerClassName);
                listener.setName(listenerName);
                registeredFileListeners.put(listenerName, listener);
                listener.listen(listenerProps);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
            logger.severeException(ex.getMessage(), ex);
            throw new StartupException(ex.getMessage());
        }
    }

    /**
     * Shutdown the file listeners.
     */
    public void onShutdown() {
        for (String listenerName : registeredFileListeners.keySet()) {
            logger.info("Deregistering File Listener: " + listenerName);
            FileListener listener = registeredFileListeners.get(listenerName);
            listener.stopListening();
        }
    }


    private Map<String, Properties> getFileListeners() throws XmlException, IOException, PropertyException {

        Map<String, Properties> fileListeners = new HashMap<String, Properties>();
        Properties fileListenerProperties = PropertyManager.getInstance().getProperties(PropertyNames.MDW_LISTENER_FILE);
        for (String pn : fileListenerProperties.stringPropertyNames()) {
            String[] pnParsed = pn.split("\\.");
            if (pnParsed.length==5) {
                String name = pnParsed[3];
                String attrname = pnParsed[4];
                Properties procspec = fileListeners.get(name);
                if (procspec==null) {
                    procspec = new Properties();
                    fileListeners.put(name, procspec);
                }
                String value = fileListenerProperties.getProperty(pn);
                procspec.put(attrname, value);
            }
        }
        return fileListeners;
    }

    private FileListener getFileListenerInstance(String className) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        Class<? extends FileListener> listenerClass = Class.forName(className).asSubclass(FileListener.class);
        return listenerClass.newInstance();
    }

    public boolean isEnabled() {
        try {
            Properties listenerProps = PropertyUtil.getInstance().getPropertyManager().getProperties(PropertyNames.MDW_LISTENER_FILE);
            return !listenerProps.isEmpty();
        }
        catch (PropertyException ex){
            LoggerUtil.getStandardLogger().severeException(ex.getMessage(), ex);
            return false;
        }
    }

}
