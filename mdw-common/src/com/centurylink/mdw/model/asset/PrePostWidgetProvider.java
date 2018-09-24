/*
 * Copyright (C) 2018 CenturyLink, Inc.
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
package com.centurylink.mdw.model.asset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.centurylink.mdw.activity.types.AdapterActivity;
import com.centurylink.mdw.model.asset.Pagelet.Widget;

public class PrePostWidgetProvider implements WidgetProvider {

    @Override
    public List<Widget> getWidgets(String implCategory) {
        if (AdapterActivity.class.getName().equals(implCategory)) {
            List<Widget> widgets = new ArrayList<>();
            Widget preScript = new Widget("PreScript", "edit");
            preScript.setAttribute("section", "Script");
            preScript.setAttribute("languages", "Groovy,Kotlin Script");
            widgets.add(preScript);
            Widget language = new Widget("PreScriptLang", "dropdown");
            language.setAttribute("label", "Language");
            language.setAttribute("default", "Groovy");
            language.setAttribute("section", "Script");
            language.setOptions(Arrays.asList(new String[] {"Groovy", "Kotlin Script"}));
            widgets.add(language);
            Widget postScript = new Widget("PostScript", "edit");
            postScript.setAttribute("section", "Script");
            postScript.setAttribute("languages", "Groovy,Kotlin Script");
            widgets.add(postScript);
            language = new Widget("PostScriptLang", "dropdown");
            language.setAttribute("label", "Language");
            language.setAttribute("default", "Groovy");
            language.setAttribute("section", "Script");
            language.setOptions(Arrays.asList(new String[] {"Groovy", "Kotlin Script"}));
            widgets.add(language);
            return widgets;
        }
        return null;
    }
}
