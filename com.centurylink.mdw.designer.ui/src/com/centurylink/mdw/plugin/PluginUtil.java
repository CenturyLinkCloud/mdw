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
package com.centurylink.mdw.plugin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.eclipse.core.internal.resources.ResourceException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import com.centurylink.mdw.plugin.preferences.model.MdwSettings;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

/**
 * Utilities to help with generating code.
 */
@SuppressWarnings("restriction")
public class PluginUtil {
    private static final String MAIL_PROTOCOL = "smtp";
    private static final String MAIL_FROM_ADDRESS = "mdw.plugin@centurylink.com";
    private static final String MDW_DEV_EMAIL_GROUP = "MDW.Development@centurylink.com";

    private PluginUtil() {
    } // don't instantiate

    /**
     * Add folders to a project to support a particular file location.
     *
     * @param project
     * @param folder
     * @param monitor
     */
    public static void createFoldersAsNeeded(IProject project, IFolder folder,
            IProgressMonitor monitor) throws CoreException {
        IContainer container = project;

        for (int i = 1, length = folder.getFullPath().segmentCount(); i < length; i++) {
            IFolder checkFolder = container.getFolder(new Path(folder.getFullPath().segment(i)));

            if (!checkFolder.exists()) {
                if (monitor == null)
                    checkFolder.create(true, true, null);
                else
                    checkFolder.create(true, true, new SubProgressMonitor(monitor, 1));
            }
            else {
                if (monitor != null)
                    monitor.worked(1);
            }

            container = checkFolder;
        }
    }

    /**
     * Create a folder in a project.
     *
     * @param project
     * @param path
     * @param monitor
     */
    public static IFolder createFolder(IProject project, IPath path, IProgressMonitor monitor)
            throws CoreException {
        IContainer container = project;
        IFolder folder = container.getFolder(path);
        if (!folder.exists()) {
            if (monitor == null)
                monitor = new NullProgressMonitor();
            folder.create(false, true, new SubProgressMonitor(monitor, 1));
        }
        return folder;
    }

    /**
     * Create a folder in a project.
     *
     * @param project
     * @param loc
     *            the folder path
     * @param monitor
     */
    public static void createFolder(IProject project, String loc, IProgressMonitor monitor)
            throws CoreException {
        IPath path = project.getFile(loc).getProjectRelativePath();
        createFolder(project, path, monitor);
    }

    /**
     * Copies a file from the file system into the project. Assumes that the
     * destination folder already exists. Overwrites the file if it exists.
     *
     * @param src
     *            the file to copy
     * @param project
     *            the project to copy into
     * @param destLoc
     *            destination FOLDER in the project (must exist)
     * @param monitor
     *            optional progress monitor
     */
    public static void copyFileIntoProject(File src, IProject project, String destLoc,
            IProgressMonitor monitor) throws CoreException, IOException {
        IFile destFile = project.getFile(destLoc + "/" + src.getName());
        InputStream is = new FileInputStream(src);
        if (!destFile.exists())
            destFile.create(is, true, monitor);
        else
            destFile.setContents(is, true, false, monitor);
    }

    /**
     * Downloads the contents of the web page into a string.
     *
     * @param url
     *            the url of the page to download
     * @return the page text
     */
    public static String downloadContent(URL url) throws IOException {
        InputStream is = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            URLConnection conn = url.openConnection();
            conn.setUseCaches(false);
            byte[] buffer = new byte[2048];
            is = conn.getInputStream();
            while (true) {
                int bytesRead = is.read(buffer);
                if (bytesRead == -1)
                    break;
                baos.write(buffer, 0, bytesRead);
            }
        }
        finally {
            if (is != null)
                is.close();
            if (baos != null)
                baos.close();
        }

        return baos.toString();
    }

    /**
     * Download a file from a URL.
     *
     * @param url
     * @param file
     */
    public static void downloadToFile(URL url, File file) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            URLConnection conn = url.openConnection();
            conn.setUseCaches(false);
            byte[] buffer = new byte[2048];
            is = conn.getInputStream();
            os = new FileOutputStream(file);
            while (true) {
                int bytesRead = is.read(buffer);
                if (bytesRead == -1)
                    break;
                os.write(buffer, 0, bytesRead);
            }
        }
        finally {
            if (is != null)
                is.close();
            if (os != null)
                os.close();
        }
    }

    /**
     * Copy contents of a file.
     *
     * @param source
     * @param dest
     */
    public static void copyFile(File source, File dest) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            byte[] buffer = new byte[2048];
            is = new FileInputStream(source);
            os = new FileOutputStream(dest);
            while (true) {
                int bytesRead = is.read(buffer);
                if (bytesRead == -1)
                    break;
                os.write(buffer, 0, bytesRead);
            }
        }
        finally {
            if (is != null)
                is.close();
            if (os != null)
                os.close();
        }
    }

    public static void copyJarEntryToFile(File jar, String entryPath, File dest)
            throws IOException {
        JarFile jarFile = null;
        InputStream is = null;
        OutputStream os = null;
        try {
            jarFile = new JarFile(jar);
            for (Enumeration<?> entriesEnum = jarFile.entries(); entriesEnum.hasMoreElements();) {
                JarEntry jarEntry = (JarEntry) entriesEnum.nextElement();
                if (jarEntry.getName().equals(entryPath)) {
                    is = jarFile.getInputStream(jarEntry);
                }
            }
            if (is == null)
                throw new IOException("JAR entry not found: " + dest);

            byte[] buffer = new byte[2048];
            os = new FileOutputStream(dest);
            while (true) {
                int bytesRead = is.read(buffer);
                if (bytesRead == -1)
                    break;
                os.write(buffer, 0, bytesRead);
            }
        }
        finally {
            if (is != null)
                is.close();
            if (os != null)
                os.close();
            if (jarFile != null)
                jarFile.close();
        }
    }

    /**
     * Downloads from a URL into a project. Assumes that the destination folder
     * already exists. Overwrites the file if it exists.
     *
     * @param project
     *            to download into
     * @param url
     *            to download from
     * @param destFolder
     *            will be created if it doesn't exist
     * @param destFile
     *            the downloaded file
     * @param monitor
     *            progress monitor (uses 75 ticks)
     */
    public static void downloadIntoProject(IProject project, URL url, IFolder destFolder,
            IFile destFile, String message, IProgressMonitor monitor)
            throws IOException, CoreException, InterruptedException {
        InputStream is = null;
        FileOutputStream fos = null;
        if (message == null)
            message = "Download";
        monitor.subTask("Preparing to " + message + ": " + destFile.getName());
        IProgressMonitor subMonitor = new SubProgressMonitor(monitor, 75);
        if (destFolder != null)
            createFoldersAsNeeded(project, destFolder, subMonitor);
        try {
            File file = new File(destFile.getRawLocationURI());
            fos = new FileOutputStream(file);
            URLConnection conn = url.openConnection();
            byte[] buffer = new byte[2048];
            subMonitor.beginTask("", conn.getContentLength() / buffer.length);
            subMonitor.worked(1);
            is = conn.getInputStream();
            monitor.subTask(message + ": " + destFile.getName());
            while (true) {
                int bytesRead = is.read(buffer);
                if (bytesRead == -1)
                    break;
                fos.write(buffer, 0, bytesRead);
                subMonitor.worked(1);
                if (subMonitor.isCanceled())
                    throw new InterruptedException(message + " cancelled");
            }
            subMonitor.done();
        }
        finally {
            if (is != null)
                is.close();
            if (fos != null)
                fos.close();
            if (subMonitor != null)
                subMonitor.done();
        }
    }

    /**
     * Unzips an archive file into a project.
     *
     * @param project
     *            holds the archive file and the destination folder
     * @param file
     *            the archive file to unzip
     * @param filesToIgnore
     *            filenames to exclude
     * @param destFolder
     *            directory to unzip into
     * @param a
     *            progress monitor (uses 10 ticks)
     */
    public static void unzipProjectResource(IProject project, IFile file,
            List<String> filesToIgnore, IFolder destFolder, IProgressMonitor monitor)
            throws IOException, CoreException {
        monitor.subTask("Unzipping: " + file.getName());
        JarFile jar = null;
        String outpath = null;
        try {
            jar = new JarFile(new File(file.getLocationURI()));

            if (destFolder != null && !destFolder.exists())
                project.refreshLocal(IResource.DEPTH_INFINITE, new SubProgressMonitor(monitor, 1));

            IProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
            Enumeration<JarEntry> entries = jar.entries();
            subMonitor.beginTask("", jar.size());
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();

                if (filesToIgnore != null && filesToIgnore
                        .contains(entryName.substring(entryName.lastIndexOf("/") + 1)))
                    continue;

                // write the file into the project
                if (destFolder == null)
                    outpath = project.getProjectRelativePath() + "/" + entryName;
                else
                    outpath = destFolder.getProjectRelativePath() + "/" + entryName;

                IFile outfile = project.getFile(outpath);
                if (!outfile.exists()) {
                    if (entry.isDirectory())
                        PluginUtil.createFoldersAsNeeded(project, project.getFolder(outpath),
                                subMonitor);
                    else {
                        try {
                            outfile.create(jar.getInputStream(entry), IFile.FORCE, subMonitor);
                        }
                        catch (ResourceException ex) {
                            PluginMessages.log(ex);
                            final Display display = MdwPlugin.getDisplay();
                            if (display != null) {
                                final String msg = ex.getMessage();
                                display.syncExec(new Runnable() {
                                    public void run() {
                                        MessageDialog.openError(display.getActiveShell(),
                                                "Resource Error", msg);
                                    }
                                });
                            }
                        }
                    }
                }
                else {
                    if (!entry.isDirectory())
                        outfile.setContents(jar.getInputStream(entry), IFile.FORCE, subMonitor);
                }
                subMonitor.worked(1);
            }
        }
        finally {
            if (jar != null)
                jar.close();
        }
    }

    /**
     * Creates a file in the project from a string. Assumes that the destination
     * folder already exists. Overwrites the file if it exists.
     *
     * @param contents
     *            string to create the file from
     * @param project
     *            the project to copy into
     * @param destLoc
     *            destination FOLDER in the project (must exist)
     * @param destName
     *            name for the downloaded file
     * @param monitor
     *            optional progress monitor
     */
    public static void writeFileInProject(String contents, IProject project, String destLoc,
            String destName, IProgressMonitor monitor) throws CoreException, IOException {
        IFile destFile = project.getFile(destLoc + "/" + destName);
        InputStream is = new ByteArrayInputStream(contents.getBytes());
        if (!destFile.exists())
            destFile.create(is, true, monitor);
        else
            destFile.setContents(is, true, false, monitor);
    }

    /**
     * Jar a project resource into a specified output file.
     *
     * @param project
     *            the project where the resource is located
     * @param baseLoc
     *            the base location for the jar entry
     * @param resource
     *            the resource to be jarred
     * @param output
     *            the output file
     * @param monitor
     *            a progress monitor for the ui
     * @throws CoreException
     * @throws IOException
     */
    public static void jar(IProject project, String baseLoc, String resource, String output,
            IProgressMonitor monitor) throws CoreException, IOException {
        IFile infile = project.getFile(baseLoc + "/" + resource);
        InputStream is = infile.getContents(true);

        IFile outfile = project.getFile(output);
        FileOutputStream fos = new FileOutputStream(outfile.getRawLocation().toString());
        JarOutputStream jos = new JarOutputStream(fos);
        JarEntry entry = new JarEntry(resource);
        jos.putNextEntry(entry);

        int b = -1;
        while ((b = is.read()) != -1) {
            jos.write(b);
        }
        jos.closeEntry();
        jos.close();

    }

    public static void unzipPluginResource(String srcRelUri, List<String> exclusions,
            IProject project, String destLoc, IProgressMonitor monitor)
            throws IOException, URISyntaxException, CoreException {
        URL localUrl = getLocalResourceUrl(srcRelUri);
        JarFile jar = null;

        try {
            jar = new JarFile(new File(new URI(localUrl.toString())));

            IProgressMonitor subMonitor = new SubProgressMonitor(monitor, 1);

            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();

                if (exclusions == null || !exclusions.contains(entry.getName())) {
                    // write the file into the project
                    String outpath = destLoc + "/" + entry.getName();
                    IFile outfile = project.getFile(outpath);
                    if (!outfile.exists()) {
                        if (entry.isDirectory())
                            PluginUtil.createFoldersAsNeeded(project, project.getFolder(outpath),
                                    subMonitor);
                        else
                            outfile.create(jar.getInputStream(entry), IFile.FORCE, subMonitor);
                    }
                    else {
                        if (!entry.isDirectory())
                            outfile.setContents(jar.getInputStream(entry), IFile.FORCE, subMonitor);
                    }
                }
            }
        }
        finally {
            if (jar != null)
                jar.close();
        }
    }

    /**
     * Utility method for converting a relative URI to a local resource URL.
     *
     * @param relativeUri
     * @return the local resource URL
     */
    public static URL getLocalResourceUrl(String relativeUri) throws IOException {
        String pluginId = MdwPlugin.getPluginId();
        String absUriPrefix = Platform.getBundle(pluginId).getEntry("/").toString();
        return new URL(FileLocator.toFileURL(new URL(absUriPrefix + relativeUri)).toString()
                .replaceAll(" ", "\\%20"));
    }

    public static URL getCoreResourceUrl(String relativeUri) throws IOException {
        String pluginId = "com.centurylink.mdw.designer.core";
        String absUriPrefix = Platform.getBundle(pluginId).getEntry("/").toString();
        return new URL(FileLocator.toFileURL(new URL(absUriPrefix + relativeUri)).toString()
                .replaceAll(" ", "\\%20"));
    }

    /**
     * Converts a package name to an OS-independent resource path.
     *
     * @param packageName
     * @return the package
     */
    public static String convertPackageToPath(String packageName) {
        return packageName.replaceAll("\\.", "\\/");
    }

    public static byte[] readFile(IFile file) {
        InputStream inStream = null;
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        try {
            byte[] buffer = new byte[2048];
            inStream = file.getContents(true);

            while (true) {
                int bytesRead = inStream.read(buffer);
                if (bytesRead == -1)
                    break;
                outStream.write(buffer, 0, bytesRead);
            }
        }
        catch (Exception ex) {
            PluginMessages.log(ex);
            return null;
        }
        finally {
            try {
                if (inStream != null)
                    inStream.close();
                if (outStream != null)
                    outStream.close();
            }
            catch (IOException ex) {
            }
        }

        return outStream.toByteArray();
    }

    public static byte[] readFile(File file) throws IOException {
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

    public static void writeFile(IFile file, String contents, IProgressMonitor monitor) {
        InputStream inStream = new ByteArrayInputStream(contents.getBytes());
        try {
            if (file.exists())
                file.setContents(inStream, true, true, monitor);
            else
                file.create(inStream, true, monitor);
        }
        catch (CoreException ex) {
            PluginMessages.log(ex);
        }
    }

    public static void writeFile(File file, byte[] contents) throws IOException {

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

    public static void sendError(Throwable t, String title, String message, WorkflowProject project,
            int level) {
        Writer charArrayWriter = new CharArrayWriter();
        PrintWriter writer = new PrintWriter(charArrayWriter);
        writer.println("MDW Message:\n------------\n" + message);
        writer.println("\nUser: " + System.getProperty("user.name"));
        writer.println("Plug-In Version: " + MdwPlugin.getVersionString());
        writer.println(
                org.eclipse.ui.internal.ProductProperties.getAboutText(Platform.getProduct()));
        if (t != null) {
            writer.println("\nError Details:\n--------------");
            t.printStackTrace(writer);
        }
        if (project != null) {
            writer.println("\n" + project);
        }
        try {
            sendEmail("MDW " + PluginMessages.MESSAGE_LEVELS.get(new Integer(level)) + ": " + title,
                    charArrayWriter.toString());
        }
        catch (Exception ex) {
            PluginMessages.log(ex);
        }
    }

    public static void sendEmail(String subject, String content)
            throws MessagingException, AddressException {
        MdwSettings settings = MdwPlugin.getSettings();

        Properties props = new Properties();
        props.put("mail.transport.protocol", MAIL_PROTOCOL);
        props.put("mail.host", settings.getSmtpHost());
        props.put("mail.smtp.port", settings.getSmtpPort());
        Session session = Session.getInstance(props);

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(MAIL_FROM_ADDRESS));
        message.setSubject(subject);
        message.setRecipients(Message.RecipientType.TO,
                new Address[] { new InternetAddress(MDW_DEV_EMAIL_GROUP) });

        Multipart multiPart = new MimeMultipart();
        BodyPart bodyPart = new MimeBodyPart();
        bodyPart.setContent(content, "text/plain");
        multiPart.addBodyPart(bodyPart);
        message.setContent(multiPart);
        message.setSentDate(new Date());
        Transport.send(message);
    }

    public static void deleteDirectory(File directory) throws IOException {
        if (directory.exists() && directory.isDirectory()) {
            for (File file : directory.listFiles()) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                }
                else {
                    if (!file.delete())
                        throw new IOException("Unable to delete file: " + file);
                }
            }
            if (!directory.delete())
                throw new IOException("Unable to delete directory: " + directory);
        }
    }

    public static void copyDirectory(File src, File dest, boolean force) throws IOException {
        if (!src.exists())
            throw new IOException("Source directory does not exist: " + src);
        if (src.isDirectory()) {
            if (!dest.exists()) {
                if (!dest.mkdir())
                    throw new IOException("Unable to create directory: " + dest);
            }

            for (String file : src.list()) {
                File srcFile = new File(src, file);
                File destFile = new File(dest, file);
                copyDirectory(srcFile, destFile, force);
            }
        }
        else {
            if (force || src.lastModified() > dest.lastModified()) {
                InputStream in = null;
                OutputStream out = null;
                try {
                    in = new FileInputStream(src);
                    out = new FileOutputStream(dest);
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = in.read(buffer)) > 0)
                        out.write(buffer, 0, length);
                }
                finally {
                    if (in != null)
                        in.close();
                    if (out != null)
                        out.close();
                }
            }
        }
    }

    private static DateFormat dateFormat;

    public static DateFormat getDateFormat() {
        if (dateFormat == null)
            dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm");
        return dateFormat;
    }

    /**
     * Appends two arrays.
     */
    public static Object[] appendArrays(Object[] one, Object[] two) {
        if (one == null || two == null)
            return null;
        if (!(one.getClass().getName().equals(two.getClass().getName())))
            return null;

        Object[] appended = (Object[]) Array.newInstance(
                one.length > 0 ? one[0].getClass() : two[0].getClass(), one.length + two.length);

        for (int i = 0; i < one.length; i++)
            appended[i] = one[i];
        for (int i = 0; i < two.length; i++)
            appended[i + one.length] = two[i];
        return appended;
    }

    /**
     * Adds an element to an array.
     */
    public static Object[] addToArray(Object element, Object[] array) {
        if (element == null || array == null)
            return null;
        if (!(element.getClass().getName()).equals(array[0].getClass().getName()))
            return null;

        Object[] appended = (Object[]) Array.newInstance(element.getClass(), array.length + 1);

        for (int i = 0; i < array.length; i++)
            appended[i] = array[i];

        appended[array.length] = element;
        return appended;
    }

    /**
     * Create an editable List from an Array.
     */
    public static <T> List<T> arrayToList(@SuppressWarnings("unchecked") T... a) {
        List<T> l = new ArrayList<T>();
        l.addAll(Arrays.asList(a));
        return l;
    }

    public static void setReadOnly(IResource resource, boolean readonly) throws CoreException {
        ResourceAttributes attributes = resource.getResourceAttributes();
        if (attributes == null)
            return;
        attributes.setReadOnly(readonly);
        resource.setResourceAttributes(attributes);
    }

    public static boolean isMac() {
        return System.getProperty("os.name").toLowerCase().indexOf("mac") >= 0;
    }

    public static String getJdkBin() throws IOException {
        File bin = new File(getJdkHome() + File.separator + "bin");
        if (!bin.exists()) {
            throw new IOException("Unable to locate JDK bin: '" + bin + "'");
        }

        return bin.getAbsolutePath();
    }

    /**
     * @return the JDK Home that launched the plug-in container
     */
    public static String getJdkHome() throws IOException {
        String javaHome = System.getProperty("java.home");

        if (!javaHome.endsWith(File.separator + "jre")) {
            throw new IOException("Unable to locate executable JDK Home.");
        }
        return javaHome.substring(0, javaHome.length() - 4);
    }

    public static Comparator<String> getStringComparator() {
        return new Comparator<String>() {
            public int compare(String s1, String s2) {
                return s1.compareToIgnoreCase(s2);
            }
        };
    }

    private static DateFormat df = new SimpleDateFormat("HH:mm:ss:SS");

    public static String dateTimeStamp() {
        return df.format(new Date());
    }

    private static String lineDelimiter = System.getProperty("line.separator") == null ? "\r\n"
            : System.getProperty("line.separator");

    public static String getLineDelimiter() {
        return lineDelimiter;
    }
}
