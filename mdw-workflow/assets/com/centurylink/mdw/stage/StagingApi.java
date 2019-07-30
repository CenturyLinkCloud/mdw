package com.centurylink.mdw.stage;

import com.centurylink.mdw.common.service.AuthorizationException;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.dataaccess.file.GitBranch;
import com.centurylink.mdw.model.Status;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.model.user.User;
import com.centurylink.mdw.model.user.Workgroup;
import com.centurylink.mdw.service.data.task.UserGroupCache;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.StagingServices;
import com.centurylink.mdw.services.rest.JsonRestService;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.Path;
import java.util.Map;

@Path("/")
public class StagingApi extends JsonRestService {

    @Override
    protected User authorize(String path, JSONObject content, Map<String, String> headers) throws AuthorizationException {
        User user = null;
        String userId = headers.get(Listener.AUTHENTICATED_USER_HEADER);
        if (userId != null)
            user = UserGroupCache.getUser(userId);
        if (user == null) {
            throw new AuthorizationException(HTTP_401_UNAUTHORIZED, "Requires authenticated user");
        }
        String stagingUser = getSegment(path, 4);
        if (user.getCuid().equals(stagingUser)) {
            if (!user.hasRole(Role.ASSET_DESIGN))
                throw new AuthorizationException(HTTP_401_UNAUTHORIZED, "Not authorized: " + user.getCuid());
        }
        else {
            if (!user.hasRole(Workgroup.SITE_ADMIN_GROUP))
                throw new AuthorizationException(HTTP_401_UNAUTHORIZED, "Not authorized");
        }
        return user;
    }

    @Override
    public JSONObject get(String path, Map<String, String> headers) throws ServiceException, JSONException {
        String[] segments = getSegments(path);
        try {
            if (segments.length == 4) {
                // TODO: list staging areas
                return new JSONObject();
            }
            else if (segments.length == 5) {
                String stagingUser = segments[4];
                GitBranch stagingBranch = getStaginServices().getStagingBranch(stagingUser);
                if (stagingBranch == null)
                    throw new ServiceException(ServiceException.NOT_FOUND, "Staging branch not found for " + stagingUser);
            }
        }
        catch (ServiceException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex);
        }
        throw new ServiceException(ServiceException.BAD_REQUEST, "Invalid path: " + path);
    }

    @Override
    public JSONObject post(String path, JSONObject content, Map<String, String> headers)
            throws ServiceException, JSONException {
        headers.put(Listener.METAINFO_HTTP_STATUS_CODE, String.valueOf(Status.ACCEPTED.getCode()));
        return null;
    }

    private StagingServices stagingServices;
    private StagingServices getStaginServices() {
        if (stagingServices == null)
            stagingServices = ServiceLocator.getStagingServices();
        return stagingServices;
    }
}
