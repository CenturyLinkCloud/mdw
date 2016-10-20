/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.dataaccess.file;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.activity.types.TaskActivity;
import com.centurylink.mdw.bpm.ActivityImplementorDocument;
import com.centurylink.mdw.bpm.ExternalEventHandlerDocument;
import com.centurylink.mdw.bpm.MDWActivityImplementor;
import com.centurylink.mdw.bpm.MDWExternalEvent;
import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.constant.TaskAttributeConstant;
import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.timer.ProgressMonitor;
import com.centurylink.mdw.dataaccess.AssetRevision;
import com.centurylink.mdw.dataaccess.BaselineData;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.ProcessLoader;
import com.centurylink.mdw.dataaccess.ProcessPersister;
import com.centurylink.mdw.dataaccess.VersionControl;
import com.centurylink.mdw.model.data.monitor.ServiceLevelAgreement;
import com.centurylink.mdw.model.data.task.TaskCategory;
import com.centurylink.mdw.model.value.activity.ActivityImplementorVO;
import com.centurylink.mdw.model.value.activity.ActivityVO;
import com.centurylink.mdw.model.value.attribute.AttributeVO;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.event.ExternalEventVO;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.model.value.variable.VariableTypeVO;
import com.centurylink.mdw.model.value.work.WorkTransitionVO;
import com.centurylink.mdw.task.TaskTemplate;
import com.centurylink.mdw.task.TaskTemplateDocument;

// TODO clear VersionControl & PackageDir/AssetFile caches on Cache Refresh.
public class LoaderPersisterVcs implements ProcessLoader, ProcessPersister {

    public static final String MDW_BASE_PACKAGE = "com.centurylink.mdw.base";
    public static final String PROCESS_FILE_EXTENSION = ".proc";
    public static final String IMPL_FILE_EXTENSION = ".impl";
    public static final String EVT_HANDLER_FILE_EXTENSION = ".evth";
    public static final String TASK_TEMPLATE_FILE_EXTENSION = ".task";

    private String user;
    private File storageDir;
    public File getStorageDir() { return storageDir; }
    private File archiveDir;
    private FileFilter pkgDirFilter;
    private FileFilter mdwDirFilter;
    private FileFilter subDirFilter;
    private FileFilter procFileFilter;
    private FileFilter ruleSetFileFilter;
    private FileFilter implFileFilter;
    private FileFilter evthFileFilter;
    private FileFilter taskFileFilter;
    private List<PackageDir> pkgDirs;
    private Comparator<PackageDir> pkgDirComparator;
    private BaselineData baselineData;

    private VersionControl versionControl;
    public VersionControl getVersionControl() { return versionControl; }

    public LoaderPersisterVcs(String cuid, File directory, VersionControl versionControl, BaselineData baselineData) {
        this.user = cuid;  // TODO
        this.storageDir = directory;
        if (storageDir.toString().charAt(1) == ':') // windows: avoid potential drive letter case mismatch
            storageDir = new File(Character.toLowerCase(storageDir.toString().charAt(0)) + storageDir.toString().substring(1));
        archiveDir = new File(storageDir + "/" + PackageDir.ARCHIVE_SUBDIR);
        this.versionControl = versionControl;  // should already be connected?
        this.baselineData = baselineData;

        this.mdwDirFilter = new FileFilter() {
            public boolean accept(File file) {
                return file.isDirectory() && file.getName().equals(".mdw");
            }
        };
        this.pkgDirFilter = new FileFilter() {
            public boolean accept(File file) {
                if (file.isDirectory()) {
                    return file.listFiles(mdwDirFilter).length == 1;
                }
                return false;
            }
        };
        this.subDirFilter = new FileFilter() {
            public boolean accept(File file) {
                return file.isDirectory() && !file.getName().equals(".mdw");
            }
        };
        this.procFileFilter = new FileFilter() {
            public boolean accept(File file) {
                return file.isFile() && file.getName().endsWith(PROCESS_FILE_EXTENSION);
            }
        };
        this.ruleSetFileFilter = new FileFilter() {
            public boolean accept(File file) {
                return file.isFile() && !file.getName().endsWith(IMPL_FILE_EXTENSION) && !file.getName().endsWith(EVT_HANDLER_FILE_EXTENSION)
                        && !file.getName().endsWith(TASK_TEMPLATE_FILE_EXTENSION) && !file.getName().endsWith(PROCESS_FILE_EXTENSION);
            }
        };
        this.implFileFilter = new FileFilter() {
            public boolean accept(File file) {
                return file.isFile() && file.getName().endsWith(IMPL_FILE_EXTENSION);
            }
        };
        this.evthFileFilter = new FileFilter() {
            public boolean accept(File file) {
                return file.isFile() && file.getName().endsWith(EVT_HANDLER_FILE_EXTENSION);
            }
        };
        this.taskFileFilter = new FileFilter() {
            public boolean accept(File file) {
                return file.isFile() && file.getName().endsWith(TASK_TEMPLATE_FILE_EXTENSION);
            }
        };

        this.pkgDirComparator = new Comparator<PackageDir>() {
            public int compare(PackageDir d1, PackageDir d2) {
                // prefer non-archived packages over archived
                // prefer MDW base package in case of duplicate assets
                if (d1.isArchive() && !d2.isArchive())
                    return 1;
                else if (d2.isArchive() && !d1.isArchive())
                    return -1;
                else if (d1.getName().equals(MDW_BASE_PACKAGE))
                    return -1;
                else if (d2.getName().equals(MDW_BASE_PACKAGE))
                    return 1;
                else
                    return d1.getName().compareTo(d2.getName());
            }
        };
    }

    protected PackageDir createPackage(PackageVO packageVo) throws DataAccessException, IOException {
        PackageDir pkgDir = new PackageDir(storageDir, packageVo, versionControl);
        getPackageDirs().add(0, pkgDir);
        return pkgDir;
    }

    public List<PackageDir> getPackageDirs() throws DataAccessException {
        return getPackageDirs(true);
    }

    public synchronized List<PackageDir> getPackageDirs(boolean includeArchive) throws DataAccessException {
        if (pkgDirs == null) {
            if (!storageDir.exists() || !storageDir.isDirectory())
                throw new DataAccessException("Directory does not exist: " + storageDir);
            pkgDirs = new ArrayList<PackageDir>();
            for (File pkgNode : getPkgDirFiles(storageDir, includeArchive)) {
                PackageDir pkgDir = new PackageDir(storageDir, pkgNode, versionControl);
                pkgDir.parse();
                pkgDirs.add(pkgDir);
            }
            Collections.sort(pkgDirs, pkgDirComparator);
        }
        return pkgDirs;
    }

    protected List<File> getPkgDirFiles(File parentDir) {
        return getPkgDirFiles(parentDir, true);
    }

    /**
     * For recursively finding package directories based on the filter.
     */
    protected List<File> getPkgDirFiles(File parentDir, boolean includeArchive) {
        List<File> pkgDirFiles = new ArrayList<File>();
        for (File pkgDirFile : parentDir.listFiles(pkgDirFilter))
            pkgDirFiles.add(pkgDirFile);
        for (File subDir : parentDir.listFiles(subDirFilter)) {
            if (includeArchive || !subDir.equals(archiveDir))
                pkgDirFiles.addAll(getPkgDirFiles(subDir));
        }
        return pkgDirFiles;
    }

    protected PackageDir getPackageDir(long packageId) throws DataAccessException {
        for (PackageDir pkgDir : getPackageDirs()) {
            if (pkgDir.getId() == packageId)
                return pkgDir;
        }
        return null;
    }

    protected PackageDir getPackageDir(File logicalAssetFile) throws DataAccessException {
        File logicalDir = logicalAssetFile.getParentFile();
        for (PackageDir pkgDir : getPackageDirs()) {
            if (pkgDir.getPackageName().equals(logicalDir.getName()) && pkgDir.findAssetFile(logicalAssetFile) != null)
                return pkgDir;
        }
        return null;
    }

    public PackageDir getTopLevelPackageDir(String name) throws DataAccessException {
        for (PackageDir pkgDir : getPackageDirs()) {
            if (pkgDir.getPackageName().equals(name) && !pkgDir.isArchive())
                return pkgDir;
        }
        return null;
    }

    protected byte[] read(File file) throws IOException {
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

    protected void write(byte[] contents, File file) throws IOException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            fos.write(contents);
        }
        catch (FileNotFoundException ex) {
            // dimensions annoyingly makes files read-only
            file.setWritable(true);
            fos = new FileOutputStream(file);
            fos.write(contents);
        }
        finally {
            if (fos != null)
                fos.close();
        }
    }

    protected void rename(File oldFile, File newFile) throws IOException {
        if (!oldFile.renameTo(newFile))
            throw new IOException("Unable to rename: " + oldFile);
    }

    protected void copy(File oldFile, File newFile) throws IOException {
        if (oldFile.isDirectory()) {
            if (!newFile.mkdirs())
                throw new IOException("Unable to create directory: " + newFile);
            for (File oldSub : oldFile.listFiles())
                copy(oldSub, new File(newFile + "/" + oldSub.getName()));
        }
        else {
            write(read(oldFile), newFile);
        }
    }


    /**
     * ignores subdirectories except for .mdw
     */
    public void copyPkg(File fromPkgDir, File toPkgDir) throws IOException {
            if (!toPkgDir.mkdirs())
                throw new IOException("Unable to create directory: " + toPkgDir);
            for (File oldSub : fromPkgDir.listFiles()) {
                if (oldSub.isDirectory() && oldSub.getName().equals(".mdw"))
                    copy(oldSub, new File(toPkgDir + "/" + oldSub.getName()));
                else if (oldSub.isFile())
                    write(read(oldSub), new File(toPkgDir + "/" + oldSub.getName()));
            }
    }

    /**
     * translates dots to directories
     */
    public File renamePkgDir(File oldPkgDir, File newPkg) throws IOException {
        if (newPkg.getName().contains(".")) {
            File newPkgDir = new File(newPkg.getParent() + "/" + newPkg.getName().replace('.', '/'));
            copyPkg(oldPkgDir, newPkgDir);
            delete(new File(oldPkgDir + "/.mdw")); // to be safe just delete the metainfo
            return newPkgDir;
        }
        else {
            rename(oldPkgDir, newPkg);
            return newPkg;
        }
    }

    public void delete(File file) throws IOException {
        if (file.isDirectory()) {
            for (File child : file.listFiles())
                delete(child);
        }
        if (!file.delete())
            throw new IOException("Unable to delete: " + file);
    }

    /**
     * Ignores subdirectories except for .mdw
     */
    public void deletePkg(File pkgDir) throws IOException {
        for (File child : pkgDir.listFiles()) {
            if (child.isDirectory() && child.getName().equals(".mdw"))
                delete(child);
            else if (child.isFile())
                child.delete();
        }
        if (pkgDir.listFiles().length == 0)
            pkgDir.delete();
    }

    private XmlOptions xmlOptions;
    protected XmlOptions getXmlOptions() {
        if (xmlOptions == null) {
            xmlOptions = new XmlOptions().setSavePrettyPrint().setSavePrettyPrintIndent(2);
        }
        return xmlOptions;
    }

    // FileSystemAccess methods

    public PackageVO loadPackage(PackageDir pkgDir, boolean deep) throws IOException, XmlException, JSONException, DataAccessException {
        PackageVO packageVO = new PackageVO();

        packageVO.setName(pkgDir.getPackageName());
        packageVO.setVersion(PackageVO.parseVersion(pkgDir.getPackageVersion()));
        packageVO.setId(versionControl.getId(pkgDir.getLogicalDir()));
        packageVO.setSchemaVersion(DataAccess.currentSchemaVersion);

        String pkgJson = new String(read(pkgDir.getMetaFile()));
        PackageVO jsonPkg = new PackageVO(new JSONObject(pkgJson));
        packageVO.setGroup(jsonPkg.getGroup());
        packageVO.setAttributes(jsonPkg.getAttributes());
        packageVO.setMetaContent(pkgJson);

        packageVO.setProcesses(loadProcesses(pkgDir, deep));
        packageVO.setRuleSets(loadRuleSets(pkgDir, deep));
        packageVO.setImplementors(loadActivityImplementors(pkgDir));
        packageVO.setExternalEvents(loadExternalEventHandlers(pkgDir));
        packageVO.setTaskTemplates(loadTaskTemplates(pkgDir));

        packageVO.setArchived(pkgDir.isArchive());

        return packageVO;
    }

    public long save(PackageVO packageVO, PackageDir pkgDir, boolean deep) throws IOException, XmlException, JSONException, DataAccessException {
        File mdwDir = new File(pkgDir + "/.mdw");
        if (!mdwDir.exists()) {
            if (!mdwDir.mkdirs())
                throw new IOException("Unable to create metadata directory under: " + pkgDir);
        }

        String pkgContent = packageVO.getJson(false).toString(2);
        write(pkgContent.getBytes(), pkgDir.getMetaFile());

        packageVO.setId(versionControl.getId(pkgDir.getLogicalDir()));

        if (deep) {
            saveActivityImplementors(packageVO, pkgDir);
            saveExternalEventHandlers(packageVO, pkgDir);
            saveRuleSets(packageVO, pkgDir);
            saveTaskTemplates(packageVO, pkgDir);
            saveProcesses(packageVO, pkgDir); // also saves v0 task templates
        }

        return packageVO.getPackageId();
    }

    public ProcessVO loadProcess(PackageDir pkgDir, AssetFile assetFile, boolean deep) throws IOException, XmlException, JSONException, DataAccessException {
        ProcessVO process;
        if (deep) {
            String content = new String(read(assetFile));
            process = new ProcessVO(new JSONObject(content));
            Long loadId = process.getProcessId();
            WorkTransitionVO obsoleteStartTransition = null;
            for (WorkTransitionVO t : process.getTransitions()) {
                if (t.getFromWorkId().equals(loadId)) {
                    obsoleteStartTransition = t;
                    break;
                }
            }
            if (obsoleteStartTransition != null)
                process.getTransitions().remove(obsoleteStartTransition);
        }
        else {
            process = new ProcessVO();
        }

        process.setId(assetFile.getId());
        int lastDot = assetFile.getName().lastIndexOf('.');
        process.setName(assetFile.getName().substring(0, lastDot));
        process.setLanguage(RuleSetVO.PROCESS);
        process.setInRuleSet(true);
        process.setRawFile(assetFile);
        process.setVersion(assetFile.getRevision().getVersion());  // TODO remove version from process XML for file-persist
        process.setModifyDate(assetFile.getRevision().getModDate());
        process.setModifyingUser(assetFile.getRevision().getModUser());
        process.setRevisionComment(assetFile.getRevision().getComment());
        process.setPackageName(pkgDir.getPackageName());
        process.setPackageVersion(pkgDir.getPackageVersion());

        return process;
    }

    public long save(ProcessVO process, PackageDir pkgDir) throws IOException, XmlException, JSONException, DataAccessException {
        process.removeEmptyAndOverrideAttributes();
        // save task templates
        List<ActivityImplementorVO> impls = getActivityImplementors();  // TODO maybe cache these
        for (ActivityVO activity : process.getActivities()) {
            for (ActivityImplementorVO impl : impls) {
                if (activity.getImplementorClassName().equals(impl.getImplementorClassName())) {
                    if (impl.isManualTask()) {
                        if (activity.getAttribute(TaskActivity.ATTRIBUTE_TASK_TEMPLATE) != null) {
                            removeObsoleteTaskActivityAttributes(activity);
                        }
                        else {
                            // create the task template from activity attributes
                            TaskVO taskVo = getTask(process.getProcessName(), activity);
                            taskVo.setPackageName(process.getPackageName());
                            save(taskVo, pkgDir);
                        }
                    }
                }
            }
        }
        if (process.getSubProcesses() != null) {
            for (ProcessVO embedded : process.getSubProcesses()) {
                for (ActivityVO activity : embedded.getActivities()) {
                    for (ActivityImplementorVO impl : impls) {
                        if (activity.getImplementorClassName().equals(impl.getImplementorClassName())) {
                            if (impl.isManualTask()) {
                                if (activity.getAttribute(TaskActivity.ATTRIBUTE_TASK_TEMPLATE) != null) {
                                    removeObsoleteTaskActivityAttributes(activity);
                                }
                                else {
                                    // create the task template from activity attributes
                                    TaskVO taskVo = getTask(process.getProcessName(), activity);
                                    taskVo.setPackageName(process.getPackageName());
                                    save(taskVo, pkgDir);
                                }
                            }
                        }
                    }
                }
            }
        }

        String content = process.getJson().toString(2);
        AssetFile assetFile = pkgDir.getAssetFile(getProcessFile(process), getAssetRevision(process));
        write(content.getBytes(), assetFile);
        process.setId(versionControl.getId(assetFile.getLogicalFile()));
        process.setVersion(assetFile.getRevision().getVersion());
        process.setModifyingUser(assetFile.getRevision().getModUser());
        process.setModifyDate(assetFile.getRevision().getModDate());
        process.setComment(assetFile.getRevision().getComment());
        process.setPackageName(pkgDir.getPackageName());
        process.setRawFile(assetFile);

        return process.getId();
    }

    public RuleSetVO loadRuleSet(PackageDir pkgDir, AssetFile assetFile, boolean deep) throws IOException, XmlException {
        RuleSetVO ruleSet = new RuleSetVO();
        ruleSet.setId(assetFile.getId());
        ruleSet.setPackageName(pkgDir.getPackageName());
        ruleSet.setPackageVersion(pkgDir.getPackageVersion());
        ruleSet.setName(assetFile.getName());
        ruleSet.setLanguage(RuleSetVO.getFormat(assetFile.getName()));
        ruleSet.setVersion(assetFile.getRevision().getVersion());
        ruleSet.setLoadDate(new Date());
        ruleSet.setModifyDate(assetFile.getRevision().getModDate());
        ruleSet.setRevisionComment(assetFile.getRevision().getComment());
        ruleSet.setRawFile(assetFile);
        if (deep) {
            ruleSet.setRaw(true);
            // do not load jar assets into memory
            if (!RuleSetVO.excludedFromMemoryCache(assetFile.getName()))
                ruleSet.setRawContent(read(assetFile));
        }
        return ruleSet;
    }

    public long save(RuleSetVO ruleSet, PackageDir pkgDir) throws IOException {
        AssetFile assetFile = pkgDir.getAssetFile(getRuleSetFile(ruleSet), getAssetRevision(ruleSet));
        write(ruleSet.getContent(), assetFile);
        ruleSet.setId(versionControl.getId(assetFile.getLogicalFile()));
        ruleSet.setVersion(assetFile.getRevision().getVersion());
        ruleSet.setModifyingUser(assetFile.getRevision().getModUser());
        ruleSet.setModifyDate(assetFile.getRevision().getModDate());
        ruleSet.setRevisionComment(assetFile.getRevision().getComment());
        ruleSet.setPackageName(pkgDir.getPackageName());
        ruleSet.setRawFile(assetFile);

        return ruleSet.getId();
    }

    public ActivityImplementorVO loadActivityImplementor(PackageDir pkgDir, AssetFile assetFile) throws IOException, XmlException, JSONException {
        String content = new String(read(assetFile));
        ActivityImplementorVO implVo;
        if (content.trim().startsWith("{")) {
            implVo = new ActivityImplementorVO(new JSONObject(content));
        }
        else {
            ActivityImplementorDocument doc = ActivityImplementorDocument.Factory.parse(content);
            MDWActivityImplementor impl = doc.getActivityImplementor();
            implVo = new ActivityImplementorVO();
            implVo.setImplementorClassName(impl.getImplementation());
            implVo.setBaseClassName(impl.getType());
            implVo.setLabel(impl.getLabel());
            implVo.setIconName(impl.getIconFile());
            implVo.setAttributeDescription(impl.getAttributeDescription());
            implVo.setHidden(impl.getHidden());
            implVo.setShowInToolbox(!impl.getHidden());
        }
        implVo.setImplementorId(assetFile.getId());
        implVo.setPackageName(pkgDir.getPackageName());
        return implVo;
    }

    public long save(ActivityImplementorVO implVo, PackageDir pkgDir) throws IOException, JSONException {
        String content = implVo.getJson().toString(2);
        AssetFile assetFile = pkgDir.getAssetFile(getActivityImplementorFile(implVo), null); // no revs
        write(content.getBytes(), assetFile);
        implVo.setImplementorId(versionControl.getId(assetFile.getLogicalFile()));
        return implVo.getImplementorId();
    }

    public ExternalEventVO loadExternalEventHandler(PackageDir pkgDir, AssetFile assetFile) throws IOException, XmlException, JSONException {
        String content = new String(read(assetFile));
        ExternalEventVO evthVo;
        if (content.trim().startsWith("{")) {
            evthVo = new ExternalEventVO(new JSONObject(content));
        }
        else {
            ExternalEventHandlerDocument doc = ExternalEventHandlerDocument.Factory.parse(content);
            MDWExternalEvent evth = doc.getExternalEventHandler();
            evthVo = new ExternalEventVO();
            evthVo.setEventHandler(evth.getEventHandler());
            evthVo.setEventName(evth.getEventName());
        }
        evthVo.setId(assetFile.getId());
        evthVo.setPackageName(pkgDir.getPackageName());
        return evthVo;
    }

    public long save(ExternalEventVO evthVo, PackageDir pkgDir) throws IOException, JSONException {
        String content = evthVo.getJson().toString(2);
        AssetFile assetFile = pkgDir.getAssetFile(getExternalEventHandlerFile(evthVo), null); // no revs
        write(content.getBytes(), assetFile);
        evthVo.setId(versionControl.getId(assetFile.getLogicalFile()));
        return evthVo.getId();
    }

    public TaskVO loadTaskTemplate(PackageDir pkgDir, AssetFile assetFile) throws IOException, XmlException, JSONException {
        String content = new String(read(assetFile));
        TaskVO taskVO;
        if (content.trim().startsWith("{")) {
            taskVO = new TaskVO(new JSONObject(content));
        }
        else {
            TaskTemplateDocument doc = TaskTemplateDocument.Factory.parse(content);
            TaskTemplate taskTemplate = doc.getTaskTemplate();
            taskVO = new TaskVO(taskTemplate);
        }
        taskVO.setName(assetFile.getName());
        taskVO.setTaskId(assetFile.getId());
        taskVO.setPackageName(pkgDir.getPackageName());
        taskVO.setVersion(assetFile.getRevision().getVersion());
        return taskVO;
    }

    public long save(TaskVO taskVo, PackageDir pkgDir) throws IOException, JSONException {
        String content = taskVo.getJson().toString(2);
        AssetFile assetFile = pkgDir.getAssetFile(getTaskTemplateFile(taskVo), taskVo.getVersion() > 0 ? getAssetRevision(taskVo) : null);
        write(content.getBytes(), assetFile);
        taskVo.setTaskId(versionControl.getId(assetFile.getLogicalFile()));
        return taskVo.getTaskId();
    }

    public List<ProcessVO> loadProcesses(PackageDir pkgDir, boolean deep) throws IOException, XmlException, JSONException, DataAccessException {
        List<ProcessVO> processes = new ArrayList<ProcessVO>();
        for (File procFile : pkgDir.listFiles(procFileFilter))
            processes.add(loadProcess(pkgDir, pkgDir.getAssetFile(procFile), deep));
        return processes;
    }

    public void saveProcesses(PackageVO packageVo, PackageDir pkgDir) throws IOException, XmlException, JSONException, DataAccessException {
        if (packageVo.getProcesses() != null) {
            for (ProcessVO process : packageVo.getProcesses()) {
                process.setPackageName(packageVo.getName());
                save(process, pkgDir);
            }
        }
    }

    public List<RuleSetVO> loadRuleSets(PackageDir pkgDir, boolean deep) throws IOException, XmlException {
        List<RuleSetVO> ruleSets = new ArrayList<RuleSetVO>();
        for (File rsFile : pkgDir.listFiles(ruleSetFileFilter))
            ruleSets.add(loadRuleSet(pkgDir, pkgDir.getAssetFile(rsFile), deep));
        Collections.sort(ruleSets, new Comparator<RuleSetVO>() {
            public int compare(RuleSetVO rs1, RuleSetVO rs2) {
                if (rs1.getName().equals(rs2.getName()))
                    return rs2.getVersion() - rs1.getVersion(); // later versions first
                else
                    return rs1.getName().compareToIgnoreCase(rs2.getName()); // alphabetically by name
            }
        });
        return ruleSets;
    }

    public void saveRuleSets(PackageVO packageVo, PackageDir pkgDir) throws IOException {
        // TODO custom attributes
        if (packageVo.getRuleSets() != null) {
            for (RuleSetVO ruleSet : packageVo.getRuleSets()) {
                if (!ruleSet.isEmpty()) {
                    ruleSet.setPackageName(packageVo.getName());
                    save(ruleSet, pkgDir);
                }
            }
        }
    }

    public List<ActivityImplementorVO> loadActivityImplementors(PackageDir pkgDir) throws IOException, XmlException, JSONException {
        List<ActivityImplementorVO> impls = new ArrayList<ActivityImplementorVO>();
        for (File implFile : pkgDir.listFiles(implFileFilter))
            impls.add(loadActivityImplementor(pkgDir, pkgDir.getAssetFile(implFile)));
        return impls;
    }

    public void saveActivityImplementors(PackageVO packageVo, PackageDir pkgDir) throws IOException, XmlException, JSONException, DataAccessException {
        if (packageVo.getImplementors() != null && !packageVo.getImplementors().isEmpty()) {
            List<ActivityImplementorVO> existingImpls = new ArrayList<ActivityImplementorVO>();
            for (PackageDir existPkgDir : getPackageDirs()) {
                if (!existPkgDir.isArchive())
                    existingImpls.addAll(loadActivityImplementors(existPkgDir));
            }
            for (ActivityImplementorVO impl : packageVo.getImplementors()) {
                boolean alreadyPresentInAnotherPackage = false;
                for (ActivityImplementorVO existingImpl : existingImpls) {
                    if (existingImpl.getImplementorClassName().equals(impl.getImplementorClassName())) {
                        alreadyPresentInAnotherPackage = true;
                        break;
                    }
                }
                // silently fail to save implementor if it already exists in another package
                if (!alreadyPresentInAnotherPackage) {
                    impl.setPackageName(pkgDir.getPackageName());
                    save(impl, pkgDir);
                }
            }
        }
    }

    public List<ExternalEventVO> loadExternalEventHandlers(PackageDir pkgDir) throws IOException, XmlException, JSONException {
        List<ExternalEventVO> evtHandlers = new ArrayList<ExternalEventVO>();
        for (File evthFile : pkgDir.listFiles(evthFileFilter))
            evtHandlers.add(loadExternalEventHandler(pkgDir, pkgDir.getAssetFile(evthFile)));
        return evtHandlers;
    }

    public void saveExternalEventHandlers(PackageVO packageVo, PackageDir pkgDir) throws IOException, JSONException {
        if (packageVo.getExternalEvents() != null) {
            for (ExternalEventVO evth : packageVo.getExternalEvents()) {
                evth.setPackageName(pkgDir.getPackageName());
                save(evth, pkgDir);
            }
        }
    }

    public List<TaskVO> loadTaskTemplates(PackageDir pkgDir) throws IOException, XmlException, JSONException {
        List<TaskVO> tasks = new ArrayList<TaskVO>();
        for (File taskFile : pkgDir.listFiles(taskFileFilter))
            tasks.add(loadTaskTemplate(pkgDir, pkgDir.getAssetFile(taskFile)));
        return tasks;
    }

    public void saveTaskTemplates(PackageVO packageVo, PackageDir pkgDir) throws IOException, JSONException {
        if (packageVo.getTaskTemplates() != null) {
            for (TaskVO task : packageVo.getTaskTemplates()) {
                task.setPackageName(pkgDir.getPackageName());
                save(task, pkgDir);
            }
        }
    }

    /**
     * Only for top-level packages, and relies on getPackageName().
     */
    private File getProcessFile(ProcessVO process) {
        String fileName = process.getName() + PROCESS_FILE_EXTENSION;
        return new File(storageDir + "/" + process.getPackageName().replace('.', '/') + "/" + fileName);
    }

    /**
     * Only for top-level packages, and relies on getPackageName().
     */
    private File getRuleSetFile(RuleSetVO ruleSet) {
        String fileName = ruleSet.getName();
        if (fileName.indexOf('.') < 0)
            fileName += RuleSetVO.getFileExtension(ruleSet.getLanguage());
        return new File(storageDir + "/" + ruleSet.getPackageName().replace('.', '/') + "/" + fileName);
    }

    /**
     * Only for top-level packages, and relies on getPackageName().
     */
    private File getActivityImplementorFile(ActivityImplementorVO implementor) {
        String fileName = implementor.getSimpleName() + IMPL_FILE_EXTENSION;
        return new File(storageDir + "/" + implementor.getPackageName().replace('.', '/') + "/" + fileName);
    }

    /**
     * Only for top-level packages, and relies on getPackageName().
     */
    private File getExternalEventHandlerFile(ExternalEventVO eventHandler) {
        String fileName = eventHandler.getSimpleName() + EVT_HANDLER_FILE_EXTENSION;
        return new File(storageDir + "/" + eventHandler.getPackageName().replace('.', '/') + "/" + fileName);
    }

    /**
     * Only for top-level packages, and relies on getPackageName().
     */
    private File getTaskTemplateFile(TaskVO taskTemplate) {
        String fileName;
        if (taskTemplate.getVersion() > 0)
            fileName = taskTemplate.getName();  // use asset name
        else
            fileName = taskTemplate.getTaskName() + TASK_TEMPLATE_FILE_EXTENSION;
        return new File(storageDir + "/" + taskTemplate.getPackageName().replace('.', '/') + "/" + fileName);
    }

    public AssetRevision getAssetRevision(RuleSetVO ruleSet) {
        AssetRevision rev = new AssetRevision();
        rev.setVersion(ruleSet.getVersion());
        rev.setModDate(new Date());
        rev.setModUser(user);
        rev.setComment(ruleSet.getRevisionComment());
        return rev;
    }

    public AssetRevision getAssetRevision(int version, String comment) {
        AssetRevision rev = new AssetRevision();
        rev.setVersion(version);
        rev.setModDate(new Date());
        rev.setModUser(user);
        rev.setComment(comment);
        return rev;
    }

    protected void removeObsoleteTaskActivityAttributes(ActivityVO manualTaskActivity) {
        if (manualTaskActivity.getAttribute(TaskActivity.ATTRIBUTE_TASK_TEMPLATE) != null) {
            List<AttributeVO> attributes = new ArrayList<AttributeVO>();
            List<String> obsoleteAttributes = Arrays.asList(TaskActivity.ATTRIBUTES_MOVED_TO_TASK_TEMPLATE);
            for (AttributeVO attribute : manualTaskActivity.getAttributes()) {
                if (!obsoleteAttributes.contains(attribute.getAttributeName()))
                    attributes.add(attribute);
            }
            manualTaskActivity.setAttributes(attributes);
        }
    }

    protected TaskVO getTask(String processName, ActivityVO manualTaskActivity) {
        TaskVO task = new TaskVO();
        String logicalId = manualTaskActivity.getAttribute(TaskActivity.ATTRIBUTE_TASK_LOGICAL_ID);
        if (StringHelper.isEmpty(logicalId)) {
            logicalId = processName + ":" + manualTaskActivity.getAttribute(WorkAttributeConstant.LOGICAL_ID);
            manualTaskActivity.setAttribute(TaskActivity.ATTRIBUTE_TASK_LOGICAL_ID, logicalId);
        }
        task.setLogicalId(manualTaskActivity.getAttribute(TaskActivity.ATTRIBUTE_TASK_LOGICAL_ID));
        task.setTaskName(manualTaskActivity.getAttribute(TaskActivity.ATTRIBUTE_TASK_NAME));
        task.setTaskCategory(manualTaskActivity.getAttribute(TaskActivity.ATTRIBUTE_TASK_CATEGORY));
        task.setComment(manualTaskActivity.getAttribute(TaskActivity.ATTRIBUTE_TASK_DESC));
        // attributes
        String sla = manualTaskActivity.getAttribute(TaskActivity.ATTRIBUTE_TASK_SLA);
        String slaUnits = manualTaskActivity.getAttribute(TaskActivity.ATTRIBUTE_TASK_SLA_UNITS);
        if (StringHelper.isEmpty(slaUnits))
            slaUnits = "Hours";
        if (sla != null && sla.trim().length() > 0)
            task.setAttribute(TaskAttributeConstant.TASK_SLA, String.valueOf(ServiceLevelAgreement.unitsToSeconds(sla, slaUnits)));
        String alertInterval = manualTaskActivity.getAttribute(TaskActivity.ATTRIBUTE_TASK_ALERT_INTERVAL);
        int alertSecs;
        if (alertInterval != null && alertInterval.trim().length() > 0) {
            String alertIntervalUnits = manualTaskActivity.getAttribute(TaskActivity.ATTRIBUTE_TASK_ALERT_INTERVAL_UNITS);
            if (StringHelper.isEmpty(alertIntervalUnits))
                alertIntervalUnits = ServiceLevelAgreement.INTERVAL_MINUTES;
            alertSecs = ServiceLevelAgreement.unitsToSeconds(alertInterval, alertIntervalUnits);
            if (alertSecs != 0)
                task.setAttribute(TaskAttributeConstant.ALERT_INTERVAL, String.valueOf(alertSecs));
        }
        task.setAttribute(TaskAttributeConstant.VARIABLES, manualTaskActivity.getAttribute(TaskActivity.ATTRIBUTE_TASK_VARIABLES));
        task.setAttribute(TaskAttributeConstant.GROUPS, manualTaskActivity.getAttribute(TaskActivity.ATTRIBUTE_TASK_GROUPS));
        task.setAttribute(TaskAttributeConstant.INDICES, manualTaskActivity.getAttribute(TaskActivity.ATTRIBUTE_TASK_INDICES));
        task.setAttribute(TaskAttributeConstant.NOTICES, manualTaskActivity.getAttribute(TaskActivity.ATTRIBUTE_TASK_NOTICES));
        task.setAttribute(TaskAttributeConstant.NOTICE_GROUPS, manualTaskActivity.getAttribute(TaskActivity.ATTRIBUTE_NOTICE_GROUPS));
        task.setAttribute(TaskAttributeConstant.RECIPIENT_EMAILS, manualTaskActivity.getAttribute(TaskActivity.ATTRIBUTE_RECIPIENT_EMAILS));
        task.setAttribute(TaskAttributeConstant.CC_GROUPS, manualTaskActivity.getAttribute(TaskActivity.ATTRIBUTE_CC_GROUPS));
        task.setAttribute(TaskAttributeConstant.CC_EMAILS, manualTaskActivity.getAttribute(TaskActivity.ATTRIBUTE_CC_EMAILS));
        task.setAttribute(TaskAttributeConstant.AUTO_ASSIGN, manualTaskActivity.getAttribute(TaskActivity.ATTRIBUTE_TASK_AUTOASSIGN));
        task.setAttribute(TaskAttributeConstant.AUTO_ASSIGN_RULES, manualTaskActivity.getAttribute(TaskActivity.ATTRIBUTE_AUTO_ASSIGN_RULES));
        task.setAttribute(TaskAttributeConstant.ROUTING_STRATEGY, manualTaskActivity.getAttribute(TaskActivity.ATTRIBUTE_TASK_ROUTING));
        task.setAttribute(TaskAttributeConstant.ROUTING_RULES, manualTaskActivity.getAttribute(TaskActivity.ATTRIBUTE_ROUTING_RULES));
        task.setAttribute(TaskAttributeConstant.SUBTASK_STRATEGY, manualTaskActivity.getAttribute(TaskActivity.ATTRIBUTE_SUBTASK_STRATEGY));
        task.setAttribute(TaskAttributeConstant.SUBTASK_RULES, manualTaskActivity.getAttribute(TaskActivity.ATTRIBUTE_SUBTASK_RULES));
        task.setAttribute(TaskAttributeConstant.INDEX_PROVIDER, manualTaskActivity.getAttribute(TaskActivity.ATTRIBUTE_INDEX_PROVIDER));
        task.setAttribute(TaskAttributeConstant.ASSIGNEE_VAR, manualTaskActivity.getAttribute(TaskActivity.ATTRIBUTE_ASSIGNEE_VAR));
        task.setAttribute(TaskAttributeConstant.FORM_NAME, manualTaskActivity.getAttribute(TaskActivity.ATTRIBUTE_FORM_NAME));
        task.setAttribute(TaskAttributeConstant.PRIORITY, manualTaskActivity.getAttribute(TaskActivity.ATTRIBUTE_TASK_PRIORITY));
        task.setAttribute(TaskAttributeConstant.PRIORITY_STRATEGY, manualTaskActivity.getAttribute(TaskActivity.ATTRIBUTE_TASK_PRIORITIZATION));
        task.setAttribute(TaskAttributeConstant.PRIORITIZATION_RULES, manualTaskActivity.getAttribute(TaskActivity.ATTRIBUTE_PRIORITIZATION_RULES));
        task.setAttribute(TaskAttributeConstant.CUSTOM_PAGE, manualTaskActivity.getAttribute(TaskActivity.ATTRIBUTE_CUSTOM_PAGE));
        task.setAttribute(TaskAttributeConstant.CUSTOM_PAGE_ASSET_VERSION, manualTaskActivity.getAttribute(TaskActivity.ATTRIBUTE_CUSTOM_PAGE_ASSET_VERSION));
        task.setAttribute(TaskAttributeConstant.RENDERING_ENGINE, manualTaskActivity.getAttribute(TaskActivity.ATTRIBUTE_RENDERING));

        return task;
    }

    // loader api methods
    public PackageVO loadPackage(Long packageId, boolean deep) throws DataAccessException {
        try {
            PackageDir pkgDir = getPackageDir(packageId);
            return pkgDir == null ? null : loadPackage(pkgDir, deep);
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    public PackageVO getPackage(String name) throws DataAccessException {
        PackageDir pkgDir = getTopLevelPackageDir(name);
        if (pkgDir == null)
            return null;
        try {
            return loadPackage(pkgDir, false);
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    protected PackageDir getPackageDir(Long packageId) throws DataAccessException {
        File logicalDir = versionControl.getFile(packageId);
        for (PackageDir pkgDir : getPackageDirs()) {
            if (pkgDir.getLogicalDir().equals(logicalDir))
                return pkgDir;
        }
        return null;
    }

    /**
     * this clears the cached package list
     */
    public synchronized List<PackageVO> getPackageList(boolean deep, ProgressMonitor progressMonitor) throws DataAccessException {

        pkgDirs = null;
        versionControl.clear();

        if (progressMonitor != null)
            progressMonitor.progress(10);

        List<PackageVO> packages = new ArrayList<PackageVO>();

        for (PackageDir pkgDir : getPackageDirs()) {
            try {
                packages.add(loadPackage(pkgDir, deep));
            }
            catch (DataAccessException ex) {
                throw ex;
            }
            catch (Exception ex) {
                throw new DataAccessException(ex.getMessage(), ex);
            }
        }

        return packages;
    }

    public List<ProcessVO> getProcessList() throws DataAccessException {
        List<ProcessVO> processes = new ArrayList<ProcessVO>();
        try {
            for (PackageDir pkgDir : getPackageDirs())
                processes.addAll(loadProcesses(pkgDir, false));
            Collections.sort(processes);
            return processes;
        }
        catch (DataAccessException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    public ProcessVO loadProcess(Long processId, boolean withSubProcesses) throws DataAccessException {
        File logicalFile = versionControl.getFile(processId);
        if (logicalFile == null)
            return null; // process not found
        PackageDir pkgDir = getPackageDir(logicalFile);
        try {
            return loadProcess(pkgDir, pkgDir.findAssetFile(logicalFile), true);
        }
        catch (DataAccessException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    public ProcessVO getProcessBase(Long processId) throws DataAccessException {
        return loadProcess(processId, true);
    }

    public ProcessVO getProcessBase(String name, int version) throws DataAccessException {
        List<ProcessVO> versions = version == 0 ? new ArrayList<ProcessVO>() : null;
        for (PackageDir pkgDir : getPackageDirs()) {
            for (File procFile : pkgDir.listFiles(procFileFilter)) {
                String fileName = procFile.getName();
                if (fileName.substring(0, fileName.length() - PROCESS_FILE_EXTENSION.length()).equals(name)) {
                    try {
                        ProcessVO process = loadProcess(pkgDir, pkgDir.getAssetFile(procFile), true);
                        if (version == 0) {
                            versions.add(process);
                        }
                        else if (process.getVersion() == version) {
                            return process;
                        }
                    }
                    catch (DataAccessException ex) {
                        throw ex;
                    }
                    catch (Exception ex) {
                        throw new DataAccessException(ex.getMessage(), ex);
                    }
                }
            }
        }
        ProcessVO found = null;
        if (version == 0 && !versions.isEmpty()) {
            for (ProcessVO proc : versions) {
                if (found == null || found.getVersion() < proc.getVersion())
                    found = proc;
            }
        }
        return found;
    }

    public List<RuleSetVO> getRuleSets() throws DataAccessException {
        List<RuleSetVO> ruleSets = new ArrayList<RuleSetVO>();
        try {
            for (PackageDir pkgDir : getPackageDirs())
                ruleSets.addAll(loadRuleSets(pkgDir, false));
            return ruleSets;
        }
        catch (IOException ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
        catch (XmlException ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    public RuleSetVO getRuleSet(Long id) throws DataAccessException {
        try {
            File logicalFile = versionControl.getFile(id);
            PackageDir pkgDir = getPackageDir(logicalFile);
            return loadRuleSet(pkgDir, pkgDir.findAssetFile(logicalFile), true);
        }
        catch (IOException ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
        catch (XmlException ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    public RuleSetVO getRuleSet(String name, String language, int version) throws DataAccessException {
        for (RuleSetVO ruleSet : getRuleSets()) {
            if (ruleSet.getName().equals(name) && (ruleSet.getVersion() == version || version == 0)) {
                try {
                    File logicalFile = versionControl.getFile(ruleSet.getId());
                    PackageDir pkgDir = getPackageDir(logicalFile);
                    return loadRuleSet(pkgDir, pkgDir.findAssetFile(logicalFile), true);
                }
                catch (Exception ex) {
                    throw new DataAccessException(ex.getMessage(), ex);
                }
            }
        }
        return null;
    }

    public RuleSetVO getRuleSet(Long packageId, String name) throws DataAccessException {
        PackageDir pkgDir = getPackageDir(packageId);
        for (File ruleSetFile : pkgDir.listFiles(ruleSetFileFilter)) {
            if (ruleSetFile.getName().equals(name)) {
                try {
                    return loadRuleSet(pkgDir, pkgDir.getAssetFile(ruleSetFile), true);
                }
                catch (IOException ex) {
                    throw new DataAccessException(ex.getMessage(), ex);
                }
                catch (XmlException ex) {
                    throw new DataAccessException(ex.getMessage(), ex);
                }
            }
        }
        return null;
    }

    public RuleSetVO getRuleSetForOwner(String ownerType, Long ownerId) throws DataAccessException {
        if (ownerType.equals(OwnerType.PACKAGE)) {
            PackageDir pkgDir = getPackageDir(ownerId);
            try {
                PackageVO pkgVO = loadPackage(pkgDir, false);
                if (pkgVO.getMetaContent() != null) {
                    RuleSetVO ruleSet = new RuleSetVO();
                    ruleSet.setLanguage(RuleSetVO.CONFIG);
                    ruleSet.setRuleSet(pkgVO.getMetaContent());
                    return ruleSet;
                }
            }
            catch (Exception ex) {
                throw new DataAccessException(ex.getMessage(), ex);
            }
        }
        return null;
    }

    public List<ActivityImplementorVO> getActivityImplementors() throws DataAccessException {
        try {
            Map<String,ActivityImplementorVO> impls = new HashMap<String,ActivityImplementorVO>();
            for (PackageDir pkgDir : getPackageDirs()) {
                for (ActivityImplementorVO impl : loadActivityImplementors(pkgDir)) {
                    if (!impls.containsKey(impl.getImplementorClassName()))
                        impls.put(impl.getImplementorClassName(), impl);
                }
            }
            List<ActivityImplementorVO> modifiableList = new ArrayList<ActivityImplementorVO>();
            modifiableList.addAll(Arrays.asList(impls.values().toArray(new ActivityImplementorVO[0])));
            return modifiableList;
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    public List<ActivityImplementorVO> getReferencedImplementors(PackageVO packageVO) throws DataAccessException {
        PackageDir pkgDir = getPackageDir(packageVO.getId());
        List<String> implClasses = new ArrayList<String>();
        try {
            for (ProcessVO process : loadProcesses(pkgDir, true)) {
                for (ActivityVO activity : process.getActivities()) {
                    String implClass = activity.getImplementorClassName();
                    if (!implClasses.contains(implClass))
                        implClasses.add(implClass);
                }
            }
            List<ActivityImplementorVO> referencedImpls = new ArrayList<ActivityImplementorVO>();
            List<ActivityImplementorVO> allImpls = getActivityImplementors();  // TODO cache these
            for (String implClass : implClasses) {
                for (ActivityImplementorVO impl : allImpls) {
                    if (impl.getImplementorClassName().equals(implClass)) {
                        referencedImpls.add(impl);
                        break;
                    }
                }
            }
            return referencedImpls;
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    public List<ExternalEventVO> loadExternalEvents() throws DataAccessException {
        try {
            List<ExternalEventVO> evtHandlers = new ArrayList<ExternalEventVO>();
            for (PackageDir pkgDir : getPackageDirs())
                evtHandlers.addAll(loadExternalEventHandlers(pkgDir));
            return evtHandlers;
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    public List<TaskVO> getTaskTemplates() throws DataAccessException {
        try {
            List<TaskVO> tasks = new ArrayList<TaskVO>();
            for (PackageDir pkgDir : getPackageDirs()) {
                for (TaskVO pkgTask : loadTaskTemplates(pkgDir)) {
                    // do not load duplicate tasks -- keep only latest
                    if (!tasks.contains(pkgTask))
                        tasks.add(pkgTask);
                }
            }
            return tasks;
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    public List<VariableTypeVO> getVariableTypes() throws DataAccessException {
        List<VariableTypeVO> types = baselineData.getVariableTypes();
        return types;
    }

    public List<TaskCategory> getTaskCategories() throws DataAccessException {
        List<TaskCategory> taskCats = new ArrayList<TaskCategory>();
        taskCats.addAll(baselineData.getTaskCategories().values());
        Collections.sort(taskCats);
        return taskCats;
    }

    public Set<String> getTaskCategorySet() throws DataAccessException {
        return new HashSet<String>(baselineData.getTaskCategoryCodes().values());
    }

    /**
     * uses db loader method to avoid code duplication
     */
    public List<ProcessVO> findCallingProcesses(ProcessVO subproc) throws DataAccessException {
        List<ProcessVO> callers = new ArrayList<ProcessVO>();
        try {
            Pattern singleProcPattern = Pattern.compile(".*Attribute Name=\"processname\" Value=\"[^\"]*" + subproc.getName() + "\".*", Pattern.DOTALL);
            Pattern multiProcPattern = Pattern.compile(".*Attribute Name=\"processmap\" Value=\"[^\"]*" + subproc.getName() + "[^>]*.*", Pattern.DOTALL);
            for (PackageDir pkgDir : getPackageDirs()) {
                for (File procFile : pkgDir.listFiles(procFileFilter)) {
                    String xml = new String(read(procFile));
                    if (singleProcPattern.matcher(xml).matches() || multiProcPattern.matcher(xml).matches()) {
                        ProcessVO procVO = loadProcess(pkgDir, pkgDir.getAssetFile(procFile), true);
                        for (ActivityVO activity : procVO.getActivities()) {
                            if (activityInvokesProcess(activity, subproc) && !callers.contains(procVO))
                                callers.add(procVO);
                        }
                        for (ProcessVO embedded : procVO.getSubProcesses()) {
                            for (ActivityVO activity : embedded.getActivities()) {
                                if (activityInvokesProcess(activity, subproc) && !callers.contains(procVO))
                                    callers.add(procVO);
                            }
                        }
                    }
                }
            }
            return callers;
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    /**
     * uses db loader method to avoid code duplication
     */
    public List<ProcessVO> findCalledProcesses(ProcessVO mainproc) throws DataAccessException {
        // make sure process is loaded
        mainproc = loadProcess(mainproc.getId(), false);
        return findInvoked(mainproc, getProcessList());
    }

    public List<ProcessVO> getProcessListForImplementor(Long implementorId, String implementorClass) throws DataAccessException {
        List<ProcessVO> processes = new ArrayList<ProcessVO>();
        try {
            String implDecl = "Implementation=\"" + implementorClass + "\"";  // crude but fast
            for (PackageDir pkgDir : getPackageDirs()) {
                for (File procFile : pkgDir.listFiles(procFileFilter)) {
                    String xml = new String(read(procFile));
                    if (xml.contains(implDecl))
                        processes.add(loadProcess(pkgDir, pkgDir.getAssetFile(procFile), false));
                }
            }
            return processes;
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    /**
     * actually schema version
     */
    public int getDatabaseVersion() {
        return DataAccess.currentSchemaVersion;
    }

    /**
     * Only for top-level packages.
     */
    public Long persistPackage(PackageVO packageVO, PersistType persistType) throws DataAccessException {
        try {
            PackageDir pkgDir;
            if (persistType == PersistType.NEW_VERSION || persistType == PersistType.CREATE_JSON) {
                packageVO.setVersion(packageVO.getVersion() + 1);
                if (packageVO.getVersion() == 1) {
                    pkgDir = createPackage(packageVO);
                }
                else {
                    pkgDir = getTopLevelPackageDir(packageVO.getName());
                    // for process version increment, existing package will have been archived when process was saved
                }
            }
            else if (persistType == PersistType.IMPORT || persistType == PersistType.IMPORT_JSON) {
                PackageDir existingTopLevel = getTopLevelPackageDir(packageVO.getName());
                if (existingTopLevel != null) {
                    if (!packageVO.getVersionString().equals(existingTopLevel.getPackageVersion())) {
                        // move the existing package to the archive
                        File archiveDest = new File(archiveDir + "/" + packageVO.getName() + " v" + existingTopLevel.getPackageVersion());
                        if (archiveDest.exists())
                            deletePkg(archiveDest);
                        copyPkg(existingTopLevel, archiveDest);
                    }
                    pkgDirs.remove(existingTopLevel);
                    deletePkg(existingTopLevel);
                    versionControl.clearId(existingTopLevel.getLogicalDir());
                }
                pkgDir = createPackage(packageVO);
            }
            else {
                pkgDir = getTopLevelPackageDir(packageVO.getName());
            }
            Long id = save(packageVO, pkgDir, persistType == PersistType.IMPORT || persistType == PersistType.IMPORT_JSON);
            pkgDir.parse();  // sync
            return id;
        }
        catch (DataAccessException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    private PackageDir archivePackage(PackageDir pkgDir) throws IOException, DataAccessException {
        File archiveDest = new File(archiveDir + "/" + pkgDir.getPackageName() + " v" + pkgDir.getPackageVersion());
        if (archiveDest.exists())
            delete(archiveDest);
        copyPkg(pkgDir, archiveDest);

        PackageDir archivePkgDir = new PackageDir(storageDir, archiveDest, versionControl);
        archivePkgDir.parse();
        pkgDirs.add(archivePkgDir);
        return archivePkgDir;
    }

    /**
     * Only for top-level packages.
     */
    public long renamePackage(Long packageId, String newName, int newVersion) throws DataAccessException {
        PackageDir oldPkgDir = getPackageDir(packageId);
        if (oldPkgDir.isArchive())
            throw new DataAccessException("Cannot rename archived package: " + oldPkgDir);
        File newPkg = new File(storageDir + "/" + newName);
        try {
            PackageVO pkg = loadPackage(packageId, false);
            pkg.setName(newName);
            pkg.setVersion(newVersion);
            save(pkg, oldPkgDir, false); // at this point package.xml name doesn't match oldPkgDir
            newPkg = renamePkgDir(oldPkgDir, newPkg);
            versionControl.clearId(oldPkgDir.getLogicalDir());
            PackageDir newPkgDir = new PackageDir(storageDir, newPkg, versionControl);
            newPkgDir.parse();
            // prime the cache with the new pkg
            loadPackage(newPkgDir, false);
            getPackageDirs().add(0, newPkgDir);
            getPackageDirs().remove(oldPkgDir);
            // save the version info
            return save(pkg, newPkgDir, false);
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    public int deletePackage(Long packageId) throws DataAccessException {
        PackageDir pkgDir = getPackageDir(packageId);
        try {
            pkgDirs.remove(pkgDir);
            deletePkg(pkgDir);
            versionControl.clearId(pkgDir.getLogicalDir());
            return 0; // not used
        }
        catch (IOException ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    /**
     * Saves to top-level package.  Expects process.getPackageName() to be set.
     * This has the side effect of archiving the process's package if a new version is being saved.
     */
    public Long persistProcess(ProcessVO process, PersistType persistType) throws DataAccessException {
        try {
            PackageDir pkgDir = getTopLevelPackageDir(process.getPackageName());
            if (persistType == PersistType.NEW_VERSION) {
                archivePackage(pkgDir);
                // remove old process version from top-level package
                String prevVer = process.getAttribute("previousProcessVersion");
                if (prevVer != null) {
                    File prevLogicalFile = new File(process.getPackageName() + "/" + process.getName() + ".proc v" + prevVer);
                    PackageDir prevPkgDir = getPackageDir(prevLogicalFile);
                    if (prevPkgDir != null)
                        prevPkgDir.removeAssetFile(prevLogicalFile);
                    process.removeAttribute("previousProcessVersion");
                }
            }
            return save(process, pkgDir);
        }
        catch (DataAccessException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    public long renameProcess(Long processId, String newName, int newVersion) throws DataAccessException {
        File logicalFile = versionControl.getFile(processId);
        PackageDir pkgDir = getPackageDir(logicalFile);
        try {
            File existFile = pkgDir.findAssetFile(logicalFile);
            ProcessVO proc = loadProcess(pkgDir, pkgDir.getAssetFile(existFile), true);
            proc.setName(newName);
            proc.setVersion(newVersion);
            save(proc, pkgDir); // update version and name
            File newFile = new File(existFile.getParentFile() + "/" + newName + PROCESS_FILE_EXTENSION);
            delete(existFile);
            versionControl.clearId(logicalFile);
            versionControl.deleteRev(existFile);
            versionControl.setRevision(newFile, getAssetRevision(newVersion, "Renamed from " + existFile));
            // prime the cache
            loadProcess(pkgDir, pkgDir.getAssetFile(newFile), false);
            return proc.getId();
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    public void deleteProcess(ProcessVO process) throws DataAccessException {
        deleteProcess(process.getId());
    }

    /**
     * may have already been deleted by removeProcessFromPackage()
     */
    public void deleteProcess(Long processId) throws DataAccessException {
        File logicalFile = versionControl.getFile(processId);
        if (logicalFile != null) {
            PackageDir pkgDir = getPackageDir(logicalFile);
            try {
                File procFile = pkgDir.findAssetFile(logicalFile);
                if (procFile.exists()) {
                    delete(procFile);
                    versionControl.clearId(logicalFile);
                    versionControl.deleteRev(procFile);
                }
            }
            catch (IOException ex) {
                throw new DataAccessException(ex.getMessage(), ex);
            }
        }
    }

    /**
     * in move scenario, must be called before removeProcessFromPackage()
     */
    public long addProcessToPackage(Long processId, Long packageId) throws DataAccessException {
        File logicalFile = versionControl.getFile(processId);
        PackageDir pkgDir = getPackageDir(logicalFile);
        try {
            ProcessVO process = loadProcess(pkgDir, pkgDir.findAssetFile(logicalFile), true);
            PackageDir newPkgDir = getPackageDir(packageId);
            File newFile = new File(newPkgDir + "/" + process.getName() + PROCESS_FILE_EXTENSION);
            save(process, newPkgDir);
            versionControl.setRevision(newFile, getAssetRevision(process.getVersion(), null));
            return loadProcess(newPkgDir, newPkgDir.getAssetFile(newFile), false).getId(); // prime the cache
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    /**
     * file may have been deleted in deleteProcess()
     */
    public void removeProcessFromPackage(Long processId, Long packageId) throws DataAccessException {
        deleteProcess(processId);
    }

    /**
     * Saves to top-level package.  Expects getPackageName() to be set.
     */
    public Long createRuleSet(RuleSetVO ruleSet) throws DataAccessException {
        try {
          ruleSet.setVersion(1);
          long id = save(ruleSet, getTopLevelPackageDir(ruleSet.getPackageName()));
          ruleSet.setId(id);
          return id;
        }
        catch (IOException ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    /**
     * asset is saved locally; just update the version
     */
    public void updateRuleSet(RuleSetVO ruleSet) throws DataAccessException {
        try {
            versionControl.setRevision(getRuleSetFile(ruleSet), getAssetRevision(ruleSet));
        }
        catch (IOException ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    public void renameRuleSet(RuleSetVO ruleset, String newName) throws DataAccessException  {
        File logicalFile = versionControl.getFile(ruleset.getId());
        PackageDir pkgDir = getPackageDir(logicalFile);
        try {
            File oldFile = pkgDir.findAssetFile(logicalFile);
            File newFile = new File(oldFile.getParentFile() + "/" + newName);
            rename(oldFile, newFile);
            ruleset.setName(newName);
            versionControl.setRevision(getRuleSetFile(ruleset), getAssetRevision(ruleset));
            versionControl.clearId(logicalFile);
            versionControl.setRevision(newFile, getAssetRevision(ruleset.getVersion(), "Renamed from " + oldFile));
            RuleSetVO newRuleSet = loadRuleSet(pkgDir, pkgDir.getAssetFile(newFile), false); // prime
            ruleset.setId(newRuleSet.getId());
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    /**
     * may have already been deleted by removeRuleSetFromPackage()
     */
    public void deleteRuleSet(Long ruleSetId) throws DataAccessException {
        File logicalFile = versionControl.getFile(ruleSetId);
        if (logicalFile != null) {
            PackageDir pkgDir = getPackageDir(logicalFile);
            try {
                File ruleSetFile = pkgDir.findAssetFile(logicalFile);
                if (ruleSetFile.exists()) {
                    delete(ruleSetFile);
                    versionControl.clearId(logicalFile);
                    versionControl.deleteRev(ruleSetFile);
                }
            }
            catch (IOException ex) {
                throw new DataAccessException(ex.getMessage(), ex);
            }
        }
    }

    /**
     * in move scenario, must be called before removeRuleSetFromPackage()
     */
    public long addRuleSetToPackage(Long ruleSetId, Long packageId) throws DataAccessException {
        File logicalFile = versionControl.getFile(ruleSetId);
        PackageDir pkgDir = getPackageDir(logicalFile);
        try {
            AssetFile existFile = pkgDir.findAssetFile(logicalFile);
            RuleSetVO asset = loadRuleSet(pkgDir, existFile, true);
            PackageDir newPkgDir = getPackageDir(packageId);
            File newFile = new File(newPkgDir + "/" + existFile.getName());
            save(asset, newPkgDir);
            versionControl.setRevision(newFile, getAssetRevision(asset.getVersion(), null));
            return loadRuleSet(newPkgDir, newPkgDir.getAssetFile(newFile), false).getId();
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    /**
     * file may have been deleted in deleteRuleSet()
     */
    public void removeRuleSetFromPackage(Long ruleSetId, Long packageId) throws DataAccessException {
        deleteRuleSet(ruleSetId);
    }

    /**
     * Saves to top-level package.  Expects getPackageName() to be set.
     */
    public Long createActivityImplementor(ActivityImplementorVO implementor) throws DataAccessException {
        try {
            return save(implementor, getTopLevelPackageDir(implementor.getPackageName()));
        }
        catch (DataAccessException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    public void updateActivityImplementor(ActivityImplementorVO implementor) throws DataAccessException {
        createActivityImplementor(implementor);
    }

    /**
     * may have already been deleted due to removeActivityImplFromPackage
     */
    public void deleteActivityImplementor(Long implementorId) throws DataAccessException {
        File logicalFile = versionControl.getFile(implementorId);
        if (logicalFile != null) {
            PackageDir pkgDir = getPackageDir(logicalFile);
            try {
                File implFile = pkgDir.findAssetFile(logicalFile);
                if (implFile.exists()) {
                    delete(implFile);
                    versionControl.clearId(logicalFile);
                    versionControl.deleteRev(implFile);
                }
            }
            catch (IOException ex) {
                throw new DataAccessException(ex.getMessage(), ex);
            }
        }
    }

    /**
     * in move scenario, must be called before removeActivityImplFromPackage()
     */
    public long addActivityImplToPackage(Long activityImplId, Long packageId) throws DataAccessException {
        File logicalFile = versionControl.getFile(activityImplId);
        PackageDir pkgDir = getPackageDir(logicalFile);
        try {
            ActivityImplementorVO implementor = loadActivityImplementor(pkgDir, pkgDir.findAssetFile(logicalFile));
            PackageDir newPkgDir = getPackageDir(packageId);
            File newFile = new File(newPkgDir + "/" + implementor.getSimpleName() + IMPL_FILE_EXTENSION);
            implementor.setPackageName(newPkgDir.getPackageName());  // required for save()
            save(implementor, newPkgDir);
            return loadActivityImplementor(newPkgDir, newPkgDir.getAssetFile(newFile)).getImplementorId(); // prime the cache
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    /**
     * may have already been deleted in deleteImplementor()
     */
    public void removeActivityImplFromPackage(Long activityImplId, Long packageId) throws DataAccessException {
        deleteActivityImplementor(activityImplId);
    }

    /**
     * Saves to top-level package.  Expects getPackageName() to be set.
     */
    public void createExternalEvent(ExternalEventVO eventHandler) throws DataAccessException {
        try {
            save(eventHandler, getTopLevelPackageDir(eventHandler.getPackageName()));
        }
        catch (DataAccessException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    public void updateExternalEvent(ExternalEventVO eventHandler) throws DataAccessException {
        createExternalEvent(eventHandler);
    }

    /**
     * may have already been deleted due to removeExternalEventFromPackage()
     */
    public void deleteExternalEvent(Long eventHandlerId) throws DataAccessException {
        File logicalFile = versionControl.getFile(eventHandlerId);
        if (logicalFile != null) {
            PackageDir pkgDir = getPackageDir(logicalFile);
            try {
                File evthFile = pkgDir.findAssetFile(logicalFile);
                if (evthFile.exists()) {
                    delete(evthFile);
                    versionControl.clearId(logicalFile);
                    versionControl.deleteRev(evthFile);
                }
            }
            catch (IOException ex) {
                throw new DataAccessException(ex.getMessage(), ex);
            }
        }
    }

    /**
     * in move scenario, must be called before removeExternalEventHandlerFromPackage()
     */
    public long addExternalEventToPackage(Long externalEventId, Long packageId) throws DataAccessException {
        File logicalFile = versionControl.getFile(externalEventId);
        PackageDir pkgDir = getPackageDir(logicalFile);
        try {
            ExternalEventVO eventHandler = loadExternalEventHandler(pkgDir, pkgDir.findAssetFile(logicalFile));
            PackageDir newPkgDir = getPackageDir(packageId);
            File newFile = new File(newPkgDir + "/" + eventHandler.getSimpleName() + EVT_HANDLER_FILE_EXTENSION);
            eventHandler.setPackageName(newPkgDir.getPackageName());  // required for save()
            save(eventHandler, newPkgDir);
            return loadExternalEventHandler(newPkgDir, newPkgDir.getAssetFile(newFile)).getId();
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    /**
     * file may have been deleted in deleteExternalEvent()
     */
    public void removeExternalEventFromPackage(Long externalEventId, Long packageId) throws DataAccessException {
        deleteExternalEvent(externalEventId);
    }

    /**
     * Saves to top-level package.  Expects getPackageName() to be set.
     */
    public void createTaskTemplate(TaskVO taskTemplate) throws DataAccessException {
        try {
            save(taskTemplate, getTopLevelPackageDir(taskTemplate.getPackageName()));
        }
        catch (DataAccessException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    /**
     * may have already been deleted by removeTaskTemplateFromPackage()
     */
    public void deleteTaskTemplate(Long taskId) throws DataAccessException {
        File logicalFile = versionControl.getFile(taskId);
        if (logicalFile != null) {
            PackageDir pkgDir = getPackageDir(logicalFile);
            try {
                File taskFile = pkgDir.findAssetFile(logicalFile);
                if (taskFile.exists()) {
                    delete(taskFile);
                    versionControl.clearId(logicalFile);
                    versionControl.deleteRev(taskFile);
                }
            }
            catch (IOException ex) {
                throw new DataAccessException(ex.getMessage(), ex);
            }
        }
    }

    public void updateTaskTemplate(TaskVO taskTemplate) throws DataAccessException {
        createTaskTemplate(taskTemplate);
    }

    /**
     * in move scenario, must be called before removeTaskTemplateFromPackage()
     */
    public long addTaskTemplateToPackage(Long taskId, Long packageId) throws DataAccessException {
        File logicalFile = versionControl.getFile(taskId);
        PackageDir pkgDir = getPackageDir(logicalFile);
        try {
            TaskVO taskTemplate = loadTaskTemplate(pkgDir, pkgDir.findAssetFile(logicalFile));
            PackageDir newPkgDir = getPackageDir(packageId);
            File newFile = new File(pkgDir + "/" + taskTemplate.getTaskName() + TASK_TEMPLATE_FILE_EXTENSION);
            taskTemplate.setPackageName(newPkgDir.getPackageName());
            save(taskTemplate, newPkgDir);
            return loadTaskTemplate(newPkgDir, newPkgDir.getAssetFile(newFile)).getTaskId();
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    /**
     * file may have been deleted in deleteTaskTemplate()
     */
    public void removeTaskTemplateFromPackage(Long taskId, Long packageId) throws DataAccessException {
        deleteTaskTemplate(taskId);
    }

    /**
     * used for package tagging and override attributes
     */
    public Long setAttribute(String ownerType, Long ownerId, String attrname, String attrvalue) throws DataAccessException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Used for override attributes.  Performed through server for VCS assets.
     * TODO: Also used for user-set values for custom attributes.
     */
    public Map<String,String> getAttributes(String owner, Long ownerId) throws DataAccessException {
        return null;
    }
    public void setAttributes(String owner, Long ownerId, Map<String,String> attributes) throws DataAccessException {
    }

    public boolean activityInvokesProcess(ActivityVO activity, ProcessVO subproc) {
        String procName = activity.getAttribute(WorkAttributeConstant.PROCESS_NAME);
        if (procName != null && (procName.equals(subproc.getName()) || procName.endsWith("/" + subproc.getName()))) {
            String verSpec = activity.getAttribute(WorkAttributeConstant.PROCESS_VERSION);
            try {
                // compatibility
                int ver = Integer.parseInt(verSpec);
                verSpec = RuleSetVO.formatVersion(ver);
            }
            catch (NumberFormatException ex) {}

            if (subproc.meetsVersionSpec(verSpec))
                return true;
        }
        else {
            String procMap = activity.getAttribute(WorkAttributeConstant.PROCESS_MAP);
            if (procMap != null) {
                List<String[]> procmap = StringHelper.parseTable(procMap, ',', ';', 3);
                for (int i = 0; i < procmap.size(); i++) {
                    String nameSpec = procmap.get(i)[1];
                    if (nameSpec != null && (nameSpec.equals(subproc.getName()) || nameSpec.endsWith("/" + subproc.getName()))) {
                        String verSpec = procmap.get(i)[2];
                        try {
                            // compatibility
                            int ver = Integer.parseInt(verSpec);
                            verSpec = RuleSetVO.formatVersion(ver);
                        }
                        catch (NumberFormatException ex) {}

                        if (subproc.meetsVersionSpec(verSpec))
                            return true;
                    }
                }
            }
        }
        return false;
    }

    public List<ProcessVO> findInvoked(ProcessVO caller, List<ProcessVO> processes) {
        List<ProcessVO> called = new ArrayList<ProcessVO>();
        if (caller.getActivities() != null) {
            for (ActivityVO activity : caller.getActivities()) {
                String procName = activity.getAttribute(WorkAttributeConstant.PROCESS_NAME);
                if (procName != null) {
                    String verSpec = activity.getAttribute(WorkAttributeConstant.PROCESS_VERSION);
                    if (verSpec != null) {
                        try {
                            // compatibility
                            int ver = Integer.parseInt(verSpec);
                            verSpec = RuleSetVO.formatVersion(ver);
                        }
                        catch (NumberFormatException ex) {}

                        ProcessVO latestMatch = null;
                        for (ProcessVO process : processes) {
                            if ((procName.equals(process.getName()) || procName.endsWith("/" + process.getName()))
                            && (process.meetsVersionSpec(verSpec) && (latestMatch == null || latestMatch.getVersion() < process.getVersion()))) {
                                latestMatch = process;
                            }
                        }
                        if (latestMatch != null && !called.contains(latestMatch))
                            called.add(latestMatch);
                    }
                }
                else {
                    String procMap = activity.getAttribute(WorkAttributeConstant.PROCESS_MAP);
                    if (procMap != null) {
                        List<String[]> procmap = StringHelper.parseTable(procMap, ',', ';', 3);
                        for (int i = 0; i < procmap.size(); i++) {
                            String nameSpec = procmap.get(i)[1];
                            if (nameSpec != null) {
                                String verSpec = procmap.get(i)[2];
                                if (verSpec != null) {
                                    try {
                                        // compatibility
                                        int ver = Integer.parseInt(verSpec);
                                        verSpec = RuleSetVO.formatVersion(ver);
                                    }
                                    catch (NumberFormatException ex) {}

                                ProcessVO latestMatch = null;
                                for (ProcessVO process : processes) {
                                    if ((nameSpec.equals(process.getName()) || nameSpec.endsWith("/" + process.getName()))
                                      && (process.meetsVersionSpec(verSpec) && (latestMatch == null || latestMatch.getVersion() < process.getVersion()))) {
                                        latestMatch = process;
                                    }
                                }
                                if (latestMatch != null && !called.contains(latestMatch))
                                    called.add(latestMatch);
                                }
                            }
                        }
                    }
                }
            }
        }

        return called;
    }
}