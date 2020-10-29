package com.centurylink.mdw.config;

import java.util.Map;

/**
 * Translate old-style java properties to yaml.
 */
@FunctionalInterface
public interface YamlPropertyTranslator {

    /**
     * Map key is rule.  Value is property value.
     */
    public YamlBuilder translate(Map<String,Object> ruleProps);

}
