/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.camel;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.naming.NamingException;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.model.RoutesDefinition;
import org.springframework.beans.factory.annotation.Autowired;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.cache.CacheService;
import com.centurylink.mdw.cache.CachingException;
import com.centurylink.mdw.cache.PreloadableCache;
import com.centurylink.mdw.cache.impl.AssetCache;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.asset.AssetVersionSpec;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

@RegisteredService(CacheService.class)
public class CamelRouteCache implements PreloadableCache, CamelContextAware {

    private static final String[] LANGUAGES = new String[] {Asset.CAMEL_ROUTE};
    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private static volatile Map<String,RoutesDefinitionRuleSet> routesMap = Collections.synchronizedMap(new TreeMap<String,RoutesDefinitionRuleSet>());
    private static String[] preLoaded;

    @Autowired
    private static CamelContext camelContext;

    public CamelContext getCamelContext() {
        return camelContext;
    }

    public void setCamelContext(CamelContext context) {
        camelContext = context;
    }

    public CamelRouteCache() {
        camelContext = getCamelContext();
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


    public static RoutesDefinitionRuleSet RoutesDefinitionRuleSet(String name) {
        return getRoutesDefinitionRuleSet(name, null, null);
    }

    public static RoutesDefinitionRuleSet getRoutesDefinitionRuleSet(String name, String modifier) {
        return getRoutesDefinitionRuleSet(name, modifier, null);
    }

    public static RoutesDefinitionRuleSet getRoutesDefinitionRuleSet(String name, String modifier, Map<String,String> attributes) {

        Key key = new Key(name, modifier, attributes);

        RoutesDefinitionRuleSet routesRuleSet = routesMap.get(key.toString());

        if (routesRuleSet == null) {
            try {
                synchronized (routesMap) {
                    routesRuleSet = routesMap.get(key.toString());
                    if (routesRuleSet == null) {
                        logger.info("Loading RoutesDefinition RuleSet based on key: " + key);
                        routesRuleSet = loadRoutesDefinitionRuleSet(key);
                        routesMap.put(key.toString(), routesRuleSet);
                    }
                }
            }
            catch (Exception ex) {
                logger.severeException(ex.getMessage(), ex);
            }
        }
        return routesRuleSet;
    }

    /**
     * Routes definition based on the route definition name , version and attributes
     *
     * @param routeVersionSpec
     * @param modifier
     * @param attributes
     * @return RoutesDefinitionRuleSet
     */
    public static RoutesDefinitionRuleSet getRoutesDefinitionRuleSet(AssetVersionSpec routeVersionSpec, String modifier, Map<String,String> attributes) {
        Key key = new Key(routeVersionSpec, modifier, attributes);
        RoutesDefinitionRuleSet routesRuleSet = routesMap.get(key.toString());
        if (routesRuleSet == null) {
            try {
                synchronized (routesMap) {
                    routesRuleSet = routesMap.get(key.toString());
                    if (routesRuleSet == null) {
                        logger.info("Loading RoutesDefinition RuleSet based on key: " + key);
                        routesRuleSet = loadRoutesDefinitionRuleSet(key);
                        routesMap.put(key.toString(), routesRuleSet);
                    }
                }
            }
            catch (Exception ex) {
                logger.severeException(ex.getMessage(), ex);
            }
        }
        return routesRuleSet;
    }

    public static RoutesDefinition getRoutesDefinition(String name) {
        return getRoutesDefinition(name, null, null);
    }

    public static RoutesDefinition getRoutesDefinition(String name, String modifier) {
        return getRoutesDefinition(name, modifier, null);
    }

    public static RoutesDefinition getRoutesDefinition(String name, String modifier, Map<String,String> attributes) {
        RoutesDefinitionRuleSet rdrs = getRoutesDefinitionRuleSet(name, modifier, attributes);
        if (rdrs == null)
            return null;
        else
            return rdrs.getRoutesDefinition();
    }

    public void clearCache() {
        getCamelContext();
        synchronized (routesMap) {
            for (String routeName : routesMap.keySet()) {
                RoutesDefinitionRuleSet rdrs = routesMap.get(routeName);
                try {
                    camelContext.removeRouteDefinitions(rdrs.getRoutesDefinition().getRoutes());
                }
                catch (Exception ex) {
                    logger.severeException(ex.getMessage(), ex);
                }
            }
            routesMap.clear();
        }
    }

    public int getCacheSize() {
        return routesMap.size();
    }

    public void loadCache() throws CachingException {
        if (preLoaded != null) {   // otherwise load is performed lazily
            try {
                synchronized (routesMap) {
                    for (String preLoadKey : preLoaded) {
                        Key key = new Key(preLoadKey);

                        logger.info("PreLoading Camel Route based on key: " + key);
                        RoutesDefinitionRuleSet rdrs = loadRoutesDefinitionRuleSet(key);
                        if (rdrs != null) {
                            routesMap.put(preLoadKey, rdrs);
                        }
                    }
                }
            }
            catch (Exception ex) {
                throw new CachingException(ex.getMessage(), ex);
            }
        }
    }

    public void refreshCache() throws CachingException {
        synchronized (routesMap) {
            clearCache();
            loadCache();
        }
    }

    private static RoutesDefinitionRuleSet loadRoutesDefinitionRuleSet(Key key) throws CachingException {
        if (camelContext == null) {
            try {
              camelContext = TomcatCamelContext.getInstance().getCamelContext();
            }
            catch (NamingException ex) {
                throw new CachingException("Cannot access Tomcat CamelContext", ex);
            }
        }
        if (camelContext == null)
            throw new CachingException("Cannot access CamelContext");

        Asset ruleSet = getRuleSet(key);
        if (ruleSet == null)
            throw new CachingException("No rule set found for: '" + key.name + "'");

        String appendToUri = "/" + (key.name == null ? key.routeVersionSpec.getQualifiedName() : key.name);
        if (key.modifier != null && key.modifier.trim().length() > 0)
            appendToUri += "?" + key.modifier;

        // TODO better expression
        String subst = ruleSet.getStringContent().replaceAll("mdw:workflow/this", "mdw:workflow" + appendToUri);
        if (subst.equals(ruleSet.getStringContent()))
            subst = ruleSet.getStringContent().replaceAll("mdw:workflow", "mdw:workflow" + appendToUri);
        ruleSet = new Asset(ruleSet);  // create a copy
        ruleSet.setStringContent(subst);

        if (logger.isDebugEnabled())
            logger.debug("Loading substituted camel routes " + ruleSet.getDescription() + ":\n" + ruleSet.getStringContent() + "\n================================");

        String format = ruleSet.getLanguage();

        CamelContext localCamelContext = camelContext;

        if (format.equals(Asset.CAMEL_ROUTE)) {
            // Spring DSL Camel Route
            logger.info("Loading Camel Route '" + ruleSet.getLabel() + "' with CamelContext: " + localCamelContext);
            try {
                ByteArrayInputStream inStream = new ByteArrayInputStream(ruleSet.getRawContent());
                RoutesDefinition routesDefinition = localCamelContext.loadRoutesDefinition(inStream);
                if (localCamelContext.hasComponent("mdw") == null)
                    localCamelContext.addComponent("mdw", new MdwComponent());
                localCamelContext.addRouteDefinitions(routesDefinition.getRoutes());
                return new RoutesDefinitionRuleSet(routesDefinition, ruleSet);
            }
            catch (Exception ex) {
                logger.severeException(ex.getMessage(), ex);
                throw new CachingException(ex.getMessage(), ex);
            }
        }

        throw new CachingException("Unsupported ruleSet language: " + format);
    }

    public static Asset getRuleSet(Key key) {
        Asset ruleSet = null;
        if (key.routeVersionSpec != null) {
            ruleSet = key.attributes == null ? AssetCache.getAsset(key.routeVersionSpec) : AssetCache.getAsset(key.routeVersionSpec, key.attributes);
        }
        if (ruleSet != null)
            return ruleSet;

        String ruleSetName = key.name == null ? key.routeVersionSpec.getQualifiedName() : key.name;
        if (key.attributes == null) {
            ruleSet = AssetCache.getAsset(ruleSetName, LANGUAGES);
        }
        else {
            for (int i = 0; i < LANGUAGES.length && ruleSet == null; i++) {
                ruleSet = AssetCache.getLatestAssets(ruleSetName, LANGUAGES[i], key.attributes);
            }
        }
        return ruleSet;
    }

    /**
     * eg: MyRoutesName~myModifier{attr1=attr1val,attr2=attr2val}
     * eg with versionspec : MyPackage/MyRoutesName v[.5,1)~myModifier{attr1=attr1val,attr2=attr2val}
     */
    static class Key {
        String name;
        String modifier;
        Map<String,String> attributes;
        AssetVersionSpec routeVersionSpec;

        public Key(String name, String mod, Map<String,String> attrs) {
            this.name = name;
            this.modifier = mod;
            this.attributes = attrs;
        }

        public Key(AssetVersionSpec routeVersionSpec, String modifier, Map<String, String> attributes) {
            super();
            this.modifier = modifier;
            this.routeVersionSpec = routeVersionSpec;
            this.attributes = attributes;
        }

        public Key(String stringVal) {
            String toParse = stringVal;
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
            AssetVersionSpec routeSpec = AssetVersionSpec.parse(toParse);
            if (routeSpec != null) {
                this.routeVersionSpec = routeSpec;
            } else {
                name = toParse;
            }
        }

        public String toString() {
            String key = routeVersionSpec == null ? name : routeVersionSpec.toString();
            if (modifier != null)
                key += "~" + modifier;
            if (attributes != null && !attributes.isEmpty())
                key += mapToString(attributes);
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
}
