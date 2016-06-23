/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

import org.apache.xmlbeans.XmlException;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.config.service.ConfigurationService;
import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.exception.PropertyException;
import com.centurylink.mdw.common.exception.StartupException;
import com.centurylink.mdw.common.utilities.MiniEncrypter;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.common.utilities.property.impl.PropertyManagerDatabase;
import com.centurylink.mdw.container.NamingProvider;

public class OsgiPropertyManager extends PropertyManager implements ConfigurationService {

    public static final String MDW_OSGI_CONFIG_PID = "com.centurylink.mdw";
    public static final String MDW_OSGI_PROPERTIES_FILE = MDW_OSGI_CONFIG_PID + ".cfg";
    public static final String APP_OSGI_CONFIG_PIDS_PROP = "mdw.application.config.pids";
    public static final String PROP_SOURCE_OSGI = "OSGi";
    public static final char GROUP_SEPARATOR = '-';

    Properties _properties = new Properties();

    public OsgiPropertyManager() throws StartupException {
        try {
            refreshCache();
        }
        catch (PropertyException ex) {
            ex.printStackTrace();
            throw new StartupException(StartupException.FAIL_TO_LOAD_PROPERTIES, ex.getMessage(), ex);
        }
    }

    public void clearCache() {
        getSources().clear();
        _properties.clear();
    }

    public void refreshCache() throws PropertyException {
        getSources().clear();
        _properties.clear();
        File mdwPropertyFile = null;
        try {
            String configLoc = getPropertyFileLocation();
            if (configLoc != null)
                mdwPropertyFile = new File(configLoc + MDW_OSGI_PROPERTIES_FILE);
            if (mdwPropertyFile != null && mdwPropertyFile.exists()) {
                loadFromFile(mdwPropertyFile);
                // application-specific properties
                String configPids = _properties.getProperty(APP_OSGI_CONFIG_PIDS_PROP);
                if (configPids != null) {
                    for (String pid : configPids.split(",")) {
                        loadFromFile(new File(configLoc + pid + ".cfg"));
                    }
                }
            }
            else {
                _properties = new Properties();
                BundleContext bundleContext = ApplicationContext.getOsgiBundleContext();
                loadFromOsgi(bundleContext, MDW_OSGI_CONFIG_PID);
                // application-specific properties
                String configPids = _properties.getProperty(APP_OSGI_CONFIG_PIDS_PROP);
                if (configPids != null) {
                    for (String pid : configPids.split(",")) {
                      loadFromOsgi(bundleContext, pid);
                    }
                }
            }

            if (isDbConfigEnabled())
              loadFromDatabase();
        }
        catch (Exception ex) {
            throw new PropertyException(-1, ex.getMessage(), ex);
        }
    }

    @Override
    public boolean isDbConfigEnabled() {
        return "true".equalsIgnoreCase(System.getProperty(DB_CONFIG_ENABLED));
    }

    private void loadFromFile(File propFile) throws IOException {
        try {
            loadFromStream(_properties, new FileInputStream(propFile), propFile.getName());
        }
        catch (XmlException ex) {
            throw new IOException(ex.getMessage(), ex);
        }
    }

    @SuppressWarnings("rawtypes")
    private void loadFromOsgi(BundleContext bundleContext, String pid) throws IOException {
        ServiceReference caRef = bundleContext.getServiceReference(ConfigurationAdmin.class.getName());
        ConfigurationAdmin configAdmin = (ConfigurationAdmin)  bundleContext.getService(caRef);
        Configuration config = configAdmin.getConfiguration(pid);
        Dictionary props = config.getProperties();
        Enumeration en = props.keys();
        while (en.hasMoreElements()) {
            String key = en.nextElement().toString();
            String value = (String)props.get(key);
            _properties.setProperty(key, value);
            getSources().put(key, PROP_SOURCE_OSGI);
        }
    }

    private void loadFromDatabase() throws StartupException {
        // bootstrap with straight jdbc connection (no DataSource)
        String jdbcUrl = getStringProperty(PropertyNames.MDW_DB_URL);
        String dbUser = getStringProperty(PropertyNames.MDW_DB_USERNAME);
        String dbPassword = getStringProperty(PropertyNames.MDW_DB_PASSWORD);
        int atIdx = jdbcUrl.indexOf('@');
        jdbcUrl = jdbcUrl.substring(0, atIdx) + dbUser + "/" + dbPassword + jdbcUrl.substring(atIdx);
        PropertyManagerDatabase pmdb = new PropertyManagerDatabase(NamingProvider.OSGI, jdbcUrl, null);
        Properties dbProps = pmdb.getAllProperties();
        for (Object key : dbProps.keySet()) {
            String newKey = key.toString().replace('/', '-');
            _properties.put(newKey, dbProps.get(key));
            getSources().put(newKey.toString(), DATABASE);
        }
    }

    public String getPropertySource(String name) {
        String source = getSources().get(name);
        if (source == null)
            source = getSources().get(name.replace('/', '-'));
        return source;
    }

    /**
     * Group separator is ':' character for OSGi.
     */
    public Properties getProperties(String group) {
        Properties props = new Properties();
        for (Object key : _properties.keySet()) {
            String name = key.toString();
            if (name.startsWith(group + "."))
                props.put(name, _properties.get(name));
            else if (name.startsWith(group + GROUP_SEPARATOR))
                props.put(name.substring(group.length() + 1), _properties.get(name));
        }
        return props;
    }

    public String getStringProperty(String group, String name) {
        if (group != null)
            return this.getStringProperty(group + GROUP_SEPARATOR + name);
        else
            return this.getStringProperty(name);
    }

    public String getStringProperty(String name) {
        int slash = name.indexOf('/');
        String propName = slash == -1 ? name : name.replace('/', GROUP_SEPARATOR);
        String value = _properties.getProperty(propName);
        if (value != null && value.startsWith("###"))
            value = MiniEncrypter.decrypt(value.substring(3));
        return value;
    }

    public Properties getAllProperties() {
        return _properties;
    }

    @Override
    public void setStringProperty(String name, String value) {
        if ( value == null || value.length() == 0)
            _properties.remove(name);
        else
            _properties.put(name, value);
    }

    /**
     * <p>
     * Callback from OSGi config admin update.
     * </p>
     * @throws PropertyException
     */
    public void updateProperties(Map<String,?> props) throws PropertyException {
        if (props.isEmpty())
            return; // ignore as config admin service may not be available yet
        /**
         * Previously we were explicitly removing properties
         * from the _properties variable, however this was causing
         * problems with custom property files, so we can just do a full cache
         * refresh instead (which also reloads the custom properties file)
         */
        try {
            refreshCache();
            LoggerUtil.getStandardLogger().refreshCache();  // in case log props have changed
        }
        catch (PropertyException ex) {
            throw new PropertyException(-1, ex.getMessage(), ex);
        }
    }

    public PropertyManager getPropertyManager() {
        return getLocalInstance();  // will return the local instance through the service
    }

    public static String getOsgiProperty(BundleContext bundleContext, String pid, String propName) throws IOException {
        ServiceReference caRef = bundleContext.getServiceReference(ConfigurationAdmin.class.getName());
        ConfigurationAdmin configAdmin = (ConfigurationAdmin)  bundleContext.getService(caRef);
        Configuration config = configAdmin.getConfiguration(pid);
        Object value = config.getProperties().get(propName);
        return value == null ? null : value.toString();
    }

}
