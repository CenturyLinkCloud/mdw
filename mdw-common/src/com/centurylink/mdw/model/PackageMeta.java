package com.centurylink.mdw.model;

import com.centurylink.mdw.yaml.YamlLoader;

import java.io.IOException;
import java.util.Map;

/**
 * Info from package.yaml.
 * TODO: dependencies
 */
public class PackageMeta {

    private String name;
    public String getName() { return name; }

    public PackageMeta(String name) {
        this.name = name;
    }

    public PackageMeta(byte[] yaml) throws IOException {
        YamlLoader loader = new YamlLoader(new String(yaml));
        Map<?,?> top = (Map<?,?>)loader.getTop();
        name = loader.get("name", top);
        version = loader.get("version", top);
        schemaVersion = loader.get("schemaVersion", top);
        icon = loader.get("icon", top);
        provider = loader.get("provider", top);

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

    @Override
    public String toString() {
        return name + (version == null ? "" : " v" + version);
    }

}
