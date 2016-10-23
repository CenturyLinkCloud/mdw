/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.task.strategy;

import org.drools.KnowledgeBase;

import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.common.StrategyException;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.observer.task.ParameterizedStrategy;
import com.centurylink.mdw.workflow.drools.cache.DroolsKnowledgeBaseCache;
import com.centurylink.mdw.workflow.drools.cache.KnowledgeBaseAsset;

public abstract class RulesBasedStrategy extends ParameterizedStrategy {

    public static final String DECISION_TABLE_SHEET = "Decision Table Sheet";

    protected KnowledgeBase getKnowledgeBase() throws StrategyException {
        Object kbName = getParameter(getKnowledgeBaseAttributeName());
        if (kbName == null)
            throw new StrategyException("Missing strategy parameter: " + getKnowledgeBaseAttributeName());
        KnowledgeBase knowledgeBase = getKnowledgeBase(kbName.toString(), getDecisionTableSheetName());
        if (knowledgeBase == null)
            throw new StrategyException("Cannot load knowledge base: " + kbName);

        return knowledgeBase;
    }

    protected String getDecisionTableSheetName() {
        Object sheetName = getParameter(DECISION_TABLE_SHEET);
        return sheetName == null ? null : sheetName.toString();
    }

    /**
     * Returns the latest version.
     *
     * Modifier is sheet name.
     *
     * Override to apply additional or non-standard attribute conditions.
     * @throws StrategyException
     */
    protected KnowledgeBase getKnowledgeBase(String name, String modifier) throws StrategyException {
        KnowledgeBaseAsset kbrs = DroolsKnowledgeBaseCache.getKnowledgeBaseAsset(name, modifier, null, getClassLoader());

        if (kbrs == null) {
            return null;
        }
        else {
            return kbrs.getKnowledgeBase();
        }
    }
    /**
     * <p>
     * By default returns the package CloudClassloader
     * The knowledge based name is of the format "packageName/assetName", e.g "com.centurylink.mdw.test/TestDroolsRules.drl"
     * If an application needs a different class loader for Strategies then this method should be overridden
     * </p>
     * @return Class Loader used for Knowledge based strategies
     * @throws StrategyException
     */
    public ClassLoader getClassLoader() throws StrategyException {

        // Determine the package by parsing the attribute name (different name for different types of Rules based strategies)
        String kbAttributeName = getKnowledgeBaseAttributeName();
        Object kbName = getParameter(kbAttributeName);
        if (kbName == null)
            throw new StrategyException("Missing strategy parameter: " + kbAttributeName);

        // Get the package Name
        String kbNameStr = kbName.toString();
        int lastSlash = kbNameStr.lastIndexOf('/');
        String pkg = lastSlash == -1 ? null : kbNameStr.substring(0, lastSlash) ;

        Package pkgVO = PackageCache.getPackage(pkg);
        if (pkgVO == null)
            throw new StrategyException("Unable to get package name from strategy: " + kbAttributeName +" value="+kbNameStr);
        // return the cloud class loader by default, unless the bundle spec is set
        return pkgVO.getClassLoader();
    }

    protected abstract String getKnowledgeBaseAttributeName();

}
