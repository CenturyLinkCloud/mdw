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
package com.centurylink.mdw.export;

import java.awt.Dimension;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.centurylink.mdw.constant.WorkAttributeConstant;
import com.centurylink.mdw.html.FlexmarkInstances;
import com.centurylink.mdw.model.Project;
import com.centurylink.mdw.model.workflow.Activity;
import com.centurylink.mdw.model.workflow.Process;
import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;

public class ExportHelper {
    protected Set<String> excludedAttributes;
    protected Map<String, List<String>> tabularAttributes; // name, headers
    protected Map<String, String> textboxAttributes;
    protected Map<String, String> excludedAttributesForSpecificValues;
    protected Project project;


    public ExportHelper(Project project) {
        this.project = project;

        excludedAttributes = new HashSet<>();
        excludedAttributes.add(WorkAttributeConstant.LOGICAL_ID);
        excludedAttributes.add(WorkAttributeConstant.REFERENCE_ID);
        excludedAttributes.add(WorkAttributeConstant.WORK_DISPLAY_INFO);
        excludedAttributes.add(WorkAttributeConstant.REFERENCES);
        excludedAttributes.add(WorkAttributeConstant.DOCUMENTATION);
        excludedAttributes.add(WorkAttributeConstant.SIMULATION_STUB_MODE);
        excludedAttributes.add(WorkAttributeConstant.SIMULATION_RESPONSE);
        excludedAttributes.add(WorkAttributeConstant.DESCRIPTION);
        excludedAttributes.add("BAM@START_MSGDEF");
        excludedAttributes.add("BAM@FINISH_MSGDEF");
        excludedAttributesForSpecificValues = new HashMap<>();
        excludedAttributesForSpecificValues.put("DoNotNotifyCaller", "false");
        excludedAttributesForSpecificValues.put("DO_LOGGING", "True");
        tabularAttributes = new HashMap<>();
        tabularAttributes.put("Notices",
                Arrays.asList("Outcome", "Template", "Notifier Class(es)"));
        tabularAttributes.put("Variables",
                Arrays.asList("Variable", "ReferredAs", "Display", "Seq.", "Index"));
        tabularAttributes.put("WAIT_EVENT_NAMES",
                Arrays.asList("Event Name", "Completion Code", "Recurring"));
        tabularAttributes.put("variables",
                Arrays.asList("=", "SubProcess Variable", "Binding Expression"));
        tabularAttributes.put("processmap",
                Arrays.asList("=", "Logical Name", "Process Name", "Process Version"));
        tabularAttributes.put("Bindings", Arrays.asList("=", "Variable", "LDAP Attribute"));
        tabularAttributes.put("Parameters",
                Arrays.asList("=", "Input Variable", "Binding Expression"));
        textboxAttributes = new HashMap<>();
        textboxAttributes.put("Rule", "Code");
        textboxAttributes.put("Java", "Java");
        textboxAttributes.put("PreScript", "Pre-Script");
        textboxAttributes.put("PostScript", "Post-Script");
    }

    public Dimension getGraphSize(Process process) {
        int w = 0;
        int h = 0;
        List<Activity> activities = process.getActivities();
        for (Activity act : activities) {
            String[] attrs = act.getAttribute(WorkAttributeConstant.WORK_DISPLAY_INFO).split(",");
            w = getWidth(attrs, w);
            h = getHeight(attrs, h);
        }
        List<Process> subProcesses = process.getSubprocesses();
        for (Process subProc : subProcesses) {
            String[] attrs = subProc.getAttribute(WorkAttributeConstant.WORK_DISPLAY_INFO)
                    .split(",");
            w = getWidth(attrs, w);
            h = getHeight(attrs, h);
        }
        return new Dimension(w, h);
    }

    private int getWidth(String[] attrs, int w) {
        int localW = Integer.parseInt(attrs[0].substring(2))
                + Integer.parseInt(attrs[2].substring(2));
        if (localW > w)
            w = localW;
        return w;
    }

    private int getHeight(String[] attrs, int h) {
        int localH = Integer.parseInt(attrs[1].substring(2))
                + Integer.parseInt(attrs[3].substring(2));
        if (localH > h)
            h = localH;
        return h;
    }

    public String escapeXml(String str) {
        return str.replaceAll("&", "&amp;").replaceAll("<", "&lt;");
    }

    public boolean excludeAttribute(String name, String value) {
        return (value == null || value.isEmpty() || excludedAttributes.contains(name)
                || value.equals(excludedAttributesForSpecificValues.get(name)));
    }

    public String getHtmlContent (String content) {
        Parser parser = FlexmarkInstances.getParser(null);
        HtmlRenderer renderer = FlexmarkInstances.getRenderer(null);

        Node document = parser.parse(content);
        return renderer.render(document);
    }
}
