package com.centurylink.mdw.cache;

import com.centurylink.mdw.annotations.Variable;
import com.centurylink.mdw.cache.asset.AssetCache;
import com.centurylink.mdw.cache.asset.PackageCache;
import com.centurylink.mdw.dataaccess.MdwVariableTypes;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.variable.VariableType;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.variable.VariableTranslator;

import java.util.HashMap;

/**
 * Cache for variable types
 */
public class VariableTypeCache implements PreloadableCache {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private static HashMap<String,VariableType> variableTypes = new HashMap<>();

    public void loadCache() throws CachingException {
        load();
    }

    public synchronized void refreshCache() throws CachingException {
        loadCache();
    }

    public void clearCache(){
        variableTypes.clear();
    }

    public static VariableType getVariableType(String typeName) {
        return variableTypes.get(typeName);
    }

    public static VariableType getVariableType(Integer typeId) {
        for (VariableType varType : variableTypes.values()) {
            if (varType.getId().equals(typeId))
                return varType;
        }
        return null;
    }

    private synchronized void load() {
        HashMap<String,VariableType> myVariableTypes = new HashMap<>();
        for (VariableType variableType : new MdwVariableTypes().getVariableTypes()) {
            myVariableTypes.put(variableType.getName(), variableType);
        }
        for (Asset asset : AssetCache.getAssets("java")) {
            Package pkg = PackageCache.getPackage(asset.getPackageName());
            VariableType variableType = getAnnotatedVariableType(pkg, asset);
            if (variableType != null) {
                myVariableTypes.put(variableType.getName(), variableType);
            }
        }
        for (Asset asset : AssetCache.getAssets("kt")) {
            Package pkg = PackageCache.getPackage(asset.getPackageName());
            VariableType variableType = getAnnotatedVariableType(pkg, asset);
            if (variableType != null) {
                myVariableTypes.put(variableType.getName(), variableType);
            }
        }
        variableTypes = myVariableTypes;
    }

    /**
     * TODO: Supplier-driven (mdw-spring-boot AnnotationsScanner).
     */
    private VariableType getAnnotatedVariableType(Package pkg, Asset asset) {
        String translatorClass = pkg.getName() + "." + asset.getRootName();
        try {
            if (!asset.isLoaded())
                asset.load();
            if (asset.getText().contains("@Variable")) {
                VariableTranslator translator = pkg.getVariableTranslator(translatorClass);
                Variable annotation = translator.getClass().getAnnotation(Variable.class);
                return new VariableType(annotation.type(), translatorClass);
            }
        }
        catch (Throwable t) {
            logger.error("Cannot load " + translatorClass, t);
        }
        return null;
    }
}
