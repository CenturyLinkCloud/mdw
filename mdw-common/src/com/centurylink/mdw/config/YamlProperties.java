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
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.centurylink.mdw.yaml.YamlLoader;

/**
 * Reads and writes yaml configuration settings as if they were properties.
 * Filename (minus extension) is the default property prefix.
 */
public class YamlProperties {

    public enum PropType {
        string,
        list,
        group
    }

    private String prefix;
    private YamlLoader loader;
    public YamlLoader getLoader() { return loader; }

    private Map<?,?> root;
    public Map<?,?> getRoot() { return root; }

    public YamlProperties(File yamlFile) throws IOException {
        this.prefix = yamlFile.getName().substring(0, yamlFile.getName().lastIndexOf('.'));
        this.loader = new YamlLoader(yamlFile);
        root = loader.getRequiredMap("", loader.getTop(), "");
    }

    /**
     * Examples:
     *
     * (in mdw.yaml):
     * <pre>
     *   # structured
     *   database:
     *     driver: org.mariadb.jdbc.Driver
     *   # flat
     *   hub.url: http://localhost:8080/mdw
     *   # mixed
     *   git:
     *     remote.url: https://github.com/CenturyLinkCloud/mdw.git
     * </pre>
     *
     * <code>
     * getString("mdw.database.driver")
     * getString("mdw.hub.url")
     * getString("mdw.git.remote.url")
     * </code>
     *
     * @param name
     * @return
     */
    public String getString(String name) {
        return (String)get(name, PropType.string);
    }

    @SuppressWarnings("unchecked")
    public List<String> getList(String name) {
        return (List<String>)get(name, PropType.list);
    }

    /**
     * Returns a flattened value map with full-path keys (including prefix).
     */
    @SuppressWarnings("unchecked")
    public Map<String,String> getGroup(String name) {
        Map<String,String> map = null;
        Map<String,Object> groupMap = (Map<String,Object>)get(name, PropType.group);
        if (groupMap != null) {
            map = new HashMap<>();
            for (String groupKey : groupMap.keySet()) {
                Map<String,Object> innerMap = loader.getMap(groupKey, groupMap);
                for (String innerKey : innerMap.keySet()) {
                    map.put(groupKey + "." + innerKey, innerMap.get(innerKey).toString());
                }
            }
        }
        return map;
    }

    public Object get(String name, PropType type) {
        String key = name;
        if (key.startsWith(this.prefix + ".")) {
            key = key.substring(this.prefix.length() + 1);
        }
        Map<?,?> deepest = root;
        String residual = key;
        for (String segment : key.split("\\.")) {
            Object value = null;
            if (type == PropType.string)
                value = loader.get(residual, deepest);
            else if (type == PropType.list)
                value = loader.getList(residual, deepest);
            else if (type == PropType.group)
                value = loader.getMap(residual, deepest);
            if (value == null) {
                Map<?,?> map = loader.getMap(segment, deepest);
                if (map == null) {
                    return null;
                }
                else {
                    deepest = map;
                    residual = residual.substring(segment.length() + 1);
                }
            }
            else {
                if (type == PropType.group) {
                    Map<String,Object> map = new HashMap<>();
                    Map<?,?> groupMap = (Map<?,?>)value;
                    for (Object groupKey : groupMap.keySet()) {
                        map.put(name + "." + groupKey, groupMap.get(groupKey));
                    }
                    return map;
                }
                return value;
            }
        }
        return null;
    }

    public static String translate(Properties properties) {

        // TODO:

        return null;
    }

}
