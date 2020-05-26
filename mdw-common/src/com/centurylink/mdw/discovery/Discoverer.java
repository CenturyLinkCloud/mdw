package com.centurylink.mdw.discovery;

import com.centurylink.mdw.model.workflow.PackageMeta;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface Discoverer {

    String getAssetPath() throws IOException;

    /**
     * Excludes MDW packages unless GitHub public repo.
     */
    List<String> getPackages() throws IOException;
    Map<String,PackageMeta> getPackageInfo() throws IOException;
    PackageMeta findPackage(String name, String version) throws IOException;
}
