/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.container.plugins.jboss;

import javax.naming.NamingException;
import javax.sql.DataSource;

import com.centurylink.mdw.container.DataSourceProvider;

public class JBossDataSource implements DataSourceProvider {

	public DataSource getDataSource(String dataSourceName) throws NamingException {
        return (DataSource) (new JBossNaming()).lookup(null,dataSourceName,DataSource.class);
    }
}
