package com.centurylink.mdw.service.rest;

import java.util.List;
import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.MdwServiceRegistry;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.monitor.ScheduledJob;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.services.rest.JsonRestService;
import com.centurylink.mdw.util.CallURL;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

import io.swagger.annotations.Api;

@Path("/ScheduledJobs")
@Api("Scheduled Job Service")
public class ScheduledJobs extends JsonRestService {

    public static final String JOB_CLASS_NAME = "className";

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    @Override
    public List<String> getRoles(String path) {
        List<String> roles = super.getRoles(path);
        roles.add(Role.PROCESS_EXECUTION);
        return roles;
    }

    @Override
    @Path("/run")
    public JSONObject post(String path, JSONObject content, Map<String,String> headers) throws ServiceException, JSONException {
        try {
            String[] segments = getSegments(path);
            if (segments.length > 1 && segments[1].equals("run")) {
                String className = null;
                if (!content.has(JOB_CLASS_NAME) && (segments.length < 3 || segments[2] == null))
                    throw new ServiceException(ServiceException.BAD_REQUEST, "Cannot identify Scheduled Job to run");
                if (segments.length > 2)
                    className = segments[2];

                if (className == null)
                    className = content.getString(JOB_CLASS_NAME);
                CallURL url = new CallURL(className);
                for (String param : content.keySet()) {
                    if (!param.equals(JOB_CLASS_NAME))
                        url.setParameter(param,  content.getString(param));
                }
                ScheduledJob job = MdwServiceRegistry.getInstance().getScheduledJob(className);
                if (job == null) {
                    Class<? extends ScheduledJob> jobClass = Class.forName(className).asSubclass(ScheduledJob.class);
                    job = jobClass.newInstance();
                }
                if (logger.isDebugEnabled())
                    logger.debug("Starting Scheduled Job via REST Service: " + className);
                job.run(url, s -> {});
                return null;
            }
            else
                throw new ServiceException(ServiceException.BAD_REQUEST, "Unsupported path: " + path);
        }
        catch (ServiceException ex) {
            throw ex;
        }
        catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }
}
