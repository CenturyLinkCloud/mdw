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
import com.centurylink.mdw.export.ExportHelper;
import com.centurylink.mdw.image.ProcessCanvas;
import com.centurylink.mdw.model.Project;
import com.centurylink.mdw.model.attribute.Attribute;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.workflow.Activity;
import com.centurylink.mdw.model.workflow.ActivityNodeSequencer;
import com.centurylink.mdw.model.workflow.Process;
import com.itextpdf.text.*;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.tool.xml.ElementList;
import com.itextpdf.tool.xml.XMLWorkerHelper;

import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class PdfExportHelper extends ExportHelper {

    private Font chapterFont;
    private Font sectionFont;
    private Font subSectionFont;
    private Font normalFont;
    private Font fixedWidthFont;
    private Font boldFont;

    public static final String ACTIVITY = "Activity ";

    public PdfExportHelper(Project project) {
        super(project);
        chapterFont = FontFactory.getFont(FontFactory.TIMES, 20, Font.BOLD);
        sectionFont = FontFactory.getFont(FontFactory.TIMES, 16, Font.BOLD);
        subSectionFont = FontFactory.getFont(FontFactory.TIMES, 14, Font.BOLD);
        normalFont = FontFactory.getFont(FontFactory.TIMES, 12, Font.NORMAL);
        boldFont = FontFactory.getFont(FontFactory.TIMES, 12, Font.BOLD);
        fixedWidthFont = FontFactory.getFont(FontFactory.COURIER, 11, Font.NORMAL);
    }

    public byte[] exportProcess(Process process, File outputFile) throws Exception {
        new ActivityNodeSequencer(process).assignNodeSequenceIds();
        Document document = new Document();
        DocWriter writer = PdfWriter.getInstance(document, new FileOutputStream(outputFile));
        document.open();
        document.setPageSize(PageSize.LETTER);
        Rectangle pageSize = document.getPageSize();
        Chapter chapter = printProcessPdf(writer, 1, process, pageSize);
        document.add(chapter);
        document.close();

        return Files.readAllBytes(Paths.get(outputFile.getPath()));
    }

    private Chapter printProcessPdf(DocWriter writer, int chapterNumber, Process process,
            Rectangle pageSize) throws Exception {
        Paragraph cTitle = new Paragraph("Workflow Process: \"" + process.getName() + "\"",
                chapterFont);
        Chapter chapter = new Chapter(cTitle, chapterNumber);
        // print image
        ProcessCanvas canvas = new ProcessCanvas(project, process);
        printGraph(writer, canvas, process, pageSize, chapter);
        // print documentation text
        printGraphDocumentation(process, chapter);
        for (Activity node : process.getActivitiesOrderBySeq()) {
            printActivity(node, chapter);
        }
        for (Process subproc : process.getSubprocesses()) {
            printGraphDocumentation(subproc, chapter);
            for (Activity node : subproc.getActivitiesOrderBySeq()) {
                printActivity(node, chapter);
            }
        }
        printVariables(chapter, process.getVariables(), 1);
        return chapter;
    }

    private void printAttributes(Section section, List<Attribute> attrs, int parentLevel) {
        Paragraph sTitle = new Paragraph("Activity Attributes", subSectionFont);
        Section subsection = section.addSection(sTitle, parentLevel == 0 ? 0 : (parentLevel + 1));
        com.itextpdf.text.List list = new com.itextpdf.text.List(false, false, 20.0f);
        for (Attribute attr : attrs) {
            if (excludeAttribute(attr.getAttributeName(), attr.getAttributeValue()))
                continue;
            Phrase phrase = new com.itextpdf.text.Phrase();
            phrase.add(new Chunk(attr.getAttributeName(), fixedWidthFont));
            String v = attr.getAttributeValue();
            if (v == null)
                v = "";
            phrase.add(new Chunk(": " + v, normalFont));
            list.add(new ListItem(phrase));
        }
        subsection.add(list);
    }

    private void printVariables(Chapter chapter, List<Variable> variables, int parentLevel) {
        Paragraph sTitle = new Paragraph("Process Variables", sectionFont);
        Section section = chapter.addSection(sTitle, parentLevel == 0 ? 0 : (parentLevel + 1));
        com.itextpdf.text.List list = new com.itextpdf.text.List(false, false, 10.0f);
        for (Variable var : variables) {
            Phrase phrase = new Phrase();
            phrase.add(new Chunk(var.getName(), fixedWidthFont));
            String v = var.getType();
            if (var.getDescription() != null)
                v += " (" + var.getDescription() + ")";
            phrase.add(new Chunk(": " + v + "\n", normalFont));
            list.add(new ListItem(phrase));
        }
        section.add(list);
    }

    private void printActivity(Activity act, Chapter chapter) throws Exception {
        Section section;
        String tmp = ACTIVITY + act.getLogicalId() + ": \"" + act.getName().replace('\n', ' ')
                + "\n";
        Paragraph sTitle = new Paragraph(tmp, sectionFont);
        section = chapter.addSection(sTitle, 2);
        String summary = act.getAttribute(WorkAttributeConstant.DESCRIPTION);
        if (summary != null && summary.length() > 0) {
            printBoldParagraphs(section, summary);
        }

        String detail = act.getAttribute(WorkAttributeConstant.DOCUMENTATION);
        if (detail != null && detail.length() > 0) {
            printHtmlParagraphs(section, detail, 2);
            section.add(
                    new Paragraph("\n", FontFactory.getFont(FontFactory.TIMES, 4, Font.NORMAL)));
        }
        if (!act.getAttributes().isEmpty()) {
            printAttributes(section, act.getAttributes(), 2);
        }
        section.add(new Paragraph("\n", normalFont));
    }

    private void printGraph(DocWriter writer, ProcessCanvas canvas, Process process,
            Rectangle pageSize, Chapter chapter)
            throws Exception {
        Dimension graphsize = getGraphSize(process);
        // we create a template and a Graphics2D object that corresponds with it
        int w;
        int h;
        float scale;
        if ((float) graphsize.width < pageSize.getWidth() * 0.8
                && (float) graphsize.height < pageSize.getHeight() * 0.8) {
            w = graphsize.width + 36;
            h = graphsize.height + 36;
            scale = -1f;
        }
        else {
            scale = pageSize.getWidth() * 0.8f / (float) graphsize.width;
            if (scale > pageSize.getHeight() * 0.8f / (float) graphsize.height)
                scale = pageSize.getHeight() * 0.8f / (float) graphsize.height;
            w = (int) (graphsize.width * scale) + 36;
            h = (int) (graphsize.height * scale) + 36;
        }
        Image img;
        canvas.setBackground(Color.white);
        PdfContentByte cb = ((PdfWriter) writer).getDirectContent();
        PdfTemplate tp = cb.createTemplate(w, h);
        Graphics2D g2 = tp.createGraphics(w, h);
        if (scale > 0)
            g2.scale(scale, scale);
        tp.setWidth(w);
        tp.setHeight(h);
        canvas.paintComponent(g2);
        g2.dispose();
        img = new ImgTemplate(tp);
        chapter.add(img);
    }

    private void printGraphDocumentation(Process process, Chapter chapter) throws Exception {
        Section section;
        String tmp;
        if (process.isEmbeddedProcess()) {
            String id = process.getAttribute(WorkAttributeConstant.LOGICAL_ID);
            if (id == null || id.length() == 0)
                id = process.getId().toString();
            tmp = "Subprocess " + id + ": \"" + process.getName().replace('\n', ' ') + "\"";
        }
        else {
            tmp = "Process Description";
        }
        Paragraph sTitle = new Paragraph(tmp, sectionFont);
        sTitle.setSpacingBefore(10);
        section = chapter.addSection(sTitle, 2);
        String summary = process.getDescription();
        if (summary != null && summary.length() > 0)
            printBoldParagraphs(section, summary);
        String detail = process.getAttribute(WorkAttributeConstant.DOCUMENTATION);
        if (detail != null && detail.length() > 0) {
            printHtmlParagraphs(section, getHtmlContent(detail), 0);
        }

        if (!process.getAttributes().isEmpty() && process.isEmbeddedProcess()) {
            printAttributes(section, process.getAttributes(), 2);
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

    private void printHtmlParagraphs(Section section, String content, int parentLevel)
            throws Exception {
        if (content == null || content.length() == 0)
            return;
        Paragraph comb = new Paragraph();
        ElementList list = XMLWorkerHelper.parseToElementList(content, null);
        for (Element element : list) {
            comb.add(element);
        }
        section.add(comb);
        section.add(new Paragraph("\n", normalFont));
    }
}
