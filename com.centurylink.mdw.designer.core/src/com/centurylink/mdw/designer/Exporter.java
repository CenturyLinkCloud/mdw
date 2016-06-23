/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.RemoteException;

import org.apache.xmlbeans.XmlException;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.timer.ActionCancelledException;
import com.centurylink.mdw.dataaccess.VersionControl;
import com.centurylink.mdw.dataaccess.file.VersionControlGit;
import com.centurylink.mdw.designer.utils.RestfulServer;
import com.centurylink.mdw.model.value.process.PackageVO;

/**
 * Command-line exporter for saving workflow packages to XML-formatted files.
 */
public class Exporter {

    public static void main(String[] args) {
        if (args.length == 1 && args[0].equals("-h")) {
            System.out.println("Example Usage: ");
            System.out.println("java com.centurylink.mdw.designer.Exporter "
                    + "jdbc:oracle:thin:mdwdemo/mdwdemo@mdwdevdb.dev.qintra.com:1594:mdwdev (or /path/to/root/storage) "
                    + "com.centurylink.mdw.tests "
                    + "/path/to/packageDef.xml");
            System.exit(0);
        }
        if (args.length != 2 && args.length != 3) {
            System.err.println("arguments: <jdbcUrl|fileBasedRootDir> <packageName> <xmlFile>");
            System.err.println("(-h for example usage)");
            System.exit(-1);
        }

        String arg0 = args[0];  // either jdbcUrl or local file path
        String packageName = args[1];
        String xmlFile = args[2];

        try {
            boolean local = !arg0.startsWith("jdbc:");
            RestfulServer restfulServer = new RestfulServer(local ? "jdbc://dummy" : arg0, null, "http://dummy");
            DesignerDataAccess dataAccess = null;

            if (local) {
                VersionControl versionControl = new VersionControlGit();
                versionControl.connect(null, null, null, new File(arg0));
                restfulServer.setVersionControl(versionControl);
                restfulServer.setRootDirectory(new File(arg0));
                dataAccess = new DesignerDataAccess(restfulServer, null, "export", false);
            }
            else {
                dataAccess = new DesignerDataAccess(restfulServer, null, "export", true);
            }

            System.out.println("Exporting with arguments: " + arg0 + " " + packageName + " " + xmlFile);
            Exporter exporter = new Exporter(dataAccess);
            long before = System.currentTimeMillis();
            String xml = exporter.exportPackage(packageName);
            File outputFile = new File(xmlFile);
            if (outputFile.exists()) {
                System.out.println("Overwriting existing file: " + outputFile);
            }
            else if (!outputFile.getParentFile().exists()) {
                if (!outputFile.getParentFile().mkdirs())
                    throw new IOException("Cannot create directory: " + outputFile.getParentFile());
            }
            exporter.writeFile(outputFile, xml.getBytes());
            long afterExport = System.currentTimeMillis();
            System.out.println("Time taken for export: " + ((afterExport - before)/1000) + " s");
        }
        catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        }
    }

    private DesignerDataAccess dataAccess;

    /**
     * For VCS assets only.
     */
    public Exporter(String assetRoot) throws IOException {
        try {
            RestfulServer restfulServer = new RestfulServer("jdbc://dummy", null, "http://dummy");
            VersionControl versionControl = new VersionControlGit();
            versionControl.connect(null, null, null, new File(assetRoot));
            restfulServer.setVersionControl(versionControl);
            restfulServer.setRootDirectory(new File(assetRoot));
            dataAccess = new DesignerDataAccess(restfulServer, null, "export", false);
        }
        catch (Exception ex) {
            throw new IOException(ex.getMessage(), ex);
        }
    }

    Exporter(DesignerDataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    private boolean isLocal() {
        return dataAccess.isVcsPersist();
    }

    public String exportPackage(String packageName)
    throws DataAccessException, RemoteException, ActionCancelledException, XmlException {

        PackageVO packageVo = dataAccess.getPackage(packageName);
        int schemaVersion = dataAccess.getDatabaseSchemaVersion();
        String xml = dataAccess.exportPackage(packageVo.getId(), schemaVersion, null);
        if (!isLocal()) {
          packageVo.setExported(true);
          dataAccess.savePackage(packageVo);
        }

        return xml;
    }

    public void writeFile(File file, byte[] contents) throws IOException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            fos.write(contents);
        }
        finally {
            if (fos != null)
                fos.close();
        }
    }
}
