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

public class SpringConfig extends WorkflowAsset {
    public SpringConfig() {
        super();
    }

    public SpringConfig(RuleSetVO ruleSetVO, WorkflowPackage packageVersion) {
        super(ruleSetVO, packageVersion);
    }

    public SpringConfig(SpringConfig cloneFrom) {
        super(cloneFrom);
    }

    @Override
    public String getTitle() {
        return "Spring Config";
    }

    @Override
    public String getIcon() {
        return "spring.gif";
    }

    public boolean isSpring() {
        return getLanguage().equals(RuleSetVO.SPRING);
    }

    public void setLanguageFriendly(String friendly) {
        if ("Spring".equals(friendly))
            setLanguage(RuleSetVO.SPRING);
        else
            super.setLanguageFriendly(friendly);
    }

    @Override
    public String getDefaultExtension() {
        return ".spring";
    }

    private static List<String> languages;

    @Override
    public List<String> getLanguages() {
        if (languages == null) {
            languages = new ArrayList<String>();
            languages.add("Spring");
        }
        return languages;
    }
}