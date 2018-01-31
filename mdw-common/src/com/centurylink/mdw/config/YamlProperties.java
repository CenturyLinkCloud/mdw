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
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.yaml.YamlLoader;

/**
 * Filename (minus extension) is the default property prefix.
 */
public class YamlProperties {

    public enum PropType {
        string,
        list
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

    public Object get(String name, PropType type) {
        if (name.startsWith(this.prefix + ".")) {
            name = name.substring(this.prefix.length() + 1);
        }
        Map<?,?> deepest = root;
        String residual = name;
        for (String segment : name.split("\\.")) {
            Object value = null;
            if (type == PropType.string)
                value = loader.get(residual, deepest);
            else if (type == PropType.list)
                value = loader.getList(residual, deepest);
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
                return value;
            }
        }
        return null;
    }

}
