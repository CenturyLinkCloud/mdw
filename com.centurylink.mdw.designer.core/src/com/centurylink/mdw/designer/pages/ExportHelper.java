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
package com.centurylink.mdw.designer.pages;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.imageio.ImageIO;
import javax.swing.JEditorPane;
import javax.swing.text.AbstractDocument.AbstractElement;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.CSS;
import javax.swing.text.html.HTMLDocument;

import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.bpmn.BPMNHelper;
import com.centurylink.mdw.designer.display.Graph;
import com.centurylink.mdw.designer.display.GraphCommon;
import com.centurylink.mdw.designer.display.Node;
import com.centurylink.mdw.designer.display.SubGraph;
import com.centurylink.mdw.designer.utils.DocxBuilder;
import com.centurylink.mdw.designer.utils.DocxBuilder.DocxCodebox;
import com.centurylink.mdw.designer.utils.DocxBuilder.DocxTable;
import com.centurylink.mdw.model.value.attribute.AttributeVO;
import com.centurylink.mdw.model.value.variable.VariableVO;
import com.lowagie.text.BadElementException;
import com.lowagie.text.Cell;
import com.lowagie.text.Chapter;
import com.lowagie.text.Chunk;
import com.lowagie.text.DocWriter;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.ImgTemplate;
import com.lowagie.text.ListItem;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.Section;
import com.lowagie.text.Table;
import com.lowagie.text.html.HtmlWriter;
import com.lowagie.text.pdf.DefaultFontMapper;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.rtf.RtfWriter2;

/**
 * Export MDW processes to supported file formats.
 * TODO: improve styling for HTML output
 * TODO: use docx4j to produce RTF and PDF
 */
public class ExportHelper {

    public static final String PDF = "pdf";
    public static final String RTF = "rtf";
    public static final String HTML = "html";
    public static final String JPEG = "jpeg";
    public static final String JPG = "jpg";
    public static final String PNG = "png";
    public static final String DOCX = "docx";
    public static final String BPMN2 = "bpmn";

    public static final String VARIABLES = "Variables";
    public static final String DOCUMENTATION = "Documentation";
    public static final String ATTRIBUTES = "Attributes";

    public static final String SECTION_NUMBER = "Section Number";
    public static final String ACTIVITY_DOCUMENT = "Activity Documentation (LLD)";
    public static final String ACTIVITY_ATTRIBUTES = "Activity Attributes (LLD)";

    private Set<String> options;
    private String nodeIdType;
    private Font chapterFont, sectionFont, subSectionFont, normalFont, fixedWidthFont, boldFont;
    private Set<String> excludedAttributes;
    private Map<String,List<String>> tabularAttributes; // name, headers
    private Map<String,String> textboxAttributes;
    private Map<String,String> excludedAttributesForSpecificValues;

    /**
     * @param options options for printing, from the print dialog.
     * @param nodeIdType
     */
    public ExportHelper(Set<String> options, String nodeIdType) {
        this.options = options;
        this.nodeIdType = nodeIdType;
    }

    private void initialize(boolean isUseCase) {
        chapterFont = FontFactory.getFont(FontFactory.TIMES, isUseCase?16:20, Font.BOLD, new Color(0, 0, 0));
        sectionFont = FontFactory.getFont(FontFactory.TIMES, 16, Font.BOLD, new Color(0, 0, 0));
        subSectionFont = FontFactory.getFont(FontFactory.TIMES, 14, Font.BOLD, new Color(0, 0, 0));
        normalFont = FontFactory.getFont(FontFactory.TIMES, 12, Font.NORMAL, new Color(0, 0, 0));
        boldFont = FontFactory.getFont(FontFactory.TIMES, 12, Font.BOLD, new Color(0, 0, 0));
        fixedWidthFont = FontFactory.getFont(FontFactory.COURIER, 11, Font.NORMAL, new Color(0, 0, 0));
        excludedAttributes = new HashSet<String>();
        excludedAttributes.add(WorkAttributeConstant.LOGICAL_ID);
        excludedAttributes.add(WorkAttributeConstant.REFERENCE_ID);
        excludedAttributes.add(WorkAttributeConstant.WORK_DISPLAY_INFO);
        excludedAttributes.add(WorkAttributeConstant.REFERENCES);
        excludedAttributes.add(WorkAttributeConstant.DOCUMENTATION);
        excludedAttributes.add(WorkAttributeConstant.SIMULATION_STUB_MODE);
        excludedAttributes.add(WorkAttributeConstant.SIMULATION_RESPONSE);
        excludedAttributes.add(WorkAttributeConstant.DESCRIPTION);
        excludedAttributes.add(WorkAttributeConstant.BAM_START_MSGDEF);
        excludedAttributes.add(WorkAttributeConstant.BAM_FINISH_MSGDEF);
        excludedAttributesForSpecificValues = new HashMap<String,String>();
        excludedAttributesForSpecificValues.put("DoNotNotifyCaller", "false");
        excludedAttributesForSpecificValues.put("DO_LOGGING", "True");
        tabularAttributes = new HashMap<String,List<String>>();
        tabularAttributes.put("Notices", Arrays.asList(new String[]{"Outcome","Template","Notifier Class(es)"}));
        tabularAttributes.put("Variables", Arrays.asList(new String[]{"Variable","ReferredAs","Display","Seq.","Index"}));
        tabularAttributes.put("WAIT_EVENT_NAMES", Arrays.asList(new String[]{"Event Name","Completion Code","Recurring"}));
        tabularAttributes.put("variables", Arrays.asList(new String[]{"=","SubProcess Variable","Binding Expression"}));
        tabularAttributes.put("processmap", Arrays.asList(new String[]{"=","Logical Name","Process Name","Process Version"}));
        tabularAttributes.put("Bindings", Arrays.asList(new String[]{"=","Variable","LDAP Attribute"}));
        tabularAttributes.put("Parameters", Arrays.asList(new String[]{"=","Input Variable","Binding Expression"}));
        textboxAttributes = new HashMap<String,String>();
        textboxAttributes.put("Rule", "Code");
        textboxAttributes.put("Java", "Java");
        textboxAttributes.put("PreScript", "Pre-Script");
        textboxAttributes.put("PostScript", "Post-Script");
    }

    /**
     * Export a process
     * @param filename the file name (including path) where the document will be generated
     * @param format can be docx, pdf, rtf, html and bpmn
     * @param canvas for printing process images
     * @param graph the process to be printed.
     */
    public void exportProcess(String filename, String format, Graph process, DesignerCanvas canvas) throws Exception {

        initialize(false);

        String oldNodeIdType = process.getNodeIdType();

        try {
            process.setNodeIdType(nodeIdType);
            options.add(SECTION_NUMBER);
            if (format.equals(DOCX)) {
                DocxBuilder builder = printProcessDocx(filename, process, canvas);
                builder.save(new java.io.File(filename));
                return;
            }
            else if (format.equals(HTML)) {
                StringBuffer sb = printPrologHtml("Process " + process.getName());
                printProcessHtml(sb, canvas, 0, process, filename);
                printEpilogHtml(sb, filename);
                return;
            }
            else if (format.equals(JPG) || format.equals(PNG)) {
                byte[] imgBytes = printImage(-1f, canvas, process.getGraphSize(), format.equals(JPG) ? "jpeg" : "png");
                OutputStream os = null;
                try {
                    os = new FileOutputStream(new File(filename));
                    os.write(imgBytes);
                    return;
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                    throw ex;
                }
                finally {
                    if (os != null)
                        os.close();
                }
            }
            else if (format.equals(BPMN2)) {
              new BPMNHelper().exportProcess(process.getProcessVO(), filename);
            }
            else { // itext processor
                Document document = new Document();
                try {
                    DocWriter writer = null;
                    if (format.equals(RTF)) {
                        writer = RtfWriter2.getInstance(document, new FileOutputStream(filename));
                    } else if (format.equals(PDF)) {
                        writer = PdfWriter.getInstance(document, new FileOutputStream(filename));
                    }

                    document.open();
                    document.setPageSize(PageSize.LETTER);
                    Rectangle page_size = document.getPageSize();
                    Chapter chapter = printOneProcessPdf(writer, canvas, format, 1, process, filename, page_size);
                    document.add(chapter);
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                    throw ex;
                }
                finally {
                    // step 5: we close the document
                    document.close();
                }
            }
        }
        finally {
            process.setNodeIdType(oldNodeIdType);
        }
    }

    /**
     * Export multiple processes
     * @param filename the file name (including path) where the document will be generated
     * @param type can be pdf, rtf and html
     * @param flowchart the designer page (for using its canvas and report errors)
     * @param graphs the list of processes to be printed.
     * @param options options for printing, from the print dialog.
     */
    public void exportProcesses(String filename, String type, FlowchartPage flowchart, List<Graph> graphs) throws Exception {
        initialize(false);
        options.add(SECTION_NUMBER);
        // step 1: creation of a document-object
        Document document = new Document();
        try {
            // step 2: create PDF or RTF writer
            DocWriter writer;
            if (type.equals(RTF)) {
                writer = RtfWriter2.getInstance(document, new FileOutputStream(filename));
            } else if (type.equals(PDF)) {
                writer = PdfWriter.getInstance(document, new FileOutputStream(filename));
            } else {
                boolean directHtml = true;
                if (directHtml) {
                    StringBuffer sb = printPrologHtml("Processes");
                    Graph process;
                    for (int i=0; i<graphs.size(); i++) {
                        process = graphs.get(i);
                        flowchart.setProcess(process);
                        this.printProcessHtml(sb, flowchart.canvas, i+1, process, filename);
                    }
                    printEpilogHtml(sb, filename);
                    return;
                }
                writer = HtmlWriter.getInstance(document, new FileOutputStream(
                        filename));
            }
            // step 3: we open the document
            document.open();
            // step 4: we add contents to the document
            document.setPageSize(PageSize.LETTER);
            Graph process;
            Chapter chapter;
            Rectangle page_size = document.getPageSize();
            for (int i=0; i<graphs.size(); i++) {
                process = graphs.get(i);
                process.setNodeIdType(nodeIdType);
                flowchart.setProcess(process);
                chapter = printOneProcessPdf(writer, flowchart.canvas, type, i+1, process, filename, page_size);
                document.add(chapter);
            }
        } finally {
            // step 5: we close the document
            document.close();
        }
    }

    private DocxBuilder printProcessDocx(String filename, Graph process, DesignerCanvas canvas)
    throws Exception {

        DocxBuilder builder = new DocxBuilder();

        // title
        String title = "Workflow: \"" + process.getName() + "\"";
        builder.addParagraph("Heading1", title);

        // process image
        int zoomSave = process.zoom;
        process.zoom = 100;
        byte[] imgBytes = printImage(-1f, canvas, process.getGraphSize(), "png");
        process.zoom = zoomSave;
        builder.addImage(imgBytes);

        printProcessBodyDocx(builder, process);

        // embedded subprocesses
        for (SubGraph subprocess : process.getSubgraphs(nodeIdType)) {
            String id = subprocess.getDisplayId(nodeIdType);
            String spTitle = "Embedded Subprocess " + id + ": \"" + subprocess.getName() + "\"";
            builder.addParagraph("Heading1", spTitle);
            printProcessBodyDocx(builder, subprocess);
        }

        // process variables
        if (options.contains(VARIABLES)) {
            List<VariableVO> variables = process.getProcessVO().getVariables();
            if (variables != null && !variables.isEmpty()) {
                builder.addParagraph("Heading2", "Process Variables");
                String[] headers = new String[] { "Name", "Type", "Mode", "Description" };
                String[][] values = new String[4][variables.size()];
                for (int i = 0; i < variables.size(); i++) {
                    VariableVO var = variables.get(i);
                    values[0][i] = var.getName();
                    values[1][i] = var.getVariableType();
                    values[2][i] = VariableVO.VariableCategories[var.getVariableCategory()];
                    values[3][i] = var.getVariableReferredAs();
                }

                builder.addTable(headers, values, 11, 120);
            }
        }

        // System.out.println(builder.toXml());

        return builder;
    }

    private void printProcessBodyDocx(DocxBuilder builder, GraphCommon process) throws Exception {
        String summary = process.getProcessVO().getProcessDescription();
        if (summary != null && summary.length() > 0) {
            builder.addBoldParagraph(summary);
        }
        if (options.contains(DOCUMENTATION)) {
            String detail = process.getAttribute(WorkAttributeConstant.DOCUMENTATION);
            if (detail != null && detail.length() > 0) {
                if (detail.startsWith("<html>")) {
                    builder.addHtml(detail);
                }
                else {
                    byte[] docBytes = decodeBase64(detail);
                    builder.addDoc(docBytes);
                }
            }
        }
        builder.addBreak();

        // activities
        for (Node node : process.getNodes(nodeIdType))
            printActivityDocx(builder, node);
    }

    private void printActivityDocx(DocxBuilder builder, Node node) throws Exception {
        String id = node.getDisplayId(nodeIdType);
        String actTitle = "Activity " + id + ": \"" + node.getName().replace('\n', ' ') + "\"";
        builder.addParagraph("Heading2", actTitle);

        String summary = node.getAttribute(WorkAttributeConstant.DESCRIPTION);
        if (summary != null && summary.length() > 0) {
            builder.addBoldParagraph(summary);
        }

        // documentation
        if (options.contains(DOCUMENTATION)) {
            String detail = node.getAttribute(WorkAttributeConstant.DOCUMENTATION);
            if (detail != null && detail.length() > 0) {
                if (detail.startsWith("<html>")) {
                    builder.addHtml(detail);
                }
                else {
                    byte[] docBytes = decodeBase64(detail);
                    builder.addDoc(docBytes);
                }
            }
        }

        // attributes
        if (options.contains(ATTRIBUTES)) {
            Map<String,Object> attrs = new TreeMap<String,Object>();
            if (node.getAttributes() != null)
            {
                for (AttributeVO attr : node.getAttributes()) {
                    String name = attr.getAttributeName();
                    String val = attr.getAttributeValue();
                    if (!excludeAttribute(name, val)) {
                        if (tabularAttributes.containsKey(name)) {
                            DocxTable docxTable = parseTableDocx(builder, tabularAttributes.get(name), val);
                            docxTable.setFontSize(9);
                            docxTable.setIndent(800);
                            attrs.put(name, docxTable);
                        }
                        else if (textboxAttributes.containsKey(name)) {
                            DocxCodebox docxTextbox = builder.new DocxCodebox(textboxAttributes.get(name), val);
                            attrs.put(name, docxTextbox);
                        }
                        else {
                          attrs.put(name, val);
                        }
                    }
                }
            }
            if (!attrs.isEmpty()) {
                builder.addParagraph("Heading3", "Activity Attributes");
                builder.addBulletList(attrs);
            }
        }

        builder.addBreak();
    }

    private DocxBuilder.DocxTable parseTableDocx(DocxBuilder builder, List<String> labels, String value) {
        List<String> cols = new ArrayList<String>(labels);
        char colDelim = ',';
        if (cols.get(0).equals("=")) {
            colDelim = '=';
            cols.remove(0);
        }
        String[] headers = cols.toArray(new String[0]);
        List<String[]> rows = StringHelper.parseTable(value, colDelim, ';', headers.length);
        String[][] values = new String[headers.length][rows.size()];
        for (int i = 0; i < headers.length; i++) {
            for (int j = 0; j < rows.size(); j++) {
                values[i][j] = rows.get(j)[i];
            }
        }
        return builder.new DocxTable(headers, values);
    }

    private void printProcessHtml(StringBuffer sb, DesignerCanvas canvas, int chapter, Graph process, String filename) throws Exception {
        sb.append("<h1>");
        if (chapter>0) sb.append(Integer.toString(chapter)).append(". ");
        sb.append("Workflow: \"").append(process.getName()).append("\"</h1>\n");
        // print image
        printGraphHtml(sb, canvas, process, filename, chapter);
        // print documentation text
        sb.append("<br/>");
        printProcessBodyHtml(sb, process);
        for (Node node : process.getNodes(nodeIdType)) {
            printActivityHtml(sb, node);
        }
        for (SubGraph subgraph : process.getSubgraphs(nodeIdType)) {
            printProcessBodyHtml(sb, subgraph);
            for (Node node : subgraph.getNodes(nodeIdType)) {
                printActivityHtml(sb, node);
            }
        }
        if (options.contains(VARIABLES)) {
            List<VariableVO> variables = process.getProcessVO().getVariables();
            if (variables != null && !variables.isEmpty()) {
                sb.append("<h2>Process Variables</h2>\n");
                String[] headers = new String[] { "Name", "Type", "Mode", "Description" };
                String[][] values = new String[4][variables.size()];
                for (int i = 0; i < variables.size(); i++) {
                    VariableVO var = variables.get(i);
                    values[0][i] = var.getName();
                    values[1][i] = var.getVariableType();
                    values[2][i] = VariableVO.VariableCategories[var.getVariableCategory()];
                    values[3][i] = var.getVariableReferredAs();
                }
                printTableHtml(sb, headers, values);
            }
        }
    }

    private void printTableHtml(StringBuffer sb, String[] headers, String[][] values) {
        String border = "border:1px solid black;";
        String padding = "padding:3px;";
        sb.append("<table style='width:90%;text-align:left;border:1px solid black;border-spacing:0;border-collapse:collapse;font-size:12px;'>\n");
        sb.append("<thead style='font-weight:bold'>\n<tr>\n");
        for (int i = 0; i < headers.length; i++) {
            sb.append("<th style='" + border + padding + "'>").append(headers[i]).append("</th>\n");
        }
        sb.append("</tr></thead>\n");
        sb.append("<tbody>\n");
        for (int i = 0; i < values[0].length; i++) {
            sb.append("<tr>");
            for (int j = 0; j < headers.length; j++) {
                sb.append("<td style='" + border + padding + "'>").append(values[j][i]).append("</td>");
            }
            sb.append("</tr>\n");
        }
        sb.append("</tbody>\n");
        sb.append("</table>\n");
    }

    private void printProcessBodyHtml(StringBuffer sb, GraphCommon graph) throws Exception {
        String tmp = null;
        if (graph instanceof SubGraph) {
            SubGraph subgraph = (SubGraph)graph;
            String id = subgraph.getDisplayId(nodeIdType);
            if (id==null || id.length()==0) id = subgraph.getId().toString();
            tmp = "Subprocess " + id + " - " + subgraph.getName().replace('\n', ' ');
        }
        if (tmp != null)
            sb.append("<h2>").append(tmp).append("</h2>\n");
        String summary = (graph instanceof SubGraph)?
                ((SubGraph)graph).getProcessVO().getProcessDescription():((Graph)graph).getProcessVO().getProcessDescription();
        if (summary!=null && summary.length()>0)
            sb.append("<span style='font-weight:bold'>").append(summary).append("</span>");
        if (options.contains(DOCUMENTATION)) {
            String detail = graph.getProcessVO().getAttribute(WorkAttributeConstant.DOCUMENTATION);
            if (detail!=null && detail.length()>0) {
                printParagraphsHtml(sb, detail, 2);
            }
        }
        if (options.contains(ATTRIBUTES) && graph instanceof SubGraph) {
            printAttributesHtml(sb, graph.getProcessVO().getAttributes());
        }
        sb.append("<br/>");
    }

    private void printGraphHtml(StringBuffer sb, CanvasCommon canvas,
            Graph process, String filename, int chapterNumber) throws Exception {
        int zoomSave = process.zoom;
        process.zoom = 100;
        String imgfilepath;
        int k = filename.lastIndexOf('/');
        if (k<0) k = filename.lastIndexOf('\\');
        String imgfilename = process.getName() + "_" + process.getProcessVO().getVersionString() +
            "_ch" + chapterNumber + ".jpg";
        if (k>0) imgfilepath = filename.substring(0,k+1) + imgfilename;
        else imgfilepath = imgfilename;
        // TODO need to generate different names for the same process
        // in different use cases, as the step markers may be different.
        printImage(imgfilepath, -1f, canvas, process.getGraphSize());
        process.zoom = zoomSave;
        sb.append("<img src='").append(escapeXml(imgfilename)).append("'/>\n");
    }

    private void printActivityHtml(StringBuffer sb, Node node) throws Exception {
        String id = node.getDisplayId(nodeIdType);
        if (id==null || id.length()==0) id = node.getId().toString();
        String tmp = "Activity " + id + ": \"" + node.getName().replace('\n', ' ') + "\"";
        sb.append("<h2>").append(tmp).append("</h2>\n");
        String summary = node.getAttribute(WorkAttributeConstant.DESCRIPTION);
        if (summary!=null && summary.length()>0)
            sb.append("<span style='font-weight:bold'>").append(summary).append("</span>");

        if (options.contains(DOCUMENTATION)) {
            String detail = node.getAttribute(WorkAttributeConstant.DOCUMENTATION);
            if (detail!=null && detail.length()>0) {
                printParagraphsHtml(sb, detail, 2);
            }
        }
        if (options.contains(ATTRIBUTES)) {
            printAttributesHtml(sb, node.getAttributes());
        }
        sb.append("<br/>");
    }

    private void printAttributesHtml(StringBuffer sb, List<AttributeVO> attributes) {
        if (attributes != null) {
            List<AttributeVO> sortedAttrs = new ArrayList<AttributeVO>(attributes);
            Collections.sort(sortedAttrs);
            sb.append("<h3>Activity Attributes</h3>\n");
            sb.append("<ul>\n");
            for (AttributeVO attr : sortedAttrs) {
                String name = attr.getAttributeName();
                String val = attr.getAttributeValue();
                if (!excludeAttribute(name, val)) {
                    sb.append("<li>");
                    if (tabularAttributes.containsKey(name)) {
                        sb.append(name + ":");
                        List<String> cols = new ArrayList<String>(tabularAttributes.get(name));
                        char colDelim = ',';
                        if (cols.get(0).equals("=")) {
                            colDelim = '=';
                            cols.remove(0);
                        }
                        String[] headers = cols.toArray(new String[0]);
                        List<String[]> rows = StringHelper.parseTable(escapeXml(val), colDelim, ';', headers.length);
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

    private String escapeXml(String str) {
        return str.replaceAll("&", "&amp;").replaceAll("<", "&lt;");
    }

    private void printCodeBoxHtml(StringBuffer sb, String content) {
        sb.append("<pre style='border:1px solid black;font-size:12px;'>").append(content).append("</pre>");
    }

    private void printElementHtml(Element element, Object parent, int depth, Font font, int parentLevel) {
        String tag = element.getName();
        Object av;
        if (element instanceof HTMLDocument.RunElement) {
            HTMLDocument.RunElement re = (HTMLDocument.RunElement)element;
            int start = re.getStartOffset();
            int end = re.getEndOffset();
            try {
                String content = re.getDocument().getText(start, end-start);
                printAttributesHtml(re);
                av = re.getAttribute(CSS.Attribute.FONT_SIZE);
                String fontsize = av==null?null:av.toString();
                av = re.getAttribute(CSS.Attribute.FONT_FAMILY);
                String fontfamily = av==null?null:av.toString();
                av = re.getAttribute(CSS.Attribute.COLOR);
                String fontcolor = av==null?null:av.toString();
                if (fontcolor!=null || fontsize!=null || fontfamily!=null) {
                    if (fontfamily==null) fontfamily = font.getFamilyname();
                    float size = fontsize==null?font.getSize():(Float.parseFloat(fontsize)+9);
                    int style = font.getStyle();
                    Color color;
                    if (fontcolor!=null) {
                        color = Color.decode(fontcolor);
                    } else color = font.getColor();
                    font = FontFactory.getFont(fontfamily, size, style, color);
                }
                if (parent instanceof Paragraph) {
                    ((Paragraph)parent).add(new Chunk(content, font));
                } else {
                    System.err.println("chunk with parent "
                            + (parent==null?"null":parent.getClass().getName()) + ": " + content);
                }
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        } else if (element instanceof HTMLDocument.BlockElement) {
            HTMLDocument.BlockElement be = (HTMLDocument.BlockElement)element;
            printAttributesHtml(be);
            av = be.getAttribute(javax.swing.text.html.HTML.Attribute.ALIGN);
            String align = av==null?null:av.toString();
            if (tag.equalsIgnoreCase("html")) {
                printElementChildrenHtml(element, parent, depth+1, font, parentLevel);
            } else if (tag.equalsIgnoreCase("head")) {
                // do nothing
            } else if (tag.equalsIgnoreCase("body")) {
                printElementChildrenHtml(element, parent, depth+1, font, parentLevel);
            } else if (tag.equalsIgnoreCase("p")) {
                if (parent instanceof Section) {
                    Paragraph paragraph = new Paragraph();
                    if (align!=null) {
                        paragraph.setAlignment(align);
                    }
                    printElementChildrenHtml(element, paragraph, depth+1, normalFont, parentLevel);
                    ((Section)parent).add(paragraph);
                } else {
                    System.err.println("p with parent " + (parent==null?"null":parent.getClass().getName()));
                }
            } else if (tag.equalsIgnoreCase("h1") || tag.equalsIgnoreCase("h2") || tag.equalsIgnoreCase("h3") ) {
                if (parent instanceof Section) {
                    Paragraph title = new Paragraph();
                    printElementChildrenHtml(element, title, depth+1, subSectionFont, parentLevel);
                    ((Section)parent).addSection(title, parentLevel==0?0:(parentLevel+1));
                } else {
                    System.err.println("list with parent " + (parent==null?"null":parent.getClass().getName()));
                }
            } else if (tag.equalsIgnoreCase("ul")) {
                if (parent instanceof Section) {
                    com.lowagie.text.List list = new com.lowagie.text.List(false, false, 20.0f);
                    printElementChildrenHtml(element, list, depth+1, normalFont, parentLevel);
                    ((Section)parent).add(list);
                } else {
                    System.err.println("list with parent " + (parent==null?"null":parent.getClass().getName()));
                }
            } else if (tag.equalsIgnoreCase("ol")) {
                if (parent instanceof Section) {
                    com.lowagie.text.List list = new com.lowagie.text.List(true, false, 20.0f);
                    printElementChildrenHtml(element, list, depth+1, normalFont, parentLevel);
                    ((Section)parent).add(list);
                } else {
                    System.err.println("list with parent " + (parent==null?"null":parent.getClass().getName()));
                }
            } else if (tag.equalsIgnoreCase("li")) {
                ListItem li = new ListItem();
                li.setSpacingAfter(0.0f);
                printElementChildrenHtml(element, li, depth+1, normalFont, parentLevel);
                ((com.lowagie.text.List)parent).add(li);
            } else if (tag.equalsIgnoreCase("p-implied")) {
                if (parent instanceof ListItem) {
                    Paragraph paragraph = new Paragraph();
                    printElementChildrenHtml(element, paragraph, depth+1, normalFont, parentLevel);
                    ((ListItem)parent).add(paragraph);
                } else if (parent instanceof Cell) {
                    Paragraph paragraph = new Paragraph();
                    printElementChildrenHtml(element, paragraph, depth+1, normalFont, parentLevel);
                    ((Cell)parent).add(paragraph);
                }
            } else if (tag.equalsIgnoreCase("table")) {
                try {
                    Table table = new Table(3);
                    table.setBorderWidth(1);
                    table.setBorderColor(new Color(0,128,128));
                    table.setPadding(1.0f);
                    table.setSpacing(0.5f);
                    Cell c = new Cell("header");
                    c.setHeader(true);
                    c.setColspan(3);
                    table.addCell(c);
                    table.endHeaders();
                    printElementChildrenHtml(element, table, depth+1, normalFont, parentLevel);    // TODO
                    ((Section)parent).add(table);
                } catch (BadElementException e) {
                    e.printStackTrace();
                }
            } else if (tag.equalsIgnoreCase("tr")) {
                printElementChildrenHtml(element, parent, depth+1, normalFont, parentLevel);    // TODO
            } else if (tag.equalsIgnoreCase("td")) {
                Cell cell = new Cell();
                printElementChildrenHtml(element, cell, depth+1, normalFont, parentLevel);    // TODO
                ((Table)parent).addCell(cell);
            } else {
                System.err.println("Unknown element " + element.getName());
                printElementChildrenHtml(element, parent, depth+1, normalFont, parentLevel);
            }
        } else {
            return;    // could be BidiElement - not sure what it is
        }
    }

    private List<Object> generateElementChildrenHtml(Element element, int depth, Font font) {
        int n = element.getElementCount();
        List<Object> children = new ArrayList<Object>();
        for (int i=0; i<n; i++) {
            Element subelem = element.getElement(i);
            Object child = generateElementHtml(subelem, depth, font);
            if (child!=null) children.add(child);
        }
        return children;
    }

    private Object generateElementHtml(Element element, int depth, Font font) {
        String tag = element.getName();
        Object myself;
        Object av;
        if (element instanceof HTMLDocument.RunElement) {
            HTMLDocument.RunElement re = (HTMLDocument.RunElement)element;
            int start = re.getStartOffset();
            int end = re.getEndOffset();
            try {
                String content = re.getDocument().getText(start, end-start);
                HtmlAttr htmlattr = printAttributesHtml(re);
                av = re.getAttribute(CSS.Attribute.FONT_SIZE);
                String fontsize = av==null?null:av.toString();
                av = re.getAttribute(CSS.Attribute.FONT_FAMILY);
                String fontfamily = av==null?null:av.toString();
                av = re.getAttribute(CSS.Attribute.COLOR);
                String fontcolor = av==null?null:av.toString();
                if (fontcolor!=null || fontsize!=null || fontfamily!=null) {
                    if (fontfamily==null) fontfamily = font.getFamilyname();
                    if (fontsize != null && fontsize.endsWith("pt"))
                        fontsize = fontsize.substring(0, fontsize.indexOf("pt"));
                    float size = fontsize==null?font.getSize():(Float.parseFloat(fontsize)+8);
                    int style = font.getStyle();
                    Color color;
                    if (fontcolor!=null) {
                        color = Color.decode(fontcolor);
                    } else color = font.getColor();
                    font = FontFactory.getFont(fontfamily, size, style, color);
                } else if (htmlattr.bold || htmlattr.italic) {
                    String family = font.getFamilyname();
                    float size = font.getSize();
                    Color color = font.getColor();
                    if (htmlattr.bold && htmlattr.italic)
                        font = FontFactory.getFont(family, size, Font.BOLDITALIC, color);
                    else if (htmlattr.italic)
                        font = FontFactory.getFont(family, size, Font.ITALIC, color);
                    else if (htmlattr.bold)
                        font = FontFactory.getFont(family, size, Font.BOLD);
                }
                myself = new Chunk(content, font);
            } catch (BadLocationException e) {
                e.printStackTrace();
                myself = null;
            }
        } else if (element instanceof HTMLDocument.BlockElement) {
            HTMLDocument.BlockElement be = (HTMLDocument.BlockElement)element;
            HtmlAttr htmlattr = printAttributesHtml(be);
            if (htmlattr.bold) {
                System.out.println("+++BOLD!!!");
            }
            av = be.getAttribute(javax.swing.text.html.HTML.Attribute.ALIGN);
            String align = av==null?null:av.toString();
            if (htmlattr.bold || htmlattr.italic) {
                String family = font.getFamilyname();
                float size = font.getSize();
                Color color = font.getColor();
                if (htmlattr.bold && htmlattr.italic)
                    font = FontFactory.getFont(family, size, Font.BOLDITALIC, color);
                else if (htmlattr.italic)
                    font = FontFactory.getFont(family, size, Font.ITALIC, color);
                else if (htmlattr.bold)
                    font = FontFactory.getFont(family, size, Font.BOLD, Color.blue);
            }
            if (tag.equalsIgnoreCase("html")) {
                myself = generateElementChildrenHtml(element, depth+1, font);
            } else if (tag.equalsIgnoreCase("head")) {
                myself = null;
            } else if (tag.equalsIgnoreCase("body")) {
                myself = generateElementChildrenHtml(element, depth+1, font);
            } else if (tag.equalsIgnoreCase("p") || tag.equalsIgnoreCase("p-implied")) {
                List<Object> children = generateElementChildrenHtml(element, depth+1, normalFont);
                Paragraph paragraph = new Paragraph();
                paragraph.setFirstLineIndent(0F);
                for (Object child : children) {
                    if (child instanceof Chunk) {
                        Chunk chunk = (Chunk)child;
                        /*if (!chunk.getContent().equals("\n"))*/ paragraph.add(chunk);
                    } else paragraph.add(child);
                }
                if (align!=null) paragraph.setAlignment(align);
                myself = paragraph;
            } else if (tag.equalsIgnoreCase("h1") || tag.equalsIgnoreCase("h2") || tag.equalsIgnoreCase("h3") ) {
                List<Object> children = generateElementChildrenHtml(element, depth+1, subSectionFont);
                Paragraph title = new Paragraph();
                for (Object child : children) {
                    title.add(child);
                }
                myself = new TempSectionPdf(title);
            } else if (tag.equalsIgnoreCase("ul")) {
                com.lowagie.text.List list = new com.lowagie.text.List(false, false, 20.0f);
                list.setIndentationLeft(25.0f);
                List<Object> children = generateElementChildrenHtml(element, depth+1, normalFont);
                for (Object child : children) {
                    list.add(child);
                }
                myself = list;
            } else if (tag.equalsIgnoreCase("ol")) {
                com.lowagie.text.List list = new com.lowagie.text.List(true, false, 20.0f);
                list.setIndentationLeft(25.0f);
                List<Object> children = generateElementChildrenHtml(element, depth+1, normalFont);
                for (Object child : children) {
                    list.add(child);
                }
                myself = list;
            } else if (tag.equalsIgnoreCase("li")) {
                ListItem li = new ListItem();
                li.setSpacingAfter(0.0f);
                List<Object> children = generateElementChildrenHtml(element, depth+1, normalFont);
                for (Object child : children) {
                    li.add(child);
                }
                myself = li;
            } else if (tag.equalsIgnoreCase("table")) {
                List<Object> rows = generateElementChildrenHtml(element, depth+1, normalFont);
                try {
                    int ncols = 0;
                    for (Object row : rows) {
                        if (row instanceof List<?>) {
                            int n = ((List<?>)row).size();
                            if (n>ncols) ncols = n;
                        }
                    }
                    Table table = new Table(2);
                    table.setBorderWidth(1);
                    table.setBorderColor(new Color(0,128,128));
                    table.setPadding(1.0f);
                    table.setSpacing(0.5f);
                    Cell c = new Cell("header");
                    c.setHeader(true);
                    c.setColspan(ncols);
                    table.addCell(c);
                    table.endHeaders();
                    for (Object row : rows) {
                        if (row instanceof List<?>) {
                            for (Object cell : (List<?>)row) {
                                if (cell instanceof Cell)
                                    table.addCell((Cell)cell);
                            }
                        }
                    }
                    myself = table;
                } catch (BadElementException e) {
                    e.printStackTrace();
                    myself = null;
                }
            } else if (tag.equalsIgnoreCase("tr")) {
                List<Object> children = generateElementChildrenHtml(element, depth+1, normalFont);
                myself = children;
            } else if (tag.equalsIgnoreCase("td")) {
                Cell cell = new Cell();
                List<Object> children = generateElementChildrenHtml(element, depth+1, normalFont);
                for (Object child : children) {
                    cell.add(child);
                }
                myself = cell;
            } else if (tag.equalsIgnoreCase("div")) {
                List<Object> children = generateElementChildrenHtml(element, depth+1, normalFont);
                Paragraph paragraph = new Paragraph();
                paragraph.setFirstLineIndent(0F);
                for (Object child : children) {
                    paragraph.add(child);
                }
                if (align!=null) paragraph.setAlignment(align);
                myself = paragraph;
            } else {
                System.err.println("Unknown element " + element.getName());
                myself = null;
            }
        } else {
            myself = null;    // could be BidiElement - not sure what it is
        }
        return myself;
    }

    private void printElementChildrenHtml(Element element, Object parent, int depth,
            Font font, int parent_level) {
        int n = element.getElementCount();
        for (int i=0; i<n; i++) {
            Element subelem = element.getElement(i);
            printElementHtml(subelem, parent, depth, font, parent_level);
        }
    }

    private void printParagraphsHtml(StringBuffer sb, String content, int parentLevel) throws Exception {
        if (content==null || content.length()==0) return;
        if (isBase64Encoded(content) || "true".equals(System.getProperty("mdw.designer.force.msword"))) {
              byte[] docBytes = decodeBase64(content);
              content = new DocxBuilder(docBytes).toHtml();
        }
        if (content.contains("<html>")) {
            int k1 = content.indexOf("<html>");
            int k2 = content.indexOf("<body>");
            int k3 = content.indexOf("</body>");
            int k4 = content.indexOf("</html>");
            if (k2>0) {
                if (k3>0) content = content.substring(k2+6,k3);
                else if (k4>0) content = content.substring(k2+6,k4);
                else content = content.substring(k2+6);
            } else {
                if (k3>0) content = content.substring(k1+6,k3);
                else if (k4>0) content = content.substring(k1+6,k4);
                else content = content.substring(k1+6);
            }
            sb.append(content);
        }
        else {
            String[] details = content.split("\n");
            for (int j = 0; j < details.length; j++) {
                if (details[j].length() == 0) sb.append("<br/>\n");
                else sb.append(escapeXml(details[j])).append("\n");
            }
        }
    }

    private StringBuffer printPrologHtml(String title) {
        StringBuffer sb = new StringBuffer();
        sb.append("<html>\n<head>\n");
        sb.append("<title>").append(title).append("</title>\n");
        sb.append("<style>\n");
        sb.append("body {font-family: Arial; font-size: smaller; padding:5px; }\n");
        sb.append("em { color: #a52a2a; }\n");
        sb.append("</style>\n");
        sb.append("</head>\n<body>\n");
        return sb;
    }

    private void printEpilogHtml(StringBuffer sb, String filename) throws Exception {
        FileOutputStream os = null;
        try {
            sb.append("</body></html>\n");
            os = new FileOutputStream(filename);
            os.write(sb.toString().getBytes());
            os.close();
        } finally {
            if (os!=null) {
                os.close();
            }
        }
    }

    private class HtmlAttr {
        boolean bold = false;
        boolean italic = false;
    }

    private HtmlAttr printAttributesHtml(AbstractElement ae) {
        Enumeration<?> attrs = ae.getAttributeNames();
        HtmlAttr htmlattr = new HtmlAttr();
        while (attrs.hasMoreElements()) {
            Object attr = attrs.nextElement();
            if (attr.equals(StyleConstants.NameAttribute)) continue;
            if (attr.equals("CR")) continue;
            if (attr.equals(javax.swing.text.html.HTML.Tag.FONT)) continue;
//            if (attr.equals(CSS.Attribute.FONT_FAMILY)) continue;
//            if (attr.equals(CSS.Attribute.FONT_SIZE)) continue;
            if (attr instanceof javax.swing.text.html.HTML.Tag) {
                if (attr.toString().equals("b")) htmlattr.bold = true;
                else if (attr.toString().equals("i")) htmlattr.italic = true;
                else printUnknownAttributeHtml(ae, attr);
            } else if (attr instanceof javax.swing.text.html.CSS.Attribute) {
                // it seems both HTML.Tag and CSS.Attribute are set for bold/italic
                // the following code duplicate above, have it here just to
                // avoid printing unknown attributes
                if (attr.toString().equals("font-weight")) {
                    if (ae.getAttribute(attr).toString().equals("bold")) htmlattr.bold = true;
                } else if (attr.toString().equals("font-style")) {
                    if (ae.getAttribute(attr).toString().equals("italic")) htmlattr.italic = true;
                } else printUnknownAttributeHtml(ae, attr);
            } else printUnknownAttributeHtml(ae, attr);
        }
        return htmlattr;
    }

    private void printUnknownAttributeHtml(AbstractElement ae, Object attr) {
        Object v = ae.getAttribute(attr);
        System.out.println(" attr: " + attr.toString() + " (" +
                attr.getClass().getName() + "): " +
                v.toString() + " (" + v.getClass().getName() + ")");
    }

    private Chapter printOneProcessPdf(DocWriter writer, DesignerCanvas canvas, String type,
            int chapter_number, Graph process, String filename, Rectangle page_size) throws Exception {
        Paragraph cTitle = new Paragraph("Workflow Process: \"" + process.getName() + "\"", chapterFont);
        Chapter chapter = new Chapter(cTitle, chapter_number);
        // print image
        printGraphPdf(writer, canvas, process, page_size, type, filename, chapter, chapter_number);
        // print documentation text
        printGraphPdf(process, chapter);
        for (Node node : process.getNodes(nodeIdType)) {
            printNodePdf(node, chapter);
        }
        for (SubGraph subgraph : process.getSubgraphs(nodeIdType)) {
            printGraphPdf(subgraph, chapter);
            for (Node node : subgraph.getNodes(nodeIdType)) {
                printNodePdf(node, chapter);
            }
        }
        if (options.contains(VARIABLES)) {
            printVariablesPdf(chapter, process.getProcessVO().getVariables(), options.contains(SECTION_NUMBER)?1:0);
        }

        return chapter;
    }

    private void printGraphPdf(GraphCommon graph, Chapter chapter) throws Exception {
        Section section;
        String tmp = null;
        if (graph instanceof SubGraph) {
            SubGraph subgraph = (SubGraph)graph;
            String id = subgraph.getDisplayId(nodeIdType);
            if (id==null || id.length()==0) id = subgraph.getId().toString();
            tmp = "Subprocess " + id + ": \"" + subgraph.getName().replace('\n', ' ') + "\"";
        }
        else {
            tmp = "Process Description";
        }
        Paragraph sTitle = new Paragraph(tmp, sectionFont);
        sTitle.setSpacingBefore(10);
        section = chapter.addSection(sTitle, options.contains(SECTION_NUMBER)?2:0);
        String summary = (graph instanceof SubGraph)?
                ((SubGraph)graph).getProcessVO().getProcessDescription():((Graph)graph).getProcessVO().getProcessDescription();
        if (summary!=null && summary.length()>0)
            printBoldParagraphsPdf(section, summary);
        if (options.contains(DOCUMENTATION)) {
            String detail = graph.getProcessVO().getAttribute(WorkAttributeConstant.DOCUMENTATION);
            if (detail!=null && detail.length()>0) {
                printHtmlParagraphsPdf(section, detail, options.contains(SECTION_NUMBER)?2:0);
            }
        }
        if (options.contains(ATTRIBUTES) && !graph.getProcessVO().getAttributes().isEmpty() && graph instanceof SubGraph) {
            printAttributesPdf(section, graph.getProcessVO().getAttributes(), options.contains(SECTION_NUMBER)?2:0);
        }
    }


    private void printNodePdf(Node node, Chapter chapter) throws Exception {
        Section section;
        String id = node.getDisplayId(nodeIdType);
        if (id==null || id.length()==0) id = node.getId().toString();
        String tmp = "Activity " + id + ": \"" + node.getName().replace('\n', ' ') + "\n";
        Paragraph sTitle = new Paragraph(tmp, sectionFont);
        section = chapter.addSection(sTitle, options.contains(SECTION_NUMBER)?2:0);
        String summary = node.getAttribute(WorkAttributeConstant.DESCRIPTION);
        if (summary != null && summary.length() > 0) {
            printBoldParagraphsPdf(section, summary);
        }

        if (options.contains(DOCUMENTATION)) {
            String detail = node.getAttribute(WorkAttributeConstant.DOCUMENTATION);
            if (detail!=null && detail.length()>0) {
                printHtmlParagraphsPdf(section, detail, options.contains(SECTION_NUMBER)?2:0);
                section.add(new Paragraph("\n", FontFactory.getFont(FontFactory.TIMES, 4, Font.NORMAL, new Color(0, 0, 0))));
            }
        }
        if (options.contains(ATTRIBUTES) && !node.getAttributes().isEmpty()) {
            printAttributesPdf(section, node.getAttributes(), options.contains(SECTION_NUMBER)?2:0);
        }
        section.add(new Paragraph("\n", normalFont));
    }

    public void printImage(String filename, float scale, CanvasCommon canvas, Dimension graphsize) {
        try {
            int h_margin = 72, v_margin = 72;
            BufferedImage image = new BufferedImage(graphsize.width + h_margin,
                    graphsize.height + v_margin, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = image.createGraphics();
            if (scale > 0)
                g2.scale(scale, scale);
            g2.setBackground(Color.WHITE);
            g2.clearRect(0, 0, image.getWidth(), image.getHeight());
            // canvas.paint(g2);
            Color bgsave = canvas.getBackground();
            canvas.setBackground(Color.white);
            canvas.paintComponent(g2);
            canvas.setBackground(bgsave);
            g2.dispose();
            ImageIO.write(image, "jpeg", new File(filename));
            image = null;
            Runtime r = Runtime.getRuntime();
            r.gc();
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    public byte[] printImage(float scale, CanvasCommon canvas, Dimension graphsize, String format) throws IOException {
        int h_margin = 72, v_margin = 72;
        BufferedImage image = new BufferedImage(graphsize.width + h_margin,
                graphsize.height + v_margin, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        if (scale > 0)
            g2.scale(scale, scale);
        g2.setBackground(Color.WHITE);
        g2.clearRect(0, 0, image.getWidth(), image.getHeight());
        // canvas.paint(g2);
        Color bgsave = canvas.getBackground();
        boolean edsave = canvas.editable;
        canvas.editable = false;
        canvas.setBackground(Color.white);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        canvas.paintComponent(g2);
        canvas.setBackground(bgsave);
        canvas.editable = edsave;
        g2.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format, baos);
        image = null;
        Runtime r = Runtime.getRuntime();
        r.gc();
        return baos.toByteArray();
    }

    public void printImagePdf(String filename, DesignerCanvas canvas, Dimension graphsize) {
        try {
            DefaultFontMapper mapper = new DefaultFontMapper();
            FontFactory.registerDirectories();
            mapper.insertDirectory("c:\\winnt\\fonts");
            // mapper.insertDirectory("c:\\windows\\fonts");
            // we create a template and a Graphics2D object that corresponds
            // with it
            int margin = 72; // 1 inch
            float scale = 0.5f;
            boolean multiple_page = true;
            Rectangle page_size;
            if (multiple_page) {
                page_size = PageSize.LETTER.rotate();
            } else {
                page_size = new Rectangle((int) (graphsize.getWidth() * scale)
                        + margin, (int) (graphsize.getHeight() * scale)
                        + margin);
            }
            Document document = new Document(page_size);
            DocWriter writer = PdfWriter.getInstance(document,
                    new FileOutputStream(filename));
            document.open();
            document.setPageSize(page_size);
            int image_w = (int) page_size.getWidth() - margin;
            int image_h = (int) page_size.getHeight() - margin;
            boolean edsave = canvas.editable;
            canvas.editable = false;
            Color bgsave = canvas.getBackground();
            canvas.setBackground(Color.white);
            if (multiple_page) {
                int horizontal_pages = (int) (graphsize.width * scale)
                        / image_w + 1;
                int vertical_pages = (int) (graphsize.height * scale) / image_h
                        + 1;
                for (int i = 0; i < horizontal_pages; i++) {
                    for (int j = 0; j < vertical_pages; j++) {
                        Image img;
                        PdfContentByte cb = ((PdfWriter) writer)
                                .getDirectContent();
                        PdfTemplate tp = cb.createTemplate(image_w, image_h);
                        Graphics2D g2 = tp.createGraphics(image_w, image_h,
                                mapper);
                        tp.setWidth(image_w);
                        tp.setHeight(image_h);
                        g2.scale(scale, scale);
                        g2
                                .translate(-i * image_w / scale, -j * image_h
                                        / scale);
                        canvas.paintComponent(g2);
                        g2.dispose();
                        img = new ImgTemplate(tp);
                        document.add(img);
                    }
                }
            } else {
                Image img;
                PdfContentByte cb = ((PdfWriter) writer).getDirectContent();
                PdfTemplate tp = cb.createTemplate(image_w, image_h);
                Graphics2D g2 = tp.createGraphics(image_w, image_h, mapper);
                tp.setWidth(image_w);
                tp.setHeight(image_h);
                g2.scale(scale, scale);
                canvas.paintComponent(g2);
                g2.dispose();
                img = new ImgTemplate(tp);
                document.add(img);
            }
            canvas.setBackground(bgsave);
            canvas.editable = edsave;
            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void printGraphPdf(DocWriter writer, CanvasCommon canvas,
            Graph process, Rectangle page_size, String type, String filename,
            Chapter chapter, int chapter_number) throws Exception {
        Dimension graphsize = process.getGraphSize();
        // we create a fontMapper and read all the fonts in the font directory
        DefaultFontMapper mapper = new DefaultFontMapper();
        FontFactory.registerDirectories();
        mapper.insertDirectory("c:\\winnt\\fonts");
        // mapper.insertDirectory("c:\\windows\\fonts");
        // we create a template and a Graphics2D object that corresponds with it
        int w, h;
        float scale;
        if ((float) graphsize.width < page_size.getWidth() * 0.8
                && (float) graphsize.height < page_size.getHeight() * 0.8
                || type.equals(HTML)) {
            w = graphsize.width + 36;
            h = graphsize.height + 36;
            scale = -1f;
        } else {
            scale = page_size.getWidth() * 0.8f / (float) graphsize.width;
            if (scale > page_size.getHeight() * 0.8f / (float) graphsize.height)
                scale = page_size.getHeight() * 0.8f / (float) graphsize.height;
            w = (int) (graphsize.width * scale) + 36;
            h = (int) (graphsize.height * scale) + 36;
        }
        Image img;
        int zoomSave = process.zoom;
        process.zoom = 100;
        Color bgsave = canvas.getBackground();
        boolean edsave = canvas.editable;
        canvas.editable = false;
        canvas.setBackground(Color.white);

        if (type.equals(PDF)) {
            PdfContentByte cb = ((PdfWriter) writer).getDirectContent();
            PdfTemplate tp = cb.createTemplate(w, h);
            Graphics2D g2 = tp.createGraphics(w, h, mapper);
            if (scale > 0)
                g2.scale(scale, scale);
            tp.setWidth(w);
            tp.setHeight(h);
            canvas.paintComponent(g2);
            g2.dispose();
            // cb.addTemplate(tp, 50, 400);
            img = new ImgTemplate(tp);
        } else {
            String imgfilename = filename + "." + process.getName() + "_ch"
                    + chapter_number + ".jpg";
            printImage(imgfilename, -1f, canvas, graphsize);
            img = Image.getInstance(imgfilename);
            if (scale > 0)
                img.scalePercent(scale * 100);
        }
        process.zoom = zoomSave;
        canvas.setBackground(bgsave);
        canvas.editable = edsave;
        if (img != null)
            chapter.add(img);
    }

    private void printBoldParagraphsPdf(Section section, String content) {
        if (content==null || content.length()==0) return;
        String[] details = content.split("\n");
        String tmp = null;
        for (int j = 0; j < details.length; j++) {
            if (details[j].length() == 0) {
                if (tmp != null)
                    section.add(new Paragraph(tmp + "\n", boldFont));
                tmp = null;
            } else if (tmp == null) {
                tmp = details[j];
            } else {
                tmp += " " + details[j];
            }
        }
        if (tmp != null) {
            section.add(new Paragraph(tmp + "\n", boldFont));
        }
    }

    private void printHtmlParagraphsPdf(Section section, String content, int parentLevel) throws Exception {
        if (content==null || content.length()==0) return;
        if (isBase64Encoded(content) || "true".equals(System.getProperty("mdw.designer.force.msword"))) {
              byte[] docBytes = decodeBase64(content);
              content = new DocxBuilder(docBytes).toHtml();
              content = content.replaceAll("&nbsp;", "&#160;");
        }
        JEditorPane documentation = new JEditorPane();
        documentation.setContentType("text/html");
        documentation.setText(content);
        javax.swing.text.Document swingdoc = documentation.getDocument();
        Element[] elements = swingdoc.getRootElements();
        boolean useGenerate = true;
        if (useGenerate) {
            for (Element e: elements) {
                Object gen = generateElementHtml(e, 0, normalFont);
                addSectionContentPdf(section, gen, parentLevel);
            }
        } else {    // use print
            for (Element e: elements) {
                printElementHtml(e, section, 0, normalFont, parentLevel);
            }
        }
    }

    class TempSectionPdf {
        Paragraph title;
        TempSectionPdf(Paragraph title) { this.title = title; }
    }

    private void addSectionContentPdf(Section section, Object one, int parent_level) {
        if (one==null) return;
        if (one instanceof List<?>) {
            for (Object two : (List<?>)one) {
                addSectionContentPdf(section, two, parent_level);
            }
        } else if (one instanceof TempSectionPdf) {
            section.addSection(((TempSectionPdf)one).title, parent_level==0?0:(parent_level+1));
        } else {
            section.add(one);
        }
    }

    private void printAttributesPdf(Section section, List<AttributeVO> attrs, int parentLevel) {
        Paragraph sTitle = new Paragraph("Activity Attributes", subSectionFont);
        Section subsection = section.addSection(sTitle, parentLevel==0?0:(parentLevel+1));
        com.lowagie.text.List list = new com.lowagie.text.List(false, false, 20.0f);
        for (AttributeVO attr : attrs) {
            if (excludeAttribute(attr.getAttributeName(), attr.getAttributeValue())) continue;
            Phrase phrase = new Phrase();
            phrase.add(new Chunk(attr.getAttributeName(), fixedWidthFont));
            String v = attr.getAttributeValue();
            if (v==null) v = "";
            phrase.add(new Chunk(": " + v, normalFont));
            list.add(new ListItem(phrase));
        }
        subsection.add(list);
    }

    private void printVariablesPdf(Chapter chapter, List<VariableVO> variables, int parentLevel) {
        Paragraph sTitle = new Paragraph("Process Variables", sectionFont);
        Section section = chapter.addSection(sTitle, parentLevel==0?0:(parentLevel+1));
        com.lowagie.text.List list = new com.lowagie.text.List(false, false, 10.0f);
        for (VariableVO var : variables) {
            Phrase phrase = new Phrase();
            phrase.add(new Chunk(var.getVariableName(), fixedWidthFont));
            String v = var.getVariableType();
            if (var.getDescription() != null)
                v += " (" + var.getDescription() + ")";
            phrase.add(new Chunk(": " + v + "\n", normalFont));
            list.add(new ListItem(phrase));
        }
        section.add(list);
    }

    private boolean excludeAttribute(String name, String value) {
        if (value == null || value.isEmpty())
            return true;
        if (excludedAttributes.contains(name))
            return true;
        if (value.equals(excludedAttributesForSpecificValues.get(name)))
            return true;
        return false;
    }

    private boolean isBase64Encoded(String input) {
        return input != null && input.length() > 20 && !input.startsWith("<html>")
          && input.matches("^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)$");
    }

    private byte[] decodeBase64(String inputString) throws Exception {
        // avoid Designer Classic dependency
        return (byte[]) Class.forName("org.apache.commons.codec.binary.Base64").getMethod("decodeBase64", byte[].class).invoke(null, inputString.getBytes());
    }
}
