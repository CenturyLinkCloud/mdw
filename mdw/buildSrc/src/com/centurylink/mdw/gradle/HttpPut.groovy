import org.apache.commons.codec.binary.Base64;
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.tooling.BuildException

import java.net.URL
import java.io.File

/**
 * Perform an HTTP PUT to upload an artifact.  Either toUrl, baseUrl, groupUrl or assetsUrl must be specified.  
 * See the javadocs for these methods for details about the difference between the four URL options.
 */
class HttpPut extends DefaultTask {
    
    private List<File> fromList = new ArrayList<File>()
    List<File> getFromList() { return this.fromList; }
    
    private Object from
    public void from(Object from) { 
        if (this.from != null) {
            if (fromList.isEmpty()) {
                if (this.from instanceof File)
                    fromList.add(this.from)
                else
                    fromList.add(project.file(this.from))
            }
            if (from instanceof File)
                fromList.add(from)
            else
                fromList.add(project.file(from))
        }
        else {
            this.from = from;
        } 
    }

    private Object toUrl
    /**
     * If toUrl is specified then it should include the entire destination path, including filename.
     */
    public void toUrl(Object toUrl) { this.toUrl = toUrl; }

    private Object baseUrl
    /**
     * If baseUrl is specified then the artifact is uploaded directly to the url
     * with only the "from" filepath appended.
     */
    public void baseUrl(Object baseUrl) { this.baseUrl = baseUrl; }

    /**
     * If groupUrl is specified then the artifact is assumed to follow the Maven repo
     * naming conventions, so the url is appended with &lt;artifact_id>/&lt;version>/&lt;filename>.
     * Values for artifact_id and version are inferred from the "from" filename.
     */
    private Object groupUrl
    public void groupUrl(Object groupUrl) { this.groupUrl = groupUrl; }

    /**
     * If assetsUrl is specified then the artifact is uploaded to the specified URL
     * appended with the "from" filepath under the &lt;version>.
     */
    private Object assetsUrl
    public void assetsUrl(Object assetsUrl) { this.assetsUrl = assetsUrl; }

    private String user
    public void user(String user) { this.user = user; }
    
    private String password
    public void password(String pw) { this.password = pw; }
    
    private boolean failOnError = false;
    public void failOnError(boolean foe) { this.failOnError = foe; }
    
    private boolean overwrite = true;
    public void overwrite(boolean overwrite) { this.overwrite = overwrite; }
    
    private boolean withSources = false;
    public void withSources(boolean withSources) { this.withSources = withSources }
    
    private boolean dryRun = false;
    public void dryRun(boolean dryRun) { this.dryRun = dryRun; }
    
    @TaskAction
    def perform() {
        
        if (from == null || (toUrl == null && baseUrl == null && groupUrl == null && assetsUrl == null))
            throw new IllegalArgumentException("Required parameters: from && (toUrl || baseUrl || groupUrl || assetsUrl).")

        File fromFile = null            
        if (fromList.isEmpty()) {
            if (from instanceof File)
                fromFile = from
            else
                fromFile = project.file(from)
                
            if (fromFile.isDirectory()) {
                fromFile.eachFileRecurse(groovy.io.FileType.FILES) {
                    getFromList().add(it)
                }
            }
            else if (fromFile instanceof File) {
                this.fromList.add(fromFile)
            }
        }
        
        try {
            for (File file in fromList) {
                URL url
                File sourcesFile = null;
                URL sourcesUrl = null;
                if (withSources) {
                    String sourcesFileName = file.getName().substring(0, file.getName().lastIndexOf(".")) + "-sources.jar"
                    sourcesFile = new File(file.getParent() + "/" + sourcesFileName)
                }

                if (toUrl != null) {
                    url = new URL(toUrl)
                    if (withSources)
                        sourcesUrl = new URL(url.toString().substring(0, url.toString().lastIndexOf("/") + 1) + sourcesFile.getName())
                }
                else if (baseUrl != null) {
                    if (fromFile != null && fromFile.isDirectory()) {
                        String filepath = file.toString().substring(fromFile.toString().length() + 1).replace('\\', '/')
                        url = new URL(baseUrl + "/" + filepath)
                    }
                    else {
                        url = new URL(baseUrl + "/" + file.getName())
                        if (withSources)
                            sourcesUrl = new URL(baseUrl + "/" + sourcesFile.getName())
                    }
                }
                else {
                    // this logic needs reworking if SNAPSHOT build versions have timestamps
                    String filename = file.getName()
                    int lastDot = filename.lastIndexOf(".")
                    String rootname = filename.substring(0, lastDot)
                    int versionStart;
                    if (rootname.endsWith("-SNAPSHOT"))
                        versionStart = rootname.substring(0, rootname.length() - 9).lastIndexOf("-")
                    else
                        versionStart = rootname.lastIndexOf("-")
                    String artifactId = rootname.substring(0, versionStart)
                    String version = rootname.substring(versionStart + 1)
                    if (assetsUrl != null) {
                        if (fromFile != null && fromFile.isDirectory()) {
                            String path = file.toString().substring(fromFile.toString().length() + 1, file.toString().length() - filename.length() - 1).replace('\\', '/')
                            url = new URL(assetsUrl + "/" + path + "/" + version + "/" + filename)
                        }
                    }
                    else { // groupUrl
                        url = new URL(groupUrl + "/" + artifactId + "/" + version + "/" + filename)
                        if (withSources)
                            sourcesUrl = new URL(groupUrl + "/" + artifactId + "/" + version + "/" + sourcesFile.getName())
                    }
                }
            
                try {
                    doUpload(file, url)
                    if (withSources && sourcesFile.exists())
                        doUpload(sourcesFile, sourcesUrl)
                }
                catch (IOException ex) {
                    if (failOnError)
                        throw ex
                    else
                        println "Failed to upload " + file + ": " + ex
                }
            }
        }
        catch (Exception ex) {
            throw new BuildException(ex.getMessage(), ex)
        }
    }
       
    public void doUpload(File file, URL url) throws IOException {
        if (!file.exists())
            throw new IOException("File not found: " + file.getAbsoluteFile())
            
        println "Uploading: " + file + "\n   -> " + url
        
        if (dryRun)
            return
        
        long fileLastModified = file.lastModified()

        HttpURLConnection conn = null
        OutputStream outStream = null
        InputStream inStream = null
        
        try {
        
            conn = (HttpURLConnection) url.openConnection()
            long urlLastModified = conn.getLastModified()
            conn.disconnect()
    
            if (!overwrite && (urlLastModified >= fileLastModified)) {
                println "Destination file is up-to-date, not uploading."
                return;
            }
            else {
              conn = (HttpURLConnection) url.openConnection()
              conn.setRequestProperty("Content-Type", "application/octet-stream")
              conn.setRequestMethod("PUT")
              if (user != null) {
                  String value = user + ":" + password
                  conn.setRequestProperty("Authorization", "Basic " + new String(Base64.encodeBase64(value.getBytes())))
              }
    
              conn.setDoOutput(true)
    
              outStream = conn.getOutputStream()
              inStream = new FileInputStream(file)
    
              byte[] buf = new byte[1024]
              int len = 0
              while (len != -1) {
                  len = inStream.read(buf)
                  if (len > 0)
                      outStream.write(buf, 0, len)
              }
    
              int code = conn.getResponseCode()
              if (code < 200 || code >= 300)
                  throw new IOException("Error uploading file: " + code + " -- " + conn.getResponseMessage())
            }
        }
        finally {
            if (conn != null)
                conn.disconnect()
            if (outStream != null)
                outStream.close()
            if (inStream != null)
                inStream.close()
        }

    }
}