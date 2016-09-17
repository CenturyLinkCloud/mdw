/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.dataaccess.file;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.xmlbeans.XmlException;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.utilities.JsonUtil;
import com.centurylink.mdw.model.value.attribute.AttributeVO;
import com.centurylink.mdw.model.value.process.PackageVO;

/**
 * TODO: Import/Export override attributes.
 */
public class ImporterExporterJson {

    public List<PackageVO> importPackages(String packagesContent) throws JSONException {
        JSONObject packagesJson = new JSONObject(packagesContent).getJSONObject("packages");
        List<PackageVO> packages = new ArrayList<PackageVO>();
        Map<String,JSONObject> pkgObjects = JsonUtil.getJsonObjects(packagesJson);
        for (String pkgName : pkgObjects.keySet()) {
            JSONObject pkgJson = pkgObjects.get(pkgName);
            PackageVO pkg = new PackageVO(pkgJson);
            pkg.setName(pkgName);
            packages.add(pkg);
        }
        return packages;
    }

    public String exportPackages(List<PackageVO> packages) throws JSONException, XmlException {
        JSONObject packagesJson = new JSONObject();
        for (PackageVO pkg : packages) {
            JSONObject pkgJson = pkg.getJson();
            List<AttributeVO> metaAttributes = pkg.getMetaAttributes();
            if (metaAttributes != null)
                pkgJson.put("attributes", JsonUtil.getAttributesJson(metaAttributes, true));
            packagesJson.put(pkg.getJsonName(), pkgJson);
        }
        JSONObject json = new JSONObject();
        json.put("packages", packagesJson);
        return json.toString(2);
    }

}
