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
package com.centurylink.mdw.cli;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DbInfo {

    public DbInfo(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public DbInfo(Props props) throws IOException {
        this(props.get(Props.Db.URL), props.get(Props.Db.USER), props.get(Props.Db.PASSWORD));
    }

    private String url;
    public String getUrl() { return url; }

    private String user;
    public String getUser() { return user; }

    private String password;
    String getPassword() { return password; }

    public String toString() {
        return "url=" + url + ", user=" + user;
    }

    public static String getDatabaseDriver(String dbUrl) {
        if (dbUrl.startsWith("jdbc:mariadb"))
            return "org.mariadb.jdbc.Driver";
        else if (dbUrl.startsWith("jdbc:mysql"))
            return "com.mysql.jdbc.Driver";
        else if (dbUrl.startsWith("jdbc:oracle"))
            return "oracle.jdbc.driver.OracleDriver";
        else
            return null;
    }

    public List<Dependency> getDependencies() {
        List<Dependency> dependencies = new ArrayList<>();
        if (url.startsWith("jdbc:mariadb"))
            dependencies.add(new Dependency("org/mariadb/jdbc/mariadb-java-client/2.5.1/mariadb-java-client-2.5.1.jar", 566457L));
        else if (url.startsWith("jdbc:mysql"))
            dependencies.add(new Dependency("mysql/mysql-connector-java/5.1.29/mysql-connector-java-5.1.29.jar", 876730L));
        else if (url.startsWith("jdbc:oracle"))
            dependencies.add(new Dependency("com/oracle/ojdbc6/12.1.0.2.0/ojdbc6-12.1.0.2.0.jar", 3692096L));
        dependencies.add(new Dependency("com/google/guava/guava/23.0/guava-23.0.jar", 2614708L));
        return dependencies;
    }


    public void loadDbDriver() throws IOException {
        try {
            Class.forName(DbInfo.getDatabaseDriver(this.getUrl()));
        }
        catch (ClassNotFoundException ex) {
            throw new IOException(ex.getMessage(), ex);
        }
    }

    public Connection getConnection() throws SQLException, IOException {
        loadDbDriver();
        return DriverManager.getConnection(getUrl(), getUser(), getPassword());
    }
}
