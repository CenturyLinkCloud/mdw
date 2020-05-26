package com.centurylink.mdw.model.asset;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * Maps known asset extension to mime type.
 */
public class ContentTypes {

    public static final String DEFAULT = "application/octet-stream";
    static final Map<String,String> contentTypes;

    static {
        contentTypes = new HashMap<>();
        contentTypes.put("spring", "text/xml");
        contentTypes.put("camel", "text/xml");
        contentTypes.put("css", "text/css");
        contentTypes.put("csv", "application/CSV");
        contentTypes.put("xls", "application/vnd.ms-excel");
        contentTypes.put("xlsx", "application/vnd.ms-excel");
        contentTypes.put("docx", "application/vnd.ms-word");
        contentTypes.put("pagelet", "application/json");
        contentTypes.put("html", "text/html");
        contentTypes.put("gif", "image/gif");
        contentTypes.put("jpg", "image/jpeg");
        contentTypes.put("png", "image/png");
        contentTypes.put("svg", "image/svg+xml");
        contentTypes.put("js", "application/javascript");
        contentTypes.put("jsx", "application/javascript");
        contentTypes.put("json", "application/json");
        contentTypes.put("wsdl", "text/xml");
        contentTypes.put("xml", "text/xml");
        contentTypes.put("xsd", "text/xml");
        contentTypes.put("xsl", "text/xml");
        contentTypes.put("yaml", "text/yaml");
        contentTypes.put("yml", "text/yaml");
        contentTypes.put("md", "text/markdown");
    }

    public static String getContentType(String extension) {
        if (extension == null)
            return DEFAULT;
        String contentType = contentTypes.get(extension);
        if (contentType == null) {
            if (isBinary(extension)) {
                contentType = DEFAULT;
            }
            else {
                contentType = "text/plain";
            }
        }
        return contentType;
    }

    public static String getContentType(File file) throws IOException {
        int lastDot = file.getName().lastIndexOf('.');
        if (lastDot > 0 && lastDot < file.getName().length() - 2) {
            String ext = file.getName().substring(lastDot + 1);
            return getContentType(ext);
        }
        String contentType = Files.probeContentType(file.toPath());
        return contentType == null ? DEFAULT : contentType;
    }

    public static boolean isBinary(String extension) {
        return extension.equals("gif")
                || extension.equals("jpg")
                || extension.equals("png")
                || extension.equals("xls")
                || extension.equals("xlsx")
                || extension.equals("docx")
                || extension.equals("jar")
                || extension.equals("class")
                || extension.equals("zip")
                || extension.equals("eot")
                || extension.equals("ttf")
                || extension.equals("woff")
                || extension.equals("woff2");
    }

    public static boolean isImage(String extension) {
        return extension.equals("gif")
                || extension.equals("jpg")
                || extension.equals("png")
                || extension.equals("svg");
    }

    public static boolean isExcludedFromMemoryCache(String extension) {
        return "jar".equals(extension); // jar files not loaded into memory
    }
}
