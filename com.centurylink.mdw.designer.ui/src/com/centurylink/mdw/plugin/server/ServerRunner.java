/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.PluginUtil;
import com.centurylink.mdw.plugin.designer.views.JavaProcessConsole;
import com.centurylink.mdw.plugin.preferences.model.PreferenceConstants;
import com.centurylink.mdw.plugin.project.model.ServerSettings;

/**
 * Runs Java Server in a spawned shell and prints output to the dedicated server
 * console implemented as part of this plugin.
 */
public class ServerRunner implements ServerStatusListener {
    private Display display; // the display for user interface thread
    private ServerSettings settings;
    private ServerConfigurator serverConfig;

    private IJavaProject javaProject;

    public IJavaProject getJavaProject() {
        return javaProject;
    }

    public void setJavaProject(IJavaProject javaProject) {
        this.javaProject = javaProject;
    }

    private ServerRunnerMonitor serverRunnerMonitor;

    private ServerConsole console;

    public ServerConsole getConsole() {
        // lazily get a handle to the console
        if (console == null) {
            console = ServerConsole.find();
        }

        return console;
    }

    /**
     * constructor - sets up the standard command options
     *
     * @param settings
     *            - the server settings to use
     * @param display
     *            - display for user interface thread, enables async
     */
    public ServerRunner(ServerSettings serverSettings, Display display) {
        this.settings = serverSettings;
        this.display = display;
        this.serverConfig = ServerConfigurator.Factory.create(settings);
        serverRunnerMonitor = new ServerRunnerMonitor(serverSettings);
    }

    public void start() {
        if (getConsole() == null)
            return;
        getConsole().clear();
        getConsole().setJavaProject(javaProject);
        String javaProjectName = javaProject == null ? "" : javaProject.getProject().getName();
        String serverName = settings.getServerName();
        if (serverName == null)
            getConsole().setStartText("Start " + javaProjectName);
        else
            getConsole().setStartText("Start " + settings.getServerName());
        getConsole().setHasClientShell(serverConfig.hasClientShell());
        getConsole().setDeployText("Deploy " + javaProjectName);

        String msg = "Starting " + settings.getContainerName();
        if (serverName != null)
            msg += " " + serverName;
        if (javaProject != null)
            msg += " with " + javaProjectName;
        getConsole().print(msg + PluginUtil.getLineDelimiter(),
                JavaProcessConsole.LINE_TYPE_STATUS);
        PluginMessages.log(msg + " with Command:\n" + serverConfig.getStartCommand(), IStatus.INFO);
        Runner runner = new Runner(serverConfig.getStartCommand());
        runner.setStarting(true);
        new Thread(runner).start();
        serverRunnerMonitor.startup();
    }

    public void stop() {
        new Thread(new Runner(serverConfig.getStopCommand())).start();
        getConsole().print("Stopping " + settings.getContainerName() + " Server"
                + PluginUtil.getLineDelimiter(), JavaProcessConsole.LINE_TYPE_STATUS);
    }

    public static boolean isServerRunning() {
        IPreferenceStore store = MdwPlugin.getDefault().getPreferenceStore();
        return store.getBoolean(PreferenceConstants.PREFS_SERVER_RUNNING);
    }

    /**
     * runs the command to start or stop the server
     */
    class Runner implements Runnable {
        private boolean startingServer;

        void setStarting(boolean b) {
            startingServer = b;
        }

        private IWindowListener windowListener;
        private String command;

        public Runner(String command) {
            this.command = command;
        }

        public void run() {
            Runtime rt = Runtime.getRuntime();
            try {
                Process p = null;

                if (startingServer) {
                    if (windowListener != null)
                        PlatformUI.getWorkbench().removeWindowListener(windowListener);
                    windowListener = new ServerRunnerWindowListener();
                    PlatformUI.getWorkbench().addWindowListener(windowListener);
                    IPreferenceStore store = MdwPlugin.getDefault().getPreferenceStore();
                    // cycle if in an inconsistent state to synchronize
                    if (store.getBoolean(PreferenceConstants.PREFS_SERVER_RUNNING))
                        store.setValue(PreferenceConstants.PREFS_SERVER_RUNNING, false);
                    store.setValue(PreferenceConstants.PREFS_SERVER_RUNNING, true);
                    p = rt.exec(command, serverConfig.getEnvironment(true),
                            new File(serverConfig.getCommandDir()));
                }
                else {
                    p = rt.exec(command, serverConfig.getEnvironment(false),
                            new File(serverConfig.getCommandDir()));
                }

                Reader stdOutReader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                ProcInputHandler stdOut = new ProcInputHandler(stdOutReader,
                        JavaProcessConsole.LINE_TYPE_NORMAL);
                Thread tStdOut = new Thread(stdOut);
                tStdOut.start();

                Reader stdErrReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                ProcInputHandler stdErr = new ProcInputHandler(stdErrReader,
                        JavaProcessConsole.LINE_TYPE_ERROR);
                Thread tStdErr = new Thread(stdErr);
                tStdErr.setPriority(Thread.MAX_PRIORITY);
                tStdErr.start();

                p.waitFor();
                stdOutReader.close();
                stdErrReader.close();

                if (startingServer) {
                    PlatformUI.getWorkbench().removeWindowListener(windowListener);
                    windowListener = null;
                    final String msg = "Server process exited with code: " + p.exitValue();
                    if (getConsole().isActive())
                        display.syncExec(new Runnable() {
                            public void run() {
                                if (getConsole().isActive())
                                    getConsole().print(msg + PluginUtil.getLineDelimiter(),
                                            JavaProcessConsole.LINE_TYPE_STATUS);
                            }
                        });
                    IPreferenceStore store = MdwPlugin.getDefault().getPreferenceStore();
                    // cycle if in an inconsistent state to synchronize
                    if (!store.getBoolean(PreferenceConstants.PREFS_SERVER_RUNNING))
                        store.setValue(PreferenceConstants.PREFS_SERVER_RUNNING, true);
                    store.setValue(PreferenceConstants.PREFS_SERVER_RUNNING, false);
                }
            }
            catch (Exception ex) {
                PluginMessages.log(ex);
            }
        }
    }

    /**
     * inner class for handling process input
     */
    class ProcInputHandler implements Runnable {
        private Reader reader;
        private int lineType;

        public ProcInputHandler(Reader reader, int lineType) {
            this.reader = reader;
            this.lineType = lineType;
        }

        StringBuffer line = new StringBuffer(128);

        public void run() {
            try {
                char c = 0;
                while (c != -1) {
                    c = (char) reader.read();
                    line.append(c);
                    if (c == '\n') {
                        final String toPrint = line.toString();
                        if (!display.isDisposed()) {
                            display.asyncExec(new Runnable() {
                                public void run() {
                                    if (getConsole().isActive())
                                        getConsole().print(toPrint, lineType);
                                }
                            });
                        }
                        line = new StringBuffer(128);
                    }
                }
            }
            catch (IOException ex) {
                // PluginMessages.log(ex);
                ex.printStackTrace(); // avoid Error Log entry
            }
        }
    }

    /**
     * inner class for hooking eclipse shutdown event
     */
    class ServerRunnerWindowListener implements IWindowListener {
        public void windowActivated(IWorkbenchWindow window) {
        }

        public void windowDeactivated(IWorkbenchWindow window) {
        }

        public void windowOpened(IWorkbenchWindow window) {
        }

        public void windowClosed(IWorkbenchWindow window) {
            IWorkbenchWindow windows[] = PlatformUI.getWorkbench().getWorkbenchWindows();

            if (windows.length > 1) {
                // something still open
                return;
            }

            PlatformUI.getWorkbench().removeWindowListener(this); // avoid
                                                                  // duplicate
                                                                  // notices

            String msg = settings.getContainerName() + " appears to be still running.\n\n"
                    + "Would you like to stop the server before exiting Eclipse?";
            boolean kill = MessageDialog.openQuestion(window.getShell(), "MDW Server Runner", msg);
            if (kill) {
                Runtime rt = Runtime.getRuntime();
                String command = serverConfig.getStopCommand();
                try {
                    Process p = rt.exec(command, serverConfig.getEnvironment(false),
                            new File(serverConfig.getCommandDir()));

                    final BufferedReader stdOutReader = new BufferedReader(
                            new InputStreamReader(p.getInputStream()));
                    final BufferedReader stdErrReader = new BufferedReader(
                            new InputStreamReader(p.getErrorStream()));

                    new Thread(new Runnable() {
                        public void run() {
                            String line = null;
                            try {
                                while ((line = stdOutReader.readLine()) != null) {
                                    PluginMessages.log(line);
                                }
                            }
                            catch (IOException ex) {
                                PluginMessages.log(ex);
                            }
                        }
                    }).start();

                    new Thread(new Runnable() {
                        public void run() {
                            String line = null;
                            try {
                                while ((line = stdErrReader.readLine()) != null) {
                                    PluginMessages.log(line);
                                }
                            }
                            catch (IOException ex) {
                                PluginMessages.log(ex);
                            }
                        }
                    }).start();

                    p.waitFor();

                    stdOutReader.close();
                    stdErrReader.close();

                    IPreferenceStore store = MdwPlugin.getDefault().getPreferenceStore();
                    store.setValue(PreferenceConstants.PREFS_SERVER_RUNNING, false);
                }
                catch (Exception ex) {
                    PluginMessages.log(ex);
                }
            }
        }
    }

    public void statusChanged(String newStatus) {
        // TODO Auto-generated method stub

    }

}