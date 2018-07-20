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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.core.io.AbstractResource;

import com.centurylink.mdw.cache.impl.AssetCache;
import com.centurylink.mdw.model.asset.Asset;

/**
 * TODO: move to mdw-services
 */
public class WorkflowAssetSpringResource extends AbstractResource {

    private String assetPath;

    public WorkflowAssetSpringResource(String assetPath) {
        this.assetPath = assetPath;
    }

    public Asset getAsset() {
        return AssetCache.getAsset(assetPath, Asset.SPRING);
    }

    @Override
    public String getDescription() {
        return getAsset().getLabel();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(getAsset().getStringContent().getBytes());
    }

    @Override
    public boolean exists() {
        return getAsset() != null;
    }

    @Override
    public long contentLength() throws IOException {
        return getAsset().getStringContent().length();
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
