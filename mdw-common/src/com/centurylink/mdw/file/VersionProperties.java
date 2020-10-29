package com.centurylink.mdw.file;

import java.io.*;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.TreeSet;

/**
 * Extends java.util.Properties to make sure the keys are sorted in a
 * predictable order also to avoid setting date comment which causes Git conflicts.
 */
public class VersionProperties extends Properties {

    private File propFile;

    public VersionProperties(File propFile) throws IOException {
        super(null);
        load(new FileInputStream(propFile));
        this.propFile = propFile;
    }

    public VersionProperties(ByteArrayInputStream byteArrayInputStream) throws IOException {
        super(null);
        load(byteArrayInputStream);
    }

    @Override
    public synchronized Enumeration<Object> keys() {
        return Collections.enumeration(new TreeSet<Object>(super.keySet()));
    }

    @Override
    public void store(OutputStream out, String comments) throws IOException {
        if (comments != null) {
            super.store(out, comments);
        }
        else {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out, "8859_1"));
            synchronized (this) {
                for (Enumeration<?> e = keys(); e.hasMoreElements();) {
                    String key = (String)e.nextElement();
                    String val = (String)get(key);
                    bw.write(key.replaceAll(" ", "\\\\ ").replaceAll("!", "\\\\!") + "=" + val + "\n");
                }
            }
            bw.flush();
        }
    }

    public void save() throws IOException {
        if (isEmpty()) {
            if (propFile.exists() && !propFile.delete())
                throw new IOException("Unable to delete file: " + propFile);
        }
        else {
            OutputStream out = null;
            try {
                out = new FileOutputStream(propFile);
                store(out, null);
            }
            catch (FileNotFoundException ex) {
                // maybe read-only
                propFile.setWritable(true);
                out = new FileOutputStream(propFile);
                store(out, null);
            }
            finally {
                if (out != null)
                    out.close();
            }
        }
    }

    public int getVersion(String assetName) throws NumberFormatException {
        String prop = getProperty(assetName);
        return prop == null ? 0 : Integer.parseInt(prop.split(" ")[0]);
    }
}
