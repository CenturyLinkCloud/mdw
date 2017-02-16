/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.project.model;

import java.io.File;
import java.io.IOException;
import java.util.jar.Manifest;

import aQute.lib.osgi.Analyzer;
import aQute.lib.osgi.Jar;

public class OsgiManifestDescriptor {
    private String exportPackage = "";

    public String getExportPackage() {
        return exportPackage;
    }

    public void setExportPackage(String s) {
        this.exportPackage = s;
    }

    private String privatePackage = "";

    public String getPrivatePackage() {
        return privatePackage;
    }

    public void setPrivatePackage(String s) {
        this.privatePackage = s;
    }

    private String importPackage = "";

    public String getImportPackage() {
        return importPackage;
    }

    public void setImportPackage(String s) {
        this.importPackage = s;
    }

    private String dynamicImportPackage = "";

    public String getDynamicImportPackage() {
        return dynamicImportPackage;
    }

    public void setDynamicImportPackage(String s) {
        this.dynamicImportPackage = s;
    }

    private String bundleActivator = "";

    public String getBundleActivator() {
        return bundleActivator;
    }

    public void setBundleActivator(String s) {
        this.bundleActivator = s;
    }

    private String bundleClasspath = "";

    public String getBundleClasspath() {
        return bundleClasspath;
    }

    public void setBundleClasspath(String s) {
        this.bundleClasspath = s;
    }

    private String webContextPath = "";

    public String getWebContextPath() {
        return webContextPath;
    }

    public void setWebContextPath(String s) {
        this.webContextPath = s;
    }

    private String symbolicName = "";

    public String getSymbolicName() {
        return symbolicName;
    }

    public void setSymbolicName(String s) {
        this.symbolicName = s;
    }

    private File outputDir;

    public File getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(File outputDir) {
        this.outputDir = outputDir;
    }

    public OsgiManifestDescriptor() {
    }

    public OsgiManifestDescriptor(File outputDir) {
        this.outputDir = outputDir;
    }

    public Manifest createManifest(String version, String mdwVersion) throws Exception {
        Analyzer analyzer = null;
        try {
            analyzer = new Analyzer();
            if (!outputDir.exists())
                throw new IOException("Output directory not found: " + outputDir);
            Jar outDir = new Jar(outputDir);
            analyzer.setJar(outDir);

            // (if META-INF/MANIFEST.MF already exists in outDir due to a Maven
            // build,
            // then it's contents will be merged in)

            String normVer = version.replace("-", ".");
            analyzer.setProperty("Bundle-SymbolicName", getSymbolicName());
            analyzer.setProperty("Bundle-Version", normVer);
            if (exportPackage != null && !exportPackage.isEmpty()) {
                String[] exports = exportPackage.split(",");
                exportPackage = "";
                for (int i = 0; i < exports.length; i++) {
                    String export = exports[i];
                    exportPackage += export;
                    if (!export.startsWith("!") && export.indexOf(";version=") == -1) {
                        exportPackage += ";version=" + normVer;
                    }
                    if (i < exports.length - 1)
                        exportPackage += ",";
                }
                analyzer.setProperty("Export-Package", exportPackage.replaceAll("\\s+", ""));
            }
            analyzer.setProperty("Import-Package", importPackage == null || importPackage.isEmpty()
                    ? "*" : importPackage.replaceAll("\\s+", ""));
            if (privatePackage != null && !privatePackage.isEmpty())
                analyzer.setProperty("Private-Package", privatePackage.replaceAll("\\s+", ""));
            if (dynamicImportPackage != null && !dynamicImportPackage.isEmpty())
                analyzer.setProperty("DynamicImport-Package",
                        dynamicImportPackage.replaceAll("\\s+", ""));
            if (bundleActivator != null && !bundleActivator.isEmpty())
                analyzer.setProperty("Bundle-Activator", bundleActivator.replaceAll("\\s+", ""));
            if (bundleClasspath != null && !bundleClasspath.isEmpty() && mdwVersion != null)
                analyzer.setProperty("Bundle-ClassPath", bundleClasspath.replaceAll("\\s+", "")
                        .replaceAll("\\$\\{mdw\\.version\\}", mdwVersion));
            if (webContextPath != null && !webContextPath.isEmpty())
                analyzer.setProperty("Web-ContextPath", webContextPath.replaceAll("\\s+", ""));

            analyzer.setProperty("nodefaultversion", "false");

            return analyzer.calcManifest();
        }
        finally {
            if (analyzer != null)
                analyzer.close();
        }
    }

}
