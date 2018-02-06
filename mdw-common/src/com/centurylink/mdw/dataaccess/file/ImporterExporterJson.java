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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.xmlbeans.XmlException;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.attribute.Attribute;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.util.JsonUtil;

/**
 * TODO: Import/Export override attributes.
 */
public class ImporterExporterJson {

    public List<Package> importPackages(String packagesContent) throws JSONException {
        JSONObject packagesJson = new JsonObject(packagesContent).getJSONObject("packages");
        List<Package> packages = new ArrayList<Package>();
        Map<String,JSONObject> pkgObjects = JsonUtil.getJsonObjects(packagesJson);
        for (String pkgName : pkgObjects.keySet()) {
            JSONObject pkgJson = pkgObjects.get(pkgName);
            Package pkg = new Package(pkgJson);
            pkg.setName(pkgName);
            packages.add(pkg);
        }
        return packages;
    }

    public String exportPackages(List<Package> packages) throws JSONException, XmlException {
        JSONObject packagesJson = new JsonObject();
        for (Package pkg : packages) {
            JSONObject pkgJson = pkg.getJson();
            List<Attribute> metaAttributes = pkg.getMetaAttributes();
            if (metaAttributes != null)
                pkgJson.put("attributes", JsonUtil.getAttributesJson(metaAttributes, true));
            packagesJson.put(pkg.getJsonName(), pkgJson);
        }
        JSONObject json = new JsonObject();
        json.put("packages", packagesJson);
        return json.toString(2);
    }
}
