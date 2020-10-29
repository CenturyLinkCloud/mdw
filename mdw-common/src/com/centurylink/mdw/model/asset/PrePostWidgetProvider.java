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
            Widget outputDocs = new Widget("Output Documents", "picklist");
            outputDocs.setAttribute("label", "Documents");
            outputDocs.setAttribute("unselectedLabel", "Read-Only");
            outputDocs.setAttribute("selectedLabel", "Writable");
            outputDocs.setAttribute("source", "DocumentVariables");
            outputDocs.setAttribute("section", "Script");
            widgets.add(outputDocs);

            return widgets;
        }
        return null;
    }
}
