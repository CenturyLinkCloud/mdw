package com.centurylink.mdw.service.rest;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.listener.ListenerHelper;
import com.centurylink.mdw.model.Request;
import com.centurylink.mdw.model.Response;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.services.rest.JsonRestService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/Invoke")
@Api("Access to MDW service endpoints")
public class Invoke extends JsonRestService {

    @Override
    public List<String> getRoles(String path, String method) {
        if (method.equals("GET")) {
            return super.getRoles(path, method);
        }
        else {
            List<String> roles = super.getRoles(path);
            roles.add(Role.PROCESS_EXECUTION);
            return roles;
        }
    }

    @Override
    @ApiOperation(value="Invoke an MDW service endpoint", response=Response.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name="Request", paramType="body", required=true, dataType="com.centurylink.mdw.model.Request")})
    public JSONObject post(String path, JSONObject content, Map<String,String> headers)
            throws ServiceException, JSONException {
        Request request = new Request(content);
        Map<String,String> meta = new HashMap<>(headers);
        meta.put(Listener.METAINFO_REQUEST_PATH, request.getPath());
        meta.put(Listener.METAINFO_PROTOCOL, request.getProtocol());
        if (request.getOperation() != null)
            meta.put(Listener.METAINFO_HTTP_METHOD, request.getOperation());

        String output = new ListenerHelper().processEvent(request.getContent(), meta);

        Response response = new Response(output);
        String status = meta.get(Listener.METAINFO_HTTP_STATUS_CODE);
        if  (status == null && Listener.METAINFO_PROTOCOL_REST.equals(request.getProtocol()))
            status = "200";
        if (status != null) {
            headers.put(Listener.METAINFO_HTTP_STATUS_CODE, status);
            response.setStatusCode(Integer.parseInt(status));
        }
        response.setPath(request.getPath());

        return response.getJson();
    }
}
