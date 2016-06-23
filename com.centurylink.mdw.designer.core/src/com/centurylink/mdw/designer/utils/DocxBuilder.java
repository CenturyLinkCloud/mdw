/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;
import javax.xml.transform.stream.StreamResult;

import org.docx4j.XmlUtils;
import org.docx4j.convert.in.xhtml.XHTMLImporter;
import org.docx4j.convert.out.html.AbstractHtmlExporter;
import org.docx4j.convert.out.html.AbstractHtmlExporter.HtmlSettings;
import org.docx4j.convert.out.html.HtmlExporterNG2;
import org.docx4j.convert.out.pdf.viaXSLFO.PdfSettings;
import org.docx4j.dml.wordprocessingDrawing.Inline;
import org.docx4j.fonts.IdentityPlusMapper;
import org.docx4j.fonts.Mapper;
import org.docx4j.jaxb.Context;
import org.docx4j.model.table.TblFactory;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.AltChunkType;
import org.docx4j.openpackaging.parts.WordprocessingML.BinaryPartAbstractImage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.docx4j.openpackaging.parts.WordprocessingML.NumberingDefinitionsPart;
import org.docx4j.wml.BooleanDefaultTrue;
import org.docx4j.wml.Drawing;
import org.docx4j.wml.HpsMeasure;
import org.docx4j.wml.Numbering;
import org.docx4j.wml.ObjectFactory;
import org.docx4j.wml.P;
import org.docx4j.wml.PPr;
import org.docx4j.wml.PPrBase.NumPr;
import org.docx4j.wml.PPrBase.NumPr.Ilvl;
import org.docx4j.wml.PPrBase.NumPr.NumId;
import org.docx4j.wml.PPrBase.PStyle;
import org.docx4j.wml.PPrBase.Spacing;
import org.docx4j.wml.Pict;
import org.docx4j.wml.R;
import org.docx4j.wml.RPr;
import org.docx4j.wml.Tbl;
import org.docx4j.wml.TblPr;
import org.docx4j.wml.TblWidth;
import org.docx4j.wml.Tc;
import org.docx4j.wml.Text;
import org.docx4j.wml.Tr;

/**
 * DocxBuilder for creating and saving MS Word document format.
 */
public class DocxBuilder {

    private WordprocessingMLPackage wordMLPackage;

    public DocxBuilder(byte[] docBytes) throws Docx4JException {
        wordMLPackage = WordprocessingMLPackage.load(new ByteArrayInputStream(docBytes));
    }

    public DocxBuilder(String html, String filepath) throws Docx4JException {
        WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.createPackage();
        html = html.replaceAll("&nbsp;", "&#160;");
        wordMLPackage.getMainDocumentPart().getContent().addAll(
            XHTMLImporter.convert(new ByteArrayInputStream(html.getBytes()), "file:///" + filepath, wordMLPackage));
    }

    public DocxBuilder() throws InvalidFormatException {
        wordMLPackage = WordprocessingMLPackage.createPackage();
    }

    protected MainDocumentPart getMdp() {
        return wordMLPackage.getMainDocumentPart();
    }

    public P addParagraph(String text) {
        return getMdp().addParagraphOfText(text);
    }

    public P addParagraph(String style, String text) {
        return getMdp().addStyledParagraphOfText(style, text);
    }

    public P createParagraph(String text, int fontSize, boolean bold) {
        ObjectFactory factory = Context.getWmlObjectFactory();
        RPr rprDoc = factory.createRPr();
        if (fontSize != 0) {
            HpsMeasure size = new HpsMeasure();
            size.setVal(BigInteger.valueOf(fontSize*2));
            rprDoc.setSz(size);
        }
        if (bold) {
            BooleanDefaultTrue b = new BooleanDefaultTrue();
            b.setVal(true);
            rprDoc.setB(b);
        }
        P pDoc = getMdp().createParagraphOfText(text);
        R rDoc = (R)pDoc.getContent().get(0);
        rDoc.setRPr(rprDoc);
        return pDoc;
    }

    public P addBoldParagraph(String text) {
        ObjectFactory factory = Context.getWmlObjectFactory();
        RPr rprDoc = factory.createRPr();
        BooleanDefaultTrue b = new BooleanDefaultTrue();
        b.setVal(true);
        rprDoc.setB(b);
        P pDoc = getMdp().addParagraphOfText(text);
        R rDoc = (R)pDoc.getContent().get(0);
        rDoc.setRPr(rprDoc);
        return pDoc;
    }

    public P createBoldParagraph(String style, String text) {
        ObjectFactory factory = Context.getWmlObjectFactory();
        RPr rprDoc = factory.createRPr();
        BooleanDefaultTrue b = new BooleanDefaultTrue();
        b.setVal(true);
        rprDoc.setB(b);
        P pDoc = getMdp().createStyledParagraphOfText(style, text);
        R rDoc = (R)pDoc.getContent().get(0);
        rDoc.setRPr(rprDoc);
        return pDoc;
    }

    public void addBreak() {
        addParagraph("");
    }

    public void addImage(byte[] imageBytes) throws Exception {
        BinaryPartAbstractImage.setDensity(600);
        BinaryPartAbstractImage imagePart = BinaryPartAbstractImage.createImagePart(wordMLPackage, imageBytes);
        Inline inline = imagePart.createImageInline(null, null, 0, 1, false);
        ObjectFactory factory = Context.getWmlObjectFactory();
        P imgP = factory.createP();
        R imgRun = factory.createR();
        imgP.getContent().add(imgRun);
        Drawing drawing = factory.createDrawing();
        imgRun.getContent().add(drawing);
        drawing.getAnchorOrInline().add(inline);
        getMdp().addObject(imgP);
    }

    public void addHtml(String html) throws Docx4JException {
        // String html = detail.replaceAll("&nbsp;", "&#160;");
        // mdp.getContent().addAll(XHTMLImporter.convert(new ByteArrayInputStream(html.getBytes()), null, wordMLPackage));
        getMdp().addAltChunk(AltChunkType.Html, html.getBytes());
    }

    public void addDoc(byte[] docBytes) throws Docx4JException {
         getMdp().addAltChunk(AltChunkType.WordprocessingML, new ByteArrayInputStream(docBytes));
    }

    private Numbering.Num numberingNum;
    private BigInteger numberingNumId;
    public void addNumberedList(List<String> list) throws Exception {
        if (numberingNum == null) {
            numberingNum = getNdp().addAbstractListNumberingDefinition((Numbering.AbstractNum)XmlUtils.unmarshalString(getFragment("numbering")));
            numberingNumId = numberingNum.getNumId();
        }
        else {
            numberingNumId = BigInteger.valueOf(ndp.restart(numberingNumId.longValue(), 0, 1));
        }

        for (String item : list) {
            ObjectFactory factory = Context.getWmlObjectFactory();
            P listP = factory.createP();
            Text t = factory.createText();
            t.setValue(item);
            R listRun = factory.createR();
            listRun.getContent().add(t);
            listP.getContent().add(listRun);
            PPr listPpr = factory.createPPr();
            listP.setPPr(listPpr);
            // create and add <w:numPr>
            NumPr numPr = factory.createPPrBaseNumPr();
            listPpr.setNumPr(numPr);
            // the <w:ilvl> element
            Ilvl ilvlElement = factory.createPPrBaseNumPrIlvl();
            numPr.setIlvl(ilvlElement);
            ilvlElement.setVal(BigInteger.valueOf(0));
            // The <w:numId> element
            NumId numIdElement = factory.createPPrBaseNumPrNumId();
            numPr.setNumId(numIdElement);
            numIdElement.setVal(numberingNumId);
            getMdp().addObject(listP);
        }
    }

    private Numbering.Num bulletNum;
    private BigInteger bulletNumId;
    public void addBulletList(Map<String,Object> items) throws Exception {
        if (bulletNum == null) {
            bulletNum = getNdp().addAbstractListNumberingDefinition((Numbering.AbstractNum)XmlUtils.unmarshalString(getFragment("bulletNumbering")));
            bulletNumId = bulletNum.getNumId();
        }
        else {
            bulletNumId = BigInteger.valueOf(ndp.restart(bulletNumId.longValue(), 0, 1));
        }

        boolean wasTable = false;
        for (String name : items.keySet()) {
            ObjectFactory factory = Context.getWmlObjectFactory();
            P listP = factory.createP();
            Object item = items.get(name);
            PPr listPpr = factory.createPPr();
            listP.setPPr(listPpr);

            Text t = factory.createText();
            R listRun = factory.createR();
            listRun.getContent().add(t);
            listP.getContent().add(listRun);
            if (item instanceof String) {
                t.setValue(name + " = " + item);
                if (wasTable)
                    listPpr.setSpacing(createSpacing(100));
                wasTable = false;
            }
            else if (item instanceof DocxTable) {
                t.setValue(name + ":");
                listPpr.setSpacing(createSpacing(100, 10));
                wasTable = true;
            }
            else if (item instanceof DocxCodebox) {
                t.setValue(((DocxCodebox)item).label + ":");
                try {
                  listRun.getContent().add(createCodeBox((DocxCodebox)item));
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                    t.setValue(((DocxCodebox)item).label + ": ![" + ex + "]");
                }
                wasTable = false;
            }

            listPpr.setNumPr(createBulletListNumPr());
            PStyle pStyle = factory.createPPrBasePStyle();
            pStyle.setVal("ListParagraph");
            listPpr.setPStyle(pStyle);
            getMdp().addObject(listP);

            if (item instanceof DocxTable) {
                try {
                  addTable((DocxTable)item);
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                    t.setValue(name + ": ![" + ex + "]");
                }
            }
        }
    }

    public void addBulletList(List<String> items) throws Exception {
        if (bulletNum == null) {
            bulletNum = getNdp().addAbstractListNumberingDefinition((Numbering.AbstractNum)XmlUtils.unmarshalString(getFragment("bulletNumbering")));
            bulletNumId = bulletNum.getNumId();
        }
        else {
            bulletNumId = BigInteger.valueOf(ndp.restart(bulletNumId.longValue(), 0, 1));
        }

        for (String item : items) {
            ObjectFactory factory = Context.getWmlObjectFactory();
            P listP = factory.createP();
            Text t = factory.createText();
            t.setValue(item);
            R listRun = factory.createR();
            listRun.getContent().add(t);
            listP.getContent().add(listRun);
            PPr listPpr = factory.createPPr();
            listP.setPPr(listPpr);
            listPpr.setNumPr(createBulletListNumPr());
            PStyle pStyle = factory.createPPrBasePStyle();
            pStyle.setVal("ListParagraph");
            listPpr.setPStyle(pStyle);
            getMdp().addObject(listP);
        }
    }

    public NumPr createBulletListNumPr() {
        ObjectFactory factory = Context.getWmlObjectFactory();
        NumPr numPr = factory.createPPrBaseNumPr();
        Ilvl ilvlElement = factory.createPPrBaseNumPrIlvl();
        numPr.setIlvl(ilvlElement);
        ilvlElement.setVal(BigInteger.valueOf(0));
        NumId numIdElement = factory.createPPrBaseNumPrNumId();
        numPr.setNumId(numIdElement);
        numIdElement.setVal(bulletNumId);
        return numPr;
    }

    private NumberingDefinitionsPart ndp;
    private NumberingDefinitionsPart getNdp() throws InvalidFormatException {
        if (ndp == null) {
            ndp = new NumberingDefinitionsPart();
            wordMLPackage.getMainDocumentPart().addTargetPart(ndp);
            ndp.setJaxbElement(Context.getWmlObjectFactory().createNumbering());
        }
        return ndp;
    }

    public Spacing createSpacing(int before, int after) {
        Spacing spacing = new Spacing();
        spacing.setBefore(BigInteger.valueOf(before));
        spacing.setAfter(BigInteger.valueOf(after));
        return spacing;
    }

    public Spacing createSpacing(int before) {
        Spacing spacing = new Spacing();
        spacing.setBefore(BigInteger.valueOf(before));
        return spacing;
    }

    public class DocxTable {
        String[] headers;
        String[][] values;
        int fontSize;
        public void setFontSize(int size) { fontSize = size; }
        int indent;
        public void setIndent(int indent) { this.indent = indent; }

        public DocxTable(String[] headers, String[][] values) {
            this.headers = headers;
            this.values = values;
        }
    }

    public class DocxCodebox {
        String label;
        String value;

        public DocxCodebox(String label, String value) {
            this.label = label;
            this.value = value;
        }
    }

    public Pict createCodeBox(DocxCodebox docxCodebox) throws JAXBException, IOException {
        return createCodeBox(docxCodebox.value);
    }

    public Pict createCodeBox(String text) throws JAXBException, IOException {
        StringBuffer content = new StringBuffer();
        String[] lines;
        if (text.indexOf("\r\n") >= 0)
            lines = text.split("\r\n");
        else
            lines = text.split("\n");
        for (String line : lines) {
            String codeLineTemplate = getFragment("codeLineTemplate");
            int cIdx = codeLineTemplate.indexOf("${content}");
            String insLine = codeLineTemplate.substring(0, cIdx) + line.replaceAll("<", "&lt;").replaceAll("&", "&amp;") + codeLineTemplate.substring(cIdx + 10);
            content.append(insLine);
        }

        String codeBoxTemplate = getFragment("codeBoxTemplate");
        int cIdx = codeBoxTemplate.indexOf("${content}");
        String template = codeBoxTemplate.substring(0, cIdx) + content + codeBoxTemplate.substring(cIdx + 10);

        HashMap<String,String> mappings = new HashMap<String,String>();
        mappings.put("fontSize", "16");
        return (Pict)XmlUtils.unmarshallFromTemplate(template, mappings);
    }

    public Tbl addTable(DocxTable docxTable) {
        return addTable(docxTable.headers, docxTable.values, docxTable.fontSize, docxTable.indent);
    }

    public Tbl addTable(String[] headers, String[][] values) {
        return addTable(headers, values, 0, 0);
    }

    public Tbl addTable(String[] headers, String[][] values, int fontSize, int indent) {
        ObjectFactory factory = Context.getWmlObjectFactory();
        int writableWidthTwips = wordMLPackage.getDocumentModel().getSections().get(0).getPageDimensions().getWritableWidthTwips();
        int cols = headers.length;
        int cellWidthTwips = new Double(Math.floor((writableWidthTwips/cols))).intValue();
        Tbl tbl = TblFactory.createTable(0, cols, cellWidthTwips);
        Tr thead = factory.createTr();
        for (int i = 0; i < headers.length; i++) {
            Tc tc = factory.createTc();
            tc.getContent().add(createParagraph(headers[i], fontSize, true));
            thead.getContent().add(tc);
        }
        tbl.getContent().add(0, thead);
        for (int i = 0; i < values[0].length; i++) {
            Tr tr = factory.createTr();
            for (int j = 0; j < headers.length; j++) {
                Tc tc = factory.createTc();
                tc.getContent().add(createParagraph(values[j][i], fontSize, false));
                tr.getContent().add(tc);
            }
            tbl.getContent().add(i + 1, tr);
        }
        getMdp().addObject(tbl);
        if (indent != 0) {
            TblPr tblPr = tbl.getTblPr();
            TblWidth tblIndent = factory.createTblWidth();
            tblIndent.setType("dxa");
            tblIndent.setW(BigInteger.valueOf(indent));
            tblPr.setTblInd(tblIndent);
        }
        return tbl;
    }

    public String toHtml() throws Exception {
        String top = "<com.centurylink.mdw.doc>";
        String tail = "</com.centurylink.mdw.doc>";

        AbstractHtmlExporter exporter = new HtmlExporterNG2();

        HtmlSettings htmlSettings = new HtmlSettings();

        // TODO image path
        htmlSettings.setUserBodyTop(top);
        htmlSettings.setUserBodyTail(tail);

        OutputStream os = new ByteArrayOutputStream();
        StreamResult result = new StreamResult(os);

        exporter.html(wordMLPackage, result, htmlSettings);
        String html = os.toString();

        int start = html.indexOf(top) + top.length();
        int stop = html.indexOf(tail);

        return "<html>" + html.substring(start, stop) + "</html>";
    }

    public byte[] toPdf() throws Exception {
        Mapper fontMapper = new IdentityPlusMapper();
        wordMLPackage.setFontMapper(fontMapper);

        org.docx4j.convert.out.pdf.PdfConversion c
//            = new org.docx4j.convert.out.pdf.viaHTML.Conversion(wordMLPackage);
            = new org.docx4j.convert.out.pdf.viaXSLFO.Conversion(wordMLPackage);
//              = new org.docx4j.convert.out.pdf.viaIText.Conversion(wordMLPackage);

        // PdfConversion writes to an output stream
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        c.output(os, new PdfSettings());
        return os.toByteArray();
    }

    public String toXml() {
        return XmlUtils.marshaltoString(getMdp().getJaxbElement(), true, true);
    }

    public void save(File file) throws Docx4JException {
        wordMLPackage.save(file);
    }

    private Map<String,String> fragments = new HashMap<String,String>();
    private String getFragment(String name) throws IOException {
        String frag = fragments.get(name);
        if (frag == null) {
            frag = readFragment(name + ".docxfragment");
            fragments.put(name, frag);
        }
        return frag;
    }
    private String readFragment(String filename) throws IOException {
        String path = "META-INF/mdw/" + filename;
        InputStreamReader reader = null;
        String frag = "";
        try {
            reader = new InputStreamReader(DocxBuilder.class.getClassLoader().getResourceAsStream(path), "UTF-8");
            int data = reader.read();
            while (data != -1) {
                frag += (char)data;
                data = reader.read();
            }
        }
        finally {
            if (reader != null)
                reader.close();
        }
        return frag;
    }
}
