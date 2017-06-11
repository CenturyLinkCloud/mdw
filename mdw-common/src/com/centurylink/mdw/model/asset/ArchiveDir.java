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
package com.centurylink.mdw.model.asset;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;

/**
 * Read-only jsonable representing an asset archive directory.
 */
public class ArchiveDir implements Jsonable {

    private String name;
    private List<String> metas;
    private List<String> files;

    public ArchiveDir(File dir) {
        name = dir.getName();
        metas = new ArrayList<>();
        File metaDir = new File(dir + "/.mdw");
        if (metaDir.isDirectory()) {
            for (File metaFile : metaDir.listFiles())
                metas.add(metaFile.getName());
        }
        files = new ArrayList<>();
        for (File file : dir.listFiles()) {
            if (!file.getName().equals(".mdw"))
                files.add(file.getName());
        }
    }

    @Override
    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        if (metas != null) {
            JSONArray metaArr = new JSONArray();
            for (String meta : metas)
                metaArr.put(meta);
            json.put("metas", metas);
        }
        if (files != null) {
            JSONArray fileArr = new JSONArray();
            for (String file : files)
                fileArr.put(file);
            json.put("files", files);
        }
        return json;
    }

    @Override
    public String getJsonName() {
        return name;
    }
}
