package com.centurylink.mdw.drools;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.cache.CacheService;
import com.centurylink.mdw.cache.CachingException;
import com.centurylink.mdw.cache.PreloadableCache;
import com.centurylink.mdw.cache.asset.AssetCache;
import com.centurylink.mdw.cache.asset.PackageCache;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.asset.AssetVersionSpec;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import org.kie.api.KieBase;
import org.kie.api.KieBaseConfiguration;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;
import org.kie.api.runtime.KieContainer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

@RegisteredService(CacheService.class)
public class DroolsKnowledgeBaseCache implements PreloadableCache  {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private static volatile Map<String,KnowledgeBaseAsset> kbaseMap = Collections.synchronizedMap(new TreeMap<String,KnowledgeBaseAsset>());

    private static String[] preLoaded;

    public DroolsKnowledgeBaseCache() {

    }

    public void initialize(Map<String,String> params) {
        if (params != null) {
            String preLoadString = params.get("PreLoaded");
            if (preLoadString != null && preLoadString.trim().length() > 0) {
                List<String> preLoadList = new ArrayList<>();
                preLoaded = preLoadString.split("\\\n");
                for (int i = 0; i < preLoaded.length; i++) {
                    String preLoad = preLoaded[i].trim();
                    if (!preLoad.isEmpty())
                      preLoadList.add(preLoad);
                }
                preLoaded = preLoadList.toArray(new String[]{});
            }
        }
    }

    public static KnowledgeBaseAsset getKnowledgeBaseAsset(String name) {
        return getKnowledgeBaseAsset(name, null, null);
    }

    public static KnowledgeBaseAsset getKnowledgeBaseAsset(String name, String modifier) {
        return getKnowledgeBaseAsset(name, modifier, null);
    }

    public static synchronized KnowledgeBaseAsset getKnowledgeBaseAsset(String name, String modifier, ClassLoader loader) {

        Key key = new Key(name, modifier, loader);

        KnowledgeBaseAsset knowledgeBaseAsset = kbaseMap.get(key.toString());

        if (knowledgeBaseAsset == null) {
            try {
                logger.info("Loading KnowledgeBase Asset based on key: " + key);
                knowledgeBaseAsset = loadKnowledgeBaseAsset(key);
                kbaseMap.put(key.toString(), knowledgeBaseAsset);
            }
            catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
        return knowledgeBaseAsset;
    }

    // load asset based on specified version/range
    public static synchronized KnowledgeBaseAsset getKnowledgeBaseAsset(AssetVersionSpec drlAssetVerSpec, String modifier, ClassLoader loader) {
        Key key = new Key(drlAssetVerSpec, modifier, loader);
        KnowledgeBaseAsset knowledgeBaseAsset = kbaseMap.get(key.toString());
        if (knowledgeBaseAsset == null) {
            try {
                logger.info("Loading KnowledgeBase Asset based on key : "+ key);
                knowledgeBaseAsset = loadKnowledgeBaseAsset(key);
                kbaseMap.put(key.toString(), knowledgeBaseAsset);
            }
            catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
        return knowledgeBaseAsset;
    }

    public static KieBase getKnowledgeBase(String name) {
        return getKnowledgeBase(name, null);
    }

    public static KieBase getKnowledgeBase(String name, String modifier) {
        KnowledgeBaseAsset kbrs = getKnowledgeBaseAsset(name, modifier);
        if (kbrs == null)
            return null;
        else
            return kbrs.getKnowledgeBase();
    }

    public void clearCache() {
        kbaseMap.clear();
    }

    public int getCacheSize() {
        return kbaseMap.size();
    }

    public void loadCache() throws CachingException {
        load();
    }

    private synchronized void load() throws CachingException {
        Map<String,KnowledgeBaseAsset> kbaseMapTemp = Collections.synchronizedMap(new TreeMap<String,KnowledgeBaseAsset>());
        if (preLoaded != null) {   // otherwise load is performed lazily
            try {
                for (String preLoadKey : preLoaded) {
                    Key key = new Key(preLoadKey);
                    logger.info("PreLoading KnowledgeBase Asset based on key: " + key);
                    KnowledgeBaseAsset kbrs = loadKnowledgeBaseAsset(key);
                    if (kbrs != null) {
                        kbaseMapTemp.put(preLoadKey, kbrs);
                    }
                }
                kbaseMap = kbaseMapTemp;
            }
            catch (Exception ex) {
                throw new CachingException(ex.getMessage(), ex);
            }
        }
        else
            clearCache();
    }

    public synchronized void refreshCache() throws CachingException {
        //    clearCache();
            loadCache();
    }

    private static KnowledgeBaseAsset loadKnowledgeBaseAsset(Key key) throws IOException, CachingException {

        Asset asset = getAsset(key);

        if (asset == null) {
            throw new CachingException("No asset found for: '" + key.name + "'");
        }

        String rules = null;
        String extension = asset.getExtension();
        String name = asset.getName();

        if (extension.equals("xls") || extension.equals("xlsx") || extension.equals("csv")) {
            // decision table XLS, XLSX or CSV
            byte[] decodeBytes = asset.getContent();

            // modifier for decision table must be worksheet name
            DecisionTableProvider dtProvider = new DecisionTableProvider();
            if (key.modifier == null)
                rules = dtProvider.loadFromInputStream(new ByteArrayInputStream(decodeBytes), extension);
            else
                rules = dtProvider.loadFromInputStream(new ByteArrayInputStream(decodeBytes), extension, key.modifier);

            if (logger.isDebugEnabled())
                logger.debug("Converted rule for " + asset.getLabel() + ":\n" + rules + "\n================================");

            name = name.substring(0, name.lastIndexOf('.')) + ".drl";
        }
        else if (extension.equals("drl") || extension.equals("brl")) {
            // drools DRL or BRL
            rules = asset.getText();
        }
        else {
            throw new CachingException("Unsupported rules extension '" + extension + "' for " + asset.getLabel());
        }

        KieServices kieServices = KieServices.Factory.get();
        KieFileSystem kfs = kieServices.newKieFileSystem();
        kfs.write("src/main/resources/" + name, // this is bewildering
                    kieServices.getResources().newByteArrayResource(rules.getBytes()));

        // force setting system properties or else the following throws an NPE
        getProperties();
        KieBuilder kieBuilder = kieServices.newKieBuilder(kfs, key.loader).buildAll();
        Results results = kieBuilder.getResults();
        if(results.hasMessages(Message.Level.ERROR)) {
            if (extension.equals("xls") || extension.equals("xlsx")) {
                // log the converted rules
                logger.error("Converted rule for " + asset.getLabel() + ":\n" + rules + "\n================================");
            }
            throw new CachingException("Error parsing knowledge base from rules for " + asset.getLabel() + "\n" + results.getMessages());
        }
        else {
            KieContainer kieContainer =
                    kieServices.newKieContainer(kieServices.getRepository().getDefaultReleaseId());
            KieBaseConfiguration kieBaseConfiguration = kieServices.newKieBaseConfiguration(getProperties());
            KieBase kieBase = kieContainer.newKieBase(kieBaseConfiguration);
            System.out.println("Loaded KnowledgeBase: " + asset.getLabel());
            return new KnowledgeBaseAsset(kieBase, asset);
        }
    }

    private static Properties properties;
    public static Properties getProperties() throws IOException {
        if (properties == null) {
            properties = new Properties();
            File file = new File(System.getProperty(PropertyManager.MDW_CONFIG_LOCATION) + "/drools/drools.packagebuilder.conf");
            if (file.exists()) {
                properties.load(new FileInputStream(file));
            }
            else {
                Package thisPkg = PackageCache.getPackage(DroolsKnowledgeBaseCache.class.getPackage().getName());
                properties.load(thisPkg.getClassLoader().getResourceAsStream("drools.packagebuilder.conf"));
            }
            for (Object key : properties.keySet())
                System.setProperty(key.toString(), (String)properties.get(key));
        }
        return properties;
    }

    public static Asset getAsset(Key key) throws IOException {
        Asset asset = null;

        if (key.drlVersionSpec != null) {
            asset = AssetCache.getAsset(key.drlVersionSpec);
        }
        if (asset != null)
            return asset;

        String assetName = key.name == null ? key.drlVersionSpec.getQualifiedName() : key.name;
        return AssetCache.getAsset(assetName);
    }


    /**
     * eg: MyRuleName~myModifier#classLoader
     * eg with versionspec: MyPackgeName/MyRuleName v[0.1,1)~myModifier#classLoader
     */
    static class Key {
        String name;
        String modifier;
        AssetVersionSpec drlVersionSpec;
        ClassLoader loader;

        public Key(String name, String mod, ClassLoader loader) {
            this.name = name;
            this.modifier = mod;
            this.loader = loader;
        }

        public Key(AssetVersionSpec drlVersionSepc, String modifier, ClassLoader loader) {
            super();
            this.modifier = modifier;
            this.drlVersionSpec = drlVersionSepc;
            this.loader = loader;
        }

        public Key(String stringVal) {
            String toParse = stringVal;
            int hash = toParse.indexOf('#');
            if (hash > 0) {
                //TODO : how to convert a string to class loader
                toParse = toParse.substring(0, hash);
            }
            int squig = toParse.indexOf('~');
            if (squig >= 0) {
                modifier = toParse.substring(squig + 1);
                toParse = toParse.substring(0, squig);
            }
            AssetVersionSpec drlVerSpec = AssetVersionSpec.parse(toParse);
            if (drlVerSpec != null) {
                this.drlVersionSpec = drlVerSpec;
            } else {
                name = toParse;
            }
        }

        public String toString() {
            String key = drlVersionSpec != null ? drlVersionSpec.toString() : name;
            if (modifier != null)
                key += "~" + modifier;
            if (loader != null)
                key += "#" + loader;
            return key;
        }
    }
}