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
package com.centurylink.mdw.html;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import com.centurylink.mdw.constant.WorkAttributeConstant;
import com.centurylink.mdw.image.ImageExportHelper;
import com.centurylink.mdw.model.Project;
import com.centurylink.mdw.model.attribute.Attribute;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.workflow.Activity;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.util.StringHelper;

public class HtmlExportHelper {

    public static final String BR = "<br/>";
    public static final String HTMLTAG = "<html>";
    public static final String ACTIVITY = "Activity ";

    private Set<String> excludedAttributes;
    private Map<String, List<String>> tabularAttributes; // name, headers
    private Map<String, String> textboxAttributes;
    private Map<String, String> excludedAttributesForSpecificValues;

    private Project project;

    public HtmlExportHelper(Project project) {
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

    public String exportProcess(Process processVO, File outputDir) throws IOException {
        StringBuilder sb = printPrologHtml("Process " + processVO.getName());
        printProcessHtml(sb, 0, processVO, outputDir);
        printEpilogHtml(sb);
        return sb.toString();
    }

    private StringBuilder printPrologHtml(String title) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>\n<head>\n");
        sb.append("<title>").append(title).append("</title>\n");
        sb.append("<style>\n");
        sb.append("body {font-family: Arial; font-size: smaller; padding:5px; }\n");
        sb.append("em { color: #a52a2a; }\n");
        sb.append("</style>\n");
        sb.append("</head>\n<body>\n");
        return sb;
    }

    private void printProcessBodyHtml(StringBuilder sb, Process subproc) {
        String tmp = null;
        if (subproc.isEmbeddedProcess()) {
            tmp = "Subprocess " + subproc.getId() + " - " + subproc.getName().replace('\n', ' ');
            sb.append("<h2>").append(tmp).append("</h2>\n");
        }
        String summary = subproc.getDescription();
        if (summary != null && summary.length() > 0)
            sb.append("<span style='font-weight:bold'>").append(summary).append("</span>");
        String detail = subproc.getAttribute(WorkAttributeConstant.DOCUMENTATION);
        if (detail != null && detail.length() > 0) {
            printParagraphsHtml(sb, detail);
        }
        if (subproc.isEmbeddedProcess())
            printAttributesHtml(sb, subproc.getAttributes());
        sb.append(BR);
    }

    private void printParagraphsHtml(StringBuilder sb, String content) {
        if (content == null || content.length() == 0)
            return;
        String vContent = content;
        if (vContent.contains(HTMLTAG)) {
            int k1 = vContent.indexOf(HTMLTAG);
            int k2 = vContent.indexOf("<body>");
            int k3 = vContent.indexOf("</body>");
            int k4 = vContent.indexOf("</html>");
            if (k2 > 0) {
                if (k3 > 0)
                    vContent = vContent.substring(k2 + 6, k3);
                else if (k4 > 0)
                    vContent = vContent.substring(k2 + 6, k4);
                else
                    vContent = vContent.substring(k2 + 6);
            }
            else {
                if (k3 > 0)
                    vContent = vContent.substring(k1 + 6, k3);
                else if (k4 > 0)
                    vContent = vContent.substring(k1 + 6, k4);
                else
                    vContent = vContent.substring(k1 + 6);
            }
            sb.append(vContent);
        }
        else {
            String[] details = vContent.split("\n");
            for (int j = 0; j < details.length; j++) {
                if (details[j].length() == 0)
                    sb.append("<br/>\n");
                else
                    sb.append(escapeXml(details[j])).append("\n");
            }
        }
    }

    private String escapeXml(String str) {
        return str.replaceAll("&", "&amp;").replaceAll("<", "&lt;");
    }

    private void printAttributesHtml(StringBuilder sb, List<Attribute> attributes) {
        if (attributes != null) {
            List<Attribute> sortedAttrs = new ArrayList<>(attributes);
            Collections.sort(sortedAttrs);
            sb.append("<h3>Activity Attributes</h3>\n");
            sb.append("<ul>\n");
            for (Attribute attr : sortedAttrs) {
                String name = attr.getAttributeName();
                String val = attr.getAttributeValue();
                if (!excludeAttribute(name, val)) {
                    sb.append("<li>");
                    if (tabularAttributes.containsKey(name)) {
                        sb.append(name + ":");
                        List<String> cols = new ArrayList<>(tabularAttributes.get(name));
                        char colDelim = ',';
                        if ("=".equals(cols.get(0))) {
                            colDelim = '=';
                            cols.remove(0);
                        }
                        String[] headers = cols.toArray(new String[0]);
                        List<String[]> rows = StringHelper.parseTable(escapeXml(val), colDelim, ';',
                                headers.length);
                        String[][] values = new String[headers.length][rows.size()];
                        for (int i = 0; i < headers.length; i++) {
                            for (int j = 0; j < rows.size(); j++) {
                                values[i][j] = rows.get(j)[i];
                            }
                        }
                        printTableHtml(sb, headers, values);
                    }
                    else if (textboxAttributes.containsKey(name)) {
                        sb.append(textboxAttributes.get(name) + ":");
                        printCodeBoxHtml(sb, escapeXml(val));
                    }
                    else {
                        sb.append(name + " = " + val);
                    }
                    sb.append("</li>");
                }
            }
            sb.append("</ul>");
        }
    }

    private boolean excludeAttribute(String name, String value) {
        return (value == null || value.isEmpty() || excludedAttributes.contains(name)
                || value.equals(excludedAttributesForSpecificValues.get(name)));
    }

    private void printCodeBoxHtml(StringBuilder sb, String content) {
        sb.append("<pre style='border:1px solid black;font-size:12px;'>").append(content)
                .append("</pre>");
    }

    private void printTableHtml(StringBuilder sb, String[] headers, String[][] values) {
        String border = "border:1px solid black;";
        String padding = "padding:3px;";
        sb.append(
                "<table style='width:90%;text-align:left;border:1px solid black;border-spacing:0;border-collapse:collapse;font-size:12px;'>\n");
        sb.append("<thead style='font-weight:bold'>\n<tr>\n");
        for (int i = 0; i < headers.length; i++) {
            sb.append("<th style='" + border + padding + "'>").append(headers[i]).append("</th>\n");
        }
        sb.append("</tr></thead>\n");
        sb.append("<tbody>\n");
        for (int i = 0; i < values[0].length; i++) {
            sb.append("<tr>");
            for (int j = 0; j < headers.length; j++) {
                sb.append("<td style='" + border + padding + "'>").append(values[j][i])
                        .append("</td>");
            }
            sb.append("</tr>\n");
        }
        sb.append("</tbody>\n");
        sb.append("</table>\n");
    }

    private void printProcessHtml(StringBuilder sb, int chapter, Process process, File outputDir)
            throws IOException {
        sb.append("<h1>");
        if (chapter > 0)
            sb.append(Integer.toString(chapter)).append(". ");
        sb.append("Workflow: \"").append(process.getName()).append("\"</h1>\n");
        // print image
        printGraphHtml(sb, process, outputDir, chapter);
        // print documentation text
        sb.append(BR);
        printProcessBodyHtml(sb, process);
        for (Activity act : process.getActivities()) {
            printActivityHtml(sb, act);
        }
        for (Process subproc : process.getSubprocesses()) {
            printProcessBodyHtml(sb, subproc);
            for (Activity act : subproc.getActivities()) {
                printActivityHtml(sb, act);
            }
        }
        List<Variable> variables = process.getVariables();
        if (variables != null && !variables.isEmpty()) {
            sb.append("<h2>Process Variables</h2>\n");
            String[] headers = new String[] { "Name", "Type", "Mode" };
            String[][] values = new String[3][variables.size()];
            for (int i = 0; i < variables.size(); i++) {
                Variable var = variables.get(i);
                values[0][i] = var.getName();
                values[1][i] = var.getType();
                values[2][i] = var.getCategory();
            }
            printTableHtml(sb, headers, values);
        }
    }

    private void printGraphHtml(StringBuilder sb, Process process, File outputDir,
            int chapterNumber) throws IOException {
        String imgfilename = process.getName() + "_" + process.getVersionString()
                + "_ch" + chapterNumber + ".jpg";
        String imgfilepath = outputDir + "/" + imgfilename;
        printImage(imgfilepath, process);
        sb.append("<img src='").append(escapeXml(imgfilename)).append("'/>\n");
    }

    private void printActivityHtml(StringBuilder sb, Activity act) {
        Long id = act.getId();
        String tmp = ACTIVITY + id + ": \"" + act.getName().replace('\n', ' ') + "\"";
        sb.append("<h2>").append(tmp).append("</h2>\n");
        String summary = act.getAttribute(WorkAttributeConstant.DESCRIPTION);
        if (summary != null && summary.length() > 0)
            sb.append("<span style='font-weight:bold'>").append(summary).append("</span>");

        String detail = act.getAttribute(WorkAttributeConstant.DOCUMENTATION);
        if (detail != null && detail.length() > 0) {
            printParagraphsHtml(sb, detail);
        }

        printAttributesHtml(sb, act.getAttributes());

        sb.append(BR);
    }

    public void printImage(String fileName, Process processVO) throws IOException {
        ImageExportHelper imageHelper = new ImageExportHelper(project);
        ImageIO.write(imageHelper.printImage(processVO), "jpeg", new File(fileName));
        Runtime r = Runtime.getRuntime();
        r.gc();
    }

    private void printEpilogHtml(StringBuilder sb) {
            sb.append("</body></html>\n");
    }

}
