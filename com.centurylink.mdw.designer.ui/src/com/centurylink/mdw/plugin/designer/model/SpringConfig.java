/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
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