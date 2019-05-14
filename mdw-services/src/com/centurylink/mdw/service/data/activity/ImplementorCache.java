package com.centurylink.mdw.service.data.activity;

import com.centurylink.mdw.activity.types.GeneralActivity;
import com.centurylink.mdw.cache.CachingException;
import com.centurylink.mdw.cache.PreloadableCache;
import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.asset.AssetInfo;
import com.centurylink.mdw.model.project.Data;
import com.centurylink.mdw.model.workflow.ActivityImplementor;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.services.AssetServices;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.cache.CacheRegistration;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cached activity implementors do not include pagelet.
 */
public class ImplementorCache implements PreloadableCache {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private static Map<String,ActivityImplementor> implementors = new ConcurrentHashMap<>();
    public static Map<String,ActivityImplementor> getImplementors() { return implementors; }

    public static ActivityImplementor get(String implClass) {
        return implementors.get(implClass);
    }

    /**
     * Nothing is cleared.  Old impls stick around until/unless replaced by new.
     */
    @Override
    public void clearCache() {
    }

    @Override
    public void loadCache() throws CachingException {
        try {
            // declared in .impl files
            DataAccess.getProcessLoader().getActivityImplementors().forEach(impl -> {
                // qualify the icon location
                String icon = impl.getIcon();
                if (icon != null && !icon.startsWith("shape:") && icon.indexOf('/') <= 0) {
                    for (Package pkg : PackageCache.getPackages()) {
                        for (Asset asset : pkg.getAssets()) {
                            if (asset.getName().equals(icon)) {
                                impl.setIcon(pkg.getName() + "/" + icon);
                                break;
                            }
                        }
                    }
                }
                impl.setPagelet(null);
                implementors.put(impl.getImplementorClass(), impl);
            });

            // annotation-driven
            CacheRegistration.getInstance().getCache("");
            AssetServices assetServices = ServiceLocator.getAssetServices();
            Map<String,List<AssetInfo>> annotatedAssets = assetServices.getAssetsOfTypes(new String[]{"java", "kt"});
            for (String packageName : annotatedAssets.keySet()) {
                Package pkg = PackageCache.getPackage(packageName);
                for (AssetInfo assetInfo : annotatedAssets.get(packageName)) {
                    ActivityImplementor impl = getAnnotatedImpl(pkg, assetInfo);
                    if (impl != null)
                        implementors.put(impl.getImplementorClass(), impl);
                }
            }

            implementors.put(Data.Implementors.DUMMY_ACTIVITY, new ActivityImplementor(Data.Implementors.DUMMY_ACTIVITY,
                    GeneralActivity.class.getName(), "Dummy Activity", "shape:activity", "{}"));
        }
        catch (DataAccessException | ServiceException ex) {
            throw new CachingException(ex.getMessage(), ex);
        }
    }

    @Override
    public void refreshCache() {
        loadCache();
    }

    private ActivityImplementor getAnnotatedImpl(Package pkg, AssetInfo assetInfo) {
        String implClass = pkg.getName() + "." + assetInfo.getRootName();
        try {
            String contents = new String(Files.readAllBytes(assetInfo.getFile().toPath()));
            if (contents.contains("@Activity")) {
                GeneralActivity activity = pkg.getActivityImplementor(implClass);
                com.centurylink.mdw.annotations.Activity annotation =
                        activity.getClass().getAnnotation(com.centurylink.mdw.annotations.Activity.class);
                return new ActivityImplementor(implClass, annotation);
            }
        }
        catch (Exception ex) {
            logger.severeException("Cannot load " + implClass, ex);
        }
        return null;
    }


}
