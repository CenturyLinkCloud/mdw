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
