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