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

import com.centurylink.mdw.cli.Decrypt;
import com.centurylink.mdw.yaml.YamlLoader;

import java.io.File;
import java.io.IOException;
import java.util.*;

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
        this(yamlFile.getName().substring(0, yamlFile.getName().lastIndexOf('.')), yamlFile);
    }

    public YamlProperties(String prefix, File yamlFile) throws IOException {
        this.prefix = prefix;
        this.loader = new YamlLoader(yamlFile);
        root = loader.getRequiredMap("", loader.getTop(), "");
    }

    public YamlProperties(String prefix, String yaml) throws IOException {
        this.prefix = prefix;
        this.loader = new YamlLoader(yaml);
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
        String value = (String)get(name, PropType.string);
        if (value != null && value.startsWith("~[") && value.endsWith("]")) {
            try {
                Decrypt decrypter = new Decrypt();
                decrypter.setInput(value.substring(2, value.length() - 1));
                value = decrypter.decrypt();
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return value;
    }

    public boolean isEncrypted(String name) {
        String value = (String)get(name, PropType.string);
        return value != null && value.startsWith("~[") && value.endsWith("]");
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
                if (groupMap.get(groupKey) instanceof String) {
                    map.put(groupKey, (String)groupMap.get(groupKey));
                }
                else {
                    Map<String, Object> innerMap = loader.getMap(groupKey, groupMap);
                    for (String innerKey : innerMap.keySet()) {
                        map.put(groupKey + "." + innerKey, innerMap.get(innerKey).toString());
                    }
                }
            }
        }
        return map;
    }

    /**
     * Finds a single-depth prop map for the given name (prefix omitted).
     */
    @SuppressWarnings("unchecked")
    public Map<String,String> getMap(String name) {
        Map<String,String> map = null;
        Map<String,Object> groupMap = (Map<String,Object>)get(name, PropType.group);
        if (groupMap != null) {
            map = new HashMap<>();
            for (String groupKey : groupMap.keySet()) {
                String value = groupMap.get(groupKey).toString();
                map.put(groupKey.substring(name.length() + 1), value);
            }
        }
        return map;
    }

    public Object get(String name, PropType type) {
        String key = name;
        if (prefix != null && key.startsWith(prefix + ".")) {
            key = key.substring(prefix.length() + 1);
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

    /**
     * Translate old-style properties to yaml according to rules in
     * compatibility mapping file configurations.map.
     */
    public static YamlBuilder translate(String prefix, Properties properties, Properties map)
    throws IOException, ReflectiveOperationException {

        // map translators to ruleProps
        Map<YamlPropertyTranslator,Map<String,Object>> translators = new LinkedHashMap<>();
        YamlPropertyTranslator defaultTranslator = new DefaultYamlTranslator();
        for (String name : properties.stringPropertyNames()) {
            if ((prefix == null || name.startsWith(prefix + ".")) ||
                    (prefix.equals("mdw") && (name.startsWith("MDWFramework") || name.startsWith("LDAP")))) {
                YamlPropertyTranslator translator = null;
                String rule = map.getProperty(name);
                if (rule != null && rule.isEmpty()) {
                    // blank means remove this prop (no translator)
                    System.out.println("Info: Obsolete property: '" + name + "' -- removed");
                }
                else if (!"mdw".equals(prefix) && (name.startsWith("mdw")
                        || name.startsWith("MDWFramework") || name.startsWith("LDAP"))) {
                    String msg = "Warning: '" + name + "' belongs in mdw.yaml ";
                    if (rule != null)
                        msg += "(as " + rule + ") ";
                    System.err.println(msg + "-- removed");
                }
                else if (rule == null) {
                    // fully structured
                    rule = prefix == null ? name.replace('.', '/') : name.substring(prefix.length() + 1).replace('.', '/');
                    translator = defaultTranslator;
                }
                else if (rule.startsWith("[")) {
                    // custom translator -- reuse existing instance if found
                    int endBracket = rule.lastIndexOf(']');
                    String className = rule.substring(1, endBracket);
                    for (YamlPropertyTranslator instance : translators.keySet()) {
                        if (instance.getClass().getName().equals(className)) {
                            translator = instance;
                            break;
                        }
                    }
                    if (translator == null) {
                        translator = Class.forName(className).asSubclass(YamlPropertyTranslator.class).newInstance();
                    }
                }
                else {
                    translator = defaultTranslator;
                }
                if (translator != null) {
                    Map<String,Object> ruleProps = translators.get(translator);
                    if (ruleProps == null) {
                        ruleProps = new LinkedHashMap<>();
                        translators.put(translator, ruleProps);
                    }
                    Object propValue = properties.getProperty(name);
                    try {
                        // integers should be treated as such
                        propValue = Integer.parseInt(propValue.toString());
                    }
                    catch (NumberFormatException ex) {
                    }
                    ruleProps.put(rule, propValue);
                }
            }
        }

        // perform translations
        YamlBuilder yamlBuilder = new YamlBuilder();
        for (YamlPropertyTranslator translator : translators.keySet()) {
            yamlBuilder.append(translator.translate(translators.get(translator))).newLine();
        }
        return yamlBuilder;
    }
}
