/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.jsf;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.faces.view.facelets.FaceletCache;

import org.apache.myfaces.view.facelets.impl.FaceletCacheFactoryImpl;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.cache.impl.WebPageCache;

public class FaceletCacheFactory extends FaceletCacheFactoryImpl {

    private static File devWebRoot;
    private static File hubOverrideRoot;

    /**
     * Do not cache when dev web root system prop is set.
     */
    @Override
    @SuppressWarnings("rawtypes")
    public FaceletCache getFaceletCache() {

        hubOverrideRoot = ApplicationContext.getHubOverrideRoot();
        if (hubOverrideRoot != null && hubOverrideRoot.isDirectory()) {
            return new FaceletCache() {
                public Object getFacelet(URL url) throws IOException {
                    String key = url.toString();
                    Object facelet = isPageUrl(url) ? null : WebPageCache.getPage(key);
                    if (facelet == null) {
                        File file = getResourceFromRoot(hubOverrideRoot, url);
                        if (file != null && file.exists())
                            facelet = getMemberFactory().newInstance(file.toURI().toURL());
                        else
                            facelet = getMemberFactory().newInstance(url);
                        if (!isPageUrl(url))
                            WebPageCache.putPage(key, facelet);
                    }
                    return facelet;
                }
                public boolean isFaceletCached(URL url) {
                    return WebPageCache.getPage(url.toString()) != null;
                }
                public Object getViewMetadataFacelet(URL url) throws IOException {
                    String key = url.toString() + ".metadata"; // to cache separately from facelet
                    Object facelet = isPageUrl(url) ? null : WebPageCache.getPage(key);
                    if (facelet == null) {
                        File file = getResourceFromRoot(hubOverrideRoot, url);
                        if (file != null && file.exists())
                            facelet = getMetadataMemberFactory().newInstance(file.toURI().toURL());
                        else
                            facelet = getMetadataMemberFactory().newInstance(url);
                        if (!isPageUrl(url))
                            WebPageCache.putPage(key, facelet);
                    }
                    return facelet;
                }
                public boolean isViewMetadataFaceletCached(URL url) {
                    return WebPageCache.getPage(url.toString() + ".metadata") != null;
                }
            };
        }

        if (FacesUtil.getDevWebRoot() != null) {
            devWebRoot = new File(FacesUtil.getDevWebRoot());
            return new FaceletCache() {
                public Object getFacelet(URL url) throws IOException {
                    if (devWebRoot != null) {
                        File file = getResourceFromRoot(devWebRoot, url);
                        if (file != null && file.exists())
                            return getMemberFactory().newInstance(file.toURI().toURL());
                    }
                    return getMemberFactory().newInstance(url);
                }
                public boolean isFaceletCached(URL url) {
                    return false;
                }
                public Object getViewMetadataFacelet(URL url) throws IOException {
                    return getMetadataMemberFactory().newInstance(url);
                }
                public boolean isViewMetadataFaceletCached(URL url) {
                    return false;
                }
            };
        }

        return super.getFaceletCache();
    }

    protected boolean isPageUrl(URL url) {
      return url.toString().equals("internal:/page.xhtml");
    }

    protected File getResourceFromRoot(File root, URL url) {
        if (url.getProtocol().equals("bundle")) {  // servicemix/fuse
            return new File(root + url.getPath());
        }
        else if (url.getProtocol().equals("jndi") || url.getProtocol().equals("file")) {  // tomcat
            String path = url.getPath();
            int idx = path.indexOf("/" + ApplicationContext.getMdwHubContextRoot() + "/");
            if (idx >= 0)
                return new File(root + path.substring(idx + 4));
        }
        return null;
    }

}
