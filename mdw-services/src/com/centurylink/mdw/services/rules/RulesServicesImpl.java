/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.rules;

import java.time.Instant;

import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.RuntimeContext;
import com.centurylink.mdw.rules.Operand;
import com.centurylink.mdw.rules.RulesExecutor;
import com.centurylink.mdw.services.RulesServices;

public class RulesServicesImpl implements RulesServices {

    public static final String DROOLS_EXECUTOR = "com.centurylink.mdw.drools.DroolsExecutor";

    public Object applyRules(Asset rulesAsset, Operand input) throws ServiceException {
        return applyRules(rulesAsset, input, DROOLS_EXECUTOR);
    }

    public Object applyRules(Asset rulesAsset, Operand input, String executor)
            throws ServiceException {
        RuntimeContext context = input.getContext();
        // prefer context package for class loading
        Package pkg = context == null ? null : context.getPackage();
        if (pkg == null) {
            // if no context, use rules asset package
            pkg = PackageCache.getPackage(rulesAsset.getPackageName());
        }
        try {
            Class<? extends RulesExecutor> executorClass = Class
                    .forName(executor, true, pkg.getCloudClassLoader())
                    .asSubclass(RulesExecutor.class);
            RulesExecutor executorImpl = executorClass.newInstance();

            input.setStart(Instant.now());
            Object result = executorImpl.execute(rulesAsset, input);
            input.setEnd(Instant.now());
            return result;
        }
        catch (Exception ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }
}
