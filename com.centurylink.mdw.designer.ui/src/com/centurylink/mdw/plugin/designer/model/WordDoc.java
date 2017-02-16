/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.model;

import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.model.value.attribute.RuleSetVO;

public class WordDoc extends WorkflowAsset {
    public WordDoc() {
        super();
    }

    public WordDoc(RuleSetVO ruleSetVO, WorkflowPackage packageVersion) {
        super(ruleSetVO, packageVersion);
    }

    public WordDoc(Rule cloneFrom) {
        super(cloneFrom);
    }

    @Override
    public String getTitle() {
        return "Word Document";
    }

    @Override
    public String getIcon() {
        return "word.gif";
    }

    @Override
    public String getLanguageFriendly() {
        return "MS Word";
    }

    @Override
    public String getDefaultExtension() {
        return ".docx";
    }

    private static List<String> docLanguages;

    @Override
    public List<String> getLanguages() {
        if (docLanguages == null) {
            docLanguages = new ArrayList<String>();
            docLanguages.add("MS_Word");
        }
        return docLanguages;
    }

    @Override
    public boolean isForceExternalEditor() {
        // eclipse bug does not affect MS Word through OLE
        return false;
    }
}