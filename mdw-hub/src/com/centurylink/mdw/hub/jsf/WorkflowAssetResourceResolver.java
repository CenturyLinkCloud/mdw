/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.jsf;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import javax.faces.FacesException;
import javax.faces.context.FacesContext;

import org.apache.myfaces.view.facelets.impl.DefaultResourceResolver;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.web.jsf.view.MDWPageContent;
import com.centurylink.mdw.web.ui.UIException;

public class WorkflowAssetResourceResolver extends DefaultResourceResolver {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private static File devWebRoot;
    private static String devWebRootProp = "";

    private static File hubWebRoot;
    private static String hubWebRootProp = "";

    @Override
    public URL resolveUrl(String path) {

        if (hubWebRootProp != null) {
            if (hubWebRootProp.isEmpty()) { // we've not checked yet
                hubWebRootProp = "x";
                hubWebRoot = ApplicationContext.getHubOverrideRoot();
            }
        }
        if (hubWebRoot != null) {
            try {
                File hubWebFile = new File(hubWebRoot + path);
                if (hubWebFile.exists())
                    return hubWebFile.toURI().toURL();
            }
            catch (MalformedURLException ex) {
                throw new FacesException(ex.getMessage(), ex);
            }
        }

        if (devWebRootProp != null) {
            if (devWebRootProp.isEmpty()) {  // we've not checked yet
                devWebRootProp = FacesUtil.getDevWebRoot();
                if (devWebRootProp != null)
                    devWebRoot = new File(devWebRootProp);
            }
        }
        if (devWebRoot != null) {
            try {
                File devWebFile = new File(devWebRoot + path);
                if (devWebFile.exists())
                    return devWebFile.toURI().toURL();
            }
            catch (MalformedURLException ex) {
                throw new FacesException(ex.getMessage(), ex);
            }
        }

        URL url = super.resolveUrl(path);
        if (url != null)
            return url;
        try {
            return new URL("internal", null, 0, path, new URLStreamHandler() {
                @Override
                protected URLConnection openConnection(URL url) throws IOException {
                    final String path = url.getFile();

                    return new URLConnection(url) {
                        @Override
                        public void connect() throws IOException {
                        }

                        @Override
                        public InputStream getInputStream() throws IOException {
                            try {
                                String content = new MDWPageContent(path).getContent();
                                if (content == null)
                                    throw new IOException("Resource does not exist: " + path);
                                return new ByteArrayInputStream(content.getBytes());
                            }
                            catch (UIException ex) {
                                logger.severeException("Cannot load resource: " + path, ex);
                                throw new IOException("Cannot load resource: " + path, ex);
                            }
                        }

                        @Override
                        public long getLastModified() {
                            return super.getLastModified();
                        }
                    };
                }
            });
        }
        catch (MalformedURLException ex) {
            logger.severeException(ex.getMessage(), ex);
            return null;
        }
    }

    public URL resolveUrl(FacesContext facesContext, String path) {
        URL url = super.resolveUrl(facesContext, path);
        if (url != null)
            return url;
        try {
            return new URL("internal", null, 0, path, new URLStreamHandler() {
                @Override
                protected URLConnection openConnection(URL url) throws IOException {
                    final String path = url.getFile();

                    return new URLConnection(url) {
                        @Override
                        public void connect() throws IOException {
                        }

                        @Override
                        public InputStream getInputStream() throws IOException {
                            try {
                                String content = new MDWPageContent(path).getContent();
                                if (content == null)
                                    throw new IOException("Resource does not exist: " + path);
                                return new ByteArrayInputStream(content.getBytes());
                            }
                            catch (UIException ex) {
                                logger.severeException("Cannot load resource: " + path, ex);
                                throw new IOException("Cannot load resource: " + path, ex);
                            }
                        }

                        @Override
                        public long getLastModified() {
                            return super.getLastModified();
                        }
                    };
                }
            });
        }
        catch (MalformedURLException ex) {
            logger.severeException(ex.getMessage(), ex);
            return null;
        }
    }

}
