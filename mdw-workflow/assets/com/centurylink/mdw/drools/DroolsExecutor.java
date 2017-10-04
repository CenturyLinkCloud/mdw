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
import java.util.Map;

import org.json.JSONObject;
import org.kie.api.KieBase;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.internal.command.CommandFactory;

import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.workflow.RuntimeContext;
import com.centurylink.mdw.script.ExecutionException;
import com.centurylink.mdw.services.rules.RulesExecutor;

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
    public JSONObject execute(Asset rulesAsset, JSONObject inputFacts,
            RuntimeContext context) throws ExecutionException {

        ClassLoader cloudClassLoader = null;
        if (context != null)
            cloudClassLoader = context.getPackage().getCloudClassLoader();
        if (cloudClassLoader == null) // fall back to rules asset pkg classloader
            cloudClassLoader = PackageCache.getPackage(rulesAsset.getPackageName()).getClassLoader();

        KnowledgeBaseAsset kbAsset = DroolsKnowledgeBaseCache
                .getKnowledgeBaseAsset(rulesAsset.getName(), null, null, cloudClassLoader);
        if (kbAsset == null) {
            throw new ExecutionException("Cannot load KnowledgeBase asset: "
                    + rulesAsset.getPackageName() + "/" + rulesAsset.getLabel());
        }

        KieBase knowledgeBase = kbAsset.getKnowledgeBase();

        StatelessKieSession kSession = knowledgeBase.newStatelessKieSession();

        List<Object> facts = new ArrayList<Object>();
        facts.add(inputFacts);

        if (context != null) {
            Map<String,Object> values = context.getVariables();
            if (values != null) {
                for (String name : values.keySet()) {
                    kSession.setGlobal(name, values.get(name));
                }
            }
        }
        JSONObject result = new JSONObject();
        // facts.add(result);

        kSession.execute(CommandFactory.newInsertElements(facts));
        return result;
    }
}
