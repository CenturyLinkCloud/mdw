package com.centurylink.mdw.stage;

import com.centurylink.mdw.common.service.AuthorizationException;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.WebSocketProgressMonitor;
import com.centurylink.mdw.model.JsonArray;
import com.centurylink.mdw.model.Status;
import com.centurylink.mdw.model.asset.Stage;
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

import static com.centurylink.mdw.services.StagingServices.STAGE;

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
            StagingServices stagingServices = getStagingServices();
            if (segments.length == 4) {
                return new JsonArray(stagingServices.getStages()).getJson();
            }
            else if (segments.length == 5) {
                String stagingUser = segments[4];
                Stage userStage = stagingServices.getUserStage(stagingUser);
                if (userStage == null)
                    throw new ServiceException(ServiceException.NOT_FOUND, "Staging branch not found for " + stagingUser);
                return userStage.getJson();
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
    public JSONObject post(String path, JSONObject content, Map<String,String> headers)
            throws ServiceException, JSONException {
        String stagingUser = getSegment(path, 4);
        if (stagingUser == null) {
            throw new ServiceException(ServiceException.BAD_REQUEST, "Invalid path: " + path);
        }
        else {
            User user = UserGroupCache.getUser(stagingUser);
            if (user == null)
                throw new ServiceException(ServiceException.NOT_FOUND, "User not found: " + stagingUser);
            WebSocketProgressMonitor progressMonitor = new WebSocketProgressMonitor(STAGE + stagingUser,
                    "Prepare staging area for " + user.getName());
            Stage userStage = getStagingServices().prepareUserStage(stagingUser, progressMonitor);
            if (userStage.getBranch().getId() == null) {
                // asynchronous prep
                headers.put(Listener.METAINFO_HTTP_STATUS_CODE, String.valueOf(Status.ACCEPTED.getCode()));
            }
            return userStage.getJson();
        }
    }

    private StagingServices stagingServices;
    private StagingServices getStagingServices() {
        if (stagingServices == null)
            stagingServices = ServiceLocator.getStagingServices();
        return stagingServices;
    }
}
