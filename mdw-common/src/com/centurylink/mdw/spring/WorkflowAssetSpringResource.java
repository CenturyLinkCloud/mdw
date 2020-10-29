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
