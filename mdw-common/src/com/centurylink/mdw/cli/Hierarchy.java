package com.centurylink.mdw.cli;

import com.beust.jcommander.Parameter;
import com.centurylink.mdw.model.workflow.Activity;
import com.centurylink.mdw.model.workflow.LinkedProcess;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.workflow.ProcessHierarchy;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Hierarchy extends Setup {
    @Parameter(names = "--process", description = "Process for which heirarchy will be shown.")
    private String process;
    public String getProcess() {
        return process;
    }
    public void setProcess(String proc) {
        this.process = proc;
    }

    private List<LinkedProcess> topLevelCallers;

    private JSONObject processHierarchy;

    public JSONObject getProcessHierarchy() {
        return processHierarchy;
    }

    Hierarchy() {

    }

    Hierarchy (String process) {
        this.process = process;
    }

    @Override
    public Operation run(ProgressMonitor... monitors) throws IOException {
        topLevelCallers = new ArrayList<LinkedProcess>();
        int index = process.lastIndexOf('/');
        String pkgName = process.substring(0, index);
        String pkgFile = getAssetRoot() + "/" + pkgName.replace('.', '/') + "/";
        String procName = process.substring(index + 1);
        Process proc = new Process(new JSONObject(Files.readAllBytes(Paths.get(pkgFile + procName))));
        proc.setName(procName.substring(0, procName.length()-5));
        proc.setPackageName(pkgName);
        Map<String,File> packageDirs = getAssetPackageDirs();
        Map<String,Properties> versionProps = getVersionProps(packageDirs);
        Properties pkgProps = versionProps.get(pkgName);
        proc.setVersion(Integer.parseInt(pkgProps.getProperty(procName)));
        addTopLevelCallers(proc);
        for (LinkedProcess topLevelCaller : topLevelCallers) {
            addCalledHierarchy(topLevelCaller);
        }
        processHierarchy = new JSONObject();
        processHierarchy.put("hierarchy", new JSONArray());
        populateProcessHierarchy(topLevelCallers, processHierarchy);
        System.out.println(processHierarchy);
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
                processes.add(loadProcess(pkg, procFile, true));
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

    private void addTopLevelCallers(Process called)
            throws IOException {
        List<Process> immediateCallers = findCallingProcesses(called);
        if (immediateCallers.isEmpty()) {
            topLevelCallers.add(new LinkedProcess(called));
        }
        else {
            for (Process caller : immediateCallers)
                addTopLevelCallers(caller);
        }
    }

    private void addCalledHierarchy(LinkedProcess caller)
            throws IOException {
        Process callerProcess = caller.getProcess();
        for (Process calledProcess : findCalledProcesses(callerProcess).toArray(new Process[0])) {
            LinkedProcess child = new LinkedProcess(calledProcess);
            child.setParent(caller);
            caller.getChildren().add(child);
            addCalledHierarchy(child);
        }
    }

    private void populateProcessHierarchy(List<LinkedProcess> processes, JSONObject json) {
        for (LinkedProcess process : processes) {
            Process proc = process.getProcess();
            JSONArray array;
            if (json.has("hierarchy"))
                array = json.getJSONArray("hierarchy");
            else
                array = json.getJSONArray("children");
            JSONObject obj = new JSONObject();
            obj.put("name" , proc.getName());
            obj.put("version", proc.getVersionString());
            array.put(obj);
            if (process.getChildren() != null && process.getChildren().size() > 0) {
                obj.put("children", new JSONArray());
                populateProcessHierarchy(process.getChildren(), obj);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        Hierarchy hierarchy = new Hierarchy();
        hierarchy.setConfigLoc("C:\\workspaces\\mdw6\\mdw\\config");
        hierarchy.setAssetLoc("C:\\workspaces\\mdw6\\mdw-workflow\\assets");
        hierarchy.setProcess("com.centurylink.mdw.tests.workflow/SmartProcessChild.proc");
        hierarchy.run(null);
    }
}
