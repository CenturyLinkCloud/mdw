import org.gradle.api.internal.file.FileResolver;
import org.gradle.process.ExecResult;
import org.gradle.process.internal.ExecAction;
import org.gradle.process.internal.ExecHandle;
import org.gradle.process.internal.ExecHandleBuilder;

import org.gradle.api.tasks.TaskAction
import org.gradle.tooling.BuildException
import org.gradle.api.tasks.AbstractExecTask
import org.gradle.process.ExecResult
import org.gradle.process.internal.ExecAction

/**
 * Start a Server.  Successful even if the Server's already running.
 * TODO: Unnecessarily instantiates duplicate objects on superclass.
 */
class ServerStart<T extends AbstractExecTask> extends AbstractExecTask {
    
    private Object verifyUrl
    public void verifyUrl(Object verifyUrl) { this.verifyUrl = verifyUrl; }
    
    private Closure verifyResponseClosure
    public void verifyResponse(Closure verifyResponse) { this.verifyResponseClosure = verifyResponse; }
    
    private int timeout = 90 // seconds
    public void timeout(Object timeout) { this.timeout = Integer.parseInt(timeout.toString()); }
    
    private ExecAction execAction;

    public ServerStart() {
        super(ServerStart.class)
        // execAction = new AsyncExecHandler(getExecActionFactory().getFileResolver())        
    }
    
    public T commandLine(Object... arguments) {
        execAction.commandLine(arguments);
        return super.commandLine(arguments);
    }

    public T commandLine(Iterable<?> args) {
        execAction.commandLine(args);
        return super.commandLine(args);
    }
    
    public T workingDir(Object dir) {
        execAction.workingDir(dir);
        return super.workingDir(dir);
    }
    
    public T setStandardOutput(OutputStream outputStream) {
        execAction.setStandardOutput(outputStream);
        return super.setStandardOutput(outputStream);
    }

    public OutputStream getStandardOutput() {
        return execAction.getStandardOutput();
    }

    public T setErrorOutput(OutputStream outputStream) {
        execAction.setErrorOutput(outputStream);
        return super.setErrorOutput(outputStream);
    }

    public OutputStream getErrorOutput() {
        return execAction.getErrorOutput();
    }
    
    @TaskAction
    protected void exec() {
        if (System.properties["os.name"].startsWith("Windows")) {
            // make sure we'll be running in the background
            if (!getExecutable().equalsIgnoreCase("cmd")) {
                def cmdLine = new ArrayList<String>()
                cmdLine.add("cmd")
                cmdLine.add("/k")
                cmdLine.add(getExecutable())
                cmdLine.addAll(getArgs())
                commandLine cmdLine
            }
        }
        println "Starting server in directory: \"" + getWorkingDir() + "\" with command: \"" + getCommandLine() + "\""
        
        execAction.execute();
        
        if (verifyUrl != null) {
            try {
                verify()
            }
            catch (BuildException ex) {
                throw ex
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
                byte[] buffer = new byte[2048]
                is = conn.getInputStream()
                ByteArrayOutputStream baos = new ByteArrayOutputStream()
                while (true) {
                    int bytesRead = is.read(buffer)
                    if (bytesRead == -1)
                        break;
                    baos.write(buffer, 0, bytesRead)
                }
                if (verifyResponseClosure == null) {
                    return // assume tomcat is up
                }
                else if (verifyResponseClosure(new String(baos.toByteArray()))) {
                    return // tomcat is up
                }
            }
            catch (SocketException ex) {
                // try again until timeout expired
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
        
        throw new RuntimeException("Server does not appear to be running at " + verifyUrl + " after " + timeout + " seconds")
    }

}