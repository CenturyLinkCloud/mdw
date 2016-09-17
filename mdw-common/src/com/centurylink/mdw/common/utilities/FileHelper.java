/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.utilities;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyManager;

public final class FileHelper {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public static void copy(File in, File out, boolean overwrite) throws IOException {
        if (!in.exists())
            throw new IOException("Input file does not exist: " + in);
        if (out.exists() && !overwrite)
            throw new IOException("Output file exists: " + out);

        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(in);
            os = new FileOutputStream(out);
            int read = 0;
            byte[] bytes = new byte[1024];
            while((read = is.read(bytes)) != -1)
                os.write(bytes, 0, read);
        }
        finally {
            if (is != null)
                is.close();
            if (os != null)
                os.close();
        }
    }

    public static void copyRecursive(File src, File dest, boolean overwrite) throws IOException {
        if (!src.exists())
            throw new IOException("Source does not exist: " + src);
        if (src.isFile()) {
            copy(src, dest, overwrite);
        }
        else {
            if (!dest.exists() && !dest.mkdirs())
                throw new IOException("Unable to create target directory: " + dest);
            for (File srcFile : src.listFiles()) {
                if (srcFile.isFile()) {
                    copy(srcFile, new File(dest + "/" + srcFile.getName()), overwrite);
                }
                else if (srcFile.isDirectory()) {
                    copyRecursive(srcFile, new File(dest + "/" + srcFile.getName()), overwrite);
                }
            }
        }
    }

    public static void deleteRecursive(File src) throws IOException {
        deleteRecursive(src, null);
    }

    public static void deleteRecursive(File src, List<File> excludes) throws IOException {
        if (!src.exists())
            throw new IOException("File/directory does not exist: " + src);

        if (excludes == null || !excludes.contains(src)) {
            if (src.isFile()) {
                if (!src.delete())
                    throw new IOException("Cannot delete file: " + src);
            }
            else {
                for (File srcFile : src.listFiles()) {
                    if (srcFile.isFile()) {
                        if (excludes == null || !excludes.contains(srcFile)) {
                            if (!srcFile.delete())
                                throw new IOException("Cannot delete: " + srcFile);
                        }
                    }
                    else if (srcFile.isDirectory()) {
                        if (excludes == null || !excludes.contains(srcFile))
                            deleteRecursive(srcFile, excludes);
                    }
                }

                boolean isParentOfExclude = false;
                if (excludes != null) {
                    for (File exclude : excludes) {
                        File parent = exclude.getParentFile();
                        while (!isParentOfExclude && parent != null) {
                            isParentOfExclude = parent.equals(src);
                            parent = parent.getParentFile();
                        }
                        if (isParentOfExclude)
                            break; // don't keep checking
                    }
                }

                if (!isParentOfExclude) {
                    if (!src.delete())
                        throw new IOException("Cannot delete: " + src);
                }
            }
        }
    }

    /**     * Method that returns the file contents
     * @param pFileName
     * @return FileContents
     */
    public static String getFileContents(String pFileName) throws IOException{
         String fileContents = null;
         InputStream is = new FileInputStream(pFileName);
         BufferedReader reader = new BufferedReader(new InputStreamReader(is));
         StringBuffer xml = new StringBuffer();
         String aLine = null;
         while((aLine = reader.readLine()) != null){
           xml.append(aLine);
           xml.append("\n");

         }
        reader.close();
        fileContents = xml.toString();
        return fileContents;
    }

    public static String readFileFromClasspath(String fileName) throws IOException {
        URL url = Thread.currentThread().getContextClassLoader().getResource(fileName);
        if (url == null)
            throw new IOException("Unable to find file: " + fileName);

        return getFileContents(getFilePath(url));
    }

    public static InputStream fileInputStreamFromClasspath(String fileName) throws IOException {
        URL url = Thread.currentThread().getContextClassLoader().getResource(fileName);
        return new FileInputStream(getFilePath(url));
    }

    public static File getFileFromClasspath(String fileName) {
        URL url = Thread.currentThread().getContextClassLoader().getResource(fileName);
        if (url == null)
            return null;
        File file = null;
        try {
            file = new File(url.toURI());
        }
        catch(URISyntaxException e) {
            file = new File(url.getPath());
        }
        return file;
    }

    public static boolean fileExistsOnClasspath(String fileName) {
        return Thread.currentThread().getContextClassLoader().getResource(fileName) != null;
    }

    public static String getFilePath(URL url) throws FileNotFoundException {
        File file = null;
        try {
            file = new File(url.toURI());
        }
        catch(URISyntaxException e) {
            file = new File(url.getPath());
        }

        if (!file.exists())
            throw new FileNotFoundException(url.getPath());

        return file.getAbsolutePath();
  }

    /**
     * Method that writes the file contents
     * @param pFileName
     * @param pContents
     * @param pAppend
     */
    public static void writeToFile(String pFileName, String pContents, boolean pAppend)
     throws IOException{
         FileWriter writer = null;
         writer = new FileWriter(pFileName, pAppend);
         writer.write(pContents);
         writer.flush();
         writer.close();

    }

    private static final int FILE_BUFFER_KB = 16;
    public static void writeToFile(InputStream inputStream, File file) throws IOException {
        OutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[FILE_BUFFER_KB * 1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
        }
        finally {
            if (outputStream != null)
                outputStream.close();
        }
    }

    public static String readFromFile(String filePath) throws IOException {
      File file = new File(filePath);
      FileInputStream fis = null;
      try
      {
        fis = new FileInputStream(file);
        byte[] bytes = new byte[(int)file.length()];
        fis.read(bytes);
        return new String(bytes);
      }
      finally
      {
        if (fis != null)
          fis.close();
      }
    }

    public static String stripDisallowedFilenameChars(String input) {
        if (input == null)
          return null;

        StringBuffer sb = new StringBuffer();
        char c;
        for (int i = 0; i < input.length(); ++i)
        {
            c = input.charAt(i);
            switch (c) {
                case 0:
                    break;
                case 13:
                    break;
                case ' ':
                    break;
                case '\n':
                    break;
                case '\t':
                    break;
                case '\\':
                    break;
                case '/':
                    break;
                case ':':
                    break;
                case '*':
                    break;
                case '?':
                    break;
                case '"':
                    break;
                case '<':
                    break;
                case '>':
                    break;
                case '|':
                    break;
                default:
                    sb.append(c);
            }
        }

        return sb.toString();
    }

    public static byte[] readFromResourceStream(InputStream is) throws IOException {
	    try {
    		int length = is.available();
			byte buffer[] = new byte[length];
			int total_read = 0;
			while (total_read<length) {
				int n = is.read(buffer, total_read, length-total_read);
				if (n>0) total_read += n;
				else break;
			}
			return buffer;
	    } finally {
			try { is.close(); } catch (IOException e) {}
	    }
    }

    public static InputStream openConfigurationFile(String filename) throws FileNotFoundException {
        return openConfigurationFile(filename, FileHelper.class.getClassLoader(), true);
    }
    public static InputStream openConfigurationFile(String filepath, ClassLoader classLoader) throws FileNotFoundException {
        return openConfigurationFile(filepath, classLoader, true);
    }

    /**
     * Open configuration file. If Java system property mdw.config.location is defined,
     * and the file exists in that directory, it will load the file. Otherwise it loads through
     * the class path.
     * @param filepath
     * @param classLoader
     * @return Input steam of the file
     * @throws FileNotFoundException if the file is not found
     */
    public static InputStream openConfigurationFile(String filepath, ClassLoader classLoader, boolean useMDWConfigLocation) throws FileNotFoundException {
        String configDir = null;
        if (useMDWConfigLocation) {
            configDir = System.getProperty(PropertyManager.MDW_CONFIG_LOCATION);
        }
        File file;
        if (configDir == null)
            file = new File(filepath);
        else if (configDir.endsWith("/"))
            file = new File(configDir + filepath);
        else
            file = new File(configDir + "/" + filepath);
        if (!file.exists() && ApplicationContext.isOsgi())  // for OSGi try etc directory before classpath
            file = new File("etc/" + filepath);
        if (file.exists()) {
            logger.info("Located configuration file: " + file.getAbsolutePath());
            return new FileInputStream(file);
        }

        // last resort is classLoader classpath
        InputStream is = classLoader.getResourceAsStream(filepath);
        if (is == null) {
            if (ApplicationContext.getWarDeployPath() != null) {
                // try META-INF/mdw
                file = new File(ApplicationContext.getWarDeployPath() + "META-INF/mdw/" + filepath);
                if (file.exists()) {
                    logger.info("Located configuration file: " + file.getAbsolutePath());
                    is = new FileInputStream(file);
                }
            }

            if (is == null)
                throw new FileNotFoundException(filepath);  // give up
        }
        return is;
    }

    public static final String BUNDLE_CLASSPATH_BASE = "META-INF/mdw";

    /**
     * Read file according to the follow precedence:
     *   - From directory specified by system property mdw.config.location
     *   - From fully qualified file name if mdw.config.location is null
     *   - From etc/ directory relative to java startup dir
     *   - From META-INF/mdw using the designated class loader
     */
    public static InputStream readFile(String filename, ClassLoader classLoader) throws IOException {
        // first option: specified through system property
        String configDir = System.getProperty(PropertyManager.MDW_CONFIG_LOCATION);
        File file;
        if (configDir == null)
            file = new File(filename);  // maybe fully-qualified file name
        else if (configDir.endsWith("/"))
            file = new File(configDir + filename);
        else
            file = new File(configDir + "/" + filename);

        // next option: etc directory
        if (!file.exists())
            file = new File("etc/" + filename);

        if (file.exists())
            return new FileInputStream(file);

        // not overridden so load from bundle classpath
        String path = BUNDLE_CLASSPATH_BASE + "/" + filename;
        return classLoader.getResourceAsStream(path);
    }

    public static String getConfigFile(String filepath) throws IOException {
        return getConfigFile(filepath, true);
    }

    /**
     * @param springContextFile
     * @param useMDWConfigLocation - whether to check for externally located files
     * @return
     * @throws IOException
     */
    public static String getConfigFile(String filepath, boolean useMDWConfigLocation) throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        InputStream inStream = null;

        try
        {
          inStream = openConfigurationFile(filepath, Thread.currentThread().getContextClassLoader(), useMDWConfigLocation);
          byte[] buffer = new byte[2048];
          while (true)
          {
            int bytesRead = inStream.read(buffer);
            if (bytesRead == -1)
              break;
            outStream.write(buffer, 0, bytesRead);
          }
        }
        finally
        {
          if (inStream != null)
            inStream.close();
        }

        return outStream.toString();
    }

    public static void createZipFile(File directory, File zipFile) throws IOException {
        createZipFile(directory, zipFile, null);
    }

    public static void createZipFile(File directory, File zipFile, List<File> excludes) throws IOException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(zipFile);
            ZipHelper.writeZip(directory, fos, excludes);
        }
        finally {
            if (fos != null)
                fos.close();
        }
    }

    public static void createZipFileWith(File directory, File zipFile, List<File> includes) throws IOException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(zipFile);
            ZipHelper.writeZipWith(directory, fos, includes);
        }
        finally {
            if (fos != null)
                fos.close();
        }
    }

    public static void unzipFile(File zipFile, File destDir) throws IOException {
        unzipFile(zipFile, destDir, null, null, false);
    }

    public static void unzipFile(File zipFile, File destDir, String baseLoc, List<String> excludes, boolean overwrite) throws IOException {
        if (!destDir.exists() || !destDir.isDirectory())
            throw new IOException("Destination directory does not exist: " + destDir);

        ZipFile zip = new ZipFile(zipFile);
        if (baseLoc != null && !baseLoc.endsWith("/"))
            baseLoc += "/";

        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();

            if ((baseLoc == null || (entry.getName().startsWith(baseLoc) && !entry.getName().equals(baseLoc)))
                    && (excludes == null || !excludes.contains(entry.getName()))) {
                // write the file
                String outpath = destDir + "/";
                if (baseLoc == null)
                    outpath += entry.getName();
                else
                    outpath += entry.getName().substring(baseLoc.length());
                File outfile = new File(outpath);
                if (outfile.exists() && !overwrite)
                    throw new IOException("Output file already exists: " + outfile);
                if (entry.isDirectory()) {
                    if (!outfile.exists() && !outfile.mkdirs())
                        throw new IOException("Unable to create directory: " + outfile);
                }
                else {
                    InputStream is = null;
                    OutputStream os = null;
                    try {
                        is = zip.getInputStream(entry);
                        os = new FileOutputStream(outfile);
                        int read = 0;
                        byte[] bytes = new byte[1024];
                        while((read = is.read(bytes)) != -1)
                            os.write(bytes, 0, read);
                    }
                    finally {
                        if (is != null)
                            is.close();
                        if (os != null)
                          os.close();
                    }
                }
            }
        }
    }

    public static byte[] read(File file) throws IOException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            byte[] bytes = new byte[(int) file.length()];
            fis.read(bytes);
            return bytes;
        }
        finally {
            if (fis != null)
                fis.close();
        }
    }
}
