package com.centurylink.mdw.container.plugin.tomcat;

import java.io.IOException;

import javax.naming.NamingException;
import javax.sql.DataSource;

import com.centurylink.mdw.container.plugin.MdwDataSource;
import com.centurylink.mdw.spring.SpringAppContext;

public class TomcatDataSource extends MdwDataSource {

    public DataSource getDataSource(String dataSourceName) throws NamingException {
        if (dataSources.get(dataSourceName) == null) {
            try {
                dataSources.put(dataSourceName, (DataSource)SpringAppContext.getInstance().getBean(dataSourceName));
            }
            catch (IOException ex) {
                NamingException ne = new NamingException(ex.getMessage());
                ne.setRootCause(ex);
                throw ne;
            }
        }
        return dataSources.get(dataSourceName);
    }
}
