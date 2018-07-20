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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.core.io.Resource;

public class MdwCloudAppContext extends AbstractXmlApplicationContext {

    public static final String MDW_SPRING_URL_PREFIX = "mdw:spring/";

    private Map<String,Resource> resources = new HashMap<String,Resource>();

    MdwCloudAppContext(String url, ApplicationContext parent) {
        super(parent);
        super.setConfigLocation(url);
    }

    @Override
    public Resource getResource(String location) {
        if (location != null && location.startsWith(MDW_SPRING_URL_PREFIX)) {
            String path = location.substring(MDW_SPRING_URL_PREFIX.length());
            Resource resource = new WorkflowAssetSpringResource(path);
            resources.put(path, resource);
            return resource;
        }
        return super.getResource(location);
    }

    @Override
    public Resource[] getResources(String locationPattern) throws IOException {
        if (locationPattern != null && locationPattern.startsWith(MDW_SPRING_URL_PREFIX))
            return new Resource[] {getResource(locationPattern)};
        else
            return super.getResources(locationPattern);
    }

    @Override
    public void refresh() throws BeansException, IllegalStateException {
        super.refresh();
    }
}

