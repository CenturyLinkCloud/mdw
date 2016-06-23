/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.camel.plugins.tomcat;

import java.io.IOException;

import javax.naming.NamingException;

import org.apache.camel.CamelContext;
import org.apache.camel.spring.CamelContextFactoryBean;

import com.centurylink.mdw.common.spring.SpringAppContext;

public class TomcatCamelContext {

    private CamelContext camelContext;

    public TomcatCamelContext() {
        if (camelContext == null) {
            try {
                CamelContextFactoryBean factoryBean = (CamelContextFactoryBean) SpringAppContext
                        .getInstance().getApplicationContext()
                        .getBean(org.apache.camel.spring.CamelContextFactoryBean.class);
                camelContext = factoryBean.getContext(true);
            }
            catch (IOException ex) {
                NamingException ne = new NamingException(ex.getMessage());
                ne.setRootCause(ex);
                this.camelContext = null;
            }
        }
    }

    public CamelContext getCamelContext() throws NamingException {
        return this.camelContext;
    }

}
