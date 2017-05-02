/**
 * Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.camel;

import java.io.IOException;

import javax.naming.NamingException;

import org.apache.camel.CamelContext;
import org.apache.camel.spring.CamelContextFactoryBean;

import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.spring.SpringAppContext;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.model.workflow.Package;

public class TomcatCamelContext {

    private CamelContext camelContext;
    public static TomcatCamelContext instance;
    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private TomcatCamelContext() {
        if (camelContext == null) {
            CamelContextFactoryBean factoryBean = null;
            Package packageVO = PackageCache.getPackage("com.centurylink.mdw.camel");
            try {
                if (packageVO != null) {
                    factoryBean = (CamelContextFactoryBean) SpringAppContext
                            .getInstance().getApplicationContext(packageVO).getBean(org.apache.camel.spring.CamelContextFactoryBean.class);
                }
                else {
                    logger.warn("Please import com.centurylink.mdw.camel package in your workspace");
                }
            }
            catch (IOException e) {
                logger.severeException(e.getMessage(), e);
            }
            if (factoryBean != null)
                camelContext = factoryBean.getContext(true);
        }
    }
    public static TomcatCamelContext getInstance()
    {
        if (instance == null)
            instance = new TomcatCamelContext();
        return instance;
    }

    public CamelContext getCamelContext() throws NamingException {
        return getInstance().camelContext;
    }

    /**
     * Method can be invoked when the server
     * shuts down
     */
    public void onShutdown(){
        if (instance==null) return;
        try {
            this.camelContext.stop();
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
        }
    }

}
