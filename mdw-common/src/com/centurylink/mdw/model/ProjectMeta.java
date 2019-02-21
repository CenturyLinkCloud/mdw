package com.centurylink.mdw.model;

import com.centurylink.mdw.yaml.YamlLoader;

import java.io.IOException;
import java.util.Map;

/**
 * Info from project.yaml.
 * TODO: base data, which should be available at runtime
 */
public class ProjectMeta {

    private String mdwVersion;
    public String getMdwVersion() { return mdwVersion; }

    private String assetLocation;
    public String getAssetLocation() { return assetLocation; }

    private String configLocation;
    public String getConfigLocation() { return configLocation; }

    public ProjectMeta(byte[] yaml) throws IOException {
        YamlLoader loader = new YamlLoader(new String(yaml));
        Map<?,?> top = (Map<?,?>)loader.getTop();
        Map<?,?> mdw = loader.getMap("mdw", top);
        if (mdw != null)
            mdwVersion = (String)mdw.get("version");
        Map<?,?> asset = loader.getMap("asset", top);
        if (asset != null)
            assetLocation = (String)asset.get("location");
        Map<?,?> config = loader.getMap("config", top);
        if (config != null)
            configLocation = (String)config.get("location");
    }
}
