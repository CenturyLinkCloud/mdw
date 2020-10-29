package com.centurylink.mdw.container.plugin;

import java.util.concurrent.ConcurrentHashMap;

import javax.naming.NamingException;
import javax.sql.DataSource;

import com.centurylink.mdw.cache.CacheService;
import com.centurylink.mdw.container.DataSourceProvider;

public class MdwDataSource implements DataSourceProvider, CacheService {

    protected static ConcurrentHashMap<String,DataSource> dataSources = new ConcurrentHashMap<String,DataSource>();

    public DataSource getDataSource(String dataSourceName) throws NamingException {
        DataSource dataSource = dataSources.get(dataSourceName);
        if (dataSource==null)
            throw new NamingException("Data source is not found: " + dataSourceName);
        return dataSource;
    }

    public void setDataSource(String dataSourceName, DataSource dataSource) {
        dataSources.put(dataSourceName, dataSource);
    }

    @Override
    public void refreshCache() throws Exception {
        dataSources.clear();
    }

    @Override
    public void clearCache() {}

}
