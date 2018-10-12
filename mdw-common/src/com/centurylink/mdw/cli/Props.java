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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.centurylink.mdw.config.YamlProperties;

public class Props {

    public static Prop ASSET_LOC;
    public static Prop DISCOVERY_URL;
    public static Prop SERVICES_URL;
    public static Prop HUB_URL;

    public static class ProjectYaml {
        public static final String MDW_VERSION = "project.mdw.version";
        public static final String ASSET_LOC = "project.asset.location";
        public static final String CONFIG_LOC = "project.config.location";
    }

    public static class Git {
        public static Prop REMOTE_URL;
        public static Prop BRANCH;
        public static Prop USER;
        public static Prop PASSWORD;
    }

    public static class Db {
        public static Prop URL;
        public static Prop USER;
        public static Prop PASSWORD;
        public static Prop DRIVER;
    }

    public static class Gradle {
        public static Prop SPRING_VERSION;
        public static Prop MAVEN_REPO_URL;
        public static Prop SOURCE_GROUP;
        public static Prop SONATYPE_REPO_URL;
    }

    public static List<Prop> ALL_PROPS = new ArrayList<>();

    private static String MDW;

    public static void init(String mdwConfig) {
        MDW = mdwConfig;

        // mdw
        ASSET_LOC = new Prop("asset-loc", MDW, "mdw.asset.location");
        DISCOVERY_URL = new Prop("discovery-url", MDW, "mdw.discovery.url");
        SERVICES_URL = new Prop("services-url", MDW, "mdw.services.url");
        HUB_URL = new Prop("hub-url", MDW, "mdw.hub.url");

        ALL_PROPS.add(ASSET_LOC);
        ALL_PROPS.add(DISCOVERY_URL);
        ALL_PROPS.add(SERVICES_URL);
        ALL_PROPS.add(HUB_URL);

        // mdw git
        Git.REMOTE_URL = new Prop("git-remote-url", MDW, "mdw.git.remote.url");
        Git.BRANCH = new Prop("git-branch", MDW, "mdw.git.branch");
        Git.USER = new Prop("git-user", MDW, "mdw.git.user");
        Git.PASSWORD = new Prop("git-password", MDW, "mdw.git.password");

        ALL_PROPS.add(Git.REMOTE_URL);
        ALL_PROPS.add(Git.BRANCH);
        ALL_PROPS.add(Git.USER);
        ALL_PROPS.add(Git.PASSWORD);

        // mdw db
        Db.URL = new Prop("database-url", MDW, "mdw.database.url");
        Db.USER = new Prop("database-user", MDW, "mdw.database.username");
        Db.PASSWORD = new Prop("database-password", MDW, "mdw.database.password");
        Db.DRIVER = new Prop("database-driver", MDW, "mdw.database.driver");

        ALL_PROPS.add(Db.URL);
        ALL_PROPS.add(Db.USER);
        ALL_PROPS.add(Db.PASSWORD);
        ALL_PROPS.add(Db.DRIVER);

        // gradle (TODO: maven support)
        String GRADLE = "gradle.properties";
        Gradle.SPRING_VERSION = new Prop("spring-version", GRADLE, "springVersion", true);
        Gradle.MAVEN_REPO_URL = new Prop("releases-url", GRADLE, "repositoryUrl", true);
        Gradle.SOURCE_GROUP = new Prop("source-group", GRADLE, "sourceGroup", true);
        Gradle.SONATYPE_REPO_URL = new Prop("snapshots-url", GRADLE, "snapshotsUrl", true);


        ALL_PROPS.add(Gradle.SPRING_VERSION);
        ALL_PROPS.add(Gradle.MAVEN_REPO_URL);
        ALL_PROPS.add(Gradle.SOURCE_GROUP);
        ALL_PROPS.add(Gradle.SONATYPE_REPO_URL);
    }

    private Map<File,Properties> propFiles = new HashMap<>();
    private Setup setup;

    public Props(Setup setup) {
        this.setup = setup;
    }


    private Properties getProperties(File file) throws IOException {
        Properties properties = propFiles.get(file);
        if (properties == null) {
            if (file.exists()) {
                if (file.getName().endsWith(".yaml")) {
                    final YamlProperties yamlProps = new YamlProperties(file);
                    properties = new Properties() {
                        @Override
                        public String getProperty(String key) {
                            return yamlProps.getString(key);
                        }
                    };
                }
                else {
                    properties = new Properties();
                    properties.load(new FileInputStream(file));
                }
                propFiles.put(file, properties);
            }
        }
        return properties;
    }

    public String get(String name) throws IOException {
        return get(new Prop(null, MDW, name), false);
    }

    public String get(Prop prop) throws IOException {
        return get(prop, true);
    }

    public String get(Prop prop, boolean required) throws IOException {
        String value = null;
        if (prop.specified && setup != null && prop.getName() != null) {
            // command-line params take precedence
            Object obj = setup.getValue(prop.getName());
            if (obj != null)
                value = obj.toString();
        }
        if (value == null) {
            // read from prop file (if exists)
            File propFile;
            if (prop.inProjectDir)
                propFile = new File(setup.getProjectDir() + "/" + prop.getFile());
            else
                propFile = new File(setup.getConfigRoot() + "/" + prop.getFile());
            Properties properties = getProperties(propFile);
            if (properties != null) {
                value = properties.getProperty(prop.getProperty());
            }
            if (value == null && setup != null && prop.getName() != null) {
                // fall back to default (non-specified) value
                Object obj = setup.getValue(prop.getName());
                if (obj != null)
                    value = obj.toString();
            }
        }
        if (value == null && required)
            throw new IOException("Missing value: " + prop);
        return value;
    }
}
