package com.centurylink.mdw.model.system;

import java.util.Map;
import java.util.TreeMap;

public class Mbean {

    private String domain;
    public String getDomain() { return domain; }

    private String name;
    public String getName() { return name; }

    private String type;
    public String getType() { return type; }

    private TreeMap<String,String> values = new TreeMap<>();
    public Map<String,String> getValues() { return values; }

    public Mbean(String domain, String name, String type) {
        this.domain = domain;
        this.name = name;
        this.type = type;
    }
}
