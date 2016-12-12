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
