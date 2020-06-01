package com.centurylink.mdw.service.rest;

import com.centurylink.mdw.cache.VariableTypeCache;
import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.JsonList;
import com.centurylink.mdw.model.variable.VariableType;
import com.centurylink.mdw.services.rest.JsonRestService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Path("/VariableTypes")
@Api("Activity implementor definitions")
public class VariableTypes extends JsonRestService {

    @Override
    @ApiOperation(value="Retrieve variable types JSON.",
            response= VariableType.class, responseContainer="List")
    public JSONObject get(String path, Map<String,String> headers)
            throws ServiceException, JSONException {

        VariableTypeCache.getVariableTypes();
        Query query = getQuery(path, headers);
        List<VariableType> variableTypes = new ArrayList<>();
        if (query.getBooleanFilter("documents")) {
            for (VariableType variableType : VariableTypeCache.getVariableTypes().values()){
                if (variableType.isDocument())
                    variableTypes.add(variableType);
            }
        }
        else {
            variableTypes.addAll(VariableTypeCache.getVariableTypes().values());
        }

        return new JsonList(variableTypes, "variableTypes").getJson();
    }

}
