/*
 * Copyright (C) 2018 CenturyLink, Inc.
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
package com.centurylink.mdw.camel;

import java.util.Map;

import org.apache.camel.Endpoint;
import org.apache.camel.impl.DefaultComponent;

public class MdwComponent extends DefaultComponent {

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String,Object> parameters) throws Exception {
        MdwEndpoint endpoint = new MdwEndpoint(uri, remaining, this);
        endpoint.setNamespaces(namespaces);
        return endpoint;
    }

    private Map<String,String> namespaces;
    public Map<String,String> getNamespaces() { return namespaces; }
    public void setNamespaces(Map<String,String> namespaces) { this.namespaces = namespaces; }

}
