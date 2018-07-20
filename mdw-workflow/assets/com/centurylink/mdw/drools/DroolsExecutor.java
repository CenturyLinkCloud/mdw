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

import java.util.ArrayList;
import java.util.List;

import org.kie.api.KieBase;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.internal.command.CommandFactory;

import com.centurylink.mdw.cache.impl.PackageCache;
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

        ClassLoader cloudClassLoader = null;
        if (input.getContext() != null && input.getContext().getPackage() != null)
            cloudClassLoader = input.getContext().getPackage().getCloudClassLoader();
        if (cloudClassLoader == null) // fall back to rules asset pkg classloader
            cloudClassLoader = PackageCache.getPackage(rulesAsset.getPackageName()).getCloudClassLoader();

        KnowledgeBaseAsset kbAsset = DroolsKnowledgeBaseCache
                .getKnowledgeBaseAsset(rulesAsset.getName(), null, null, cloudClassLoader);
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
