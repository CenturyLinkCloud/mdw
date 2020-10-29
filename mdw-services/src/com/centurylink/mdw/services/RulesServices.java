package com.centurylink.mdw.services;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.rules.Operand;

public interface RulesServices {
    /**
     * Apply rules using default impl (drools).
     * @param rulesAsset rule to apply
     * @param input input facts (which also accumulate results)
     * @return result object (same as Operand.getResult())
     */
    public Object applyRules(Asset rulesAsset, Operand input) throws ServiceException;

    /**
     * Apply rules using specified executor class.
     * @param rulesAsset rule to apply
     * @param input input facts (which also accumulate results)
     * @param executor fully-qualified class name of executor impl
     * @return result object (same as Operand.getResult())
     */
    public Object applyRules(Asset rulesAsset, Operand input, String executor)
            throws ServiceException;
}
