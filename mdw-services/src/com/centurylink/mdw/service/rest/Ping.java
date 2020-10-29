package com.centurylink.mdw.service.rest;

import java.util.List;
import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.user.UserAction.Action;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.services.rest.JsonRestService;

@Path("/Ping")
public class Ping extends JsonRestService {

    @Override
    public List<String> getRoles(String path) {
        return null;
    }

    @Override
    protected Entity getEntity(String path, Object content, Map<String,String> headers) {
        return Entity.Request;
    }

    @Override
    protected Action getAction(String path, Object content, Map<String, String> headers) {
        return Action.Ping;
    }

    @Override
    @Path("/{notused}")
    public JSONObject post(String path, JSONObject content, Map<String,String> headers)
    throws ServiceException, JSONException {
        if (content.has("ping")) {
            JSONObject ping = content.getJSONObject("ping");
            JSONObject pong = new JsonObject();
            pong.put("pong", ping);
            return pong;
        }
        else {
            return content;
        }
    }
}
