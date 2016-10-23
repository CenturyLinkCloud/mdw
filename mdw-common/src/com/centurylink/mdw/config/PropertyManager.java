/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.xmlbeans.XmlException;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.cache.CacheEnabled;
import com.centurylink.mdw.startup.StartupException;

/**
 */
public abstract class PropertyManager implements CacheEnabled {

    public static final String MDW_PROPERTIES_FILE_NAME = "mdw.properties";
    public static final String APPLICATION_PROPERTIES_FILE_NAME = "application.properties";
    public static final String PACKAGE_PROPERTIES_FILE_NAME = "package.properties";
    public static final String ENV_OVERRIDE_PROPERTIES_FILE_NAME = "env.properties";
    public static final String DATABASE = "database";
    public static final String MDW_CONFIG_LOCATION = "mdw.config.location";
    public static final String MDW_PROPERTY_MANAGER = "mdw.property.manager";
    public static final String DB_CONFIG_ENABLED = "mdw.database.config.enabled";

    private static PropertyManager instance = null;
    private Map<String,String> sources = new HashMap<String,String>();
    protected Map<String,String> getSources() { return sources; }

    /**
     * returns the properties for group
     * @param pGroupName
     * @return Properties for the group
     * @throws PropertyException
     */
    public abstract Properties getProperties(String pGroupName)
    throws PropertyException;

    /**
     * Returns the handle to the property based on the passed in
     * GroupName and the property Name
     * @param pGroupName
     * @param pPropertyName
     * @return Value defined for the property as String
     * @throws PropertyException
     */
     public abstract String getStringProperty(String pGroupName, String pPropertyName)
      throws PropertyException;

     public abstract String getStringProperty(String property_name);

     public abstract Properties getAllProperties();

     public abstract void setStringProperty(String property_name, String value);

     private String propertyFileLocation;
     protected String getPropertyFileLocation() {
         if (propertyFileLocation == null) {
             String configLoc = System.getProperty(MDW_CONFIG_LOCATION);
             if (configLoc != null) {
                 if (!configLoc.endsWith("/"))
                     configLoc = configLoc + "/";
                 propertyFileLocation = configLoc;
                 System.out.println("Loading configuration files from '" + configLoc + "'");
             }
         }
         return propertyFileLocation;
     }

     /**
      * returns the handle to the property manager
      * @return PropertyManager
      */
      public static PropertyManager getInstance() {
        return getLocalInstance();
      }

      public static PropertyManager getLocalInstance() {
        if (instance==null) {
            try {
                initializeContainerPropertyManager(ApplicationContext.getContainerName(), null);
            } catch (StartupException e) {
                // should not reach here, as the property manager should be initialized by now
                throw new RuntimeException(e);
            }
            // container/database property manager will never hit this
        }
        return instance;
      }

      public static String getProperty(String property_name) {
          return getInstance().getStringProperty(property_name);
      }

      public static int getIntegerProperty(String property_name, int defaultValue) {
          String v = getInstance().getStringProperty(property_name);
          if (v==null) return defaultValue;
          try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
      }

      public static long getLongProperty(String property_name, long defaultValue) {
          String v = getInstance().getStringProperty(property_name);
          if (v==null) return defaultValue;
          try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
      }

      public static boolean getBooleanProperty(String property_name, boolean defaultValue) {
          String v = getInstance().getStringProperty(property_name);
          if (v==null) return defaultValue;
          return v.equalsIgnoreCase("true");
      }

    public synchronized static PropertyManager initializeDesignerPropertyManager(String dburl) {
        if (instance==null) {
            try {
                instance = new PropertyManagerDatabase("Designer", dburl, null);
            } catch (Exception e) {
                String msg = "Cannot create default property manager";
                System.out.println(msg);
                e.printStackTrace();
                throw new RuntimeException(msg);
            }
        }
        return instance;
    }

    public static PropertyManager initializeUnitTestPropertyManager() {
        if (instance==null) {
            instance = new PropertyManagerUnitTest();
        }
        return instance;
    }

    public synchronized static PropertyManager initializeContainerPropertyManager(String containerName,
        String servletRealPath) throws StartupException {

        String pmname = System.getProperty(MDW_PROPERTY_MANAGER);
        if (pmname == null)
            pmname = System.getProperty("property_manager");  // try compatibility
        if (pmname!=null) {
            System.out.println("Using Property Manager: " + pmname);
            try {
                Class<?> cls = PropertyManager.class.getClassLoader().loadClass(pmname);
                instance = (PropertyManager)cls.newInstance();
            } catch (Exception e) {
                String msg = "Cannot create property manager " + pmname;
                System.out.println(msg);
                e.printStackTrace();
                throw new StartupException(StartupException.FAIL_TO_LOAD_PROPERTIES, msg);
            }
        } else {
            instance = new PropertyManagerDatabase(containerName, null, servletRealPath);
        }
        return instance;
    }

    public String getPropertySource(String propname) {
        return sources.get(propname);
    }

    public void putPropertySource(String propname, String src) {
        sources.put(propname, src);
    }

    public boolean isDbConfigEnabled() {
        return true;
    }

    final protected void loadFromStream(Properties properties, InputStream stream, String source)
            throws XmlException, IOException {
        try {
            Properties props = new Properties();
            props.load(stream);
            for (Object key : props.keySet()) {
                String pn = (String)key;
                String pv = props.getProperty(pn);
                if (source.equals(ENV_OVERRIDE_PROPERTIES_FILE_NAME)) {
                    System.out.println(" - property local override: " + pn + "=" + pv);
                }
                if (pv.length()>0) {
                    properties.put(pn, pv);
                    getSources().put(pn, source);
                } else properties.remove(pn);
            }
        } finally {
            if (stream!=null)
                { try { stream.close(); } catch (Exception e) {} };
        }
    }
}