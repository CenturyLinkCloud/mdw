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
package com.centurylink.mdw.plugin.designer.model;

import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.model.value.attribute.RuleSetVO;

public class CamelRoute extends WorkflowAsset {
    public CamelRoute() {
        super();
    }

    public CamelRoute(RuleSetVO ruleSetVO, WorkflowPackage packageVersion) {
        super(ruleSetVO, packageVersion);
    }

    public CamelRoute(CamelRoute cloneFrom) {
        super(cloneFrom);
    }

    @Override
    public String getTitle() {
        return "Camel Route";
    }

    public boolean isSpringCamelRoute() {
        return RuleSetVO.CAMEL_ROUTE.equals(getRuleSetVO().getLanguage());
    }

    @Override
    public String getIcon() {
        if (RuleSetVO.SPRING.equals(getLanguage()))
            return "spring.gif";
        else
            return "camel.gif";
    }

    public void setLanguageFriendly(String friendly) {
        if (FRIENDLY_SPRING.equals(friendly))
            setLanguage(RuleSetVO.SPRING);
        else if (FRIENDLY_CAMEL.equals(friendly))
            setLanguage(RuleSetVO.CAMEL_ROUTE);
        else
            super.setLanguageFriendly(friendly);
    }

    @Override
    public String getDefaultExtension() {
        return ".camel";
    }

    private static final String FRIENDLY_CAMEL = "Standalone Route";
    private static final String FRIENDLY_SPRING = "Full Spring Config";

    private static List<String> languages;

    @Override
    public List<String> getLanguages() {
        if (languages == null) {
            languages = new ArrayList<String>();
            languages.add(FRIENDLY_CAMEL);
            languages.add(FRIENDLY_SPRING);
        }
        return languages;
    }
}