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
package com.centurylink.mdw.services;

import org.json.JSONObject;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.workflow.RuntimeContext;

public interface RulesServices {
    /**
     * Apply rules without runtime context and with default impl (drools).
     * @param rulesAsset rule to apply
     * @param input input facts
     * @return JSON result
     */
    public JSONObject applyRules(Asset rulesAsset, JSONObject input) throws ServiceException;

    /**
     * Apply rules.
     * @param rulesAsset rule to apply
     * @param input input facts
     * @param context optional runtime context
     * @param executor class name of RulesExecutor impl (if null, use drools)
     * @return JSON result
     */
    public JSONObject applyRules(Asset rulesAsset, JSONObject input, RuntimeContext context, String executor)
            throws ServiceException;
}
