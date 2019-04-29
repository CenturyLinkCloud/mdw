package com.centurylink.mdw.cli;

import com.centurylink.mdw.model.workflow.Activity;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.workflow.ProcessHierarchy;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class Hierarchy extends Setup {

    @Override
    public Operation run(ProgressMonitor... monitors) throws IOException {
        // TODO load process hierarchy

        return this;
    }

    public List<Process> findCallingProcesses(Process subproc, ProgressMonitor... monitors) throws IOException {
        List<Process> callers = new ArrayList<>();
        Map<String,List<File>> packageProcesses = findAllAssets("proc");
        for (String pkg : packageProcesses.keySet()) {
            for (File procFile : packageProcesses.get(pkg)) {
                Process process = loadProcess(pkg, procFile, true);
                for (Activity activity : process.getActivities()) {
                    if (ProcessHierarchy.activityInvokesProcess(activity, subproc) && !callers.contains(process))
                        callers.add(process);
                }
                for (Process embedded : process.getSubprocesses()) {
                    for (Activity activity : embedded.getActivities()) {
                        if (ProcessHierarchy.activityInvokesProcess(activity, subproc) && !callers.contains(process))
                            callers.add(process);
                    }
                }
            }
        }
        return callers;
    }

    /**
     * Only looks in current processes (not archived).
     */
    public List<Process> findCalledProcesses(Process mainproc, ProgressMonitor... monitors) throws IOException {
        List<Process> processes = new ArrayList<>();
        for (String pkg : getPackageProcesses().keySet()) {
            for (File procFile : getPackageProcesses().get(pkg)) {
                processes.add(loadProcess(pkg, procFile, false));
            }
        }
        return ProcessHierarchy.findInvoked(mainproc, processes);
    }

    private Process loadProcess(String pkg, File procFile, boolean deep) throws IOException {
        Properties versionProps = getPackageVersions().get(pkg);
        Process process;
        if (deep) {
            process = new Process(new JSONObject(new String(Files.readAllBytes(procFile.toPath()))));
        }
        else {
            process = new Process();
        }
        int lastDot = procFile.getName().lastIndexOf('.');
        String assetPath = pkg + "/" + procFile.getName();
        process.setName(procFile.getName().substring(0, lastDot));
        process.setPackageName(pkg);
        String verProp = versionProps == null ? null : versionProps.getProperty(procFile.getName());
        process.setVersion(verProp == null ? 0 : Integer.parseInt(verProp.split(" ")[0]));
        process.setId(getAssetId(assetPath, process.getVersion()));
        return process;
    }

    private Map<String,List<File>> packageProcesses;
    private Map<String,List<File>> getPackageProcesses() throws IOException {
        if (packageProcesses == null)
            packageProcesses = findAllAssets("proc");
        return packageProcesses;
    }

    private Map<String,Properties> packageVersions;
    private Map<String,Properties> getPackageVersions() throws IOException {
        if (packageVersions == null)
            packageVersions = getVersionProps(getAssetPackageDirs());
        return packageVersions;
    }
}
