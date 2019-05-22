package com.centurylink.mdw.config;

import com.centurylink.mdw.model.Jsonable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public class PropertyGroup implements Jsonable {

    private String root;
    public String getRoot() { return root; }

    public String name;
    public String getName() { return name; }

    private Properties properties = new Properties();
    public Properties getProperties() { return properties; }

    private List<PropertyGroup> subgroups = new ArrayList<>();
    public List<PropertyGroup> getSubgroups() { return subgroups; }

    public PropertyGroup(String name, String root, Properties properties) {
        this(name, root);
        addProps(properties);
    }

    public PropertyGroup(String name, String root) {
        this.name = name;
        this.root = root;
    }

    private void addProps(Properties props) {
        Map<String,Properties> subprops = new HashMap<>();
        for (String fullname : props.stringPropertyNames()) {
            String name = fullname.substring(root.length() + 1);
            String value = props.getProperty(fullname);
            if (name.contains(".")) {
                String subroot = root + "." + name.substring(0, name.indexOf("."));
                Properties sub = subprops.get(subroot);
                if (sub == null) {
                    sub = new Properties();
                    subprops.put(subroot, sub);
                }
                sub.setProperty(fullname, value);
            }
            else {
                properties.put(name, value);
            }
        }
        if (!subprops.isEmpty()) {
            for (String subroot : subprops.keySet()) {
                Properties subs = subprops.get(subroot);
                String subname = subroot.substring(subroot.indexOf(".", root.length()) + 1);
                subgroups.add(new PropertyGroup(subname, subroot, subs));
            }
        }
    }

    public JSONObject getJson() {
        return getJson(true);
    }

    /**
     * @param collapse collapse empty subgroups
     */
    public JSONObject getJson(boolean collapse) {
        JSONObject json = create();
        json.put("name", name);
        if (!properties.isEmpty()) {
            JSONObject props = create();
            json.put("props", props);
            for (String propName : properties.stringPropertyNames()) {
                props.put(propName, properties.getProperty(propName));
            }
        }
        if (!subgroups.isEmpty()) {
            JSONArray groups = new JSONArray();
            json.put("groups", groups);
            for (PropertyGroup subgroup : subgroups) {
                if (collapse) {
                    for (PropertyGroup collapsedSubgroup : subgroup.collapse()) {
                        groups.put(collapsedSubgroup.getJson(false));
                    }
                }
                else {
                    groups.put(subgroup.getJson(false));
                }
            }
        }

        return json;
    }

    public List<PropertyGroup> collapse() {
        List<PropertyGroup> collapsedGroups = new ArrayList<>();
        if (properties.isEmpty()) {
            for (PropertyGroup subgroup : subgroups) {
                PropertyGroup collapsed = new PropertyGroup(name + "." + subgroup.name, root + "." + subgroup.name);
                for (String subPropName : subgroup.properties.stringPropertyNames()) {
                    collapsed.properties.setProperty(subPropName, subgroup.properties.getProperty(subPropName));
                }
                for (PropertyGroup subsubgroup : subgroup.subgroups) {
                    for (PropertyGroup collapsedSubgroup : subsubgroup.collapse()) {
                        collapsed.subgroups.add(collapsedSubgroup);
                    }
                }
                collapsedGroups.add(collapsed);
            }
        }
        else {
            PropertyGroup collapsed = new PropertyGroup(name, root);
            for (String propName : properties.stringPropertyNames())
                collapsed.properties.setProperty(propName, properties.getProperty(propName));
            for (PropertyGroup subgroup : subgroups) {
                for (PropertyGroup collapsedSubgroup : subgroup.collapse()) {
                    collapsed.subgroups.add(collapsedSubgroup);
                }
            }
            collapsedGroups.add(collapsed);
        }
        return collapsedGroups;
    }

}
