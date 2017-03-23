/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.auth;

import java.text.ParseException;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.util.StringHelper;

public class OAuthAccessToken implements Jsonable {

    private String token;
    public String getToken() { return token; }

    private String type;
    public String getType() { return type; }

    private Date expires;
    public Date getExpires() { return expires; }

    private String refreshToken;
    public String getRefreshToken() { return refreshToken; }

    private String accountGroup;
    public String getAccountGroup() { return accountGroup; }

    public OAuthAccessToken(JSONObject json) throws JSONException {
        this(json, false);
    }

    public OAuthAccessToken(JSONObject json, boolean ctlioFormat) throws JSONException {
        if (ctlioFormat) {
            if (!json.has("access_token"))
                throw new JSONException("Missing required property: access_token");
            this.token = json.getString("access_token");
            if (json.has("token_type"))
                this.type = json.getString("token_type");
            if (json.has("expiresIn"))
                this.expires = new Date(System.currentTimeMillis() + json.getInt("expiresIn") * 1000);
            if (json.has("refresh_token"))
                this.refreshToken = json.getString("refresh_token");
            if (json.has("account_name"))
                this.accountGroup = json.getString("account_name");
        }
        else {
            if (!json.has("token"))
                throw new JSONException("Missing required property: token");
            this.token = json.getString("token");
            if (json.has("type"))
                this.type = json.getString("type");
            if (json.has("expires")) {
                try {
                    this.expires = StringHelper.parseIsoDate(json.getString("expires"));
                }
                catch (ParseException ex) {
                    throw new JSONException(ex);
                }
            }
            if (json.has("refreshToken"))
                this.refreshToken = json.getString("refreshToken");
            if (json.has("accountGroup"))
                this.accountGroup = json.getString("accountGroup");
        }
    }

    public JSONObject getJson() throws JSONException {
        if (token == null)
            return null;

        JSONObject json = new JSONObject();
        json.put("token", token);
        if (type != null)
            json.put("type", type);
        if (expires != null)
            json.put("expires", StringHelper.formatIsoDate(expires));
        if (refreshToken != null)
            json.put("refreshToken", refreshToken);
        if (accountGroup != null)
            json.put("accountGroup", accountGroup);
        return json;
    }

    public String getJsonName() {
        return "oAuthAccessToken";
    }

}
