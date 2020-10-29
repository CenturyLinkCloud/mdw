package com.centurylink.mdw.drools;

import java.util.ArrayList;
import java.util.List;

import org.kie.api.KieBase;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.internal.command.CommandFactory;

import com.centurylink.mdw.cache.asset.PackageCache;
import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.rules.Operand;
import com.centurylink.mdw.rules.RulesExecutor;
import com.centurylink.mdw.script.ExecutionException;

/**
 * Drools rules executor.
 */
public class DroolsExecutor implements RulesExecutor {

    @Override
    public String getName() {
        return "Drools 6.5.0.Final";
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object execute(Asset rulesAsset, Operand input) throws ExecutionException {

        ClassLoader packageClassLoader = null;
        if (input.getContext() != null && input.getContext().getPackage() != null)
            packageClassLoader = input.getContext().getPackage().getClassLoader();
        if (packageClassLoader == null) // fall back to rules asset pkg classloader
            packageClassLoader = PackageCache.getPackage(rulesAsset.getPackageName()).getClassLoader();

        KnowledgeBaseAsset kbAsset = DroolsKnowledgeBaseCache
                .getKnowledgeBaseAsset(rulesAsset.getName(), null, packageClassLoader);
        if (kbAsset == null) {
            throw new ExecutionException("Cannot load KnowledgeBase asset: "
                    + rulesAsset.getPackageName() + "/" + rulesAsset.getLabel());
        }

        KieBase knowledgeBase = kbAsset.getKnowledgeBase();

        StatelessKieSession kSession = knowledgeBase.newStatelessKieSession();

        List<Object> facts = new ArrayList<Object>();
        if (input.getInput() != null) {
            facts.add(input.getInput()); // direct access
            if (input.getInput() instanceof Jsonable)
                facts.add(((Jsonable)input.getInput()).getJson());
        }

        kSession.setGlobal("operand", input);
        kSession.execute(CommandFactory.newInsertElements(facts));
        return input.getResult();
    }
}
