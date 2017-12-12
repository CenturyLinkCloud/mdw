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
package com.centurylink.mdw.model.asset;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.attribute.Attribute;
import com.centurylink.mdw.util.MiniCrypter;
import com.centurylink.mdw.util.file.FileHelper;

public class Asset implements Serializable, Comparable<Asset>, Jsonable {

    public static final String MILESTONE_REPORT = "MILESTONE_REPORT";
    public static final String GROOVY = "GROOVY";
    public static final String FORM = "FORM";
    public static final String USECASE = "USECASE";
    public static final String FACELET = "FACELET";
    public static final String JSP = "JSP";
    public static final String PAGELET = "PAGELET";
    public static final String VELOCITY = "VELOCITY";
    public static final String IMAGE_JPEG = "IMAGE_JPEG";
    public static final String IMAGE_GIF = "IMAGE_GIF";
    public static final String IMAGE_PNG = "IMAGE_PNG";
    public static final String IMAGE_SVG = "IMAGE_SVG";
    public static final String CSS = "CSS";
    public static final String JAVASCRIPT = "JAVASCRIPT";
    public static final String JSX = "JSX";
    public static final String WEBSCRIPT = "WEBSCRIPT";
    public static final String CONFIG = "CONFIG";
    public static final String DROOLS = "DROOLS";
    public static final String GUIDED = "GUIDED";
    public static final String EXCEL = "EXCEL";
    public static final String XLSX = "XLSX";
    public static final String EXCEL_2007 = "EXCEL_2007";
    public static final String MS_WORD = "MS_WORD";
    public static final String CSV = "CSV";
    public static final String BIRT = "BIRT";
    public static final String HTML = "HTML";
    public static final String TESTDATA = "TESTDATA";
    public static final String JAR = "JAR";
    public static final String JAVA = "JAVA";
    public static final String TEXT = "TEXT";
    public static final String PROCESS = "PROCESS";
    public static final String ATTRIBUTE_OVERFLOW = "ATTRIBUTE_OVERFLOW";
    public static final String XML = "XML";
    public static final String XSD = "XSD";
    public static final String WSDL = "WSDL";
    public static final String XSL = "XSL";
    public static final String SPRING = "SPRING";
    public static final String CAMEL_ROUTE = "CAMEL_ROUTE";
    public static final String JSON = "JSON";
    public static final String POSTMAN = "POSTMAN";
    public static final String TEST = "TEST";
    public static final String FEATURE = "FEATURE";
    public static final String YAML = "YAML";
    public static final String TASK = "TASK";
    public static final String MARKDOWN = "MARKDOWN";
    public static final String GENERAL_ALPHA = "GENERAL_ALPHA";
    public static final String GENERAL_BINARY = "GENERAL_BINARY";

    public static final String[] FORMATS = {
        MILESTONE_REPORT,
        GROOVY,
        FORM,
        USECASE,
        FACELET,
        PAGELET,
        JSP,
        VELOCITY,
        IMAGE_JPEG,
        IMAGE_GIF,
        IMAGE_PNG,
        IMAGE_SVG,
        CSS,
        JAVASCRIPT,
        JSX,
        WEBSCRIPT,
        CONFIG, // used internally for package config
        DROOLS,
        GUIDED,
        EXCEL,
        EXCEL_2007,
        MS_WORD,
        CSV,
        BIRT,
        HTML,
        TESTDATA,
        JAR,
        JAVA,
        TEXT,
        PROCESS,
        ATTRIBUTE_OVERFLOW,
        XML,
        XSD,
        WSDL,
        XSL,
        SPRING,
        CAMEL_ROUTE,
        JSON,
        TEST,
        POSTMAN,
        FEATURE,
        YAML,
        TASK,
        GENERAL_ALPHA,
        GENERAL_BINARY
    };

    private static Map<String,String> contentTypes;

    private Long id;
    private String name;
    private byte[] content;
    private String language;
    private String createUser;
    private String modifyingUser;
    private Date createDate, modifyDate, loadDate;
    private int version;
    private String comment;
    private Asset nextVersion;          // for designer use
    private Asset prevVersion;          // for designer use
    private List<Attribute> attributes;
    private Long ownerId;
    private String ownerType;
    private String packageName;
    private String revisionComment;
    private boolean raw;
    private File rawFile;

    public Asset() {

    }

    public Asset(Asset cloneFrom) {
        this.name = cloneFrom.name;
        this.content = cloneFrom.content;
        this.language = cloneFrom.language;
        this.createUser = cloneFrom.createUser;
        this.createDate = cloneFrom.createDate;
        this.modifyingUser = cloneFrom.modifyingUser;
        this.modifyDate = cloneFrom.modifyDate;
        this.version = cloneFrom.version;
        this.comment = cloneFrom.comment;
        this.nextVersion = cloneFrom.nextVersion;
        this.prevVersion = cloneFrom.prevVersion;
        if (cloneFrom.attributes != null) {
            this.attributes = new ArrayList<Attribute>();
            for (Attribute attr : cloneFrom.attributes) {
                this.attributes.add(new Attribute(attr.getAttributeName(), attr.getAttributeValue()));
            }
        }
        this.loadDate = cloneFrom.loadDate;
        this.packageName = cloneFrom.packageName;
        this.revisionComment = cloneFrom.revisionComment;
        this.raw = cloneFrom.raw;
        this.rawFile = cloneFrom.rawFile;
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getStringContent() {
        if (content == null)
            return null;
        return new String(content);
    }
    public void setStringContent(String string) {
        this.content = string == null ? null : string.getBytes();
    }

    public boolean isRaw() {
        return raw;
    }
    public void setRaw(boolean raw) {
        this.raw = raw;
    }

    public byte[] getRawContent() {
        return content;
    }
    public void setRawContent(byte[] content) {
        this.content = content;
    }

    public File getRawFile() {
        return rawFile;
    }

    public void setRawFile(File rawFile) {
        this.rawFile = rawFile;
    }

    public String getLanguage() {
        return language;
    }
    public void setLanguage(String language) {
        this.language = language;
    }
    public String getComment() {
        return comment;
    }
    public void setComment(String comment) {
        this.comment = comment;
    }
    public String getLabel() {
        return getName() + (getVersion() == 0 ? "" : " v" + getVersionString());
    }
    public String getCreateUser() {
        return createUser;
    }
    public void setCreateUser(String user) {
        this.createUser = user;
    }
    public String getModifyingUser() {
        return modifyingUser;
    }
    public void setModifyingUser(String modifyingUser) {
        this.modifyingUser = modifyingUser;
    }
    public Date getModifyDate() {
        return modifyDate;
    }
    public void setModifyDate(Date modifyDate) {
        this.modifyDate = modifyDate;
    }
    public Date getCreateDate() {
        return createDate;
    }
    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }
    public int getVersion() {
        return version;
    }
    public void setVersion(int version) {
        this.version = version;
    }

    public boolean isLoaded() {
        return language!=null && content!=null;
    }

    /**
     * based on server time
     */
    public Date getLoadDate() {
        return this.loadDate;
    }

    public void setLoadDate(Date d) {
        this.loadDate = d;
    }

    public List<Attribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<Attribute> attributes) {
        this.attributes = attributes;
    }

    public String getAttribute(String attrname) {
        return Attribute.findAttribute(attributes, attrname);
    }

    public void setAttribute(String name, String value) {
        if (attributes == null)
          attributes = new ArrayList<Attribute>();
        Attribute.setAttribute(attributes, name, value);
    }

    /**
     * Takes care of multiples.
     */
    public void removeAttribute(String name) {
        if (getAttributes() != null) {
            List<Attribute> toRemove = new ArrayList<Attribute>();
            for (Attribute attr : getAttributes()) {
                if (attr.getAttributeName().equals(name))
                    toRemove.add(attr);
            }
            for (Attribute attr : toRemove)
                getAttributes().remove(attr);
        }
    }

    public static byte[] decode(String inputString) {
        return MiniCrypter.decodeAlpha(inputString);
    }

    public static String encode(byte[] inputBytes) {
        return MiniCrypter.encodeAlpha(inputBytes);
    }

    public byte[] getContent() {
        if (content == null)
            return null;
        else if (isBinary()) {
            if (isRaw())
                return content;
            else
                return decode(getStringContent());
        }
        else
            return content;
    }

    public boolean isEmpty() {
        return content == null || content.length == 0;
    }

    public String getContentType() {
        if (contentTypes == null) {
            contentTypes = new HashMap<String,String>();
            contentTypes.put(BIRT, "text/xml");
            contentTypes.put(SPRING, "text/xml");
            contentTypes.put(CAMEL_ROUTE, "text/xml");
            contentTypes.put(CSS, "text/css");
            contentTypes.put(CSV, "application/CSV");
            contentTypes.put(EXCEL, "application/vnd.ms-excel");
            contentTypes.put(XLSX, "application/vnd.ms-excel");
            contentTypes.put(EXCEL_2007, "application/vnd.ms-excel");
            contentTypes.put(MS_WORD, "application/vnd.ms-word");
            contentTypes.put(FACELET, "text/xml");
            contentTypes.put(PAGELET, "text/xml");
            contentTypes.put(FORM, "text/html");
            contentTypes.put(HTML, "text/html");
            contentTypes.put(IMAGE_GIF, "image/gif");
            contentTypes.put(IMAGE_JPEG, "image/jpeg");
            contentTypes.put(IMAGE_PNG, "image/png");
            contentTypes.put(IMAGE_SVG, "image/svg+xml");
            contentTypes.put(JAVASCRIPT, "application/javascript");
            contentTypes.put(JSX, "application/javascript");
            contentTypes.put(WEBSCRIPT, "application/javascript");
            contentTypes.put(JSON, "application/json");
            contentTypes.put(WSDL, "text/xml");
            contentTypes.put(XML, "text/xml");
            contentTypes.put(XSD, "text/xml");
            contentTypes.put(XSL, "text/xml");
        }

        String type = contentTypes.get(language);
        if (type == null) {
            if (isBinary())
                return "application/octet-stream";
            else
                return "text/plain";
        }

        return type;
    }

    public boolean isBinary() {
        if (language == null)
            return false;
        return isBinary(language);
    }

    public static boolean isBinary(String format) {
        return format.startsWith("IMAGE")
                || format.equals(EXCEL)
                || format.equals(XLSX)
                || format.equals(EXCEL_2007)
                || format.equals(MS_WORD)
                || format.equals(JAR)
                || format.equals(GENERAL_BINARY);
    }

    public boolean isGeneralBinary() {
        return language != null && language.equals(GENERAL_BINARY);
    }

    public boolean isExcel() {
        return language != null && language.equals(EXCEL);
    }

    public boolean isExcel2007() {
        return language != null && language.equals(EXCEL_2007);
    }

    public boolean isMsWord() {
        return language != null && language.equals(MS_WORD);
    }

    public boolean isImage() {
        return language != null && language.startsWith("IMAGE");
    }

    public boolean isJar() {
        return language != null && language.equals(JAR);
    }

    /**
     * @return Returns the version.
     */
    public String getVersionString() {
        return formatVersion(version);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Asset))
            return false;

        return ((Asset)obj).getId().equals(getId());
    }

    public String getDescription() {
      return getLanguage() + ": " + getName() + " v" + getVersion();
    }

    @Override
    public String toString() {
        return getLabel();
    }

    public int compareTo(Asset other) {
        if (other == null)
            return 1;
        if (this.getName().equals(other.getName()))
            return other.version - this.version;
        return this.getName().compareToIgnoreCase(other.getName());
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerType() {
        return ownerType;
    }

    public void setOwnerType(String ownerType) {
        this.ownerType = ownerType;
    }

    /**
     * Either a specific version number can be specified, or a Smart Version can be specified which designates an allowable range.
     *
     * Smart Version Ranges
     * This is similar to the OSGi version spec.  There are four supported syntaxes:
     *    - A specific version -- such as 1.2 -- can be specified.
     *    - Zero can be specified to always use the latest asset version.
     *    - A 'half-open' range -- such as [1.2,2) -- designates an inclusive lower limit and an exclusive upper limit,
     *      denoting version 1.2 and any version after this, up to, but not including, version 2.0.
     *    - An 'unbounded' version range -- such as [1.2 -- which denotes version 1.2 and all later versions.
     */
    public boolean meetsVersionSpec(String versionSpec) {
        if (versionSpec.startsWith("[")) {
            int comma = versionSpec.indexOf(',');
            if (comma == -1) {
                String min = versionSpec.substring(1);
                return parseVersionSpec(min) <= getVersion();
            }
            else {
                String min = versionSpec.substring(1, comma);
                String maxExcl = versionSpec.substring(comma + 1, versionSpec.lastIndexOf(')'));
                return parseVersionSpec(min) <= getVersion() && parseVersionSpec(maxExcl) > getVersion();
            }
        }
        else {
            return parseVersionSpec(versionSpec) == getVersion() || versionSpec.equals("0");
        }
    }

    public String getPackageName() {
        return packageName;
    }
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
    public String getQualifiedName() {
        if (getPackageName() == null || getPackageName().isEmpty())
            return getName();
        else
            return getPackageName() + "/" + getName();
    }

    public String getRevisionComment() {
        return revisionComment;
    }
    public void setRevisionComment(String revComment) {
        this.revisionComment = revComment;
    }

    public static int parseVersion(String versionString) throws NumberFormatException {
        if (versionString == null)
            return 0;
        if (versionString.startsWith("v"))
            versionString = versionString.substring(1);
        int dot = versionString.indexOf('.');
        int major, minor;
        if (dot > 0) {
            major = Integer.parseInt(versionString.substring(0, dot));
            minor = Integer.parseInt(versionString.substring(dot + 1));
        }
        else {
            major = 0;
            minor = Integer.parseInt(versionString);
        }
        return major * 1000 + minor;
    }

    // single digit without decimal means a major version not minor
    public static int parseVersionSpec(String versionString) throws NumberFormatException {
        if (versionString == null)
            return 0;
        int dot = versionString.indexOf('.');
        int major, minor;
        if (dot > 0) {
            major = Integer.parseInt(versionString.substring(0, dot));
            minor = Integer.parseInt(versionString.substring(dot + 1));
        }
        else {
            major = Integer.parseInt(versionString);
            minor = 0;
        }
        return major * 1000 + minor;
    }

    public static String formatVersion(int version) {
        if (version == 0)
            return "0";
        else
            return version/1000 + "." + version%1000;
    }

    public static boolean excludedFromMemoryCache(String assetName) {
        return assetName.endsWith(".jar"); // jar files not loaded into memory
    }

    private static Map<String,String> languageToExtension;
    /**
     * This is one-way (not unique)
     */
    public static Map<String,String> getLanguageToExtension() {
        if (languageToExtension == null) {
            languageToExtension = new HashMap<String,String>();
            // TODO map should be driven from properties
            languageToExtension.put("Groovy", ".groovy");
            languageToExtension.put("GROOVY", ".groovy");
            languageToExtension.put("JavaScript", ".js");
            languageToExtension.put("JAVASCRIPT", ".js");
            languageToExtension.put("JSX", ".jsx");
            languageToExtension.put("WEBSCRIPT", ".js");
            languageToExtension.put("MagicBox", ".mb");
            languageToExtension.put("MAGICBOX", ".mb");
            languageToExtension.put("GPath", ".groovy");
            languageToExtension.put("GPATH", ".groovy");
            languageToExtension.put("xslt", ".xsl");
            languageToExtension.put("XSLT", ".xsl");
            languageToExtension.put("facelet", ".xhtml");
            languageToExtension.put("Facelet", ".xhtml");
            languageToExtension.put("FACELET", ".xhtml");
            languageToExtension.put("JSP", ".jsp");
            languageToExtension.put("PAGELET", ".xml");
            languageToExtension.put("Pagelet", ".xml");
            languageToExtension.put("Drools", ".drl");
            languageToExtension.put("DROOLS", ".drl");
            languageToExtension.put("Guided", ".brl");
            languageToExtension.put("GUIDED", ".brl");
            languageToExtension.put("CSS", ".css");
            languageToExtension.put("VELOCITY", ".vsl");
            languageToExtension.put("Velocity", ".vsl");
            languageToExtension.put("BIRT", ".rptdesign");
            languageToExtension.put("EXCEL", ".xls");
            languageToExtension.put("EXCEL_2007", ".xlsx");
            languageToExtension.put("MS_WORD", ".docx");
            languageToExtension.put("MS Word", ".docx");
            languageToExtension.put("HTML", ".html");
            languageToExtension.put("FORM", ".xml");
            languageToExtension.put("Java", ".java");
            languageToExtension.put("CONFIG", ".xml");
            languageToExtension.put("USECASE", ".doc");
            languageToExtension.put("TESTDATA", ".tst");
            languageToExtension.put("JAR", ".jar");
            languageToExtension.put("TEXT", ".txt");
            languageToExtension.put("XML", ".xml");
            languageToExtension.put("WSDL", ".wsdl");
            languageToExtension.put("XSL", ".xsl");
            languageToExtension.put("XSD", ".xsd");
            languageToExtension.put("CSV", ".csv");
            languageToExtension.put("SPRING", ".spring");
            languageToExtension.put("CAMEL_ROUTE", ".camel");
            languageToExtension.put("PROCESS", ".proc");
            languageToExtension.put("TEST", ".test");
            languageToExtension.put("POSTMAN", ".postman");
            languageToExtension.put("FEATURE", ".feature");
            languageToExtension.put("YAML", ".yaml");
            languageToExtension.put("JSON", ".json");
            languageToExtension.put("TASK", ".task");
            languageToExtension.put("MARKDOWN", ".md");
            languageToExtension.put("Markdown", ".md");
        }
        return languageToExtension;
    }

    public String getFileExtension() {
        return getFileExtension(language);
    }

    public static String getFileExtension(String language) {
        String ext = getLanguageToExtension().get(language);
        if (ext == null)
            ext = "." + language.toLowerCase();
        return ext;
    }

    private static Map<String,String> extensionToLanguage;
    private static Map<String,String> getExtensionToLanguage() {
        if (extensionToLanguage == null) {
            extensionToLanguage = new HashMap<String,String>();
            // TODO map should be driven from properties
            extensionToLanguage.put(".groovy", GROOVY);
            extensionToLanguage.put(".js", JAVASCRIPT);
            extensionToLanguage.put(".jsx", JSX);
            extensionToLanguage.put(".xhtml", FACELET);
            extensionToLanguage.put(".jsp", JSP);
            extensionToLanguage.put(".drl", DROOLS);
            extensionToLanguage.put(".brl", GUIDED);
            extensionToLanguage.put(".css", CSS);
            extensionToLanguage.put(".vsl", VELOCITY);
            extensionToLanguage.put(".rptdesign", BIRT);
            extensionToLanguage.put(".xls", EXCEL);
            extensionToLanguage.put(".xlsx", EXCEL_2007);
            extensionToLanguage.put(".docx", MS_WORD);
            extensionToLanguage.put(".html", HTML);
            extensionToLanguage.put(".java", JAVA);
            extensionToLanguage.put(".tst", TESTDATA);
            extensionToLanguage.put(".jar", JAR);
            extensionToLanguage.put(".txt", TEXT);
            extensionToLanguage.put(".map", TEXT);
            extensionToLanguage.put(".xml", XML);
            extensionToLanguage.put(".wsdl", WSDL);
            extensionToLanguage.put(".xsl", XSL);
            extensionToLanguage.put(".xsd", XSD);
            extensionToLanguage.put(".csv", CSV);
            extensionToLanguage.put(".jpg", IMAGE_JPEG);
            extensionToLanguage.put(".gif", IMAGE_GIF);
            extensionToLanguage.put(".png", IMAGE_PNG);
            extensionToLanguage.put(".svg", IMAGE_SVG);
            extensionToLanguage.put(".proc", PROCESS);
            extensionToLanguage.put(".camel", CAMEL_ROUTE);
            extensionToLanguage.put(".test", TEST);
            extensionToLanguage.put(".postman", POSTMAN);
            extensionToLanguage.put(".feature", FEATURE);
            extensionToLanguage.put(".yaml", YAML);
            extensionToLanguage.put(".json", JSON);
            extensionToLanguage.put(".task", TASK);
            extensionToLanguage.put(".md", MARKDOWN);
        }
        return extensionToLanguage;
    }

    /**
     * Takes into account special rules due to multiple languages per extension.
     */
    public static String getFormat(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1)
            return null;
        return getLanguage(fileName.substring(lastDot));
    }

    public static String getLanguage(String fileExtension) {
        String lang = getExtensionToLanguage().get(fileExtension);
        if (lang == null)
            lang = fileExtension.toUpperCase().substring(1);
        return lang;
    }

    /**
     * Only for VCS Assets.
     */
    public Asset(JSONObject json) throws JSONException {
        if (json.has("name")) {
            this.name = json.getString("name");
            this.language = getFormat(name);
        }
        if (json.has("version"))
            this.version = parseVersion(json.getString("version"));
        if (json.has("content"))
            this.setStringContent(json.getString("content"));
    }

    /**
     * JSON name = getName(), so not included.
     */
    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        json.put("version", getVersionString());
        if (isBinary()) {
            if (excludedFromMemoryCache(getName())) {
                try {
                    if (getRawFile() != null)
                      setRawContent(FileHelper.read(getRawFile()));
                }
                catch (IOException ex) {
                    throw new JSONException(ex);
                }
            }
            if (getContent() != null)
                json.put("content", encode(getContent()));
        }
        else {
            if (getStringContent() != null)
                json.put("content", getStringContent());
        }

        return json;
    }

    public String getJsonName() {
        return getName();
    }

    /**
     * Methods for Groovy access (eg: autotest cases).
     */
    public boolean exists() {
        return getRawFile() != null && getRawFile().exists();
    }
    public File file() {
        return getRawFile();
    }
    public String getText() throws IOException {
        return text();
    }
    public String text() throws IOException {
        return getStringContent();
    }
}
