package com.centurylink.mdw.model.workflow;

import com.centurylink.mdw.model.Yamlable;
import com.centurylink.mdw.model.system.MdwVersion;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Info from package.yaml.
 */
public class PackageMeta implements Yamlable {

    public static final String META_DIR = ".mdw";
    public static final String PACKAGE_YAML = "package.yaml";
    public static final String PACKAGE_YAML_PATH = META_DIR + "/" + PACKAGE_YAML;
    public static final String VERSIONS = "versions";
    public static final String VERSIONS_PATH = META_DIR + "/" + VERSIONS;

    private String name;
    public String getName() { return name; }

    public PackageMeta(String name) {
        this.name = name;
    }

    public PackageMeta(Map<String,Object> yaml) throws IOException {
        if (yaml.containsKey("name"))
            name = (String)yaml.get("name");
        else
            throw new IOException("Missing: name");
        if (yaml.containsKey("version"))
            version = new MdwVersion((String)yaml.get("version"));
        else
            throw new IOException("Missing: version");
        if (yaml.containsKey("schemaVersion"))
            schemaVersion = (String)yaml.get("schemaVersion");
        else
            throw new IOException("Missing: schemaVersion");
        if (yaml.containsKey("icon"))
            icon = (String)yaml.get("icon");
        if (yaml.containsKey("provider"))
            provider = (String)yaml.get("provider");
        if (yaml.containsKey("dependencies"))
            dependencies = (List<String>)yaml.get("dependencies");
    }

    private MdwVersion version;
    public MdwVersion getVersion() {
        return this.version == null ? new MdwVersion(0) : this.version;
    }
    public void setVersion(MdwVersion version){
        this.version = version;
    }

    private String schemaVersion;
    public String getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(String version) { this.schemaVersion = version; }

    private String provider;
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    private String icon;
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    private List<String> dependencies;
    public List<String> getDependencies() { return dependencies; }

    private String workgroup;
    public String getWorkgroup() {
        return workgroup;
    }
    public void setWorkgroup(String workgroup) { this.workgroup = workgroup; }

    @Override
    public String toString() {
        return name + " v" + version;
    }

    @Override
    public Map<String,Object> getYaml() {
        Map<String,Object> yaml = Yamlable.create();
        yaml.put("name", name);
        yaml.put("version", version.toString());
        yaml.put("schemaVersion", schemaVersion);
        if (provider != null)
            yaml.put("provider", provider);
        if (icon != null)
            yaml.put("icon", icon);
        if (dependencies != null)
            yaml.put("dependencies", dependencies);
        if (workgroup != null)
            yaml.put("workgroup", workgroup);

        return yaml;
    }
}
