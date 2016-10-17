/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.container;

import javax.naming.NamingException;
import javax.sql.DataSource;

public interface DataSourceProvider {

	String MDW = "MDW";
	String JBOSS = "JBoss";
    String TOMCAT = "Tomcat";

	public DataSource getDataSource(String dataSourceName)
	throws NamingException;

}
