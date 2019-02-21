package com.centurylink.mdw.discovery;

import com.centurylink.mdw.model.PackageMeta;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface Discoverer {

    String getAssetPath() throws IOException;
    List<String> getPackages() throws IOException;
    Map<String, PackageMeta> getPackageInfo() throws IOException;
}
