package com.centurylink.mdw.servicenow;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.cache.impl.AssetCache;
import com.centurylink.mdw.config.PropertyException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.task.TaskRuntimeContext;
import com.centurylink.mdw.observer.ObserverException;
import com.centurylink.mdw.observer.task.TemplatedNotifier;
import com.centurylink.mdw.util.HttpHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.yaml.YamlLoader;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

@RegisteredService(com.centurylink.mdw.observer.task.TaskNotifier.class)
public class TaskNotifier extends TemplatedNotifier {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private static final String SERVICENOW_URL = "mdw.servicenow.url";

    /**
     * Currently only handles Create.  TODO: Updates as well.
     */
    @Override
    public void sendNotice(TaskRuntimeContext runtimeContext, String taskAction, String outcome)
            throws ObserverException {
        String url = getServiceNowUrl();
        try {
            HttpHelper helper = new HttpHelper(new URL(url), getAuthUser(), getAuthPassword());
            helper.setHeaders(getRequestHeaders());
            Incident incident = getIncident(runtimeContext);
            String response = helper.post(incident.getJson().toString(2));
            if (helper.getResponseCode() > 201) {
                logger.error("Notification error for task: " + runtimeContext.getTaskInstanceId()
                        + " -- ServiceNow response(" + helper.getResponseCode() + "):\n" + response);
                throw new ObserverException("Notification error for task: " + runtimeContext.getTaskInstanceId());
            }
        }
        catch (Exception ex) {
            throw new ObserverException("Notification error for task: " + runtimeContext.getTaskInstanceId(), ex);
        }
    }

    protected String getServiceNowUrl() throws PropertyException {
        String base = PropertyManager.getProperty(SERVICENOW_URL);
        if (base == null)
            throw new PropertyException("Missing property: " + SERVICENOW_URL);
        return base + "/table/incident";
    }

    /**
     * Auth headers are expected in environment variables or encrypted properties.
     */
    protected String getAuthUser() throws PropertyException {
        String authUser = System.getenv("MDW_SERVICENOW_USER");
        if (authUser == null)
            authUser = PropertyManager.getProperty("mdw.servicenow.user");
        if (authUser == null)
            throw new PropertyException("Missing MDW_SERVICENOW_USER env or servicenow.user prop");
        return authUser;
    }

    protected String getAuthPassword() throws PropertyException {
        String authPassword = System.getenv("MDW_SERVICENOW_PASSWORD");
        if (authPassword == null)
            authPassword = PropertyManager.getProperty("mdw.servicenow.password");
        if (authPassword == null)
            throw new PropertyException("Missing MDW_SERVICENOW_PASSWORD env or servicenow.password prop");
        return authPassword;
    }

    protected Map<String,String> getRequestHeaders() {
        Map<String,String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        return headers;
    }

    protected Incident getIncident(TaskRuntimeContext context)
            throws IOException, ReflectiveOperationException, IntrospectionException {
        Asset template = AssetCache.getAsset(getTemplateSpec());
        if (template == null)
            throw new IOException("Missing template: " + getTemplateSpec());

        Incident incident = new Incident();

        YamlLoader loader = new YamlLoader(template.getStringContent());
        Map<?,?> map = (Map<?,?>)loader.getTop();
        for (Object key : map.keySet()) {
            String field = key.toString();
            String taskVal = map.get(key).toString();
            Object value;
            if (taskVal.startsWith("'") && taskVal.endsWith("'")) {
                // hardcoded value
                value = taskVal.substring(1, taskVal.length() - 1);
            }
            else {
                value = context.evaluateToString("${task." + taskVal + "}");
            }

            String setterName = "set" + field.substring(0, 1).toUpperCase() + field.substring(1);
            Method setter = getIncidentSetter(setterName);
            if (setter == null) {
                throw new NoSuchMethodException("Incident setter method not found: " + setterName);
            }
            if (value != null && !value.equals("")) {
                invoke(setter, incident, value);
            }
        }
        return incident;
    }

    private BeanInfo incidentBeanInfo;
    private Method getIncidentSetter(String name) throws IntrospectionException {
        if (incidentBeanInfo == null) {
            incidentBeanInfo = Introspector.getBeanInfo(Incident.class);
        }
        for (MethodDescriptor md : incidentBeanInfo.getMethodDescriptors()) {
            if (name.equals(md.getName()))
                return md.getMethod();
        }
        return null;
    }

    /**
     * Invoke setter, converting certain types as required by Incident.
     */
    private void invoke(Method setter, Incident incident, Object value) throws ReflectiveOperationException {
        Class<?> paramType = setter.getParameterTypes()[0];
        if (value.getClass() != paramType) {
            if (paramType == String.class) {
                value = String.valueOf(value);
            }
            else if (paramType == Incident.Level.class) {
                value = Incident.Level.of(Integer.parseInt(String.valueOf(value)));
            }
            else if (paramType == int.class) {
                value = Integer.parseInt(String.valueOf(value));
            }
            else if (paramType == LocalDateTime.class) {
                if (value.getClass() == Instant.class) {
                    value = LocalDateTime.ofInstant((Instant)value, ZoneId.systemDefault());
                }
            }
        }
        setter.invoke(incident, value);
    }
}
