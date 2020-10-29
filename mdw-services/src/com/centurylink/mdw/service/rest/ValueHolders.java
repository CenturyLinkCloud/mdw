package com.centurylink.mdw.service.rest;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.JsonArray;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.rest.JsonRestService;
import io.swagger.annotations.Api;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.Path;
import java.util.Map;

@Path("/ValueHolders")
@Api("Values by owner type and id")
public class ValueHolders extends JsonRestService {

    @Override
    protected Entity getEntity(String path, Object content, Map<String, String> headers) {
        return Entity.ValueHolder;
    }

    /**
     * Retrieve value holder IDs for specific names/values (optionally restricted by OwnerType).
     */
    @Override
    @Path("/{valueName}/{value}")
    public JSONObject get(String path, Map<String, String> headers)
            throws ServiceException, JSONException {
        Map<String, String> parameters = getParameters(headers);
        String valueName = getSegment(path, 1);
        if (valueName == null)
            throw new ServiceException("Missing path segment: {valueName}");
        String valuePattern = getSegment(path, 2);
        String ownerType = parameters.get("holderType");

        try {
            if (valuePattern == null)
                return getValues(valueName, ownerType).getJson();
            else
                return getValues(valueName, valuePattern, ownerType).getJson();
        }
        catch (Exception ex) {
            throw new ServiceException("Error loading value holders for " + valueName, ex);
        }
    }

    @Path("/{valueName}")
    public JsonArray getValues(String valueName, String ownerType) throws ServiceException {
        return new JsonArray(ServiceLocator.getWorkflowServices().getValueHolderIds(valueName, null, ownerType));
    }

    public JsonArray getValues(String valueName, String valuePattern, String ownerType) throws ServiceException {
        return new JsonArray(ServiceLocator.getWorkflowServices().getValueHolderIds(valueName, valuePattern, ownerType));
    }
}
