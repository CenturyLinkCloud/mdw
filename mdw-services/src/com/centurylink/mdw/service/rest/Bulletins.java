package com.centurylink.mdw.service.rest;

import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.SystemMessages;
import com.centurylink.mdw.model.system.Bulletin;
import com.centurylink.mdw.services.rest.JsonRestService;

public class Bulletins extends JsonRestService {

    @Override
    public JSONObject get(String path, Map<String,String> headers)
            throws ServiceException, JSONException {
        JSONObject json = new JSONObject();
        JSONArray bulletinsArr = new JSONArray();
        json.put("bulletins", bulletinsArr);
        for (Bulletin bulletin : SystemMessages.getBulletins().values()) {
            bulletinsArr.put(bulletin.getJson());
        }
        return json;
    }
}
