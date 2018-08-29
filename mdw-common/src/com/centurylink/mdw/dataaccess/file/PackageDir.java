/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.dataaccess.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import com.centurylink.mdw.dataaccess.AssetRevision;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.VersionControl;
import com.centurylink.mdw.dataaccess.file.GitDiffs.DiffType;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.workflow.Package;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value="Package", description="Asset package", parent=Object.class)
public class PackageDir extends File {

    public static final String META_DIR = ".mdw";
    public static final String PACKAGE_JSON_PATH = META_DIR + "/package.json";
    public static final String PACKAGE_YAML_PATH = META_DIR + "/package.yaml";
    public static final String VERSIONS_PATH = META_DIR + "/versions";
    public static final String ARCHIVE_SUBDIR = "Archive";

    private File storageDir; // main parent for workflow assets
    private File archiveDir;
    private VersionControl versionControl;

    /**
     * /com.centurylink.mdw.demo.intro v0.0.17
     */
    private File logicalDir;
    @ApiModelProperty(hidden=true)
    public File getLogicalDir() { return logicalDir; }

    private boolean archive;
    @ApiModelProperty(hidden=true)
    public boolean isArchive() { return archive; }

    private String pkgName;
    @ApiModelProperty(name="name")
    public String getPackageName() { return pkgName; }

    private String pkgVersion;
    @ApiModelProperty(name="version")
    public String getPackageVersion() { return pkgVersion; }
    public void setPackageVersion(String version) { this.pkgVersion = version; }

    private String schemaVersion;
    @ApiModelProperty(name="schemaVersion")
    public String getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(String version) { this.schemaVersion = version; }

    private long pkgId;
    public long getId() { return pkgId; }

    private Boolean yaml;
    public boolean isYaml() { return yaml == null ? false : yaml; }
    public void setYaml(boolean yaml) { this.yaml = yaml; }

    private File metaFile;
    public File getMetaFile() {
        metaFile = new File(toString() + "/" + PACKAGE_YAML_PATH);
        if (!isYaml() && !metaFile.exists())
            metaFile = new File(toString() + "/" + PACKAGE_JSON_PATH);
        return metaFile;
    }

    public boolean parse() throws DataAccessException {
        yaml = new File(toString() + "/" + PACKAGE_YAML_PATH).exists();
        return parse(yaml);
    }

    @SuppressWarnings("rawtypes")
    public boolean parse(boolean yaml) throws DataAccessException {
        try {
            this.yaml = yaml;
            File pkgFile = getMetaFile();
            if (!pkgFile.exists())
                return false;
            if (yaml) {
                Yaml yamlLoader = new Yaml();
                Map map = (Map) yamlLoader
                        .load(new String(Files.readAllBytes(Paths.get(pkgFile.getPath()))));
                pkgName = (String) map.get("name");
                pkgVersion = (String) map.get("version");
                schemaVersion = (String) map.get("schemaVersion");
            }
            else {
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(pkgFile);
                    byte[] bytes = new byte[(int) pkgFile.length()];
                    fis.read(bytes);
                    Package pkgVo = new Package(new JsonObject(new String(bytes)));
                    pkgName = pkgVo.getName();
                    pkgVersion = pkgVo.getVersionString();
                    schemaVersion = Asset.formatVersion(pkgVo.getSchemaVersion());
                }
                finally {
                    if (fis != null)
                        fis.close();
                    ;
                }
            }
            String archivePath = new File(storageDir.toString() + "/Archive/").toString();
            if (toString().startsWith(archivePath)) {
                // e:/workspaces/dons/DonsWorkflow/src/main/workflow/Archive/com.centurylink.mdw.demo.intro v0.17
                int spaceV = toString().lastIndexOf(" v");
                if (spaceV < 0)
                    throw new DataAccessException("Misnamed Archive package node dir (missing version): " + getName());
                String pkgVerFromDir = toString().substring(spaceV + 2);
                if (pkgVerFromDir != null && !pkgVerFromDir.equals(pkgVersion))
                    throw new DataAccessException("Package version in " + pkgFile + " is not '" + pkgVerFromDir + "'");
                String pkgNameFromDir = toString().substring(0, spaceV).substring(archiveDir.toString().length() + 1).replace('\\', '/').replace('/', '.');
                if (!pkgNameFromDir.equals(pkgName))
                    throw new DataAccessException("Package name in " + pkgFile + " is not '" + pkgNameFromDir + "'");
                archive = true;
            }
            else {
                // e:/workspaces/dons/DonsWorkflow/src/main/workflow/com/centurylink/mdw/demo/intro
                String pkgNameFromDir = toString().substring(storageDir.toString().length() + 1).replace('\\', '/').replace('/', '.');
                if (!pkgNameFromDir.equals(pkgName))
                    throw new DataAccessException("Package name in " + pkgFile + " is not '" + pkgNameFromDir + "'");
            }
            logicalDir = new File("/" + pkgName + " v" + pkgVersion);
            if (versionControl != null)
                pkgId = versionControl.getId(logicalDir);
            return true;
        }
        catch (DataAccessException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    public PackageDir(File storageDir, File pkgNode, VersionControl versionControl) {
        super(pkgNode.toString());
        this.storageDir = storageDir;
        this.archiveDir = new File(storageDir + "/" + ARCHIVE_SUBDIR);
        this.versionControl = versionControl;
    }

    /**
     * For newly-created packages.
     */
    public PackageDir(File storageDir, Package packageVo, VersionControl versionControl) throws IOException {
        this(storageDir, new File(storageDir + "/" + packageVo.getName().replace('.', '/')), versionControl);
        this.logicalDir = new File("/" + packageVo.getName() + " v" + packageVo.getVersionString());
        this.pkgId = packageVo.getId() == null ? versionControl.getId(logicalDir) : packageVo.getId();
        this.pkgName = packageVo.getName();
        this.pkgVersion = Package.formatVersion(packageVo.getVersion());
        this.schemaVersion = Asset.formatVersion(packageVo.getSchemaVersion());
    }

    /**
     * For placeholder (eg: missing) packages.
     */
    public PackageDir(File storageDir, String name, VersionControl versionControl) {
        super(storageDir + "/" + name);
        this.pkgName = name;
        this.versionControl = versionControl;
    }

    /**
     * logical file to AssetFile
     */
    private Map<File,AssetFile> assetFiles = new HashMap<File,AssetFile>();

    /**
     * Called after initial load when AssetFiles may be accessed by passing the asset logical file.
     */
    public AssetFile findAssetFile(File logicalFile) {
        return assetFiles.get(logicalFile);
    }

    @ApiModelProperty(hidden=true)
    public AssetFile getAssetFile(File file) throws IOException {
        return getAssetFile(file, null);
    }

    public AssetFile removeAssetFile(File file) {
        return assetFiles.remove(file);
    }

    /**
     * Called during initial load the file param is a standard file.
     */
    @ApiModelProperty(hidden=true)
    public AssetFile getAssetFile(File file, AssetRevision rev) throws IOException {
        AssetFile assetFile;
        if (rev == null) {
            rev = versionControl.getRevision(file);
            if (rev == null) {
                // presumably dropped-in asset
                rev = new AssetRevision();
                rev.setVersion(0);
                rev.setModDate(new Date());
            }
            assetFile = new AssetFile(this, file.getName(), rev);
            assetFile.setRevision(rev);
        }
        else {
            versionControl.setRevision(file, rev);
            assetFile = new AssetFile(this, file.getName(), rev);
            versionControl.clearId(assetFile);
        }
        assetFile.setId(versionControl.getId(assetFile.getLogicalFile()));
        assetFiles.put(assetFile.getLogicalFile(), assetFile);
        return assetFile;
    }

    private DiffType vcsDiffType;
    @ApiModelProperty(hidden=true)
    public DiffType getVcsDiffType() { return vcsDiffType; }
    public void setVcsDiffType(DiffType diffType) { this.vcsDiffType = diffType; }
}
