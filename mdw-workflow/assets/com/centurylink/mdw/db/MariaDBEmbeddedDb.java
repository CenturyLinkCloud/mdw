/*
 * Copyright (C) 2018 CenturyLink, Inc.
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
package com.centurylink.mdw.db;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

import com.centurylink.mdw.cache.impl.AssetCache;
import com.centurylink.mdw.container.EmbeddedDb;
import com.centurylink.mdw.model.user.User;
import com.centurylink.mdw.model.user.Workgroup;
import com.centurylink.mdw.util.file.ZipHelper;

import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.DBConfiguration;
import ch.vorburger.mariadb4j.DBConfigurationBuilder;
import ch.vorburger.mariadb4j.Util;

public class MariaDBEmbeddedDb implements EmbeddedDb {

    private String url;
    private int port;
    private String dbName;
    private String user;
    private String password;

    private DBConfiguration config;
    private DB db;

    private File dbJar;
    private String binariesSubLoc;

    public void init(String url, String user, String password, String assetLocation, String baseLocation, String dataLocation) {
        this.url = url;
        URI uri = URI.create(url.substring(5));
        this.port = uri.getPort();
        this.dbName = uri.getPath().substring(1);
        this.user = user;
        this.password = password;

        DbConfigBuilder configBuilder = new DbConfigBuilder();
        configBuilder.setPort(port);
        configBuilder.setBaseDir(baseLocation);
        configBuilder.setDataDir(dataLocation);
        String os = configBuilder.getOS();
        String dbVer = configBuilder.getDbVersion();
        dbJar = new File(assetLocation + "/" + DB_ASSET_PACKAGE.replace('.', '/') + "/mariaDB4j-db-" + os + "-" + dbVer.substring(8) + ".jar");
        binariesSubLoc = DBConfigurationBuilder.class.getPackage().getName().replace('.', '/') + "/" + dbVer + "/" + os;
        configBuilder.setUnpackingFromClasspath(false); // we'll unpack it ourselves
        configBuilder.addArg("--lower-case-table-names=1");
        config = configBuilder.build();
    }

    public void startup() throws SQLException {
        try {
            if (db != null)
                db.stop();

            boolean firstTime = false;
            File baseDir =  new File(config.getBaseDir());
            if (!baseDir.exists() || baseDir.listFiles().length == 0) {
                firstTime = true;
                if (!baseDir.exists() && !baseDir.mkdirs())
                    throw new IOException("Cannot create db base dir: " + baseDir);
                ZipHelper.unzip(dbJar, baseDir, binariesSubLoc, null, false);
                if (!config.isWindows()) {
                    Util.forceExecutable(new File(baseDir, "bin/my_print_defaults"));
                    Util.forceExecutable(new File(baseDir, "bin/mysql_install_db"));
                    Util.forceExecutable(new File(baseDir, "bin/mysqld"));
                    Util.forceExecutable(new File(baseDir, "bin/mysql"));
                }
            }

            if (!firstTime) {
                File dataDir =  new File(config.getDataDir());
                if (!dataDir.exists() || dataDir.listFiles().length == 0)
                    firstTime = true;
            }

            db = DB.newEmbeddedDB(config);
            db.start();
            if (firstTime) {
                String rootPass = System.getenv("MDW_EMBEDDED_DB_ROOT_PASSWORD");
                if (rootPass == null)
                    rootPass = "mdwchangeme";  // can only connect from localhost, so hardwired is okay
                db.run("CREATE DATABASE IF NOT EXISTS `" + dbName + "`;", "root", null, null);
                // set a password on the root account
                db.run("SET PASSWORD FOR 'root'@'localhost' = PASSWORD('" + rootPass + "');", "root", null, null);
                // create the app user account and grant permissions
                db.run("GRANT ALL ON " + dbName + ".* to '" + user + "'@'%' IDENTIFIED BY '" + password + "'", "root", rootPass, null);
                db.run("GRANT ALL ON " + dbName + ".* to '" + user + "'@'localhost' IDENTIFIED BY '" + password + "'", "root", rootPass, null);
            }
        }
        catch (Exception ex) {
            throw new SQLException("MariaDB4j startup error: " + ex, ex);
        }
    }

    public void shutdown() throws SQLException {
        try {
            if (db != null)
                db.stop();
        }
        catch (Exception ex) {
            throw new SQLException("MariaDB4j shutdown error: " + ex, ex);
        }
    }

    public boolean checkRunning() throws SQLException {
        Connection connection = null;
        try {
            Class.forName(getDriverClass());
            connection = DriverManager.getConnection(url, user, password);
            connection.getMetaData().getDatabaseProductName();
            return true;
        }
        catch (ClassNotFoundException ex) {
            throw new SQLException("Cannot locate JDBC driver class", ex);
        }
        catch (SQLException ex) {
            return false;
        }
        finally {
            if (connection != null)
                connection.close();
        }
    }

    public boolean checkMdwSchema() throws SQLException {
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            Class.forName(getDriverClass());
            connection = DriverManager.getConnection(url, user, password);
            statement = connection.createStatement();
            resultSet = statement.executeQuery("check table process_instance");
            resultSet.next();
            String checkResult = resultSet.getString(4);
            return checkResult.equals("OK");
        }
        catch (ClassNotFoundException ex) {
            throw new SQLException("Cannot locate JDBC driver class", ex);
        }
        finally {
            if (resultSet != null)
                resultSet.close();
            if (statement != null)
                statement.close();
            if (connection != null)
                connection.close();
        }
    }

    public void createMdwSchema() throws SQLException {
        try {
            db.source("create_tables.sql", user, password, dbName);
            db.source("create_indexes.sql", user, password, dbName);
            db.source("add_fkeys.sql", user, password, dbName);
            db.source("baseline_inserts.sql", user, password, dbName);
        }
        catch (Exception ex) {
            throw new SQLException("Error creating MDW db schema: " + ex, ex);
        }
    }

    public String getDriverClass() {
        return "org.mariadb.jdbc.Driver";
    }

    private class DbConfigBuilder extends DBConfigurationBuilder {
        DbConfigBuilder() {
            super();
        }

        public String getDbVersion() {
            return _getDatabaseVersion(); // expose this instead of replicating
        }
    }

    public void insertUser(User user) throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            Class.forName(getDriverClass());
            connection = DriverManager.getConnection(url, this.user, password);
            String insertUserQuery = getAssetContent("insert_user.sql");
            preparedStatement = connection.prepareStatement(insertUserQuery);
            preparedStatement.setString(1, user.getCuid());
            preparedStatement.setString(2, user.getName());
            preparedStatement.executeUpdate();

            if (user.getGroupNames() != null) {
                String addGroupQuery = getAssetContent("add_user_to_group.sql");
                String addCommonRoleQuery = getAssetContent("add_user_to_common_role.sql");
                String addGroupRoleQuery = getAssetContent("add_user_to_group_role.sql");
                for (Workgroup group : user.getWorkgroups()) {
                    if (Workgroup.COMMON_GROUP.equals(group.getName())) {
                        if (group.getRoles() != null) {
                            for (String role : group.getRoles()) {
                                preparedStatement = connection.prepareStatement(addCommonRoleQuery);
                                preparedStatement.setString(1, user.getCuid());
                                preparedStatement.setString(2, role);
                                preparedStatement.executeUpdate();
                            }
                        }
                    }
                    else {
                        preparedStatement = connection.prepareStatement(addGroupQuery);
                        preparedStatement.setString(1, user.getCuid());
                        preparedStatement.setString(2, group.getName());
                        preparedStatement.executeUpdate();
                        if (group.getRoles() != null) {
                            for (String role : group.getRoles()) {
                                preparedStatement = connection.prepareStatement(addGroupRoleQuery);
                                preparedStatement.setString(1, user.getCuid());
                                preparedStatement.setString(2, group.getName());
                                preparedStatement.setString(3, role);
                                preparedStatement.executeUpdate();
                            }
                        }
                    }
                }
            }

            if (user.getAttributes() != null) {
                String addAttrQuery = getAssetContent("add_user_attribute.sql");
                for (String attrName : user.getAttributes().keySet()) {
                    String attrVal = user.getAttributes().get(attrName);
                    preparedStatement = connection.prepareStatement(addAttrQuery);
                    preparedStatement.setString(1, user.getCuid());
                    preparedStatement.setString(2, attrName);
                    preparedStatement.setString(3, attrVal);
                    preparedStatement.executeUpdate();
                }
            }
        }
        catch (ClassNotFoundException ex) {
            throw new SQLException("Cannot locate JDBC driver class", ex);
        }
        finally {
            if (resultSet != null)
                resultSet.close();
            if (preparedStatement != null)
                preparedStatement.close();
            if (connection != null)
                connection.close();
        }
    };

    protected String getAssetContent(String asset) {
        return AssetCache.getAsset(EmbeddedDb.DB_ASSET_PACKAGE + "/" + asset).getStringContent();
    }

    public void source(String contents) throws SQLException {
        Scanner s = new Scanner(contents);
        s.useDelimiter("(;(\r)?\n)|(--\n)");
        Connection connection = null;
        Statement statement = null;
        try {
            Class.forName(getDriverClass());
            connection = DriverManager.getConnection(url, this.user, password);
            statement = connection.createStatement();
            while (s.hasNext()) {
                String line = s.next();
                if (line.startsWith("/*!") && line.endsWith("*/")) {
                    int i = line.indexOf(' ');
                    line = line.substring(i + 1, line.length() - " */".length());
                }

                if (line.trim().length() > 0) {
                    statement.execute(line);
                }
            }
        }
        catch (ClassNotFoundException ex) {
            throw new SQLException("Cannot locate JDBC driver class", ex);
        }

        finally {
            s.close();
            if (statement != null)
                statement.close();
            if (connection != null)
                connection.close();
        }
    }

}
