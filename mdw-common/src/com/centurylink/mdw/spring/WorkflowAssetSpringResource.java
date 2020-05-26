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
package com.centurylink.mdw.spring;

import com.centurylink.mdw.cache.asset.AssetCache;
import com.centurylink.mdw.model.asset.Asset;
import org.springframework.core.io.AbstractResource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class WorkflowAssetSpringResource extends AbstractResource {

    private String assetPath;

    public WorkflowAssetSpringResource(String assetPath) {
        this.assetPath = assetPath;
    }

    public Asset getAsset() throws IOException {
        return AssetCache.getAsset(assetPath);
    }

    @Override
    public String getDescription() {
        try {
            return getAsset().getLabel();
        } catch (IOException ex) {
            return ex.getMessage();
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(getAsset().getText().getBytes());
    }

    @Override
    public boolean exists() {
        try {
            return getAsset() != null;
        } catch (IOException ex) {
            return false;
        }
    }

    @Override
    public long contentLength() throws IOException {
        return getAsset().getText().length();
    }

    private long lastModified;
    public void setLastModified(long lastmod) {
        this.lastModified = lastmod;
    }
    @Override
    public long lastModified() throws IOException {
        return this.lastModified;
    }



}
