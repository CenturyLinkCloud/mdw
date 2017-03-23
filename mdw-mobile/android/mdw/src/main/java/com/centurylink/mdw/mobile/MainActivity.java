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
package com.centurylink.mdw.mobile;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.net.Uri;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.centurylink.mdw.mobile.R;
import com.centurylink.mdw.mobile.app.BadSettingsException;
import com.centurylink.mdw.mobile.app.Settings;

import java.net.URL;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private Settings settings;
    private Toolbar toolbar;

    private WebView webView;
    protected WebView getWebView() { return webView; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        settings = new Settings(getApplicationContext());

        webView = (WebView) findViewById(R.id.webview);
        webView.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setSupportZoom(true);
        // webView.getSettings().setBuiltInZoomControls(true);
        // allow debugging with chrome dev tools
        WebView.setWebContentsDebuggingEnabled(true);

        // do not cache in debug
        webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        webView.getSettings().setAppCacheEnabled(false);
        webView.clearCache(true);


    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            URL url = settings.getServerUrl();
            Log.i(TAG, "Loading URL: " + url);
            webView.loadUrl(url.toString());
        } catch (BadSettingsException ex) {
            Log.e(TAG, ex.getMessage(), ex);
            Snackbar.make(webView, ex.toString(), Snackbar.LENGTH_LONG).show();
        }

        // String url = settings.getServerUrl() + "/#/workflow";
//        try {
//            webView.loadUrl(url);
//        } catch (Exception ex) {
//            Log.e(TAG, ex.getMessage(), ex);
//            Snackbar.make(webView, ex.toString(), Snackbar.LENGTH_LONG).show();
//        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        String url = "login";
        if (id == R.id.nav_workflow) {
            toolbar.setTitle("MDW: " + getString(R.string.title_workflow));
            loadPath("workflow/processes");
        } else if (id == R.id.nav_dashboard) {
            toolbar.setTitle("MDW: " + getString(R.string.title_dashboard));
            loadPath("dashboard/processes");
        } else if (id == R.id.nav_services) {
            toolbar.setTitle("MDW: " + getString(R.string.title_services));
            loadPath("serviceApi");
        } else if (id == R.id.nav_tasks) {
            toolbar.setTitle("MDW: " + getString(R.string.title_tasks));
            loadPath("tasks");
        } else if (id == R.id.nav_admin) {
            toolbar.setTitle("MDW: " + getString(R.string.title_admin));
            loadPath("users");
        } else if (id == R.id.nav_system) {
            toolbar.setTitle("MDW: " + getString(R.string.title_system));
            loadPath("system/sysInfo");
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);


        return true;
    }

    private void loadPath(String path) {
        String url = settings.getServerUrl() + "/#/" + path;
        Log.i(TAG, "Loading URL: " + url);
        webView.loadUrl(url);
    }
}
