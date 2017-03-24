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
