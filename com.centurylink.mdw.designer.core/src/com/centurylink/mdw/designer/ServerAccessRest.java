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
package com.centurylink.mdw.designer;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.dataaccess.DataAccessOfflineException;
import com.centurylink.mdw.designer.utils.RestfulServer;

public class ServerAccessRest {

    private RestfulServer server;
    protected RestfulServer getServer() { return server; }

    public ServerAccessRest(RestfulServer server) {
        this.server = server;
    }

    protected String invokeResourceService(String path) throws IOException, DataAccessException {
        return server.invokeResourceService(path);
    }

    /**
     * Throws an exception on non-success response.
     */
    protected void invokeActionService(String request) throws DataAccessException {
        try {
            server.invokeService(request);
        }
        catch (RemoteException ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    /**
     * Throws an exception on non-success response.
     */
    protected void invokeActionService(String request, String user, String password) throws DataAccessException {
        try {
            server.invokeService(request, user, password);
        }
        catch (RemoteException ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    /**
     * Returns empty string if null or empty.
     */
    protected String queryParams(Map<String,String> criteria) throws UnsupportedEncodingException {
        String params = "";
        if (criteria != null) {
            Set<String> keys = criteria.keySet();
            int i = 0;
            for (String key : keys) {
                params += key + "=";
                params += URLEncoder.encode(criteria.get(key), "UTF-8");
                if (i < keys.size() - 1)
                    params += "&";
                i++;
            }
        }
        return params;
    }

    protected String getServiceBaseUrl() {
        return server.getMdwWebUrl();
    }

    private DataAccessOfflineException serverOfflineException;
    public DataAccessOfflineException getDataAccessOfflineException() { return serverOfflineException; }

    private Boolean serverOnline;
    public boolean isOnline() throws DataAccessException {
        if (serverOnline == null) {
            try {
                server.getAppSummary();
                serverOnline = true;
            }
            catch (IOException ex) {
                serverOnline = false;
                serverOfflineException = new DataAccessOfflineException("Server unavailable: " + getServiceBaseUrl(), ex);
            }
            catch (Exception ex) {
                throw new DataAccessException("Error communicating with server: " + ex.getMessage(), ex);
            }
        }
        return serverOnline;
    }

    public Map<String,String> getStandardParams() {
        Map<String,String> params = new HashMap<String,String>();
        params.put("app", "mdw");
        params.put("authUser", getServer().getUser());
        return params;
    }

}
