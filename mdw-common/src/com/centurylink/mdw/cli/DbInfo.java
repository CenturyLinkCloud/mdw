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
import java.util.HashMap;
import java.util.Map;

public class DbInfo {

    public DbInfo(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public DbInfo(Props props) throws IOException {
        this.url = props.get(Props.Db.URL);
        this.user = props.get(Props.Db.USER);
        this.password = props.get(Props.Db.PASSWORD);
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

    public static Map<String,Long> getDependencies(String dbUrl) {
        Map<String,Long> map = new HashMap<>();
        if (dbUrl.startsWith("jdbc:mariadb"))
            map.put("org/mariadb/jdbc/mariadb-java-client/1.2.2/mariadb-java-client-1.2.2.jar", 300713L);
        else if (dbUrl.startsWith("jdbc:mysql"))
            map.put("mysql/mysql-connector-java/5.1.29/mysql-connector-java-5.1.29.jar", 876730L);
        else if (dbUrl.startsWith("jdbc:oracle"))
            map.put("com/oracle/ojdbc6/12.1.0.2.0/ojdbc6-12.1.0.2.0.jar", 3692096L);
        return map;
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

}
