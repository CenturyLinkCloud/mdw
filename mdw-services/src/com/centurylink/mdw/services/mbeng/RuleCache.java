/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.mbeng;

import java.util.List;

import com.centurylink.mdw.common.cache.CacheEnabled;
import com.centurylink.mdw.common.cache.CacheStore;
import com.centurylink.mdw.common.cache.FixedCapacityCache;
import com.centurylink.mdw.common.cache.impl.FixedCapacityCacheStore;
import com.centurylink.mdw.common.provider.CacheService;
import com.centurylink.mdw.model.value.variable.VariableVO;
import com.centurylink.mdw.services.mbeng.MbengMDWRuntime.PseudoVariableVO;
import com.qwest.mbeng.MbengException;
import com.qwest.mbeng.MbengRuleSet;
import com.qwest.mbeng.MbengVariable;

/**
 * This cache is for documents
 *
 */
public class RuleCache implements CacheEnabled, FixedCapacityCache, CacheService {

	static private CacheStore<String,MbengRuleSet> cache = null;

	public RuleCache() {
	}

	public void setCapacity(int capacity) {
	    cache = new FixedCapacityCacheStore<String,MbengRuleSet>(capacity);
	}

	public final int getCacheSize() { return cache.getSize(); }

    public final void clearCache() { if (cache!=null) cache.clear(); }

    public final void refreshCache() { cache.clear(); }

	public static MbengRuleSet getRuleSet(String name, String rule, char ruletype, List<VariableVO> vs)
			throws MbengException
	{
		MbengRuleSet ruleset = cache.get(name);
		if (ruleset==null) {
			ruleset = compileMagicRule(name, rule, ruletype, vs);
			cache.add(name, ruleset);
		}
		return ruleset;
	}

    private static MbengRuleSet compileMagicRule(String name, String rule, char ruletype,
            List<VariableVO> vs) throws MbengException {
        MbengRuleSet ruleset = new MdwMbengRuleSet(name, ruletype, vs);
//        ruleset.defineVariable(VariableVO.MASTER_REQUEST_ID, false);
        ruleset.parse(rule);
        return ruleset;
    }

    private static class MdwMbengRuleSet extends MbengRuleSet {

    	private List<VariableVO> processVariables;

		public MdwMbengRuleSet(String name, char type, List<VariableVO> vars) throws MbengException {
			super(name, type, false, false);
			this.processVariables = vars;
		}

		@Override
		protected void addVariable(String name, MbengVariable var) {
			super.addVariable(name, var);
		}

		@Override
		protected MbengVariable getVariable(String name) {
			MbengVariable var = super.getVariable(name);
			if (var!=null) return var;
			for (VariableVO v : processVariables) {
				if (v.getName().equals(name)) return v;
			}
			if (name.equals(VariableVO.MASTER_REQUEST_ID)) {
				var = new PseudoVariableVO(name);
				super.addVariable(name, var);
				return var;
			}
			return null;
		}

    }

}
