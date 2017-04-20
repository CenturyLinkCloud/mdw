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
package com.centurylink.mdw.drools;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

//import org.drools.KnowledgeBase;
import org.kie.internal.KnowledgeBase;
import org.kie.internal.KnowledgeBaseFactory;
//import org.drools.KnowledgeBaseFactory;
//import org.drools.builder.KnowledgeBuilder;
import org.kie.internal.builder.KnowledgeBuilder;
import org.kie.internal.builder.KnowledgeBuilderConfiguration;
//import org.drools.builder.KnowledgeBuilderConfiguration;
//import org.drools.builder.KnowledgeBuilderFactory;
import org.kie.internal.builder.KnowledgeBuilderFactory;
import org.kie.internal.io.ResourceFactory;
import org.kie.api.io.ResourceType;
//import org.drools.builder.ResourceType;
//import org.drools.io.ResourceFactory;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.app.Compatibility;
import com.centurylink.mdw.app.Compatibility.SubstitutionResult;
import com.centurylink.mdw.cache.CachingException;
import com.centurylink.mdw.cache.PreloadableCache;
import com.centurylink.mdw.cache.impl.AssetCache;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.asset.AssetVersionSpec;
import com.centurylink.mdw.provider.CacheService;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

@RegisteredService(CacheService.class)
public class DroolsKnowledgeBaseCache implements PreloadableCache  {

    private static final String[] LANGUAGES = new String[] {Asset.DROOLS, Asset.EXCEL, Asset.EXCEL_2007, Asset.GUIDED};

    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private static volatile Map<String,KnowledgeBaseAsset> kbaseMap = Collections.synchronizedMap(new TreeMap<String,KnowledgeBaseAsset>());

    private static String[] preLoaded;

    public DroolsKnowledgeBaseCache() {

    }

    public DroolsKnowledgeBaseCache(Map<String,String> params) {

    }

    public void initialize(Map<String,String> params) {
        if (params != null) {
            String preLoadString = params.get("PreLoaded");
            if (preLoadString != null && preLoadString.trim().length() > 0) {
                List<String> preLoadList = new ArrayList<String>();
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

    public static KnowledgeBaseAsset getKnowledgeBaseAsset(String name, String modifier, Map<String,String> attributes) {
        return getKnowledgeBaseAsset(name, modifier, null, getDefaultClassLoader());
    }

    public static synchronized KnowledgeBaseAsset getKnowledgeBaseAsset(String name, String modifier, Map<String,String> attributes, ClassLoader loader) {

        Key key = new Key(name, modifier, attributes, loader);

        KnowledgeBaseAsset knowledgeBaseAsset = kbaseMap.get(key.toString());

        if (knowledgeBaseAsset == null) {
            try {
                logger.info("Loading KnowledgeBase Asset based on key: " + key);
                knowledgeBaseAsset = loadKnowledgeBaseAsset(key);
                kbaseMap.put(key.toString(), knowledgeBaseAsset);
            }
            catch (Exception ex) {
                logger.severeException(ex.getMessage(), ex);
            }
        }
        return knowledgeBaseAsset;
    }

    // load asset based on specified version/range
    public static synchronized KnowledgeBaseAsset getKnowledgeBaseAsset(AssetVersionSpec drlAssetVerSpec, String modifier, Map<String,String> attributes, ClassLoader loader) {
        Key key = new Key(drlAssetVerSpec, modifier, attributes, loader);
        KnowledgeBaseAsset knowledgeBaseAsset = kbaseMap.get(key.toString());
        if (knowledgeBaseAsset == null) {
            try {
                logger.info("Loading KnowledgeBase Asset based on key : "+ key);
                knowledgeBaseAsset = loadKnowledgeBaseAsset(key);
                kbaseMap.put(key.toString(), knowledgeBaseAsset);
            }
            catch (Exception ex) {
                logger.severeException(ex.getMessage(), ex);
            }
        }
        return knowledgeBaseAsset;
    }

    public static KnowledgeBase getKnowledgeBase(String name) {
        return getKnowledgeBase(name, null, null);
    }

    public static KnowledgeBase getKnowledgeBase(String name, String modifier) {
        return getKnowledgeBase(name, modifier, null);
    }

    public static KnowledgeBase getKnowledgeBase(String name, String modifier, Map<String,String> attributes) {
        KnowledgeBaseAsset kbrs = getKnowledgeBaseAsset(name, modifier, attributes);
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

        KnowledgeBuilderConfiguration conf = KnowledgeBuilderFactory.newKnowledgeBuilderConfiguration(null, key.loader);
        conf.setProperty("drools.dialect.default", "mvel");
        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder(conf);

        String rules = null;
        String format = asset.getLanguage();

        if (format.equals(Asset.EXCEL) || format.equals(Asset.EXCEL_2007) || format.equals(Asset.CSV)) {
            // decision table XLS, XLSX or CSV
            byte[] decodeBytes = asset.getContent();

            // modifier for decision table must be worksheet name
            DecisionTableProvider dtProvider = new DecisionTableProvider();
            if (key.modifier == null)
                rules = dtProvider.loadFromInputStream(new ByteArrayInputStream(decodeBytes), format);
            else
                rules = dtProvider.loadFromInputStream(new ByteArrayInputStream(decodeBytes), format, key.modifier);

            if (Compatibility.hasCodeSubstitutions())
                rules = doCompatibilityCodeSubstitutions(asset.getLabel(), rules);
            if (logger.isDebugEnabled())
                logger.debug("Converted rule for " + asset.getDescription() + ":\n" + rules + "\n================================");
        }
        else if (format.equals(Asset.DROOLS) || format.equals(Asset.GUIDED)) {
            // drools DRL or BRL
            rules = asset.getStringContent();
            if (Compatibility.hasCodeSubstitutions())
                rules = doCompatibilityCodeSubstitutions(asset.getLabel(), rules);
        }
        else {
            throw new CachingException("Unsupported rules format '" + format + "' for " + asset.getDescription());
        }

        ResourceType resourceType = format.equals(Asset.GUIDED) ? ResourceType.BRL : ResourceType.DRL;
        kbuilder.add(ResourceFactory.newByteArrayResource(rules.getBytes()), resourceType);

        if (kbuilder.hasErrors()) {
            if (format.equals(Asset.EXCEL) || format.equals(Asset.EXCEL_2007)) {
                // log the converted rules
                logger.severe("Converted rule for " + asset.getDescription() + ":\n" + rules + "\n================================");
            }
            throw new CachingException("Error parsing knowledge base from rules for " + asset.getDescription() + "\n" + kbuilder.getErrors());
        }
        else {
            KnowledgeBase knowledgeBase = KnowledgeBaseFactory.newKnowledgeBase();
            knowledgeBase.addKnowledgePackages(kbuilder.getKnowledgePackages());
            logger.info("Loaded KnowledgeBase from Asset: " + asset.getLabel());
            return new KnowledgeBaseAsset(knowledgeBase, asset);
        }
    }

    public static Asset getAsset(Key key) {
        Asset asset = null;

        if (key.drlVersionSpec != null) {
            asset = key.attributes == null ? AssetCache.getAsset(key.drlVersionSpec) : AssetCache.getAsset(key.drlVersionSpec, key.attributes);
        }
        if (asset != null)
            return asset;

        String assetName = key.name == null ? key.drlVersionSpec.getQualifiedName() : key.name;

        if (key.attributes == null) {
            asset = AssetCache.getAsset(assetName, LANGUAGES);
        }
        else {
            for (int i = 0; i < LANGUAGES.length && asset == null; i++) {
                asset = AssetCache.getLatestAssets(assetName, LANGUAGES[i], key.attributes);
            }
        }
        return asset;
    }


    /**
     * eg: MyRuleName~myModifier{attr1=attr1val,attr2=attr2val}#classLoader
     * eg with versionspec: MyPackgeName/MyRuleName v[0.1,1)~myModifier{attr1=attr1val,attr2=attr2val}#classLoader
     */
    static class Key {
        String name;
        String modifier;
        Map<String,String> attributes;
        AssetVersionSpec drlVersionSpec;
        ClassLoader loader;

        public Key(String name, String mod, Map<String,String> attrs, ClassLoader loader) {
            this.name = name;
            this.modifier = mod;
            this.attributes = attrs;
            this.loader = loader;
        }

        public Key(AssetVersionSpec drlVersionSepc, String modifier, Map<String, String> attributes, ClassLoader loader) {
            super();
            this.modifier = modifier;
            this.attributes = attributes;
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
            int brace = toParse.indexOf('{');
            if (brace >= 0) {
                attributes = stringToMap(toParse.substring(brace, toParse.lastIndexOf('}') + 1));
                toParse = toParse.substring(0, brace);
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
            if (attributes != null && !attributes.isEmpty())
                key += mapToString(attributes);
            if (loader != null)
                key += "#" + loader;
            return key;
        }

        String mapToString(Map<String,String> map) {
            if (map == null)
                return "";

            String string = "{";
            int i = 0;
            for (String key : map.keySet()) {
                string += key + "=" + map.get(key);
                if (i < map.keySet().size() - 1)
                    string += ",";
                i++;
            }
            string += "}";
            return string;
        }

        Map<String,String> stringToMap(String string) {
            Map<String,String> map = null;
            if (string != null) {
                map = new HashMap<String,String>();
                String toParse = string.substring(string.indexOf('{') + 1, string.lastIndexOf('}'));
                for (String attr : toParse.split(",")) {
                    int eq = attr.indexOf('=');
                    map.put(attr.substring(0, eq), attr.substring(eq + 1));
                }
            }
            return map;
        }
    }

    protected static String doCompatibilityCodeSubstitutions(String label, String in) throws IOException {
        SubstitutionResult substitutionResult = Compatibility.getInstance().performCodeSubstitutions(in);
        if (!substitutionResult.isEmpty()) {
            logger.warn("Compatibility substitutions applied for Drools asset " + label + " (details logged at debug level).");
            if (logger.isDebugEnabled())
                logger.debug("Compatibility substitutions for " + label + ":\n" + substitutionResult.getDetails());
            if (logger.isMdwDebugEnabled())
                logger.mdwDebug("Substitution output for " + label + ":\n" + substitutionResult.getOutput());
            return substitutionResult.getOutput();
        }
        return in;
    }

    public static ClassLoader getDefaultClassLoader() {
        return DroolsKnowledgeBaseCache.class.getClassLoader();
    }
}