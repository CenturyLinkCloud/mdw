/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.spring;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.core.io.Resource;
import org.springframework.osgi.context.support.OsgiBundleXmlApplicationContext;

/**
 * TODO: move to mdw-services
 */
public class MdwOsgiBundleContext extends OsgiBundleXmlApplicationContext {

    public static final String MDW_SPRING_PREFIX = "mdw:spring/";

    public MdwOsgiBundleContext(String[] configLocs, OsgiBundleXmlApplicationContext appContext) {
        super(configLocs, appContext);
        setBundleContext(appContext.getBundleContext());
    }

    private Map<String,Resource> resources = new HashMap<String,Resource>();

    @Override
    public Resource getResource(String location) {
        if (location != null && location.startsWith(MDW_SPRING_PREFIX)) {
            String path = location.substring(MDW_SPRING_PREFIX.length());
            Resource resource = new WorkflowAssetSpringResource(path);
            resources.put(path, resource);
            return resource;
        }
        return super.getResource(location);
    }

    @Override
    public Resource[] getResources(String locationPattern) throws IOException {
        if (locationPattern != null && locationPattern.startsWith(MDW_SPRING_PREFIX))
            return new Resource[] {getResource(locationPattern)};
        else
            return super.getResources(locationPattern);
    }

    @Override
    public void refresh() throws BeansException, IllegalStateException {
        super.refresh();
    }
}
