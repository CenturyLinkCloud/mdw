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
