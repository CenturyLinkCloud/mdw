/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
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
