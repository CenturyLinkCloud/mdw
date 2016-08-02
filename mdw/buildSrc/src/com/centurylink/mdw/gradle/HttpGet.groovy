import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.tooling.BuildException

import java.net.URL
import java.io.File

class HttpGet extends DefaultTask {
    
    private Object fromUrl
    public void fromUrl(Object fromUrl) { this.fromUrl = fromUrl; }
    
    private Object into
    public void into(Object into) { this.into = into; }
    
    @TaskAction
    def perform() {
        
        if (fromUrl == null || into == null)
            throw new IllegalArgumentException("Missing fromUrl or into.")
        
        try {
            URL url = fromUrl instanceof URL ? fromUrl : new URL(fromUrl) 
            
            File file = into instanceof File ? into : getProject().file(into)
            File destFile = file;
            if (file.isDirectory()) {
                def fileName = url.getFile().substring(url.getFile().lastIndexOf("/") + 1)
                destFile = new File(file.toString() + "/" + fileName)
            }
            
            if (!destFile.getParentFile().exists())
                throw new IOException("Path does not exist: " + destFile.getParentFile())
        
            doDownload(url, destFile);
        }
        catch (Exception ex) {
            throw new BuildException(ex.getMessage(), ex)
        }
    }
       
    public void doDownload(URL url, File dest) throws IOException {
        HttpURLConnection conn = null
        FileOutputStream fos = null
        InputStream is = null
        try {
            conn = (HttpURLConnection) url.openConnection()
            HttpURLConnection.setFollowRedirects(true)
            fos = new FileOutputStream(dest)
            byte[] buffer = new byte[2048]
            is = conn.getInputStream()
            while (true) {
                int bytesRead = is.read(buffer)
                if (bytesRead == -1)
                    break;
                fos.write(buffer, 0, bytesRead)
            }
        }
        finally {
            if (fos != null)
                fos.close()
            if (is != null)
                is.close()
            if (conn != null)
                conn.disconnect()
        }
    }
}