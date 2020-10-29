package com.centurylink.mdw.cli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.centurylink.mdw.git.VersionControlGit;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Uses reflection to avoid hard dependency on JGit.
 */
@Parameters(commandNames="git", commandDescription="Git commands")
public class Git implements Operation {

    public static void main(String[] args) throws IOException {
        JCommander cmd = new JCommander();
        Git git = new Git();
        cmd.addCommand("git", git);
        String[] gitArgs = new String[args.length + 1];
        gitArgs[0] = "git";
        gitArgs[1] = "args";
        boolean showProgress = true;
        for (int i = 1; i < args.length; i++) {
            gitArgs[i + 1] = args[i];
            if ("--no-progress".equals(args[i]))
                showProgress = false;
        }
        cmd.parse(gitArgs);
        git.run(Main.getMonitor(showProgress));
    }

    private VcInfo vcInfo;
    private String command;
    private Object[] params;
    private Object result;
    public Object getResult() { return result; }

    /**
     * Arguments for pass-thru git command.
     */
    @Parameter(names="args", description="Pass-thru jgit arguments", variableArity=true)
    public List<String> args = new ArrayList<>();

    // for CLI
    Git() {
        command = "git";
    }

    public Git(VcInfo vcInfo, String command, Object...params) {
        this.vcInfo = vcInfo;
        this.command = command;
        this.params = params;
    }

    public static List<Dependency> getDependencies() {
        return VcInfo.getDependencies("git");
    }

    @Override
    public Git run(ProgressMonitor... progressMonitors) throws IOException {

        if (command != null) {
            try {
                invokeVersionControl();
            }
            catch (ReflectiveOperationException ex) {
                throw new IOException(ex.getMessage(), ex);
            }
        }

        return this;
    }

    private Class<? extends VersionControlGit> vcClass;
    private VersionControlGit versionControl;
    VersionControlGit getVersionControl() { return versionControl; }

    private void invokeVersionControl()
            throws ReflectiveOperationException, IOException {
        if (vcClass == null) {
            String vcClassName = VcInfo.getVersionControlClass("git");
            vcClass = Class.forName(vcClassName).asSubclass(VersionControlGit.class);
            versionControl = vcClass.newInstance();
            if (!command.equals("git")) {
                versionControl.connect(vcInfo.getUrl(), vcInfo.getUser(), vcInfo.getPassword(), vcInfo.getLocalDir());
                if (!versionControl.exists()) {
                    getOut().println("Git local not found: " + vcInfo.getLocalDir() + " -- cloning from " + vcInfo.getUrl() + "...");
                    vcClass.getMethod("cloneNoCheckout", boolean.class).invoke(versionControl, true);
                }
            }
        }

        // run the requested command
        List<Class<?>> types = new ArrayList<>();
        List<Object> methodArgs = new ArrayList<>();
        if (params != null) {
            for (Object param : params) {
                types.add(param.getClass());
                methodArgs.add(param);
            }
        }
        Method method;
        if (command.equals("git")) {
            method = vcClass.getMethod("git", String[].class);
            result = method.invoke(versionControl, (Object)this.args.toArray(new String[0]));
        }
        else {
            method = vcClass.getMethod(command, types.toArray(new Class<?>[0]));
            result = method.invoke(versionControl, methodArgs.toArray(new Object[0]));
        }
    }

}
