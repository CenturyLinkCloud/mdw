/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.adapter.osgi;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.connector.adapter.AdapterException;
import com.centurylink.mdw.connector.adapter.ConnectionException;
import com.centurylink.mdw.workflow.adapter.PoolableAdapterBase;

public class OsgiServiceAdapter extends PoolableAdapterBase {

    private String serviceInterface;
    private BundleContext bundleContext;
    private Object[] args;

    public Object openConnection() throws ConnectionException, AdapterException {
        if (!ApplicationContext.isOsgi())
            throw new AdapterException(this.getClass().getName() + " only available in an OSGi container environment");
        // get a handle to the service
        serviceInterface = getServiceInterface();
        if (serviceInterface == null)
            throw new AdapterException("Missing attribute: " + WorkAttributeConstant.SERVICE_INTERFACE);
        if (serviceInterface.endsWith(".java"))
            serviceInterface = serviceInterface.substring(0, serviceInterface.length() - 5);
        serviceInterface = serviceInterface.replace('/', '.');

        bundleContext = ApplicationContext.getOsgiBundleContext();
        if (isLogDebugEnabled())
            logdebug("Accessing service '" + serviceInterface + "' from bundleContext: " + bundleContext);

        return bundleContext.getServiceReference(serviceInterface);
    }

    public void closeConnection(Object connection) {
        ServiceReference serviceReference = (ServiceReference) connection;
        bundleContext.ungetService(serviceReference);
    }

    /**
     * Override this to avoid default behavior since String type is not relevant.
     */
    @Override
    protected String getRequestData() throws ActivityException {
        args = null;
        if (hasPreScript()) {
            Object ret = executePreScript(null);
            if (ret instanceof Object[])
                args = (Object[]) ret;
        }

        return "N/A";
    }

    @Override
    public String invoke(Object connection, String request, int timeout, Map<String,String> headers)
    throws AdapterException, ConnectionException {
        ServiceReference serviceReference = (ServiceReference) connection;

        if (serviceReference == null)
            throw new ConnectionException("Cannot access service: " + serviceInterface);

        String serviceMethod = getServiceMethod();
        if (serviceMethod == null)
            throw new AdapterException("Missing attribute: " + WorkAttributeConstant.SERVICE_METHOD);

        Object service = bundleContext.getService(serviceReference);
        Object resultObj = invokeServiceMethod(service, serviceMethod, getServiceParameters());

        if (resultObj == null)
            return null;

        // update result
        String resultVar = getResultVariable();
        if (resultVar != null) {
            try {
                setVariableValue(resultVar, resultObj);
            }
            catch (ActivityException ex) {
                throw new AdapterException(ex.getMessage(), ex);
            }
        }

        return resultObj.toString();
    }

    protected Object invokeServiceMethod(Object serviceObj, String serviceMethod, Map<String,String> serviceParams) throws AdapterException {

        // serviceMethod eg: createDevice(String type, DeviceVO device)
        String serviceMethodName = serviceMethod.substring(0, serviceMethod.indexOf('(')).trim();

        try {
            Class<?> interfaceClass = Class.forName(serviceInterface);
            Method matchingMethod = null;
            int argCount = serviceParams == null ? 0 : serviceParams.size();
            for (Method method : interfaceClass.getMethods()) {
                if (method.getName().equals(serviceMethodName)) {
                    Class<?>[] paramTypes = method.getParameterTypes();
                    if (argCount == 0) {
                        if (paramTypes.length == 0) {
                          matchingMethod = method;
                          break;
                        }
                    }
                    else if (argCount == paramTypes.length) {
                        boolean match = true;
                        Iterator<String> keysIter = serviceParams.keySet().iterator();
                        for (int i = 0; keysIter.hasNext(); i++) {
                            Class<?> paramType = paramTypes[i];
                            String paramSpec = keysIter.next(); // eg: device (DeviceVO)
                            int openParen = paramSpec.indexOf('(');
                            int closeParen = paramSpec.indexOf(')');
                            if (openParen == -1 || closeParen == -1)
                                throw new AdapterException("Unexpected parameter spec format: " + paramSpec);
                            String paramName = paramSpec.substring(openParen + 1, closeParen);
                            if (!paramType.getSimpleName().equals(paramName)) {
                                match = false;
                                break;
                            }
                        }
                        if (match) {
                            matchingMethod = method;
                            break;
                        }
                    }
                }
            }

            if (matchingMethod == null) {
                String params = null;
                if (serviceParams != null) {
                    Iterator<String> paramsIter = serviceParams.keySet().iterator();
                    for (int i = 0; paramsIter.hasNext(); i++) {
                        params += paramsIter.next();
                        if (i < serviceParams.size() - 1)
                            params += ", ";
                    }
                }
                throw new AdapterException("Unable to locate matching method: " + serviceMethod + "with parameters [" + params + "] in interface " + serviceInterface);
            }

            if (args == null) {
                // not populated by prescript
                args = new Object[argCount];
                Iterator<String> valuesIter = serviceParams.values().iterator();
                for (int i = 0; valuesIter.hasNext(); i++) {
                    String expression = valuesIter.next();
                    args[i] = getRuntimeContext().evaluate(expression);
                }
            }

            return matchingMethod.invoke(serviceObj, args);
        }
        catch (AdapterException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new AdapterException(ex.getMessage(), ex);
        }

    }

    protected String getServiceInterface() {
        return getAttributeValue(WorkAttributeConstant.SERVICE_INTERFACE);
    }

    protected String getServiceMethod() {
        return getAttributeValue(WorkAttributeConstant.SERVICE_METHOD);
    }

    protected Map<String,String> getServiceParameters() {
        String str = getAttributeValue(WorkAttributeConstant.SERVICE_PARAMETERS);
        if (str == null)
            return null;
        else
            return StringHelper.parseMap(str);
    }

    protected String getResultVariable() {
        return getAttributeValue(WorkAttributeConstant.SERVICE_RESULT);
    }

    @Override
    public void init(Properties parameters) {
        // TODO initialize attributes here

    }

    @Override
    public void init() throws ConnectionException, AdapterException {
        // TODO initialize attributes here

    }

    public boolean ping(int timeout) {
        return false;
    }

    protected boolean canBeSynchronous() {
        return true;
    }

    protected boolean canBeAsynchronous() {
        return false;
    }
}
