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

import com.centurylink.mdw.constant.WorkAttributeConstant;
import com.centurylink.mdw.export.ExportHelper;
import com.centurylink.mdw.export.Table;
import com.centurylink.mdw.image.PngProcessExporter;
import com.centurylink.mdw.model.Attributes;
import com.centurylink.mdw.model.asset.Pagelet.Widget;
import com.centurylink.mdw.model.project.Project;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.workflow.Activity;
import com.centurylink.mdw.model.workflow.ActivityNodeSequencer;
import com.centurylink.mdw.model.workflow.Process;
import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HtmlExportHelper extends ExportHelper {

    public static final String BR = "<br/>";
    public static final String ACTIVITY = "Activity ";

    public HtmlExportHelper(Project project) {
        super(project);
    }

    public byte[] exportProcess(Process process, File outputDir) throws IOException {
        new ActivityNodeSequencer(process).assignNodeSequenceIds();
        StringBuilder sb = printPrologHtml("Process " + process.getName());
        printProcessHtml(sb, 0, process, outputDir);
        printEpilogHtml(sb);
        return sb.toString().getBytes();
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

    protected String getProcessBodyHtml(Process subproc) {
        StringBuilder sb = new StringBuilder();
        if (subproc.isEmbeddedProcess()) {
            String title = "Subprocess " + subproc.getAttribute(WorkAttributeConstant.LOGICAL_ID) + " - " + subproc.getName().replace('\n', ' ');
            sb.append("<h2>").append(title).append("</h2>\n");
        }
        else {
            sb.append("<h2>Documentation</h2>");
        }
        String summary = subproc.getDescription();
        if (summary != null && summary.length() > 0)
            sb.append("<span style='font-weight:bold'>").append(summary).append("</span>");
        String markdown = subproc.getAttribute(WorkAttributeConstant.DOCUMENTATION);
        if (markdown != null && markdown.length() > 0) {
            sb.append(getHtml(markdown));
        }
        sb.append(BR);
        return sb.toString();
    }

    protected Table getVariablesTable(Process process) {
        List<Variable> variables = process.getVariables();
        String[] headers = new String[] { "Name", "Type", "Mode" };
        String[][] values = new String[3][variables.size()];
        for (int i = 0; i < variables.size(); i++) {
            Variable var = variables.get(i);
            values[0][i] = var.getName();
            values[1][i] = escapeTags(var.getType());
            values[2][i] = var.getCategory();
        }
        Table table = new Table(headers, values);
        table.setWidths(new int[]{30, 50, 10});
        return table;
    }

    protected String getVariablesHtml(Process process) {
        StringBuilder sb = new StringBuilder();
        sb.append(getTableHtml(getVariablesTable(process)));
        return sb.toString();
    }

    protected String getAttributesHtml(Activity activity) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("<ul>\n");
        Attributes attributes = activity.getAttributes();
        if (attributes != null) {
            List<String> sortedNames = new ArrayList<>(attributes.keySet());
            Collections.sort(sortedNames);
            for (String name : sortedNames) {
                String val = attributes.get(name);
                if (!isExcludeAttribute(name, val)) {
                    sb.append("<li>");
                    sb.append(getAttributeLabel(activity, name)).append(": ");
                    sb.append(getAttributeValueHtml(activity, attributes, name));
                    sb.append("</li>");
                }
            }
        }
        sb.append("</ul>");
        return sb.toString();
    }

    protected String getAttributeValueHtml(Activity activity, Attributes attributes, String attributeName) throws IOException {
        StringBuilder sb = new StringBuilder();
        Widget widget = getWidget(activity, attributeName);
        if (widget != null || WorkAttributeConstant.MONITORS.equals(attributeName)) {
            if (isTabular(activity, attributeName)) {
                Table table = getTable(activity, attributes, attributeName);
                sb.append("<div style='margin-top:5px'>").append(getTableHtml(table)).append("</div>");
            }
            else if (isCode(activity, attributeName)) {
                sb.append(getCodeBoxHtml(attributes.get(attributeName)));
            }
            else {
                sb.append(attributes.get(attributeName));
            }
        }
        else {
            sb.append(attributes.get(attributeName));
        }
        return sb.toString();
    }

    protected String getCodeBoxHtml(String code) {
        StringBuilder sb = new StringBuilder();
        sb.append("<pre style='border:1px solid black;font-size:12px;'>").append(code).append("</pre>");
        return sb.toString();
    }

    protected String getTableHtml(Table table) {
        String[] headers = table.getColumns();
        String[][] values = table.getRows();
        StringBuilder sb = new StringBuilder();
        String border = "border:1px solid black;";
        String padding = "padding:3px;";
        sb.append("<table style='width:90%;text-align:left;border:1px solid black;border-spacing:0;border-collapse:collapse;font-size:12px;'>\n");
        sb.append("<thead style='font-weight:bold'>\n<tr>\n");
        for (int i = 0; i < headers.length; i++) {
            sb.append("<th style='").append(border).append(padding).append("'>").append(headers[i]).append("</th>\n");
        }
        sb.append("</tr></thead>\n");
        sb.append("<tbody>\n");
        for (int i = 0; i < values[0].length; i++) {
            sb.append("<tr>");
            for (int j = 0; j < headers.length; j++) {
                sb.append("<td style='").append(border).append(padding).append("'>").append(values[j][i]).append("</td>");
            }
            sb.append("</tr>\n");
        }
        sb.append("</tbody>\n");
        sb.append("</table>\n");
        return sb.toString();
    }

    public void printProcessHtml(StringBuilder sb, int chapter, Process process, File outputDir)
            throws IOException {
        sb.append("<h1>");
        if (chapter > 0)
            sb.append(chapter).append(". ");
        sb.append("Workflow: \"").append(process.getName()).append("\"</h1>\n");

        // diagram
        printDiagramHtml(sb, process, outputDir, chapter);

        // documentation
        sb.append(BR);
        sb.append(getProcessBodyHtml(process));

        // activities
        for (Activity act : process.getActivitiesOrderBySeq()) {
            printActivityHtml(sb, act);
        }

        // subprocesses
        for (Process subproc : process.getSubprocesses()) {
            sb.append(getProcessBodyHtml(subproc));
            for (Activity act : subproc.getActivitiesOrderBySeq()) {
                printActivityHtml(sb, act);
            }
        }

        // variables
        List<Variable> variables = process.getVariables();
        if (variables != null && !variables.isEmpty()) {
            sb.append("<h2>Process Variables</h2>\n");
            sb.append(getVariablesHtml(process));
        }
    }

    private void printDiagramHtml(StringBuilder sb, Process process, File outputDir,
            int chapterNumber) throws IOException {
        byte[] exported = new PngProcessExporter(project).export(process);
        String pngFile = process.getName() + "_" + process.getVersionString() + "_ch" + chapterNumber + ".png";
        Files.write(new File(outputDir + "/" + pngFile).toPath(), exported, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        sb.append("<img src='").append(pngFile).append("'/>\n");
    }

    private void printActivityHtml(StringBuilder sb, Activity activity) throws IOException {
        String tmp = ACTIVITY + activity.getLogicalId() + ": \"" + activity.getName().replace('\n', ' ') + "\"";
        sb.append("<h2>").append(tmp).append("</h2>\n");
        String summary = activity.getAttribute(WorkAttributeConstant.DESCRIPTION);
        if (summary != null && summary.length() > 0)
            sb.append("<span style='font-weight:bold'>").append(summary).append("</span>");

        String markdown = activity.getAttribute(WorkAttributeConstant.DOCUMENTATION);
        if (markdown != null && markdown.length() > 0) {
            sb.append(getHtml(markdown));
        }

        if (activity.getAttributes() != null && !isEmpty(activity.getAttributes())) {
            sb.append("<h3 style='margin-left:10px'>Attributes</h3>\n");
            sb.append(getAttributesHtml(activity));
        }

        sb.append(BR);
    }

    private void printEpilogHtml(StringBuilder sb) {
            sb.append("</body></html>\n");
    }

    public String getHtml(String markdown) {
        Parser parser = FlexmarkInstances.getParser(null);
        HtmlRenderer renderer = FlexmarkInstances.getRenderer(null);
        Node document = parser.parse(markdown);
        return renderer.render(document);
    }

    public String escapeTags(String str) {
        return str.replaceAll("&", "&amp;").replaceAll("<", "&lt;");
    }
}
