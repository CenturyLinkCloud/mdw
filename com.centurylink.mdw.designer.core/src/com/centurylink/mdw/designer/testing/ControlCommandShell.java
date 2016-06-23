/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.testing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.designer.DesignerDataAccess;
import com.centurylink.mdw.designer.utils.Server;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.model.value.process.ProcessVO;

/**
 *
 * ControlCommandShell.
 *
 * This class implements the control command shell for MDW.
 * The control commands are used by the regression tester
 * as well as batch testing tools.
 *
 * @version 1.0
 */
public class ControlCommandShell {

    protected static final String ECHO = "echo";
    public static final String SLEEP = "sleep";
    protected static final String IMPORT = "import";
    protected static final String DELETE_PROCESS = "delete_process";
    public static final String MESSAGE = "message";
    protected static final String EXIT = "exit";
    protected static final String LIST_PROCESS = "list_process";
    protected static final String LIST_PACKAGE = "list_package";
    protected static final String EXPORT = "export";

    protected PrintStream log = System.out;
    public void setLog(PrintStream outStream) { this.log = outStream; }

    protected DesignerDataAccess dao;
    protected boolean verbose;
    protected String response;	// response of last "message"

    public ControlCommandShell(DesignerDataAccess dao) {
        this.dao = dao;
        this.verbose = false;
    }

    protected void log_command(TestFileLine line) {
		if (verbose) log.println("... execute line " + line.getLineNumber() + ": " + line.getCommand());
    }

    protected void log_command(String command) {
        if (verbose) log.println("... execute command " + command);
    }

    private void performSleep(TestFileLine line) throws TestException {
    	log_command(line);
        if (line.getWordCount()!=2) {
            throw new TestException(line,"Sleep command needs an argument");
        }
        int seconds = Integer.parseInt(line.getWord(1));
        try {
            Thread.sleep(seconds*1000);
        } catch (InterruptedException e) {
            log.println("Sleep interrupted");
        }
    }

    private void performDeleteProcess(TestFileLine line) throws Exception {
    	log_command(line);
    	String procName = parseProcessName(line.getWord(1));
        int version = parseProcessVersion(line.getWord(1));
        ProcessVO vo;
        try {
            vo = dao.getProcessDefinition(procName, version);
            dao.removeProcess(vo, true);
        } catch (Exception e) {
        	log.println("Failed to delete process " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    public void executeCommand(TestFileLine line) throws Exception {
        String cmd = line.getCommand();
        if (cmd.equalsIgnoreCase(ECHO)) {
            for (int i=1; i<line.getWordCount(); i++) {
                if (i>1) log.print(' ');
                log.print(line.getWord(i));
            }
            log.println();
        } else if (cmd.equalsIgnoreCase(SLEEP)) {
        	performSleep(line);
        } else if (cmd.equalsIgnoreCase(DELETE_PROCESS)) {
            performDeleteProcess(line);
        } else if (cmd.equalsIgnoreCase(MESSAGE)) {
            legacyPerformMessage(line);
        } else if (cmd.equalsIgnoreCase(LIST_PROCESS)) {
            List<ProcessVO> procs = dao.getProcessList(0);
            for (ProcessVO proc : procs) {
                log.format("%d: %s/%s\n", proc.getProcessId(),
                        proc.getProcessName(), proc.getVersionString());
            }
        } else if (cmd.equalsIgnoreCase(LIST_PACKAGE)) {
            List<PackageVO> packages = dao.getPackageList(false);
            for (PackageVO p : packages) {
                log.format("%d: %s/%s\n", p.getPackageId(),
                        p.getPackageName(), p.getVersionString());
            }
        } else {
            if (!cmd.startsWith("#")) {
                log.format("+++++ Unknown command %s\n", cmd);
                log.format("  Available commands are: \n");
                log.format("     echo arg1 ... \n");
                log.format("     sleep seconds\n");
                log.format("     import packge-file-name\n");
                log.format("     delete_process proc-name/version\n");
                log.format("     message [jms/bus/webservice] message-content\n");
                log.format("     list_process\n");
                log.format("     list_package\n");
                log.format("     export package-name/version file-name\n");
                log.format("     exit\n");
            }
        }
    }

    public void execute(TestFile commands) throws Exception {
        for (TestFileLine cmd : commands.getLines()) {
            executeCommand(cmd);
        }
    }

    public String parseProcessName(String name_and_version) {
        int k = name_and_version.indexOf('/');
        if (k>0) return name_and_version.substring(0,k);
        else return name_and_version;
    }

    public int parseProcessVersion(String name_and_version) {
        int version = 0;
        int k = name_and_version.indexOf('/');
        if (k>0) {
            String[] vs = name_and_version.substring(k+1).split("\\.");
            version = Integer.parseInt(vs[0])*1000 + Integer.parseInt(vs[1]);
        }
        return version;
    }

    public void legacyPerformMessage(TestFileLine cmd) throws TestException {
        try {
            String protocol = cmd.getWord(1);
            String message = cmd.getWord(2);
            response = dao.sendMessage(protocol, message, getMessageHeaders());
            log.println("Response: " + response);
        } catch (Exception e) {
            throw new TestException(cmd, "failed to send message", e);
        }
    }

    protected Map<String,String> getMessageHeaders() {
        return null;
    }

    private static void usage() {
        System.out.println("Usage: mdwcl [options] engine-or-db-url [command-file]");
        System.out.println("   Or: mdwcl -c [options] engine-or-db-url command command-arg ...");
        System.out.println("where options are");
        System.out.println("   -c : run a single command");
        System.out.println("   -u<cuid> : user name");
        System.out.println("   -p<password> : pass word");
        System.out.println("The engine url has the format iiop://<host>:<port>");
        System.out.println("The DB url has the format jdbc:oracle:thin:@<host>:<port>:<sid>");
        System.exit(1);
    }

    public static void main(String[] args) throws Exception {
        String serverUrl=null;
        String cuid=null;
        String password=null;
        String command = null;
        boolean singleCommandMode = false;
        int non_options = 0;
        int command_arg_index = 0;
        for (int i=0; i<args.length; i++) {
            if (args[i].length()>=2 && args[i].charAt(0)=='-') {
                switch (args[i].charAt(1)) {
                case 'c': singleCommandMode = true; break;
                case 'u': cuid = args[i].substring(2); break;
                case 'p': password = args[i].substring(2); break;
                default: usage();
                }
            } else {
                non_options++;
                if (non_options==1) serverUrl = args[i];
                else if (non_options==2) {
                    command = args[i];
                    command_arg_index = i+1;
                }
                else {
                    if (singleCommandMode) break;
                    else usage();
                }
            }
        }
        if (serverUrl==null) usage();
        if (singleCommandMode && command==null) usage();
        DesignerDataAccess dao;
        try {
            BufferedReader in = null;
            if (cuid==null) {
                in = new BufferedReader(new InputStreamReader(System.in));
                System.out.print("User ID: ");
                cuid = in.readLine();
            }
            if (password==null) {
                if (in==null) in = new BufferedReader(new InputStreamReader(System.in));
                password = readPassword("Password: ", in);
            }
            Server server = new Server();
            if (serverUrl.startsWith("jdbc:oracle:thin:")) {
                serverUrl = serverUrl.substring(0,17)+cuid+"/"+password+serverUrl.substring(17);
                server.setDatabaseUrl(serverUrl);
                dao = new DesignerDataAccess(server, null, null);
            } else {
                server.setServerUrl(serverUrl);
                if (!password.equals("ignore"))
                    DesignerDataAccess.getAuthenticator().authenticate(cuid, password);
                dao = new DesignerDataAccess(server, null, cuid);
            }
            ControlCommandShell shell = new ControlCommandShell(dao);
            if (singleCommandMode) {
                TestFileLine cmd = new TestFileLine(command);
                for (int i=command_arg_index; i<args.length; i++) {
                    String word = args[i];
                    if (word.charAt(0)=='@') {
                        File file = new File(word.substring(1));
                        char[] parseBuffer;
                        FileReader reader = new FileReader(file);
                        parseBuffer = new char[(int)file.length()];
                        reader.read(parseBuffer);
                        reader.close();
                        word = new String(parseBuffer);
                    }
                    cmd.addWord(word);
                }
                shell.executeCommand(cmd);
            } else if (command==null) {            // interactive mode
                if (in==null) in = new BufferedReader(new InputStreamReader(System.in));
                boolean done = false;
                while (!done) {
                    System.out.print("mdw> ");
                    try {
                        TestFileLine cmd = readLine(in);
                        if (cmd==null || cmd.getWord(0).equals(EXIT)) done = true;
                        else shell.executeCommand(cmd);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {                        // batch mode
                TestFile commands = new TestFile(null, command);
                shell.execute(commands);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static TestFileLine readLine(BufferedReader in)
            throws IOException, ParseException {
        String line = in.readLine();
        if (line==null) return null;
        CommandParser parser = new CommandParser(null);
        List<TestFileLine> lines = parser.parse(line.toCharArray());
        return lines.get(0);
    }

    private static String readPassword(String prompt, BufferedReader in) {
        EraserThread et = new EraserThread(prompt);
        Thread mask = new Thread(et);
        mask.start();

        String password = "";
        try {
           password = in.readLine();
        } catch (IOException ioe) {
          ioe.printStackTrace();
        }
        // stop masking
        et.stopMasking();
        // return the password entered by the user
        return password;
     }

    private static class EraserThread implements Runnable {
       private boolean stop;

       /**
        *@param The prompt displayed to the user
        */
       public EraserThread(String prompt) {
           System.out.print(prompt);
       }

       /**
        * Begin masking...display asterisks (*)
        */
       public void run () {
          stop = true;
          while (stop) {
             System.out.print("\010*");
         try {
            Thread.sleep(1);
             } catch(InterruptedException ie) {
                ie.printStackTrace();
             }
          }
       }

       /**
        * Instruct the thread to stop masking
        */
       public void stopMasking() {
          this.stop = false;
       }
    }




}
