/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.testing;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import com.centurylink.mdw.dataaccess.file.AssetFile;
import com.centurylink.mdw.dataaccess.file.PackageDir;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;

public class TestCaseAsset {

    private PackageDir pkgDir;
    public PackageDir getPackageDir() { return pkgDir; }

    private AssetFile assetFile;
    public AssetFile getAssetFile() { return assetFile; }

    private RuleSetVO ruleSet;

    public TestCaseAsset(PackageDir pkgDir, AssetFile assetFile) {
        this.pkgDir = pkgDir;
        this.assetFile = assetFile;
    }

    /**
     * Non-VCS assets.
     */
    public TestCaseAsset(RuleSetVO ruleSet) {
        this.ruleSet = ruleSet;
    }

    public boolean isVcs() {
        return assetFile != null;
    }


    public String getName() {
        if (isVcs())
            return assetFile.getName();
        else
            return ruleSet.getName();
    }

    public String getPackageName() {
        if (isVcs())
            return pkgDir.getPackageName();
        else
            return ruleSet.getPackageName();
    }

    public boolean exists() {
        if (isVcs())
            return assetFile.exists();
        else
            return ruleSet.getRuleSet() != null;
    }

    public File file() {
        return assetFile;
    }

    public String getText() throws IOException {
        return text();
    }

    public String text() throws IOException {
        if (isVcs())
            return new String(read(file()));
        else
            return ruleSet.getRuleSet();
    }

    private byte[] read(File file) throws IOException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            byte[] bytes = new byte[(int) file.length()];
            fis.read(bytes);
            return bytes;
        }
        finally {
            if (fis != null)
                fis.close();
        }
    }

    public String toString() {
        if (isVcs())
            return assetFile.toString();
        else
            return (ruleSet.getPackageName() == null ? "" : (ruleSet.getPackageName() + "/")) + ruleSet.getName();
    }

}
