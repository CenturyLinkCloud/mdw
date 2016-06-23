/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.spring;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.core.io.AbstractResource;

import com.centurylink.mdw.common.cache.impl.RuleSetCache;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;

/**
 * TODO: move to mdw-services
 */
public class WorkflowAssetSpringResource extends AbstractResource {

    private String ruleSetPath;

    public WorkflowAssetSpringResource(String ruleSetPath) {
        this.ruleSetPath = ruleSetPath;
    }

    public RuleSetVO getRuleSet() {
        return RuleSetCache.getRuleSet(ruleSetPath, RuleSetVO.SPRING);
    }

    @Override
    public String getDescription() {
        return getRuleSet().getLabel();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(getRuleSet().getRuleSet().getBytes());
    }

    @Override
    public boolean exists() {
        return getRuleSet() != null;
    }

    @Override
    public long contentLength() throws IOException {
        return getRuleSet().getRuleSet().length();
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
