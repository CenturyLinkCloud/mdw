package com.centurylink.mdw.rules;

import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.script.ExecutionException;

public interface RulesExecutor {

    public String getName();

    /**
     * @param rulesAsset rules to apply
     * @param input to apply rules against (also accumulates result)
     * @return result of applying rules (same as Operand.getResult())
     */
    public Object execute(Asset rulesAsset, Operand input) throws ExecutionException;
}
