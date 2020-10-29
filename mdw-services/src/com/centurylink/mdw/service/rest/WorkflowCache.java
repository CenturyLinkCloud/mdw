package com.centurylink.mdw.service.rest;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.model.user.UserAction.Action;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.services.cache.CacheRegistration;
import com.centurylink.mdw.services.rest.JsonRestService;
import com.centurylink.mdw.startup.StartupException;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.Path;
import java.util.List;
import java.util.Map;

@Path("/WorkflowCache")
public class WorkflowCache extends JsonRestService {

    @Override
    public List<String> getRoles(String path) {
        List<String> roles = super.getRoles(path);
        roles.add(Role.PROCESS_DESIGN);
        return roles;
    }

    @Override
    protected Action getAction(String path, Object content, Map<String, String> headers) {
        if ("POST".equals(headers.get(Listener.METAINFO_HTTP_METHOD)))
            return Action.Refresh;
        else
            return super.getAction(path, content, headers);
    }

    @Override
    protected Entity getEntity(String path, Object content, Map<String, String> headers) {
        return Entity.Cache;
    }

    @Override
    @Path("/{cacheName}")
    public JSONObject post(String path, JSONObject content, Map<String,String> headers)
    throws ServiceException, JSONException {

        try {
            String singleCacheName = getSegment(path, 1);

            if (singleCacheName == null) {
                CacheRegistration.getInstance().refreshCaches();
            }
            else {
                new CacheRegistration().refreshCache(singleCacheName);
            }
        }
        catch (StartupException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage());
        }

        return null;
    }
}
