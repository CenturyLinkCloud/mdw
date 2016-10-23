/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.container.plugin.tomcat;

import java.io.IOException;

import javax.naming.NamingException;
import javax.sql.DataSource;

import com.centurylink.mdw.container.DataSourceProvider;
import com.centurylink.mdw.spring.SpringAppContext;

public class TomcatDataSource implements DataSourceProvider {

    private DataSource dataSource;

    public DataSource getDataSource(String dataSourceName) throws NamingException {
        if (dataSource == null) {
            try {
                dataSource = (DataSource)SpringAppContext.getInstance().getApplicationContext().getBean(dataSourceName);
            }
            catch (IOException ex) {
                NamingException ne = new NamingException(ex.getMessage());
                ne.setRootCause(ex);
                throw ne;
            }
        }
        return dataSource;
    }
}
