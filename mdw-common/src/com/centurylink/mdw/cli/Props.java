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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Props {

    private File projectDir;
    private Map<File,Properties> propFiles = new HashMap<>();
    private Setup setup;

    public Props(File projectDir, Setup setup) {
        this.projectDir = projectDir;
        this.setup = setup;
    }

    public static final String MDW = "config/mdw.properties";
    public static final String GRADLE = "gradle.properties";

    public static final Prop ASSET_LOC = new Prop("asset-loc", MDW, "mdw.asset.location");
    public static final Prop DISCOVERY_URL = new Prop("discovery-url", MDW, "mdw.discovery.url");

    public static class Git {
        public static final Prop REMOTE_URL = new Prop("git-remote-url", MDW, "mdw.git.remote.url");
        public static final Prop BRANCH = new Prop("git-branch", MDW, "mdw.git.branch");
        public static final Prop USER = new Prop("git-user", MDW, "mdw.git.user");
        public static final Prop PASSWORD = new Prop("git-password", MDW, "mdw.git.password");
    }
    public static class Db {
        public static final Prop URL = new Prop("database-url", MDW, "mdw.database.url");
        public static final Prop USER = new Prop("database-user", MDW, "mdw.database.user");
        public static final Prop PASSWORD = new Prop("database-password", MDW, "mdw.database.password");
        public static final Prop DRIVER = new Prop("database-driver", MDW, "mdw.database.driver");
    }

    public static class Gradle {
        public static final Prop MDW_VERSION = new Prop("mdw-version", GRADLE, "mdwVersion");
        public static final Prop SPRING_VERSION = new Prop("spring-version", GRADLE, "springVersion");
        public static final Prop ASSET_LOC = new Prop("asset-loc", GRADLE, "assetLoc");
        public static final Prop MAVEN_REPO_URL = new Prop("releases-url", GRADLE, "repositoryUrl");
    }

    private Properties getProperties(File file) throws IOException {
        Properties properties = propFiles.get(file);
        if (properties == null) {
            if (file.exists()) {
                properties = new Properties();
                properties.load(new FileInputStream(file));
                propFiles.put(file, properties);
            }
        }
        return properties;
    }

    public String get(Prop prop) throws IOException {
        return get(prop, true);
    }

    public String get(Prop prop, boolean required) throws IOException {
        if (setup != null) {
            // command-line params take precedence
            Object value = setup.getValue(prop.getName());
            if (value != null)
                return value.toString();
        }
        File propFile = new File(projectDir + "/" + prop.getFile());
        Properties properties = getProperties(propFile);
        String value = null;
        if (properties != null) {
            value = properties.getProperty(prop.getProperty());
        }
        if (value == null && required)
            throw new IOException("Missing value: " + prop);
        return value;
    }
}
