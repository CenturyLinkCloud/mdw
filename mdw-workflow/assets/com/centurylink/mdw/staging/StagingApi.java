package com.centurylink.mdw.staging;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.common.service.AuthorizationException;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.WebSocketProgressMonitor;
import com.centurylink.mdw.model.JsonArray;
import com.centurylink.mdw.model.JsonListMap;
import com.centurylink.mdw.model.Status;
import com.centurylink.mdw.model.asset.api.AssetInfo;
import com.centurylink.mdw.model.asset.api.StagingArea;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.model.user.User;
import com.centurylink.mdw.model.user.Workgroup;
import com.centurylink.mdw.service.data.user.UserGroupCache;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.StagingServices;
import com.centurylink.mdw.services.rest.JsonRestService;
import com.centurylink.mdw.util.JsonUtil;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.Path;
import java.util.*;

import static com.centurylink.mdw.services.StagingServices.STAGING;

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
        } else {
            if (!user.hasRole(Workgroup.SITE_ADMIN_GROUP))
                throw new AuthorizationException(HTTP_401_UNAUTHORIZED, "Not authorized");
        }
        return user;
    }

    /**
     * Paths in case this API becomes public
     * / = get all user staging areas
     * /&lt;cuid> = get staging area for user
     * /&lt;cuid>/assets = staged assets for user
     */
    @Override
    public JSONObject get(String path, Map<String, String> headers) throws ServiceException, JSONException {
        String[] segments = getSegments(path);
        try {
            if (segments.length == 4) {
                return new JsonArray(getStagingAreas()).getJson();
            } else if (segments.length == 5) {
                return getUserStagingArea(segments[4]).getJson();
            } else if (segments.length == 6) {
                StagingArea userStagingArea = getUserStagingArea(segments[4]);
                String sub = segments[5];
                if (sub.equals("assets")) {
                    LinkedHashMap<String, List<AssetInfo>> stagedAssets = getStagedAssets(userStagingArea.getUserCuid());
                    return new JsonListMap<>(stagedAssets).getJson();
                }
            }
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex);
        }
        throw new ServiceException(ServiceException.BAD_REQUEST, "Invalid path: " + path);
    }

    private List<StagingArea> getStagingAreas() throws ServiceException {
        return getStagingServices().getStagingAreas();
    }

    private StagingArea getUserStagingArea(String cuid) throws ServiceException {
        StagingArea userStagingArea = getStagingServices().getUserStagingArea(cuid);
        if (userStagingArea == null)
            throw new ServiceException(ServiceException.NOT_FOUND, "Staging area not found for " + cuid);
        return userStagingArea;
    }

    private LinkedHashMap<String, List<AssetInfo>> getStagedAssets(String cuid) throws ServiceException {
        LinkedHashMap<String, List<AssetInfo>> linkedHashMap = new LinkedHashMap<>();
        Map<String, List<AssetInfo>> stagedAssets = getStagingServices().getStagedAssets(cuid);
        for (String pkg : stagedAssets.keySet()) {
            linkedHashMap.put(pkg, stagedAssets.get(pkg));
        }
        return linkedHashMap;
    }

    /**
     * Prepare staging area, or add assets to staging area.
     */
    @Override
    public JSONObject post(String path, JSONObject content, Map<String, String> headers)
            throws ServiceException, JSONException {
        String[] segments = getSegments(path);
        if (segments.length == 5) {
            StagingArea userStagingArea = prepareStagingArea(segments[4]);
            if (userStagingArea.getBranch().getId() == null) {
                // asynchronous prep
                headers.put(Listener.METAINFO_HTTP_STATUS_CODE, String.valueOf(Status.ACCEPTED.getCode()));
            }
            return userStagingArea.getJson();
        } else if (segments.length == 6) {
            StagingArea userStagingArea = getUserStagingArea(segments[4]);
            String sub = segments[5];
            if (sub.equals("assets")) {
                List<String> assets = new JsonArray(content.getJSONArray("assets")).getList();
                stageAssets(userStagingArea.getUserCuid(), assets);
                return null;
            }
        }
        throw new ServiceException(ServiceException.BAD_REQUEST, "Invalid path: " + path);
    }

    private StagingArea prepareStagingArea(String cuid) throws ServiceException {
        User user = UserGroupCache.getUser(cuid);
        if (user == null)
            throw new ServiceException(ServiceException.NOT_FOUND, "User not found: " + cuid);
        WebSocketProgressMonitor progressMonitor = new WebSocketProgressMonitor(STAGING +
                ApplicationContext.getRuntimeEnvironment() + "_" + cuid, "Prepare staging area for " + user.getName());
        return getStagingServices().prepareStagingArea(cuid, progressMonitor);
    }

    private void stageAssets(String cuid, List<String> assets) throws ServiceException {
        getStagingServices().stageAssets(cuid, assets);
    }

    /**
     * Patch requests mean 'Promote' (or merge with main branch).
     */
    @Override
    public JSONObject patch(String path, JSONObject content, Map<String, String> headers)
            throws ServiceException, JSONException {
        String[] segments = getSegments(path);
        if (segments.length == 6 || segments.length == 8) {
            StagingArea userStagingArea = getUserStagingArea(segments[4]);
            String sub = segments[5];
            if (sub.equals("assets")) {
                String comment = content.optString("comment");
                if (comment.isEmpty())
                    throw new ServiceException(ServiceException.BAD_REQUEST, "Missing property: comment");
                if (segments.length == 6) {
                    promoteAssets(userStagingArea.getUserCuid(), comment);
                    return null;
                }
            }
        }
        throw new ServiceException(ServiceException.BAD_REQUEST, "Invalid path: " + path);
    }

    /**
     * Put requests mean stage 'Rollback' to previous asset versions.
     */
    @Override
    public JSONObject put(String path, JSONObject content, Map<String, String> headers)
            throws ServiceException, JSONException {
        String[] segments = getSegments(path);
        if (segments.length == 6) {
            StagingArea userStagingArea = getUserStagingArea(segments[4]);
            String sub = segments[5];
            if (sub.equals("assetVersions")) {
                JSONObject assetVersions = content.getJSONObject("assetVersions");
                Map<String,String> assetVers = JsonUtil.getMap(assetVersions);
                rollbackAssets(userStagingArea.getUserCuid(), assetVers);
                return null;
            }
        }
        throw new ServiceException(ServiceException.BAD_REQUEST, "Invalid path: " + path);
    }

    /**
     * Delete means unstage.
     */
    @Override
    public JSONObject delete(String path, JSONObject content, Map<String,String> headers)
            throws ServiceException, JSONException {
        String[] segments = getSegments(path);
        if (segments.length == 6 || segments.length == 8) {
            StagingArea userStagingArea = getUserStagingArea(segments[4]);
            String sub = segments[5];
            if (sub.equals("assets")) {
                if (segments.length == 6) {
                    String[] assetPaths = getQuery(path, headers).getArrayFilter("assetPaths");
                    if (assetPaths == null) {
                        // TODO remove user staging area
                        return null;
                    }
                    else {
                        unStageAssets(userStagingArea.getUserCuid(), Arrays.asList(assetPaths));
                        return null;
                    }
                }
                else {
                    List<String> assets = new ArrayList<>();
                    assets.add(segments[6] + '/' + segments[7]);
                    unStageAssets(userStagingArea.getUserCuid(), assets);
                    return null;
                }
            }
        }
        throw new ServiceException(ServiceException.BAD_REQUEST, "Invalid path: " + path);
    }

    private void unStageAssets(String cuid, List<String> assets) throws ServiceException {
        getStagingServices().unStageAssets(cuid, assets);
    }

    private void promoteAssets(String cuid, String comment) throws ServiceException {
        getStagingServices().promoteAssets(cuid, comment);
    }

    private void rollbackAssets(String cuid, Map<String,String> assetVersions) throws ServiceException {
        getStagingServices().rollbackAssets(cuid, assetVersions);
    }

    private StagingServices stagingServices;
    private StagingServices getStagingServices() {
        if (stagingServices == null)
            stagingServices = ServiceLocator.getStagingServices();
        return stagingServices;
    }
}
