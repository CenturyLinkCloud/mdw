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
package com.centurylink.mdw.mobile.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.net.MalformedURLException;
import java.net.URL;

public class Settings {
    public static final String SERVER_URL = "server_url";

    private Context appContext;
    public Context getAppContext() { return appContext; }

    private SharedPreferences prefs;
    public SharedPreferences getPrefs() { return prefs; }

    public Settings(Context appContext) {
        this.appContext = appContext;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
    }

    public URL getServerUrl() {
        String pref = prefs.getString(SERVER_URL, null);
        if (pref == null)
            throw new BadSettingsException("No setting: Server URL");
        try {
            return new URL(pref);
        }
        catch (MalformedURLException ex) {
            throw new BadSettingsException("Invalid Server URL: " + pref);
        }
    }
}
