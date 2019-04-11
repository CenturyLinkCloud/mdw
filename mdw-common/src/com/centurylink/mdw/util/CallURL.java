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
package com.centurylink.mdw.util;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

public class CallURL {

    private String action;
    private Map<String,String> params;

    public CallURL(String url) {
        int k = url.indexOf('?');
        if (k>0) {
            action = url.substring(0,k);
            try {
                params = parseUrlParameters(url);
            } catch (Exception e) {
                params = new HashMap<String,String>();
            }
        } else {
            action = url;
            params = new HashMap<String,String>();
        }
    }

    private Map<String,String> parseUrlParameters(String urlstring)
        throws MalformedURLException, UnsupportedEncodingException {
        URL url = new URL("http://site/"+urlstring);
        String query = url.getQuery();
        Hashtable<String,String> params = new Hashtable<String,String>();
        if (query!=null) {
            String[] queries = query.split("&");
            for (String q : queries) {
                int k = q.indexOf('=');
                String name, value;
                if (k>0) {
                    name = q.substring(0,k);
                    value = q.substring(k+1);
                } else {
                    name = q;
                    value = "";
                }
                value = java.net.URLDecoder.decode(value, "UTF-8");
    //          System.out.println("   decoded: " + name + "='" + value +"'");
                params.put(name, value);
            }
        }
        return params;
    }

    public void setParameter(String name, String value) {
        params.put(name, value);
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(action);
        int argn = 0;
        for (String param : params.keySet()) {
            String v = params.get(param);
            if (v==null) continue;
            sb.append(argn==0?"?":"&");
            sb.append(param).append("=");
            try {
                sb.append(java.net.URLEncoder.encode(v, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
            }
            argn++;
        }
        return sb.toString();
    }

    public String getAction() {
        return action;
    }

    public void setAction(String v) {
        action = v;
    }

    public Map<String, String> getParameters() {
        return params;
    }

    public String getParameter(String name) {
        return params.get(name);
    }

}
