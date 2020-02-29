package com.centurylink.mdw.model;

import com.centurylink.mdw.yaml.YamlLoader;
import org.yaml.snakeyaml.error.YAMLException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Info from package.yaml.
 */
public class PackageMeta {

    private String name;
    public String getName() { return name; }

    public PackageMeta(String name) {
        this.name = name;
    }

    public PackageMeta(byte[] yaml) throws YAMLException {
        YamlLoader loader = new YamlLoader(new String(yaml));
        Map<?,?> top = (Map<?,?>)loader.getTop();
        name = loader.getRequired("name", top, "");
        version = loader.getRequired("version", top, "");
        schemaVersion = loader.getRequired("schemaVersion", top, "");
        icon = loader.get("icon", top);
        provider = loader.get("provider", top);
        List<?> deps = loader.getList("dependencies", top);
        if (deps != null) {
            dependencies = new ArrayList<>();
            for (Object dep : deps)
                dependencies.add(dep.toString());
        }
    }

    private String version;
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    private String schemaVersion;
    public String getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(String version) { this.schemaVersion = version; }

    private String icon;
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    private String provider;
    public String getProvider() { return provider; }

    private List<String> dependencies;
    public List<String> getDependencies() { return dependencies; }

    @Override
    public String toString() {
        return name + (version == null ? "" : " v" + version);
    }
}
