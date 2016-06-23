/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.container.plugins;

import java.util.HashMap;

import javax.naming.NamingException;
import javax.sql.DataSource;

import com.centurylink.mdw.container.DataSourceProvider;

public class MdwDataSource implements DataSourceProvider {

	private static HashMap<String,DataSource> dataSources;
	
	public DataSource getDataSource(String dataSourceName) throws NamingException {
		DataSource dataSource = dataSources.get(dataSourceName);
		if (dataSource==null)
			throw new NamingException("Data source is not found: " + dataSourceName);
		return dataSource;
	}
	
	public void setDataSource(String dataSourceName, DataSource dataSource) {
		if (dataSources==null) dataSources = new HashMap<String,DataSource>();
		dataSources.put(dataSourceName, dataSource);
	}

}
