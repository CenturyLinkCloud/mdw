/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.constant.PaaSConstants;
import com.centurylink.mdw.container.NamingProvider;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.startup.StartupException;

public class PropertyManagerDatabase extends PropertyManager {

    protected Properties properties = new Properties();
    private String containerName;
    private String dburl;
    private String servletRealPath;
    private String mainPropertyFileName;
    public static final String APP_CONFIG_NAME = "mdw.application.config.name";

    /**
     * This method loads mdw.properties, application.properties and any property file defined for {@link #APP_CONFIG_NAME}
     * The property mdw.application.config.name takes property file names seperated by , without the .properties extension
     * @param containerName
     * @param dburl when it is null, used by container; when it is not null, used by Designer
     * @throws PropertyException
     */
    public PropertyManagerDatabase(String containerName, String dburl, String servletRealPath)
            throws StartupException {
        this.containerName = containerName;
        this.dburl = dburl;
        this.servletRealPath = servletRealPath;
        if (dburl!=null) {    // from designer. load from database only
            properties.clear();
            loadPropertiesFromDatabase(null, false);
        } else {            // from container. load from files only (load from database later)
            mainPropertyFileName = getMainPropertyFileName();
            // 1. load properties from mdw.properties or ApplicationProperties.xml
            this.loadPropertiesFromFile(null, mainPropertyFileName, true, true);
            // 2. load properties from application.properties
            loadPropertiesFromFile(null, APPLICATION_PROPERTIES_FILE_NAME, true, false);
            // 3. load properties from package.properties (only application in cloud dev environment
            loadPropertiesFromFile(null, PACKAGE_PROPERTIES_FILE_NAME, true, false);

            //This is for cloud mode. Application config files should be in same place as mdw.properties.
            String appPropertyFiles = this.getStringProperty(APP_CONFIG_NAME);
            if (appPropertyFiles != null) {
                for (String fileName : appPropertyFiles.split(",")) {
                    loadPropertiesFromFile(null, fileName + ".properties", true, false);
                }
            }
        }
    }

    /**
     * Load properties from external source such as XML file and databases into cache.
     * The method loads first from ApplicationProperties.xml in the class path,
     * then loads properties from ATTRIBUTE table of MDW database.
     *
     * You can override this method to create your own property manager that loads
     * from additional or alternative database tables, by calling loadPropertiesFromDatabase.
     * If you load from tables in a different database, you will need to obtain its url within
     * this method from places such as java system properties, or a database system property in MDW database.
     * as in <code>System.getProperty("mydb.url")</code>, or
     * <code>tempPropertyGroups.get(OwnerType.SYSTEM).getProperty("mydb.url")</code>
     *
     * @throws PropertyException If an exception is thrown, the application will not be loaded
     *      into WLS. When you override this method, if you wish to move on when failing to load properties,
     *      do not throw exception (catching exception), as the case for calling loadFromFile in this method.
     */
    public void refreshCache() throws PropertyException {
        refreshProperties(true, false);
    }

    public void clearCache() {
        properties.clear();
    }

    private String getMainPropertyFileName() throws StartupException {
        String configLoc = getPropertyFileLocation();
        File file = new File(configLoc==null?MDW_PROPERTIES_FILE_NAME:(configLoc+MDW_PROPERTIES_FILE_NAME));
        if (file.exists()) return MDW_PROPERTIES_FILE_NAME;
        URL url = this.getClass().getClassLoader().getResource(MDW_PROPERTIES_FILE_NAME);
        if (url!=null) return MDW_PROPERTIES_FILE_NAME;
        if (containerName.equals(NamingProvider.TOMCAT)) {
            file = new File(servletRealPath + "/../../conf/" + MDW_PROPERTIES_FILE_NAME);
            if (file.exists()) return MDW_PROPERTIES_FILE_NAME;
        }
        throw new StartupException(StartupException.NO_PROPERTY_FILE_FOUND,
            "No mdw.properties configuration file is found");
    }

    private void refreshProperties(boolean printStackTraceWhenError, boolean fileonly) throws PropertyException {

        Properties tempProperties = new Properties();
        getSources().clear();

        // 1. load properties from mdw.properties or ApplicationProperties.xml from configuration directory
        loadPropertiesFromFile(tempProperties, mainPropertyFileName, true, true);

        // 2. load properties from application.properties
        loadPropertiesFromFile(tempProperties, APPLICATION_PROPERTIES_FILE_NAME, true, false);
        // 3. load properties from package.properties (only application in cloud dev environment
        loadPropertiesFromFile(tempProperties, PACKAGE_PROPERTIES_FILE_NAME, true, false);

        //This is for cloud mode. Application config files should be in same place as mdw.properties.
        String appPropertyFiles = this.getStringProperty(APP_CONFIG_NAME);
        if (appPropertyFiles != null) {
            for (String fileName : appPropertyFiles.split(",")) {
                loadPropertiesFromFile(tempProperties, fileName + ".properties", true, false);
            }
        }

        if (!fileonly) {
            // 4. load properties from database
            loadPropertiesFromDatabase(tempProperties, printStackTraceWhenError);
            // 5. load properties from local override
            loadPropertiesFromFile(tempProperties, ENV_OVERRIDE_PROPERTIES_FILE_NAME, false, false);
        }

        // update the cache
        updateCache(tempProperties);
    }

    // load properties from database (override mdw.properties and application.properties)
    public void loadPropertiesFromDatabase(Properties tempProperties, boolean printStackTraceWhenError) {
        try {
            loadPropertiesFromDatabase(tempProperties==null?properties:tempProperties, getMDWDatabaseURL(),
                "select ATTRIBUTE_NAME,ATTRIBUTE_VALUE from ATTRIBUTE where ATTRIBUTE_OWNER='"
                + OwnerType.SYSTEM + "'");
            System.out.println("Loaded properties from database");
        } catch (PropertyException e) {
            if (printStackTraceWhenError) {
                System.out.println("Cannot load properties from database");
                e.printStackTrace();
            }
        }
    }

    // load local override properties (personal dev environment only)
    public void loadPropertiesFromFile(Properties tempProperties, String fileName,
            boolean verboseSuccess, boolean verboseFailure) {
        try {
            loadFromFile(tempProperties==null?properties:tempProperties, fileName);
            if (verboseSuccess) System.out.println("Loaded properties from file " + fileName);
        } catch (PropertyException e) {
            if (verboseFailure) {
                System.out.println("Cannot load properties from " + fileName);
                e.printStackTrace();
            }
        }
    }

    final protected void updateCache(Properties tempProperties) {
        synchronized (properties) {
            properties.clear();
            properties.putAll(tempProperties);
        }
    }

    final protected String getMDWDatabaseURL() {
        return dburl;
    }

    /**
         * Returns the handle to the property based on the passed in GroupName and the property Name
         * @param pGroupName
         * @param pPropertyName
         * @return Value defined for the property as String
         * @throws PropertyException
         */
    public String getStringProperty(String pGroupName, String pPropertyName) {
        if (pGroupName!=null) return this.getStringProperty(pGroupName + "/" + pPropertyName);
        else return this.getStringProperty(pPropertyName);
    }

     /**
         * returns the properties for group.
         * When the group is specified with ApplicationProperties.xml
         * (old style), the names of the properties returned
         * will not contain the group names and '/', for backward
         * compatibility. When the group is in the new style,
         * where a group is any property name prefix terminated by a '.',
         * the property names returned will contain the
         * whole property names including the group names.
         * @param pGroupName
         * @return Properties for the group
         * @throws PropertyException
         */
    public Properties getProperties(String pGroupName) {
        Properties props = new Properties();
        int k = pGroupName.length();
        for (Object key : properties.keySet()) {
            String propname = (String)key;
            int l = propname.length();
            char ch = l>k?propname.charAt(k):' ';
            if ((ch=='.'||ch=='/') && propname.startsWith(pGroupName)) {
                if (ch=='.') props.put(propname, properties.get(propname));
                else props.put(propname.substring(k+1), properties.get(propname));
            }
        }
        return props;
    }

    public String getStringProperty(String propertyName) {
        return (String)properties.get(propertyName);
    }


    final protected void loadFromFile(Properties properties, String filename)
    throws PropertyException {
        String configLoc = getPropertyFileLocation();
        InputStream stream = null;
        try {
            File file = new File(configLoc==null?filename:(configLoc+filename));
            if (file.exists()) {
                stream = new FileInputStream(file);
            }
            else if (containerName.equals(NamingProvider.TOMCAT) && PaaSConstants.PAAS_VCAP_APPLICATION == null) {
                stream = new FileInputStream(servletRealPath + "/../../conf/" + filename);
            }
            else {
                stream = this.getClass().getClassLoader().getResourceAsStream(filename);
                if (stream == null)  // try context classloader
                    stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);
            }
            if (stream==null)
                throw new PropertyException("Property file does not exist: " + filename);
            else
                loadFromStream(properties, stream, filename);
        }
        catch (Exception ex) {
            throw new PropertyException(-1, "Failed to load properties from " + filename, ex);
        }
    }

    /**
     * Load properties from database.
     *
     * @param properties a hash table of all properties
     * @param dburl database URL
     * @param query a query for retrieving the properties from database. The query should return
     *      two fields for every row, the first is the property name and the second is its value.
     * @throws PropertyException thrown when failed to connect to database or execute the query
     */
    final protected void loadPropertiesFromDatabase(Properties properties, String dburl,
                String query) throws PropertyException {
        DatabaseAccess db = null;
        try {
            db = new DatabaseAccess(dburl);
            db.openConnection();
            ResultSet rs = db.runSelect(query, null);
            while (rs.next()) {
                String propname = rs.getString(1);
                String propvalue = rs.getString(2);
                if (propvalue!=null && propvalue.length()>0) {
                    properties.put(propname, propvalue);
                    getSources().put(propname, DATABASE);
                } else properties.remove(propname);
            }
        } catch (SQLException e) {
            throw new PropertyException(-1, "Failed to load properties from database", e);
        } finally {
            if (db!=null) db.closeConnection();
        }
    }

    public Properties getAllProperties() {
        return properties;
    }

    public void setStringProperty(String property_name, String value) {
        if (value==null||value.length()==0) properties.remove(property_name);
        else properties.put(property_name, value);
    }
}