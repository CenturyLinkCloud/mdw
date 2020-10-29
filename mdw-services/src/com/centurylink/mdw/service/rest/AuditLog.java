package com.centurylink.mdw.service.rest;

import java.util.List;
import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONObject;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.model.user.UserAction;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.UserServices;
import com.centurylink.mdw.services.rest.JsonRestService;

@Path("/AuditLog")
public class AuditLog extends JsonRestService {

    @Override
    public List<String> getRoles(String path) {
        List<String> roles = super.getRoles(path);
        roles.add(Role.PROCESS_EXECUTION);
        return roles;
    }

    public String post(JSONObject request, Map<String,String> metaInfo) throws ServiceException {
        try {
            if (request == null)
                throw new ServiceException(ServiceException.BAD_REQUEST, "Missing body");
            UserAction userAction = new UserAction(request);
            UserServices userServices = ServiceLocator.getUserServices();
            userServices.auditLog(userAction);
            return null;  // success
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }
}