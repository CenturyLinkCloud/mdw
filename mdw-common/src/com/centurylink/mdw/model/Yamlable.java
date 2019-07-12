package com.centurylink.mdw.model;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.util.LinkedHashMap;
import java.util.Map;

public interface Yamlable {

    Map<String,Object> getYaml();

    static Map<String,Object> create() {
        return new LinkedHashMap<>(); // sorted keys
    }

    @SuppressWarnings("unchecked")
    static Map<String,Object> fromString(String yaml) {
        return (Map<String,Object>)new Yaml().load(yaml);
    }

    static String toString(Yamlable yamlable, int indent) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(indent);
        options.setSplitLines(false);
        return new Yaml(options).dump(yamlable.getYaml());
    }
}
