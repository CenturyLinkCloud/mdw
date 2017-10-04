/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.rules;

import org.json.JSONObject;

import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.java.CompiledJavaCache;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.RuntimeContext;
import com.centurylink.mdw.services.RulesServices;

public class RulesServicesImpl implements RulesServices {

    public static final String DROOLS_EXECUTOR = "com.centurylink.mdw.drools.DroolsExecutor";

    public JSONObject applyRules(Asset rulesAsset, JSONObject input) throws ServiceException {
        return applyRules(rulesAsset, input, null, DROOLS_EXECUTOR);
    }

    public JSONObject applyRules(Asset rulesAsset, JSONObject input, RuntimeContext context, String executor)
            throws ServiceException {
        Package pkg = context == null ? null : context.getPackage();
        if (pkg == null)
            pkg = PackageCache.getPackage(rulesAsset.getPackageName());
        try {
            Class<? extends RulesExecutor> execClass = CompiledJavaCache
                    .getClassFromAssetName(getClass().getClassLoader(), executor)
                    .asSubclass(RulesExecutor.class);
            RulesExecutor executorImpl = execClass.newInstance();

            return executorImpl.execute(rulesAsset, input, context);
        }
        catch (Exception ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }
}
