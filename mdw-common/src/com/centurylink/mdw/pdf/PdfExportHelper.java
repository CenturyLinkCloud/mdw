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
package com.centurylink.mdw.pdf;

import com.centurylink.mdw.constant.WorkAttributeConstant;
import com.centurylink.mdw.export.Table;
import com.centurylink.mdw.html.HtmlExportHelper;
import com.centurylink.mdw.image.PngProcessExporter;
import com.centurylink.mdw.image.ProcessCanvas;
import com.centurylink.mdw.model.Attributes;
import com.centurylink.mdw.model.asset.Pagelet;
import com.centurylink.mdw.model.project.Project;
import com.centurylink.mdw.model.workflow.Activity;
import com.centurylink.mdw.model.workflow.ActivityNodeSequencer;
import com.centurylink.mdw.model.workflow.Process;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.*;
import com.lowagie.text.html.simpleparser.HTMLWorker;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PdfExportHelper extends HtmlExportHelper {

    private Font chapterFont = FontFactory.getFont(FontFactory.HELVETICA, 18, Font.BOLD);
    private Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA, 14, Font.BOLD);
    private Font subSectionFont = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD);
    private Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.NORMAL);
    private Font fixedWidthFont = FontFactory.getFont(FontFactory.COURIER, 10, Font.NORMAL);
    private Font boldFont = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD);
    private Font spacerFont =  FontFactory.getFont(FontFactory.HELVETICA, 4, Font.NORMAL);
    private ParagraphBorder paragraphBorder = new ParagraphBorder();

    public static final String ACTIVITY = "Activity ";

    public PdfExportHelper(Project project) {
        super(project);
    }

    public void exportProcess(Process process, OutputStream out) throws IOException {
        new ActivityNodeSequencer(process).assignNodeSequenceIds();
        Document document = new Document();
        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(paragraphBorder);
            document.open();
            Rectangle pageSize = document.getPageSize();
            Chapter chapter = printProcess(writer, 1, process, pageSize);
            document.add(chapter);
            document.close();
        } catch (Exception ex) {
            if (ex instanceof IOException)
                throw (IOException) ex;
            else
                throw new IOException(ex);
        }
    }

    private Chapter printProcess(DocWriter writer, int chapterNumber, Process process, Rectangle pageSize) throws Exception {
        Paragraph title = new Paragraph("Workflow: \"" + process.getName() + "\"",  chapterFont);
        Chapter chapter = new Chapter(title, chapterNumber);

        // diagram
        printDiagram(writer, chapter, process, pageSize);

        // documentation
        printDocumentation(process, chapter);

        // activities
        for (Activity activity : process.getActivitiesOrderBySeq()) {
            printActivity(activity, chapter);
        }

        // subprocesses
        for (Process subproc : process.getSubprocesses()) {
            printDocumentation(subproc, chapter);
            for (Activity node : subproc.getActivitiesOrderBySeq()) {
                printActivity(node, chapter);
            }
        }

        // variables
        printVariables(chapter, process, 1);

        return chapter;
    }

    private void printDiagram(DocWriter writer, Chapter chapter, Process process,
            Rectangle pageSize) throws Exception {
        ProcessCanvas canvas = new ProcessCanvas(project, process);
        canvas.prepare();

        Dimension size = new PngProcessExporter(project).getDiagramSize(process);
        // create a template and a Graphics2D object that corresponds with it
        int w;
        int h;
        float scale;
        if ((float) size.width < pageSize.getWidth() * 0.8
                && (float) size.height < pageSize.getHeight() * 0.8) {
            w = size.width + 36;
            h = size.height + 36;
            scale = -1f;
        }
        else {
            scale = pageSize.getWidth() * 0.8f / (float) size.width;
            if (scale > pageSize.getHeight() * 0.8f / (float) size.height)
                scale = pageSize.getHeight() * 0.8f / (float) size.height;
            w = (int) (size.width * scale) + 36;
            h = (int) (size.height * scale) + 36;
        }

        PdfContentByte cb = ((PdfWriter) writer).getDirectContent();
        PdfTemplate tp = cb.createTemplate(w, h);
        Graphics2D g2 = tp.createGraphics(w, h);
        if (scale > 0)
            g2.scale(scale, scale);
        tp.setWidth(w);
        tp.setHeight(h);
        canvas.paintComponent(g2);
        g2.dispose();
        Image img = new ImgTemplate(tp);
        chapter.add(img);
        canvas.dispose();
    }

    private void printDocumentation(Process process, Chapter chapter) throws Exception {
        String title;
        if (process.isEmbeddedProcess()) {
            String id = process.getAttribute(WorkAttributeConstant.LOGICAL_ID);
            if (id == null || id.length() == 0)
                id = process.getId().toString();
            title = "Subprocess " + id + ": \"" + process.getName().replace('\n', ' ') + "\"";
        }
        else {
            title = "Documentation";
        }
        Paragraph paragraph = new Paragraph(title, sectionFont);
        paragraph.setSpacingBefore(10);
        paragraph.setSpacingAfter(5);
        Section section = chapter.addSection(paragraph, 2);

        String summary = process.getDescription();
        if (summary != null && summary.length() > 0)
            printBoldParagraphs(chapter, summary);
        String markdown = process.getAttribute(WorkAttributeConstant.DOCUMENTATION);
        if (markdown != null && markdown.length() > 0) {
            printHtml(section, getHtml(markdown), 0);
        }
    }

    private void printAttributes(Section section, Activity activity, int parentLevel) throws Exception {
        Paragraph title = new Paragraph("Attributes", subSectionFont);
        Section subsection = section.addSection(title, parentLevel == 0 ? 0 : (parentLevel + 1));
        subsection.add(new Paragraph("\n", spacerFont));

        com.lowagie.text.List list = new com.lowagie.text.List(false, false, 10);
        list.setIndentationLeft(20);
        Attributes attributes = activity.getAttributes();
        if (attributes != null) {
            List<String> sortedNames = new ArrayList<>(attributes.keySet());
            Collections.sort(sortedNames);
            for (String name : sortedNames) {
                if (!isExcludeAttribute(name, attributes.get(name))) {
                    Paragraph paragraph = new Paragraph();
                    paragraph.add(new Chunk(getAttributeLabel(activity, name) + ": ", normalFont));
                    printAttributeValue(paragraph, activity, attributes, name);
                    list.add(new ListItem(paragraph));
                }
            }
        }

        subsection.add(list);
    }

    private void printAttributeValue(Paragraph paragraph, Activity activity, Attributes attributes, String attributeName)
            throws Exception {
        if (attributes == null || !attributes.containsKey(attributeName))
            return;

        Pagelet.Widget widget = getWidget(activity, attributeName);
        if (widget != null || WorkAttributeConstant.MONITORS.equals(attributeName)) {
            if (isTabular(activity, attributeName)) {
                paragraph.setIndentationLeft(30);
                Table table = getTable(activity, attributes, attributeName);
                if (table.getColumns().length == 2)
                    table.setWidths(new int[]{40, 60});
                else if (table.getColumns().length == 3)
                    table.setWidths(new int[]{30, 40, 30});
                else if (table.getColumns().length == 4)
                    table.setWidths((new int[]{20, 40, 30, 10}));
                printTable(paragraph, table);
            }
            else if (isCode(activity, attributeName)) {
                printCodeBox(paragraph, attributes.get(attributeName));
            }
            else {
                paragraph.add(new Chunk(attributes.get(attributeName), normalFont));
            }
        }
        else {
            paragraph.add(new Chunk(attributes.get(attributeName), normalFont));
        }
    }

    private void printCodeBox(Paragraph paragraph, String code) {
        // TODO border doesn't work
        paragraphBorder.setActive(true);
        Paragraph spacer = new Paragraph("", spacerFont);
        spacer.setSpacingBefore(0);
        paragraph.add(spacer);
        Paragraph subParagraph = new Paragraph(code, fixedWidthFont);
        subParagraph.setSpacingBefore(0);
        subParagraph.setSpacingAfter(10);
        paragraph.add(subParagraph);
        paragraph.add(new Paragraph("", spacerFont));
        paragraphBorder.setActive(false);
    }

    private void printTable(TextElementArray paragraph, Table table) {
        com.lowagie.text.Table pdfTable = new com.lowagie.text.Table(table.getColumns().length);
        pdfTable.setPadding(5);
        pdfTable.setWidth(95f);
        if (table.getWidths() != null) {
            pdfTable.setWidths(table.getWidths());
        }
        if (table.getColumns() != null) {
            for (String column : table.getColumns()) {
                Cell cell = new Cell(column);
                cell.setHeader(true);
                cell.setGrayFill(0.75f);
                pdfTable.addCell(cell);
            }
            pdfTable.endHeaders();
        }
        if (table.getRows() != null && table.getRows().length > 0) {
            for (int i = 0; i < table.getRows()[0].length; i++) {
                for (int j = 0; j < table.getColumns().length; j++) {
                    pdfTable.addCell(new Cell(table.getRows()[j][i]));
                }
            }
        }

        paragraph.add(pdfTable);
    }

    private void printVariables(Chapter chapter, Process process, int parentLevel) throws Exception {
        Paragraph title = new Paragraph("Process Variables", sectionFont);
        title.setSpacingBefore(5);
        Section section = chapter.addSection(title, parentLevel == 0 ? 0 : (parentLevel + 1));
        printTable(section, getVariablesTable(process));
    }

    private void printActivity(Activity activity, Chapter chapter) throws Exception {
        String title = ACTIVITY + activity.getLogicalId() + ": \"" + activity.getName().replaceAll("\\R", " ") + "\"\n";
        Paragraph paragraph = new Paragraph(title, sectionFont);
        paragraph.setSpacingBefore(10);
        paragraph.setSpacingAfter(5);
        Section section = chapter.addSection(paragraph, 2);

        String summary = activity.getAttribute(WorkAttributeConstant.DESCRIPTION);
        if (summary != null && summary.length() > 0) {
            printBoldParagraphs(section, summary);
        }

        String markdown = activity.getAttribute(WorkAttributeConstant.DOCUMENTATION);
        if (markdown != null && markdown.length() > 0) {
            printHtml(section, getHtml(markdown), 10);
        }

        if (!isEmpty(activity.getAttributes())) {
            printAttributes(section, activity, 2);
        }
    }

    private void printBoldParagraphs(Section section, String content) {
        if (content == null || content.length() == 0)
            return;
        String[] details = content.split("\n");
        String tmp = null;
        for (int j = 0; j < details.length; j++) {
            if (details[j].length() == 0) {
                if (tmp != null)
                    section.add(new Paragraph(tmp + "\n", boldFont));
                tmp = null;
            }
            else if (tmp == null) {
                tmp = details[j];
            }
            else {
                tmp += " " + details[j];
            }
        }
        if (tmp != null) {
            section.add(new Paragraph(tmp + "\n", boldFont));
        }
    }

    private void printHtml(Section section, String html, int indent) throws Exception {
        if (html == null || html.length() == 0)
            return;

        Paragraph paragraph = new Paragraph();
        html = html.replaceAll("</p>", "</p><br/>");

        List<Element> list = HTMLWorker.parseToList(new StringReader(html), null);

        for (Element element : list) {
            element.isContent();
            paragraph.add(element);
        }
        paragraph.setSpacingBefore(0f);
        if (indent > 0)
            paragraph.setIndentationLeft(indent);
        section.add(paragraph);
    }

    class ParagraphBorder extends PdfPageEventHelper {
        public boolean active = false;
        public void setActive(boolean active) {
            this.active = active;
        }

        public float offset = 5;
        public float startPosition;

        @Override
        public void onParagraph(PdfWriter writer, Document document, float paragraphPosition) {
            this.startPosition = paragraphPosition;
        }

        @Override
        public void onParagraphEnd(PdfWriter writer, Document document, float paragraphPosition) {
            if (active) {
                PdfContentByte cb = writer.getDirectContentUnder();
                cb.rectangle(document.left(), paragraphPosition - offset, document.right() - document.left(),
                        startPosition - paragraphPosition);
                cb.stroke();
            }
        }
    }
}
