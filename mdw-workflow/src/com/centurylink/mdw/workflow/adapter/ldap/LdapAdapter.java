/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.adapter.ldap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.xmlbeans.XmlOptions;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.common.MDWException;
import com.centurylink.mdw.config.PropertyException;
import com.centurylink.mdw.connector.adapter.AdapterException;
import com.centurylink.mdw.connector.adapter.ConnectionException;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.user.Attribute;
import com.centurylink.mdw.user.UserDocument;
import com.centurylink.mdw.user.UserDocument.User;
import com.centurylink.mdw.util.ExpressionUtil;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.workflow.adapter.AdapterActivityBase;

@Tracked(LogLevel.TRACE)
public class LdapAdapter extends AdapterActivityBase {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public static final String LDAP_HOST = "LdapHost";
    public static final String LDAP_PORT = "LdapPort";
    public static final String BASE_DN = "BaseDn";
    public static final String APP_CUID = "AppCuid";
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
            String baseDn = getAttributeValueSmart(BASE_DN);;
            String appCuid = getAttributeValueSmart(APP_CUID);
            String appPassword = getAttributeValueSmart(APP_PASSWORD);
            String ldapUrl = "ldap://" + ldapHost + ":" + ldapPort + "/" + baseDn;

            Hashtable<String,String> env = new Hashtable<String,String>();
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
            logger.severeException(ex.getMessage(), ex);
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
    private UserDocument userDoc = null;

    @Override
    @SuppressWarnings("unchecked")
    protected void handleAdapterSuccess(Object response) throws ActivityException, AdapterException {

        NamingEnumeration<SearchResult> results = (NamingEnumeration<SearchResult>) response;
        Map<String,String> bindings = StringHelper.parseMap(getAttributeValue(BINDINGS));
        ldapResults = new HashMap<String,List<Object>>();

        try {
            while (results.hasMore()) {
                 SearchResult result = results.next();
                 Attributes attributes = result.getAttributes();
                 if (logger.isMdwDebugEnabled())
                     logger.mdwDebug("LDAP Attributes retrieved: " + attributes);
                 NamingEnumeration<String> attrIds = attributes.getIDs();
                 while (attrIds.hasMore()) {
                     String mappedAttr = attrIds.next();
                     Object val = null;
                     if (attributes.get(mappedAttr) != null) {
                         val = attributes.get(mappedAttr).get();
                     }
                     List<Object> resultList = ldapResults.get(mappedAttr);
                     if (resultList == null) {
                         resultList = new ArrayList<Object>();
                         ldapResults.put(mappedAttr, resultList);
                     }
                     resultList.add(val);
                 }
            }
            Process processVO = getMainProcessDefinition();


            Variable userDocVar = null;
            for (String varName : bindings.keySet()) {
                String mappedAttr = bindings.get(varName);
                Variable varVO = processVO.getVariable(varName);
                String varType = varVO.getVariableType();

                if (varVO.isDocument()) {
                    if (varVO.isXmlDocument()) {
                        if (getParameterValue(varName) == null) {
                            userDocVar = varVO;
                        }
                    }
                    else if (varVO.isJavaObject()) {
                        setVariableValue(varName, ldapResults);
                    }
                }
                if (ldapResults.containsKey(mappedAttr)) {
                    List<Object> resultList = ldapResults.get(mappedAttr);
                    if (!resultList.isEmpty()) {
                        if (varType.equals("java.lang.String[]")) {
                            String[] value = new String[resultList.size()];
                            for (int i = 0; i < resultList.size(); i++)
                                value[i] = resultList.get(i) == null ? null : resultList.get(i).toString();
                            setParameterValue(varName, value);
                        }
                        else if (varType.equals("java.lang.Integer[]")) {
                            Integer[] value = new Integer[resultList.size()];
                            for (int i = 0; i < resultList.size(); i++)
                                value[i] = resultList.get(i) == null ? null : new Integer(resultList.get(i).toString());
                            setParameterValue(varName, value);
                        }
                        else if (varType.equals("java.lang.Long[]")) {
                            Long[] value = new Long[resultList.size()];
                            for (int i = 0; i < resultList.size(); i++)
                                value[i] = resultList.get(i) == null ? null : new Long(resultList.get(i).toString());
                            setParameterValue(varName, value);
                        }
                        else {
                            setParameterValue(varName, resultList.get(0));
                        }
                    }
                }
            }
            if (userDocVar != null) {
                userDoc = UserDocument.Factory.newInstance();
                User user = userDoc.addNewUser();
                com.centurylink.mdw.user.Attributes attributes = user.addNewAttributes();
                for (String key : ldapResults.keySet()) {
                    List<Object> list = ldapResults.get(key);
                    if (list != null && list.size() > 0) {
                        String value = list.get(0) == null ? null : list.get(0).toString();
                        if (value != null) {
                            if (key.equals(LDAP_KEY_CUID))
                                user.setCuid(value);
                            else if (key.equals(LDAP_KEY_SAPID))
                                user.setSapId(value);
                            else if (key.equals(LDAP_KEY_MNETID))
                                user.setMnetId(value);
                            else if (key.equals(LDAP_KEY_FIRST_NAME))
                                user.setFirstName(value);
                            else if (key.equals(LDAP_KEY_LAST_NAME))
                                user.setLastName(value);
                            else if (key.equals(LDAP_KEY_TYPE))
                                user.setType(value);

                            Attribute attribute = attributes.addNewAttribute();
                            attribute.setName(key);
                            attribute.setStringValue(value);
                        }
                    }
                }
                XmlOptions opts = new XmlOptions().setSavePrettyPrint().setSavePrettyPrintIndent(2);
                setParameterValueAsDocument(userDocVar.getName(), userDocVar.getVariableType(), userDoc.xmlText(opts));
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
     * like symbols like '=' inside the attribute.
     *
     * @param input raw attribute value
     * @return value with expressions substituted
     */
    protected String substitute(String input) throws ActivityException {

        try {
            return ExpressionUtil.substitute(input, getParameters());
        }
        catch (MDWException ex) {
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
