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
package com.centurylink.mdw.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.centurylink.mdw.yaml.YamlLoader;

public class YamlPropertyManager extends PropertyManager {

    private File mdwYaml;
    private String yaml;

    private List<YamlProperties> yamlProps;
    private List<Properties> javaProps;

    /**
     * Previously-accessed (or programmatically set) values.
     * May contain (cached) nulls.
     */
    private Map<String,Object> cachedValues;

    public YamlPropertyManager(File mdwYaml) throws IOException {
        this.mdwYaml = mdwYaml;
        load();
    }

    /**
     * For PaaS.  Prefix is wired to "mdw",
     */
    public YamlPropertyManager(String yaml) throws IOException {
        this.yaml = yaml;
        load();
        setStringProperty("mdw.asset.location", System.getenv("MDW_ASSET_LOCATION"));
    }

    private void load() throws IOException {
        cachedValues = new HashMap<>();
        javaProps = null;

        yamlProps = new ArrayList<>();
        YamlProperties mdwYamlProps;
        if (yaml != null) {
            mdwYamlProps = new YamlProperties("mdw", yaml);
        }
        else {
            mdwYamlProps = new YamlProperties(mdwYaml);
            System.out.println("mdw config: " + mdwYaml.getAbsolutePath());
        }
        yamlProps.add(mdwYamlProps);

        // application yamls
        YamlLoader loader = mdwYamlProps.getLoader();
        Map<?,?> application = loader.getMap("application", mdwYamlProps.getRoot());
        if (application != null) {
            @SuppressWarnings("unchecked")
            List<String> appConfigs = loader.getList("configs", application);
            if (appConfigs != null) {
                for (String appConfig : appConfigs) {
                    System.out.println("  app config: " + appConfig);
                    if (appConfig.endsWith(".yaml") || appConfig.endsWith(".yml")) {
                        yamlProps.add(new YamlProperties(new File(mdwYaml.getParentFile() + "/" + appConfig)));
                    }
                    else {
                        addJavaConfig(appConfig);
                    }
                }
            }
        }
    }

    private void addJavaConfig(String name) throws IOException {
        if (javaProps == null)
            javaProps = new ArrayList<>();
        if (!name.endsWith(".properties"))
            name += ".properties";
        Properties props = new Properties();
        props.load(new FileInputStream(mdwYaml.getParentFile() + "/" + name));
        javaProps.add(props);
        for (Object key : props.keySet()) {
            if (key.toString().startsWith("mdw.")) {
                new PropertyException("Property: '" + key + "' cannot be overridden in "
                         + name + " (value is ignored)").printStackTrace();
            }
        }
    }

    @Override
    public void refreshCache() throws Exception {
        // remember asset location (for PaaS)
        String assetLocation = getProperty("mdw.asset.location");
        load();
        if (getProperty("mdw.asset.location") == null)
            setStringProperty("mdw.asset.location", assetLocation);
    }

    @Override
    public void clearCache() {
    }

    @Override
    public Properties getProperties(String group) throws PropertyException {
        Properties props = new Properties();
        for (YamlProperties yamlProp : yamlProps) {
            Map<String,String> groupMap = yamlProp.getGroup(group);
            if (groupMap != null) {
                for (String key : groupMap.keySet()) {
                    props.put(key, groupMap.get(key));
                }
            }
        }
        if (javaProps != null) {
            for (Properties javaProp : javaProps) {
                for (String name : javaProp.stringPropertyNames()) {
                    if (name.startsWith(group + ".")) {
                        String value = javaProp.getProperty(name);
                        if (value != null) {
                            props.put(name, value);
                        }

                    }
                }
            }
        }
        return props;
    }

    @Override
    public String getStringProperty(String name) {
        if (cachedValues.containsKey(name)) {
            Object obj = cachedValues.get(name);
            return obj == null ? null : obj.toString();
        }
        else {
            String value = getValue(name);
            cachedValues.put(name, value);
            return value;
        }
    }

    @Override
    public void setStringProperty(String name, String value) {
        cachedValues.put(name, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> getList(String name) {
        if (cachedValues.containsKey(name)) {
            return (List<String>)cachedValues.get(name);
        }
        else {
            List<String> value = getListValue(name);
            cachedValues.put(name, value);
            return value;
        }
    }

    /**
     * Only returns cached (previously-read) properties.
     * For other values, refer to yaml/property files.
     * Or execute CLI command <code>mdw config [name]</code>.
     */
    @Override
    public Properties getAllProperties() {
        Properties props = new Properties();
        for (String name : cachedValues.keySet()) {
            props.put(name, String.valueOf(cachedValues.get(name)));
        }
        return props;
    }

    /**
     * Returns the YAML loader containing the given root name (if any).
     */
    public YamlLoader getLoader(String name) {
        for (YamlProperties yamlProp : yamlProps) {
            if (yamlProp.getRoot().containsKey(name)) {
                return yamlProp.getLoader();
            }
        }
        return null;
    }

    /**
     * Reads flat or structured values from yaml.
     * If not found, fall back to java properties.
     */
    private String getValue(String name) {
        for (YamlProperties yamlProp : yamlProps) {
            String value = yamlProp.getString(name);
            if (value != null)
                return value;
        }
        if (javaProps != null) {
            for (Properties javaProp : javaProps) {
                String value = javaProp.getProperty(name);
                if (value != null)
                    return value;
            }
        }
        return null;
    }

    private List<String> getListValue(String name) {
        for (YamlProperties yamlProp : yamlProps) {
            List<String> value = yamlProp.getList(name);
            if (value != null)
                return value;
        }
        if (javaProps != null) {
            for (Properties javaProp : javaProps) {
                String str = javaProp.getProperty(name);
                if (str != null) {
                    return Arrays.asList(str.trim().split("\\s*,\\s*"));
                }
            }
        }
        return null;
    }
}
