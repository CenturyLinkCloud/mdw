package com.centurylink.mdw.config;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Translates properties based on rules to YAML.
 * (See also configurations.map in compatibility).
 *
 * Slash separators indicate nested elements:
 * <code>
 *   database/driver=org.mariadb.jdbc.Driver
 *   database/url=jdbc:mariadb://localhost:3308/mdw
 * </code>
 * creates:
 * <code>
 *   database:
 *     driver: org.mariadb.jdbc.Driver
 *     url: jdbc:mariadb://localhost:3308/mdw
 * </code>
 *
 * Parentheses represent a list:
 * <code>
 *   filepanel/(root.dirs)=./logs,./config
 * </code>
 * creates:
 * <code>
 * filepanel:
 *   root.dirs:
 *   - ./logs
 *   - ./config
 * </code>
 */
public class DefaultYamlTranslator implements YamlPropertyTranslator {

    Map<String,Object> top = new LinkedHashMap<>();

    @Override
    public YamlBuilder translate(Map<String,Object> ruleProps) {
        for (String rule : ruleProps.keySet()) {
            process(rule, ruleProps.get(rule), top);
        }
        return new YamlBuilder(top);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    protected void process(String rule, Object value, Object root) {
        if (!(root instanceof Map)) {
            System.err.println("Warning: non-hierarchical subelement '" + rule + "' -- not converted");
        }
        else {
            int slash = rule.indexOf('/');
            if (slash > 0) {
                String name = rule.substring(0, slash);
                Object obj = ((Map)root).get(name);
                if (obj == null) {
                    obj = new LinkedHashMap<>();
                    ((Map)root).put(name, obj);
                }
                process(rule.substring(slash + 1), value, obj);
            }
            else {
                if (rule.startsWith("(")) {
                    List list = Arrays.asList(value.toString().split("\\s*,\\s*"));
                    ((Map)root).put(rule.substring(1, rule.length() - 1), list);
                }
                else {
                    ((Map)root).put(rule, value);
                }
            }
        }
    }
}
