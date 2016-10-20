/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.drools.cache;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.compiler.PackageBuilderConfiguration;
import org.drools.io.ResourceFactory;

import com.centurylink.mdw.common.Compatibility;
import com.centurylink.mdw.common.Compatibility.SubstitutionResult;
import com.centurylink.mdw.common.cache.PreloadableCache;
import com.centurylink.mdw.common.cache.impl.RuleSetCache;
import com.centurylink.mdw.common.exception.CachingException;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.attribute.AssetVersionSpec;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.workflow.drools.DecisionTableProvider;

public class DroolsKnowledgeBaseCache implements PreloadableCache  {

    private static final String[] LANGUAGES = new String[] {RuleSetVO.DROOLS, RuleSetVO.EXCEL, RuleSetVO.EXCEL_2007, RuleSetVO.GUIDED};

    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private static volatile Map<String,KnowledgeBaseRuleSet> kbaseMap = Collections.synchronizedMap(new TreeMap<String,KnowledgeBaseRuleSet>());

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

    public static KnowledgeBaseRuleSet getKnowledgeBaseRuleSet(String name) {
        return getKnowledgeBaseRuleSet(name, null, null);
    }

    public static KnowledgeBaseRuleSet getKnowledgeBaseRuleSet(String name, String modifier) {
        return getKnowledgeBaseRuleSet(name, modifier, null);
    }

    public static KnowledgeBaseRuleSet getKnowledgeBaseRuleSet(String name, String modifier, Map<String,String> attributes) {
        return getKnowledgeBaseRuleSet(name, modifier, null, getDefaultClassLoader());
    }

    public static synchronized KnowledgeBaseRuleSet getKnowledgeBaseRuleSet(String name, String modifier, Map<String,String> attributes, ClassLoader loader) {

        Key key = new Key(name, modifier, attributes, loader);

        KnowledgeBaseRuleSet knowledgeBaseRuleSet = kbaseMap.get(key.toString());

        if (knowledgeBaseRuleSet == null) {
            try {
                logger.info("Loading KnowledgeBase RuleSet based on key: " + key);
                knowledgeBaseRuleSet = loadKnowledgeBaseRuleSet(key);
                kbaseMap.put(key.toString(), knowledgeBaseRuleSet);
            }
            catch (Exception ex) {
                logger.severeException(ex.getMessage(), ex);
            }
        }
        return knowledgeBaseRuleSet;
    }

    // load asset based on specified version/range
    public static synchronized KnowledgeBaseRuleSet getKnowledgeBaseRuleSet(AssetVersionSpec drlAssetVerSpec, String modifier, Map<String,String> attributes, ClassLoader loader) {
        Key key = new Key(drlAssetVerSpec, modifier, attributes, loader);
        KnowledgeBaseRuleSet knowledgeBaseRuleSet = kbaseMap.get(key.toString());
        if (knowledgeBaseRuleSet == null) {
            try {
                logger.info("Loading KnowledgeBase RuleSet based on key : "+ key);
                knowledgeBaseRuleSet = loadKnowledgeBaseRuleSet(key);
                kbaseMap.put(key.toString(), knowledgeBaseRuleSet);
            }
            catch (Exception ex) {
                logger.severeException(ex.getMessage(), ex);
            }
        }
        return knowledgeBaseRuleSet;
    }

    public static KnowledgeBase getKnowledgeBase(String name) {
        return getKnowledgeBase(name, null, null);
    }

    public static KnowledgeBase getKnowledgeBase(String name, String modifier) {
        return getKnowledgeBase(name, modifier, null);
    }

    public static KnowledgeBase getKnowledgeBase(String name, String modifier, Map<String,String> attributes) {
        KnowledgeBaseRuleSet kbrs = getKnowledgeBaseRuleSet(name, modifier, attributes);
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
        Map<String,KnowledgeBaseRuleSet> kbaseMapTemp = Collections.synchronizedMap(new TreeMap<String,KnowledgeBaseRuleSet>());
        if (preLoaded != null) {   // otherwise load is performed lazily
            try {
                for (String preLoadKey : preLoaded) {
                    Key key = new Key(preLoadKey);
                    logger.info("PreLoading KnowledgeBase RuleSet based on key: " + key);
                    KnowledgeBaseRuleSet kbrs = loadKnowledgeBaseRuleSet(key);
                    if (kbrs != null) {
                        kbaseMapTemp.put(preLoadKey, kbrs);
                    }
                }
                kbaseMap = kbaseMapTemp;
            }
            catch (Exception ex) {
                throw new CachingException(-1, ex.getMessage(), ex);
            }
        }
        else
            clearCache();
    }

    public synchronized void refreshCache() throws CachingException {
        //    clearCache();
            loadCache();
    }

    private static KnowledgeBaseRuleSet loadKnowledgeBaseRuleSet(Key key) throws IOException, CachingException {

        RuleSetVO ruleSet = getRuleSet(key);

        if (ruleSet == null) {
            throw new CachingException("No rule set found for: '" + key.name + "'");
        }

        PackageBuilderConfiguration pbConfig = null;
        pbConfig = new PackageBuilderConfiguration();
        pbConfig.setProperty("drools.dialect.java.compiler", "JANINO");

        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder(pbConfig);

        String rules = null;
        String format = ruleSet.getLanguage();

        if (format.equals(RuleSetVO.EXCEL) || format.equals(RuleSetVO.EXCEL_2007) || format.equals(RuleSetVO.CSV)) {
            // decision table XLS, XLSX or CSV
            byte[] decodeBytes = ruleSet.getContent();

            // modifier for decision table must be worksheet name
            DecisionTableProvider dtProvider = new DecisionTableProvider();
            if (key.modifier == null)
                rules = dtProvider.loadFromInputStream(new ByteArrayInputStream(decodeBytes), format);
            else
                rules = dtProvider.loadFromInputStream(new ByteArrayInputStream(decodeBytes), format, key.modifier);

            if (Compatibility.hasCodeSubstitutions())
                rules = doCompatibilityCodeSubstitutions(ruleSet.getLabel(), rules);
            if (logger.isDebugEnabled())
                logger.debug("Converted rule for " + ruleSet.getDescription() + ":\n" + rules + "\n================================");
        }
        else if (format.equals(RuleSetVO.DROOLS) || format.equals(RuleSetVO.GUIDED)) {
            // drools DRL or BRL
            rules = ruleSet.getRuleSet();
            if (Compatibility.hasCodeSubstitutions())
                rules = doCompatibilityCodeSubstitutions(ruleSet.getLabel(), rules);
        }
        else {
            throw new CachingException("Unsupported rules format '" + format + "' for " + ruleSet.getDescription());
        }

        ResourceType resourceType = format.equals(RuleSetVO.GUIDED) ? ResourceType.BRL : ResourceType.DRL;
        kbuilder.add(ResourceFactory.newByteArrayResource(rules.getBytes()), resourceType);

        if (kbuilder.hasErrors()) {
            if (format.equals(RuleSetVO.EXCEL) || format.equals(RuleSetVO.EXCEL_2007)) {
                // log the converted rules
                logger.severe("Converted rule for " + ruleSet.getDescription() + ":\n" + rules + "\n================================");
            }
            throw new CachingException("Error parsing knowledge base from rules for " + ruleSet.getDescription() + "\n" + kbuilder.getErrors());
        }
        else {
            KnowledgeBase knowledgeBase = KnowledgeBaseFactory.newKnowledgeBase();
            knowledgeBase.addKnowledgePackages(kbuilder.getKnowledgePackages());
            logger.info("Loaded KnowledgeBase from RuleSet: " + ruleSet.getLabel());
            return new KnowledgeBaseRuleSet(knowledgeBase, ruleSet);
        }
    }

    public static RuleSetVO getRuleSet(Key key) {
        RuleSetVO ruleSet = null;

        if (key.drlVersionSpec != null) {
            ruleSet = key.attributes == null ? RuleSetCache.getRuleSet(key.drlVersionSpec) : RuleSetCache.getRuleSet(key.drlVersionSpec, key.attributes);
        }
        if (ruleSet != null)
            return ruleSet;

        String ruleSetName = key.name == null ? key.drlVersionSpec.getQualifiedName() : key.name;

        if (key.attributes == null) {
            ruleSet = RuleSetCache.getRuleSet(ruleSetName, LANGUAGES);
        }
        else {
            for (int i = 0; i < LANGUAGES.length && ruleSet == null; i++) {
                ruleSet = RuleSetCache.getLatestRuleSet(ruleSetName, LANGUAGES[i], key.attributes);
            }
        }
        return ruleSet;
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