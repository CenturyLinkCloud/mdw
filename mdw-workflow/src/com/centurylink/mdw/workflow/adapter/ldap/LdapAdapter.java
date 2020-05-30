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
package com.centurylink.mdw.workflow.adapter.ldap;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.activity.types.AdapterActivity;
import com.centurylink.mdw.adapter.AdapterException;
import com.centurylink.mdw.adapter.ConnectionException;
import com.centurylink.mdw.annotations.Activity;
import com.centurylink.mdw.common.MdwException;
import com.centurylink.mdw.config.PropertyException;
import com.centurylink.mdw.model.user.User;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.translator.JsonTranslator;
import com.centurylink.mdw.util.ExpressionUtil;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.variable.VariableTranslator;
import com.centurylink.mdw.workflow.adapter.ObjectAdapterActivity;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.*;
import java.util.*;

@Tracked(LogLevel.TRACE)
@Activity(value="LDAP Adapter", category=AdapterActivity.class, icon="com.centurylink.mdw.base/adapter.png",
        pagelet="com.centurylink.mdw.base/ldap.pagelet")
public class LdapAdapter extends ObjectAdapterActivity {

    public static final String LDAP_HOST = "LdapHost";
    public static final String LDAP_PORT = "LdapPort";
    public static final String BASE_DN = "BaseDn";
    public static final String APP_USER = "AppCuid";
    @SuppressWarnings("squid:S2068")
    public static final String APP_PASSWORD = "AppPassword";
    public static final String SEARCH_CONTEXT = "SearchContext";
    public static final String SEARCH_FILTER = "SearchFilter";
    public static final String BINDINGS = "Bindings";

    @Override
    public final boolean isSynchronous() {
        return true;
    }

    /**
     * Returns an LDAP connection based on the configured host, port and dn.
     */
    @Override
    protected Object openConnection() throws ConnectionException {
        try {
            String ldapHost = getAttributeValueSmart(LDAP_HOST);
            String ldapPort = getAttributeValueSmart(LDAP_PORT);
            String baseDn = getAttributeValueSmart(BASE_DN);
            String appCuid = getAttributeValueSmart(APP_USER);
            String appPassword = getAttributeValueSmart(APP_PASSWORD);
            String ldapUrl = "ldap://" + ldapHost + ":" + ldapPort + "/" + baseDn;

            Hashtable<String,String> env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, ldapUrl);
            env.put(Context.SECURITY_PROTOCOL, "ssl");
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            env.put(Context.SECURITY_PRINCIPAL, "uid=" + appCuid + ",ou=people," + baseDn);
            env.put(Context.SECURITY_CREDENTIALS, appPassword);
            return new InitialDirContext(env);
        }
        catch (Exception ex) {
            throw new ConnectionException(ConnectionException.CONNECTION_DOWN, ex.getMessage(), ex);
        }
    }

    @Override
    protected void closeConnection(Object connection) {
        DirContext dirContext = (DirContext) connection;
        try {
            dirContext.close();
        }
        catch (Exception ex) {
            getLogger().error(ex.getMessage(), ex);
        }
    }

    /**
     * Builds the LDAP query.
     */
    @Override
    protected Object getRequestData() throws ActivityException {
        String rawSearchFilterAttr = getAttributeValue(SEARCH_FILTER);
        if (valueIsJavaExpression(rawSearchFilterAttr)) {
            try {
                return getAttributeValueSmart(SEARCH_FILTER);
            }
            catch (PropertyException ex) {
                throw new ActivityException(ex.getMessage(), ex);
            }
        }
        else
            return substitute(rawSearchFilterAttr);
    }

    /**
     * Sends the request to the LDAP server.
     */
    @Override
    public Object invoke(Object conn, Object requestData) throws AdapterException
    {
        try {
            DirContext dirContext = (DirContext) conn;
            String searchFilter = (String) requestData;

            String searchContext = getAttributeValueSmart(SEARCH_CONTEXT);
            SearchControls searchControls = new SearchControls();
            searchControls.setTimeLimit(20000);
            searchControls.setCountLimit(200);

            return dirContext.search(searchContext, searchFilter, searchControls);
        }
        catch (Exception ex) {
            throw new AdapterException(-1, ex.getMessage() , ex);
        }
    }

    private Map<String,List<Object>> ldapResults = null;

    @Override
    @SuppressWarnings("unchecked")
    protected void handleAdapterSuccess(Object response) throws ActivityException, AdapterException {

        NamingEnumeration<SearchResult> results = (NamingEnumeration<SearchResult>) response;
        Map<String,String> bindings = getAttributes().getMap(BINDINGS);
        ldapResults = new HashMap<>();

        try {
            while (results.hasMore()) {
                 SearchResult result = results.next();
                 Attributes attributes = result.getAttributes();
                 if (getLogger().isMdwDebugEnabled())
                     getLogger().mdwDebug("LDAP Attributes retrieved: " + attributes);
                 NamingEnumeration<String> attrIds = attributes.getIDs();
                 while (attrIds.hasMore()) {
                     String mappedAttr = attrIds.next();
                     Object val = null;
                     if (attributes.get(mappedAttr) != null) {
                         val = attributes.get(mappedAttr).get();
                     }
                     List<Object> resultList = ldapResults.get(mappedAttr);
                     if (resultList == null) {
                         resultList = new ArrayList<>();
                         ldapResults.put(mappedAttr, resultList);
                     }
                     resultList.add(val);
                 }
            }
            Process processVO = getMainProcessDefinition();


            Variable userJsonVar = null;
            for (String varName : bindings.keySet()) {
                String mappedAttr = bindings.get(varName);
                Variable varVO = processVO.getVariable(varName);
                String varType = varVO.getType();

                VariableTranslator translator = getPackage().getTranslator(varVO.getType());
                if (translator.isDocumentReferenceVariable()) {
                    if (translator instanceof JsonTranslator) {
                        if (getParameterValue(varName) == null) {
                            userJsonVar = varVO;
                        }
                    }
                    else if (varVO.isJavaObject()) {
                        setVariableValue(varName, ldapResults);
                    }
                }
                if (ldapResults.containsKey(mappedAttr)) {
                    List<Object> resultList = ldapResults.get(mappedAttr);
                    if (!resultList.isEmpty()) {
                        if (varType.equals("java.util.List<String>")) {
                            setVariableValue(varName, resultList);
                        }
                        else if (varType.equals("java.util.List<Integer>")) {
                            List<Integer> value = new ArrayList<>();
                            for (Object object : resultList)
                                value.add(new Integer(object.toString()));
                            setVariableValue(varName, value);
                        }
                        else if (varType.equals("java.util.List<Long>")) {
                            List<Long> value = new ArrayList<>();
                            for (Object object : resultList)
                                value.add(new Long(object.toString()));
                            setVariableValue(varName, value);
                        }
                        else {
                            setParameterValue(varName, resultList.get(0));
                        }
                    }
                }
            }
            if (userJsonVar != null) {
                User user = new User();
                for (String key : ldapResults.keySet()) {
                    List<Object> list = ldapResults.get(key);
                    if (list != null && list.size() > 0) {
                        String value = list.get(0) == null ? null : list.get(0).toString();
                        if (value != null) {
                            if (key.equals(LDAP_KEY_CUID))
                                user.setCuid(value);
                            else if (key.equals(LDAP_KEY_FIRST_NAME))
                                user.setName(user.getName() == null ? value : value + " " + user.getName());
                            else if (key.equals(LDAP_KEY_LAST_NAME))
                                user.setName(user.getName() == null ? value : user.getName() + " " + value);
                            else
                                user.setAttribute(key, value);
                        }
                    }
                }

                setParameterValueAsDocument(userJsonVar.getName(), userJsonVar.getType(), user.getJson());
            }
        }
        catch (ActivityException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new ActivityException(ex.getMessage(), ex);
        }
    }

    public static final String LDAP_KEY_CUID = "uid";
    public static final String LDAP_KEY_SAPID = "saploginid";
    public static final String LDAP_KEY_MNETID = "mnetid";
    public static final String LDAP_KEY_FIRST_NAME = "givenName";
    public static final String LDAP_KEY_LAST_NAME = "sn";
    public static final String LDAP_KEY_TYPE = "employeetype";

    /**
     * Replaces expressions in an attribute value.  This is used instead of
     * the logic in getAttributeValueSmart() since the Mbeng parser does not
     * like symbols like '=' inside the attribute.  TODO: still needed?
     *
     * @param input raw attribute value
     * @return value with expressions substituted
     */
    protected String substitute(String input) throws ActivityException {

        try {
            return ExpressionUtil.substitute(input, getParameters());
        }
        catch (MdwException ex) {
            throw new ActivityException(ex.getMessage(), ex);
        }
    }

    @Override
    protected Map<String,Object> getPostScriptBindings(Object response) throws ActivityException {
        Map<String,Object> bindings = super.getPostScriptBindings(response);
        if (ldapResults != null)
          bindings.put("results", ldapResults);
        return bindings;
    }
}
