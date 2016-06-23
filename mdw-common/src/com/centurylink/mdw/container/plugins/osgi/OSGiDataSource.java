/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.container.plugins.osgi;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.centurylink.mdw.container.ContainerContextAware;
import com.centurylink.mdw.container.DataSourceProvider;

public class OSGiDataSource implements DataSourceProvider, ContainerContextAware {

    private BundleContext bundleContext;
    public void setContainerContext(Object context) {
        this.bundleContext = (BundleContext) context;
    }

    public DataSource getDataSource(String dataSourceName) throws NamingException {
        String filter = "(name=" + dataSourceName + ")";
        try {
            ServiceReference[] srs = bundleContext.getServiceReferences(DataSource.class.getName(), filter);
            if (srs == null || srs.length == 0) {
                throw new NamingException("Unbound DataSource: " + dataSourceName);
            }
            return (DataSource) bundleContext.getService(srs[0]);
        }
        catch (Exception ex) {
            NamingException nex = new NamingException(ex.getMessage());
            nex.initCause(ex);
            throw nex;
        }
    }
}
