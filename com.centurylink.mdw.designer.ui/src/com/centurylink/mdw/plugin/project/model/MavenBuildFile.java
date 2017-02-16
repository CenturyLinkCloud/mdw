/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.project.model;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.concurrent.Executors;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import aQute.bnd.maven.support.Maven;
import aQute.bnd.maven.support.ProjectPom;

import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.PluginUtil;

public class MavenBuildFile implements OsgiBuildFile {
    private IProject project;

    public IProject getProject() {
        return project;
    }

    private OsgiManifestDescriptor manifestDescriptor;

    public MavenBuildFile(IProject project) {
        this.project = project;
    }

    private String groupId;

    public String getGroupId() {
        return groupId;
    }

    private String artifactId;

    public String getArtifactId() {
        return artifactId;
    }

    private String version;

    public String getVersion() {
        return version;
    }

    private String mdwVersion;

    public String getMdwVersion() {
        return mdwVersion;
    }

    private String finalName;

    public String getArtifactName() {
        if (finalName == null)
            finalName = artifactId + "-" + version;

        return finalName.replace("${project.artifactId}", artifactId).replace("${project.version}",
                version);
    }

    private String sourceDirectory = "src/main/java";

    public String getSourceDirectory() {
        return sourceDirectory;
    }

    private String outputDirectory = "target/classes";

    public String getOutputDirectory() {
        return outputDirectory;
    }

    private String appGeneratedDir = "../MDWFramework/deploy/app/generated/osgi";

    public String getArtifactGenDir() {
        return appGeneratedDir;
    }

    public boolean exists() {
        return project.getFile("pom.xml").exists();
    }

    public long lastModified() {
        return project.getFile("pom.xml").getLocation().toFile().lastModified();
    }

    public OsgiManifestDescriptor parse() throws Exception {
        File pomFile = new File(project.getFile("pom.xml").getLocation().toString());
        if (project.getName().startsWith("MDW")) {
            // since bnd assumes parent pom.xml is located in parent directory
            File parentDir = project.getProject().getLocation().toFile().getParentFile();
            File frameworkPom = new File(parentDir.toString() + "/MDWFramework/pom.xml");
            if (frameworkPom.exists()) {
                File destPom = new File(parentDir + "/pom.xml");
                if (!destPom.exists() || (destPom.lastModified() < frameworkPom.lastModified()))
                    PluginUtil.copyFile(frameworkPom, destPom);
            }
        }
        if (!project.getProject().getFolder(outputDirectory).exists())
            outputDirectory = "build/classes";

        try {
            Maven maven = new Maven(Executors.newSingleThreadExecutor());
            ProjectPom pom = maven.createProjectModel(pomFile);
            groupId = pom.getGroupId();
            artifactId = pom.getArtifactId();
            version = pom.getVersion();
            if (pom.getProperties().get("mdw.version") != null)
                mdwVersion = pom.getProperty("mdw.version");
            if (project.getName().startsWith("MDW"))
                appGeneratedDir = pom.getProperty("app.generated.dir");
        }
        catch (Exception ex) {
            PluginMessages.log(ex);
        }

        // values not accessible from BND parser
        MavenBuildFile saxParser = new MavenBuildFile(project);
        manifestDescriptor = saxParser.parseSax();
        manifestDescriptor.setOutputDir(project.getFolder(outputDirectory).getLocation().toFile());
        return manifestDescriptor;
    }

    public String parseSymbolicName() throws Exception {
        OsgiManifestDescriptor mf = parseSax();
        return mf.getSymbolicName();
    }

    private OsgiManifestDescriptor parseSax() throws Exception {
        manifestDescriptor = new OsgiManifestDescriptor();
        IFile pomFile = project.getFile("pom.xml");
        InputStream xmlStream = new ByteArrayInputStream(PluginUtil.readFile(pomFile));
        InputSource src = new InputSource(xmlStream);
        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        SAXParser parser = parserFactory.newSAXParser();
        parser.parse(src, new DefaultHandler() {
            boolean atProjectBuildLevel = false;
            boolean inGroupIdElem = false;
            boolean inArtifactIdElem = false;
            boolean inVersionElem = false;
            boolean inFinalNameElem = false;
            boolean inSourceDirElem = false;
            boolean inOutputDirElem = false;
            boolean inExportPkgElem = false;
            boolean inPrivatePkgElem = false;
            boolean inImportPkgElem = false;
            boolean inDynamicImportPkgElem = false;
            boolean inBundleActivatorElem = false;
            boolean inBundleClasspathElem = false;
            boolean inMdwVersionElem = false;
            boolean inBsnElem = false;
            int depth = 0;

            public void startElement(String uri, String localName, String qName, Attributes attrs)
                    throws SAXException {
                if (depth == 1) {
                    if (qName.equals("groupId"))
                        inGroupIdElem = true;
                    else if (qName.equals("artifactId"))
                        inArtifactIdElem = true;
                    else if (qName.equals("version"))
                        inVersionElem = true;
                }

                if (atProjectBuildLevel) {
                    if (qName.equals("finalName"))
                        inFinalNameElem = true;
                    else if (qName.equals("sourceDirectory"))
                        inSourceDirElem = true;
                    else if (qName.equals("outputDirectory"))
                        inOutputDirElem = true;
                }

                if (depth == 1 && qName.equals("build"))
                    atProjectBuildLevel = true;

                if (qName.equals("bundle.symbolicName"))
                    inBsnElem = true;
                else if (qName.equals("mdw.version"))
                    inMdwVersionElem = true;
                else if (qName.equals("Export-Package"))
                    inExportPkgElem = true;
                else if (qName.equals("Private-Package"))
                    inPrivatePkgElem = true;
                else if (qName.equals("Import-Package"))
                    inImportPkgElem = true;
                else if (qName.equals("DynamicImport-Package"))
                    inDynamicImportPkgElem = true;
                else if (qName.equals("Bundle-Activator"))
                    inBundleActivatorElem = true;
                else if (qName.equals("Bundle-ClassPath"))
                    inBundleClasspathElem = true;
                depth++;
            }

            public void endElement(String uri, String localName, String qName) throws SAXException {
                if (depth == 1 && qName.equals("build"))
                    atProjectBuildLevel = false;

                inGroupIdElem = false;
                inArtifactIdElem = false;
                inVersionElem = false;
                inFinalNameElem = false;
                inSourceDirElem = false;
                inOutputDirElem = false;
                inExportPkgElem = false;
                inPrivatePkgElem = false;
                inImportPkgElem = false;
                inDynamicImportPkgElem = false;
                inBundleActivatorElem = false;
                inBundleClasspathElem = false;
                inMdwVersionElem = false;
                inBsnElem = false;
                depth--;
            }

            public void characters(char[] ch, int start, int length) throws SAXException {
                if (inGroupIdElem)
                    groupId = new String(ch).substring(start, start + length).trim();
                else if (inArtifactIdElem)
                    artifactId = new String(ch).substring(start, start + length).trim();
                else if (inVersionElem)
                    version = new String(ch).substring(start, start + length).trim();
                else if (inFinalNameElem)
                    finalName = new String(ch).substring(start, start + length).trim();
                else if (inSourceDirElem)
                    sourceDirectory = new String(ch).substring(start, start + length).trim();
                else if (inOutputDirElem)
                    outputDirectory = new String(ch).substring(start, start + length).trim();
                else if (inExportPkgElem)
                    manifestDescriptor.setExportPackage(manifestDescriptor.getExportPackage()
                            + new String(ch).substring(start, start + length).trim());
                else if (inPrivatePkgElem)
                    manifestDescriptor.setPrivatePackage(manifestDescriptor.getPrivatePackage()
                            + new String(ch).substring(start, start + length).trim());
                else if (inImportPkgElem)
                    manifestDescriptor.setImportPackage(manifestDescriptor.getImportPackage()
                            + new String(ch).substring(start, start + length).trim());
                else if (inDynamicImportPkgElem)
                    manifestDescriptor
                            .setDynamicImportPackage(manifestDescriptor.getDynamicImportPackage()
                                    + new String(ch).substring(start, start + length).trim());
                else if (inBundleActivatorElem)
                    manifestDescriptor.setBundleActivator(manifestDescriptor.getBundleActivator()
                            + new String(ch).substring(start, start + length).trim());
                else if (inBundleClasspathElem)
                    manifestDescriptor.setBundleClasspath(manifestDescriptor.getBundleClasspath()
                            + new String(ch).substring(start, start + length).trim());
                else if (inBsnElem)
                    manifestDescriptor.setSymbolicName(manifestDescriptor.getSymbolicName()
                            + new String(ch).substring(start, start + length).trim());
                else if (inMdwVersionElem)
                    mdwVersion = new String(ch).substring(start, start + length).trim();
            }
        });
        return manifestDescriptor;
    }
}
