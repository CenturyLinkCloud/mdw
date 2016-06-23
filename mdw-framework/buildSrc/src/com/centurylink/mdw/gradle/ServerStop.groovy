import org.gradle.api.tasks.TaskAction
import org.gradle.tooling.BuildException
import org.gradle.api.tasks.AbstractExecTask
import org.gradle.process.ExecResult

/**
 * Stop a running server instance.
 */
class ServerStop extends AbstractExecTask {
    
    private Object verifyUrl
    public void verifyUrl(Object verifyUrl) { this.verifyUrl = verifyUrl; }
    
    private int timeout = 30 // seconds
    public void timeout(Object timeout) { this.timeout = Integer.parseInt(timeout.toString()); }
    
    public ServerStop() {
        super(ServerStop.class)
    }
    
    @TaskAction
    protected void exec() {
        if (System.properties["os.name"].startsWith("Windows")) {
            // make sure we'll be running in the background
            if (!getExecutable().equalsIgnoreCase("cmd")) {
                def cmdLine = new ArrayList<String>()
                cmdLine.add("cmd")
                cmdLine.add("/c")
                cmdLine.add(getExecutable())
                cmdLine.addAll(getArgs())
                commandLine cmdLine
            }
        }

        println "Shutting down server in directory: \"" + getWorkingDir() + "\" with command: \"" + getCommandLine() + "\""
        super.exec()
        if (verifyUrl != null) {
            try {
                verify()
            }
            catch (Exception ex) {
                throw new BuildException(ex.getMessage(), ex)
            }
        }
    }
    
    private void verify() {
        
        int countSecs = 0
        int interval = 5
        
        URL url = verifyUrl instanceof URL ? verifyUrl : new URL(verifyUrl.toString())
        HttpURLConnection conn = null
        InputStream is = null
                
        while (countSecs < timeout) {
            println "Checking server (" +  countSecs + "s) ..."
            try {
                conn = url.openConnection()
                is = conn.getInputStream()
            }
            catch (SocketException ex) {
                return; // assume tomcat is down
            }
            finally {
                if (is != null)
                    is.close()
                if (conn != null)
                    conn.disconnect()
            }
            sleep(interval * 1000)
            countSecs += interval
        }
        
        throw new RuntimeException("Server appears to be still running at " + verifyUrl + " after " + timeout + " seconds")
    }
}