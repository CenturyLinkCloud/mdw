/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
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
