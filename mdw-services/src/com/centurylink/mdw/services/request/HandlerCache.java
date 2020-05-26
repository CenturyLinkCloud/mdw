/*
 * Copyright (C) 2019 CenturyLink, Inc.
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
package com.centurylink.mdw.services.request;

import com.centurylink.mdw.annotations.Handler;
import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.cache.CachingException;
import com.centurylink.mdw.cache.PreloadableCache;
import com.centurylink.mdw.cache.asset.AssetCache;
import com.centurylink.mdw.cache.asset.PackageCache;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.asset.api.AssetInfo;
import com.centurylink.mdw.model.request.HandlerSpec;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.request.RequestHandler;
import com.centurylink.mdw.services.AssetServices;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.cache.CacheRegistration;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.xml.XmlPath;
import org.json.JSONObject;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HandlerCache implements PreloadableCache {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private static HashMap<String,List<HandlerSpec>> contentSpecs = new HashMap<>();
    private static HashMap<String,List<HandlerSpec>> pathSpecs = new HashMap<>();

    public void loadCache() throws CachingException {
        load();
    }

    public synchronized void refreshCache() throws CachingException {
        loadCache();
    }

    public void clearCache(){
        pathSpecs.clear();
        contentSpecs.clear();
    }

    /**
     * content-based handlers
     */
    public static List<HandlerSpec> getContentHandlers(String bucket){
        return contentSpecs.get(bucket);
    }

    /**
     * path-based handlers
     */
    public static List<HandlerSpec> getPathHandlers(String bucket){
        if (pathSpecs.get(bucket) != null)
            return pathSpecs.get(bucket);
        else if (bucket.indexOf('/') > 0) {  // We could have a sub-path
            return getPathHandlers(bucket.substring(0, bucket.lastIndexOf('/')));
        }
        return null;
    }

    public static List<HandlerSpec> getAllHandlers() {
        List<HandlerSpec> handlers = new ArrayList<>();
        for (List<HandlerSpec> contentSpecList : contentSpecs.values()) {
            for (HandlerSpec contentSpec : contentSpecList) {
                if (!handlers.contains(contentSpec))
                    handlers.add(contentSpec);
            }
        }
        for (List<HandlerSpec> pathSpecList : pathSpecs.values()) {
            for (HandlerSpec pathSpec : pathSpecList) {
                if (!handlers.contains(pathSpec))
                    handlers.add(pathSpec);
            }
        }
        return handlers;
    }

    public static HandlerSpec fallbackHandler = new HandlerSpec("FallbackHandler",
            FallbackRequestHandler.class.getName());
    public static final HandlerSpec serviceHandler = new HandlerSpec("ServiceHandler",
            ServiceRequestHandler.class.getName());

    private synchronized void load() throws CachingException {
        HashMap<String,List<HandlerSpec>> myContentSpecs = new HashMap<>();
        HashMap<String,List<HandlerSpec>> myPathSpecs = new HashMap<>();
        try {
            List<HandlerSpec> specs = loadHandlerSpecs();
            specs.addAll(loadEvthHandlerSpecs());
            for (HandlerSpec spec : specs) {
                if (spec.getName().equals("DefaultEventHandler")) {
                    fallbackHandler = spec;
                    continue;
                }
                try {
                    if (spec.isContentRouting()) {
                        XmlPath xpath = spec.getXpath();
                        List<HandlerSpec> bucket = myContentSpecs.get(xpath.getHashBucket());
                        if (bucket == null) {
                            bucket = new ArrayList<>();
                            myContentSpecs.put(xpath.getHashBucket(), bucket);
                        }
                        bucket.add(spec);
                    }
                    else {
                        if (spec.getPath().startsWith("/")) {
                            spec.setPath(spec.getPath().substring(1));
                        }
                        List<HandlerSpec> bucket = myPathSpecs.get(spec.getPath());
                        if (bucket == null) {
                            bucket = new ArrayList<>();
                            myPathSpecs.put(spec.getPath(), bucket);
                        }
                        bucket.add(spec);
                    }
                } catch (Exception ex) {
                    logger.error("Failed to process handler: " + spec.getName(), ex);
                }
            }
            contentSpecs = myContentSpecs;
            pathSpecs = myPathSpecs;
        } catch(Exception ex) {
            throw new CachingException(ex.getMessage(), ex);
        }
    }

    private List<HandlerSpec> loadHandlerSpecs() throws CachingException {
        try {
            List<HandlerSpec> handlers = new ArrayList<>();
            CacheRegistration.getInstance().getCache("");
            AssetServices assetServices = ServiceLocator.getAssetServices();
            Map<String,List<AssetInfo>> annotatedAssets = assetServices.getAssetsWithExtensions(new String[]{"java", "kt"});
            for (String pkgName : annotatedAssets.keySet()) {
                for (AssetInfo assetInfo : annotatedAssets.get(pkgName)) {
                    Package pkg = PackageCache.getPackage(pkgName);
                    HandlerSpec handler = getAnnotatedHandler(pkg, assetInfo);
                    if (handler != null) {
                        handlers.add(handler);
                    }
                }
            }
            return handlers;
        } catch (ServiceException ex) {
            throw new CachingException(ex.getMessage(), ex);
        }
    }

    /**
     * This purposefully only finds asset-based handlers.
     * TODO: Supplier-driven handlers (mdw-spring-boot AnnotationsScanner).
     */
    private HandlerSpec getAnnotatedHandler(Package pkg, AssetInfo assetInfo) {
        String handlerClass = pkg.getName() + "." + assetInfo.getRootName();
        try {
            String contents = new String(Files.readAllBytes(assetInfo.getFile().toPath()));
            if (contents.contains("@Handler")) {
                RequestHandler handler = pkg.getRequestHandler(handlerClass);
                Handler annotation = handler.getClass().getAnnotation(Handler.class);
                String name = pkg.getName() + "/" + assetInfo.getName();
                HandlerSpec handlerSpec = new HandlerSpec(name, handlerClass);
                handlerSpec.setPath(annotation.path());
                handlerSpec.setAssetPath(pkg.getName() + "/" + assetInfo.getName());
                handlerSpec.setContentRouting(annotation.match() == RequestHandler.Routing.Content);
                return handlerSpec;
            }
        }
        catch (Throwable t) {
            logger.error("Cannot load " + handlerClass, t);
        }
        return null;
    }


    // TODO: this compatibility will be removed soon
    private List<HandlerSpec> loadEvthHandlerSpecs() {
        List<HandlerSpec> evths = new ArrayList<>();
        for (Asset asset : AssetCache.getAssets("evth")) {
            try {
                JSONObject json = new JSONObject(new String(asset.getContent()));
                String handlerClass = json.getString("handlerClass");
                HandlerSpec evth = new HandlerSpec(asset.getPath(), handlerClass);
                evth.setPath(json.getString("path"));
                evth.setAssetPath(asset.getPath());
                evth.setContentRouting(!"path".equalsIgnoreCase(json.optString("routing")));
                evths.add(evth);
            } catch (Exception ex) {
                logger.error("Failed to parse handler: " + asset.getPath(), ex);
            }
            logger.warn("*** Found evth asset: " + asset.getPath());
            logger.warn("*** Support for evth assets will be removed soon.");
            logger.warn("*** Consult the docs on how to convert to RequestHandlers: "
                    + ApplicationContext.getDocsUrl() + "/help/handlers.html");
        }
        return evths;
    }
}
