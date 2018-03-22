/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.dataaccess.file;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
import org.yaml.snakeyaml.Yaml;

import com.centurylink.mdw.activity.types.TaskActivity;
import com.centurylink.mdw.cache.impl.AssetRefCache;
import com.centurylink.mdw.config.YamlBuilder;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.constant.WorkAttributeConstant;
import com.centurylink.mdw.dataaccess.AssetRef;
import com.centurylink.mdw.dataaccess.AssetRevision;
import com.centurylink.mdw.dataaccess.BaselineData;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.ProcessLoader;
import com.centurylink.mdw.dataaccess.ProcessPersister;
import com.centurylink.mdw.dataaccess.VersionControl;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.attribute.Attribute;
import com.centurylink.mdw.model.event.ExternalEvent;
import com.centurylink.mdw.model.task.TaskCategory;
import com.centurylink.mdw.model.task.TaskTemplate;
import com.centurylink.mdw.model.variable.VariableType;
import com.centurylink.mdw.model.workflow.Activity;
import com.centurylink.mdw.model.workflow.ActivityImplementor;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.util.AssetRefConverter;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.timer.ProgressMonitor;

// TODO clear VersionControl & PackageDir/AssetFile caches on Cache Refresh.
public class LoaderPersisterVcs implements ProcessLoader, ProcessPersister {

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
    private FileFilter assetFileFilter;
    private FileFilter implFileFilter;
    private FileFilter evthFileFilter;
    private FileFilter taskFileFilter;
    private List<PackageDir> pkgDirs;
    private Comparator<PackageDir> pkgDirComparator;
    private BaselineData baselineData;

    private VersionControl versionControl;
    public VersionControl getVersionControl() throws DataAccessException {
        if (pkgDirs == null)  // Can happen while refreshing PackageCache, which clears the maps in versionControlGit
            getPackageDirs();  // Once pkgDirs is set again, versionControlGit is ready to use

        return versionControl;
    }

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
        this.assetFileFilter = new FileFilter() {
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

    protected PackageDir createPackage(Package packageVo) throws DataAccessException, IOException {
        if (packageVo.getSchemaVersion() == 0)
            packageVo.setSchemaVersion(DataAccess.currentSchemaVersion);
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
            List<PackageDir> pkgDirsTemp = new ArrayList<PackageDir>();
            for (File pkgNode : getPkgDirFiles(storageDir, includeArchive, new ArrayList<>())) {
                PackageDir pkgDir = new PackageDir(storageDir, pkgNode, versionControl);
                pkgDir.parse();
                pkgDirsTemp.add(pkgDir);
            }
            Collections.sort(pkgDirsTemp, pkgDirComparator);
            pkgDirs = pkgDirsTemp;
        }
        return pkgDirs;
    }

    protected List<File> getPkgDirFiles(File parentDir, List<File> excludes) throws DataAccessException {
        return getPkgDirFiles(parentDir, true, excludes);
    }

    /**
     * For recursively finding package directories based on the filter.
     */
    protected List<File> getPkgDirFiles(File parentDir, boolean includeArchive, List<File> excludes)
            throws DataAccessException {
        List<File> pkgDirFiles = new ArrayList<>();
        File mdwIgnore = new File(parentDir + "/.mdwignore");
        if (mdwIgnore.exists()) {
            try {
                // currently only supports a straight directory list (no wildcards)
                String list = new String(Files.readAllBytes(Paths.get(mdwIgnore.getPath()))).trim();
                for (String line : list.split("\n")) {
                    line = line.trim();
                    if (!line.startsWith("#"))
                        excludes.add(new File(parentDir + "/" + line));
                }
            }
            catch (IOException ex) {
                throw new DataAccessException(ex.getMessage(), ex);
            }
        }

        for (File pkgDirFile : parentDir.listFiles(pkgDirFilter)) {
            if (!excludes.contains(pkgDirFile))
                pkgDirFiles.add(pkgDirFile);
        }
        for (File subDir : parentDir.listFiles(subDirFilter)) {
            if (!excludes.contains(subDir) && (includeArchive || !subDir.equals(archiveDir)))
                pkgDirFiles.addAll(getPkgDirFiles(subDir, excludes));
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
            if (!toPkgDir.exists() && !toPkgDir.mkdirs())
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

    public Package loadPackage(PackageDir pkgDir, boolean deep) throws IOException, XmlException, JSONException, DataAccessException {
        Package packageVO = new Package();

        packageVO.setName(pkgDir.getPackageName());
        packageVO.setVersion(Package.parseVersion(pkgDir.getPackageVersion()));
        packageVO.setSchemaVersion(Package.parseVersion(pkgDir.getSchemaVersion()));
        packageVO.setId(versionControl.getId(pkgDir.getLogicalDir()));
        packageVO.setSchemaVersion(DataAccess.currentSchemaVersion);

        String pkgJson = new String(read(pkgDir.getMetaFile()));
        if (pkgDir.isYaml()) {
            Yaml yaml= new Yaml();
            Map<String,Object> map= (Map<String, Object>) yaml.load(pkgJson);
            Package jsonPkg = new Package(map);
            packageVO.setGroup(jsonPkg.getGroup());
            packageVO.setAttributes(jsonPkg.getAttributes());
            packageVO.setMetaContent(pkgJson);
        }
        else {
            Package jsonPkg = new Package(new JsonObject(pkgJson));
            packageVO.setGroup(jsonPkg.getGroup());
            packageVO.setAttributes(jsonPkg.getAttributes());
            packageVO.setMetaContent(pkgJson);
        }

        packageVO.setProcesses(loadProcesses(pkgDir, deep));
        packageVO.setAssets(loadAssets(pkgDir, deep));
        packageVO.setImplementors(loadActivityImplementors(pkgDir));
        packageVO.setExternalEvents(loadExternalEventHandlers(pkgDir));
        packageVO.setTaskTemplates(loadTaskTemplates(pkgDir));

        packageVO.setArchived(pkgDir.isArchive());

        return packageVO;
    }

    public long save(Package packageVO, PackageDir pkgDir, boolean deep) throws IOException, XmlException, JSONException, DataAccessException {
        File mdwDir = new File(pkgDir + "/.mdw");
        if (!mdwDir.exists()) {
            if (!mdwDir.mkdirs())
                throw new IOException("Unable to create metadata directory under: " + pkgDir);
        }

        String pkgContent = new YamlBuilder(packageVO.getJson(false)).toString();

        write(pkgContent.getBytes(), pkgDir.getMetaFile());

        packageVO.setId(versionControl.getId(pkgDir.getLogicalDir()));

        if (deep) {
            saveActivityImplementors(packageVO, pkgDir);
            saveExternalEventHandlers(packageVO, pkgDir);
            saveAssets(packageVO, pkgDir);
            saveTaskTemplates(packageVO, pkgDir);
            saveProcesses(packageVO, pkgDir); // also saves v0 task templates
        }

        return packageVO.getPackageId();
    }

    public Process loadProcess(PackageDir pkgDir, AssetFile assetFile, boolean deep) throws IOException, XmlException, JSONException, DataAccessException {
        Process process;
        if (deep) {
            String content = new String(read(assetFile));
            process = new Process(new JsonObject(content));
        }
        else {
            process = new Process();
        }

        process.setId(assetFile.getId());
        int lastDot = assetFile.getName().lastIndexOf('.');
        process.setName(assetFile.getName().substring(0, lastDot));
        process.setLanguage(Asset.PROCESS);
        process.setRawFile(assetFile);
        process.setVersion(assetFile.getRevision().getVersion());
        process.setModifyDate(assetFile.getRevision().getModDate());
        process.setModifyingUser(assetFile.getRevision().getModUser());
        process.setRevisionComment(assetFile.getRevision().getComment());
        process.setPackageName(pkgDir.getPackageName());

        return process;
    }

    public long save(Process process, PackageDir pkgDir) throws IOException, XmlException, JSONException, DataAccessException {
        process.removeEmptyAndOverrideAttributes();
        // save task templates
        List<ActivityImplementor> impls = getActivityImplementors();  // TODO maybe cache these
        for (Activity activity : process.getActivities()) {
            for (ActivityImplementor impl : impls) {
                if (activity.getImplementor().equals(impl.getImplementorClassName())) {
                    if (impl.isManualTask()) {
                        if (activity.getAttribute(TaskActivity.ATTRIBUTE_TASK_TEMPLATE) != null) {
                            removeObsoleteTaskActivityAttributes(activity);
                        }
                    }
                }
            }
        }
        if (process.getSubprocesses() != null) {
            for (Process embedded : process.getSubprocesses()) {
                for (Activity activity : embedded.getActivities()) {
                    for (ActivityImplementor impl : impls) {
                        if (activity.getImplementor().equals(impl.getImplementorClassName())) {
                            if (impl.isManualTask()) {
                                if (activity.getAttribute(TaskActivity.ATTRIBUTE_TASK_TEMPLATE) != null) {
                                    removeObsoleteTaskActivityAttributes(activity);
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

    public Asset loadAsset(PackageDir pkgDir, AssetFile assetFile, boolean deep) throws IOException, XmlException {
        Asset asset = new Asset();
        asset.setId(assetFile.getId());
        asset.setPackageName(pkgDir.getPackageName());
        asset.setName(assetFile.getName());
        asset.setLanguage(Asset.getFormat(assetFile.getName()));
        asset.setVersion(assetFile.getRevision().getVersion());
        asset.setLoadDate(new Date());
        asset.setModifyDate(assetFile.getRevision().getModDate());
        asset.setRevisionComment(assetFile.getRevision().getComment());
        asset.setRawFile(assetFile);
        if (deep) {
            asset.setRaw(true);
            // do not load jar assets into memory
            if (!Asset.excludedFromMemoryCache(assetFile.getName()))
                asset.setRawContent(read(assetFile));
        }
        return asset;
    }

    public long save(Asset asset, PackageDir pkgDir) throws IOException {
        AssetFile assetFile = pkgDir.getAssetFile(getAssetFile(asset), getAssetRevision(asset));
        write(asset.getContent(), assetFile);
        asset.setId(versionControl.getId(assetFile.getLogicalFile()));
        asset.setVersion(assetFile.getRevision().getVersion());
        asset.setModifyingUser(assetFile.getRevision().getModUser());
        asset.setModifyDate(assetFile.getRevision().getModDate());
        asset.setRevisionComment(assetFile.getRevision().getComment());
        asset.setPackageName(pkgDir.getPackageName());
        asset.setRawFile(assetFile);

        return asset.getId();
    }

    public ActivityImplementor loadActivityImplementor(PackageDir pkgDir, AssetFile assetFile) throws IOException, XmlException, JSONException {
        String content = new String(read(assetFile));
        ActivityImplementor implVo = new ActivityImplementor(new JsonObject(content));
        implVo.setImplementorId(assetFile.getId());
        implVo.setPackageName(pkgDir.getPackageName());
        return implVo;
    }

    public long save(ActivityImplementor implVo, PackageDir pkgDir) throws IOException, JSONException {
        String content = implVo.getJson().toString(2);
        AssetFile assetFile = pkgDir.getAssetFile(getActivityImplementorFile(implVo), null); // no revs
        write(content.getBytes(), assetFile);
        implVo.setImplementorId(versionControl.getId(assetFile.getLogicalFile()));
        return implVo.getImplementorId();
    }

    public ExternalEvent loadExternalEventHandler(PackageDir pkgDir, AssetFile assetFile) throws IOException, XmlException, JSONException {
        String content = new String(read(assetFile));
        ExternalEvent evthVo = new ExternalEvent(new JsonObject(content));
        evthVo.setId(assetFile.getId());
        int lastDot = assetFile.getName().lastIndexOf('.');
        evthVo.setEventName(assetFile.getName().substring(0, lastDot));
        evthVo.setPackageName(pkgDir.getPackageName());
        return evthVo;
    }

    public long save(ExternalEvent evthVo, PackageDir pkgDir) throws IOException, JSONException {
        String content = evthVo.getJson().toString(2);
        AssetFile assetFile = pkgDir.getAssetFile(getExternalEventHandlerFile(evthVo), null); // no revs
        write(content.getBytes(), assetFile);
        evthVo.setId(versionControl.getId(assetFile.getLogicalFile()));
        return evthVo.getId();
    }

    public TaskTemplate loadTaskTemplate(PackageDir pkgDir, AssetFile assetFile) throws IOException, XmlException, JSONException {
        String content = new String(read(assetFile));
        TaskTemplate taskVO = new TaskTemplate(new JsonObject(content));
        taskVO.setName(assetFile.getName());
        taskVO.setTaskId(assetFile.getId());
        taskVO.setPackageName(pkgDir.getPackageName());
        taskVO.setVersion(assetFile.getRevision().getVersion());
        return taskVO;
    }

    public long save(TaskTemplate taskVo, PackageDir pkgDir) throws IOException, JSONException {
        String content = taskVo.getJson().toString(2);
        AssetFile assetFile = pkgDir.getAssetFile(getTaskTemplateFile(taskVo), taskVo.getVersion() > 0 ? getAssetRevision(taskVo) : null);
        write(content.getBytes(), assetFile);
        taskVo.setTaskId(versionControl.getId(assetFile.getLogicalFile()));
        return taskVo.getTaskId();
    }

    public List<Process> loadProcesses(PackageDir pkgDir, boolean deep) throws IOException, XmlException, JSONException, DataAccessException {
        List<Process> processes = new ArrayList<Process>();
        for (File procFile : pkgDir.listFiles(procFileFilter))
            processes.add(loadProcess(pkgDir, pkgDir.getAssetFile(procFile), deep));
        return processes;
    }

    public void saveProcesses(Package packageVo, PackageDir pkgDir) throws IOException, XmlException, JSONException, DataAccessException {
        if (packageVo.getProcesses() != null) {
            for (Process process : packageVo.getProcesses()) {
                process.setPackageName(packageVo.getName());
                save(process, pkgDir);
            }
        }
    }

    public List<Asset> loadAssets(PackageDir pkgDir, boolean deep) throws IOException, XmlException {
        List<Asset> assets = new ArrayList<Asset>();
        for (File rsFile : pkgDir.listFiles(assetFileFilter))
            assets.add(loadAsset(pkgDir, pkgDir.getAssetFile(rsFile), deep));
        Collections.sort(assets, new Comparator<Asset>() {
            public int compare(Asset rs1, Asset rs2) {
                if (rs1.getName().equals(rs2.getName()))
                    return rs2.getVersion() - rs1.getVersion(); // later versions first
                else
                    return rs1.getName().compareToIgnoreCase(rs2.getName()); // alphabetically by name
            }
        });
        return assets;
    }

    public void saveAssets(Package packageVo, PackageDir pkgDir) throws IOException {
        if (packageVo.getAssets() != null) {
            for (Asset asset : packageVo.getAssets()) {
                if (!asset.isEmpty()) {
                    asset.setPackageName(packageVo.getName());
                    save(asset, pkgDir);
                }
            }
        }
    }

    public List<ActivityImplementor> loadActivityImplementors(PackageDir pkgDir) throws IOException, XmlException, JSONException {
        List<ActivityImplementor> impls = new ArrayList<ActivityImplementor>();
        for (File implFile : pkgDir.listFiles(implFileFilter))
            impls.add(loadActivityImplementor(pkgDir, pkgDir.getAssetFile(implFile)));
        return impls;
    }

    public void saveActivityImplementors(Package packageVo, PackageDir pkgDir) throws IOException, XmlException, JSONException, DataAccessException {
        if (packageVo.getImplementors() != null && !packageVo.getImplementors().isEmpty()) {
            List<ActivityImplementor> existingImpls = new ArrayList<ActivityImplementor>();
            for (PackageDir existPkgDir : getPackageDirs()) {
                if (!existPkgDir.isArchive())
                    existingImpls.addAll(loadActivityImplementors(existPkgDir));
            }
            for (ActivityImplementor impl : packageVo.getImplementors()) {
                boolean alreadyPresentInAnotherPackage = false;
                for (ActivityImplementor existingImpl : existingImpls) {
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

    public List<ExternalEvent> loadExternalEventHandlers(PackageDir pkgDir) throws IOException, XmlException, JSONException {
        List<ExternalEvent> evtHandlers = new ArrayList<ExternalEvent>();
        for (File evthFile : pkgDir.listFiles(evthFileFilter))
            evtHandlers.add(loadExternalEventHandler(pkgDir, pkgDir.getAssetFile(evthFile)));
        return evtHandlers;
    }

    public void saveExternalEventHandlers(Package packageVo, PackageDir pkgDir) throws IOException, JSONException {
        if (packageVo.getExternalEvents() != null) {
            for (ExternalEvent evth : packageVo.getExternalEvents()) {
                evth.setPackageName(pkgDir.getPackageName());
                save(evth, pkgDir);
            }
        }
    }

    public List<TaskTemplate> loadTaskTemplates(PackageDir pkgDir) throws IOException, XmlException, JSONException {
        List<TaskTemplate> tasks = new ArrayList<TaskTemplate>();
        for (File taskFile : pkgDir.listFiles(taskFileFilter))
            tasks.add(loadTaskTemplate(pkgDir, pkgDir.getAssetFile(taskFile)));
        return tasks;
    }

    public void saveTaskTemplates(Package packageVo, PackageDir pkgDir) throws IOException, JSONException {
        if (packageVo.getTaskTemplates() != null) {
            for (TaskTemplate task : packageVo.getTaskTemplates()) {
                task.setPackageName(pkgDir.getPackageName());
                save(task, pkgDir);
            }
        }
    }

    /**
     * Only for top-level packages, and relies on getPackageName().
     */
    private File getProcessFile(Process process) {
        String fileName = process.getName() + PROCESS_FILE_EXTENSION;
        return new File(storageDir + "/" + process.getPackageName().replace('.', '/') + "/" + fileName);
    }

    /**
     * Only for top-level packages, and relies on getPackageName().
     */
    private File getAssetFile(Asset asset) {
        String fileName = asset.getName();
        if (fileName.indexOf('.') < 0)
            fileName += Asset.getFileExtension(asset.getLanguage());
        return new File(storageDir + "/" + asset.getPackageName().replace('.', '/') + "/" + fileName);
    }

    /**
     * Only for top-level packages, and relies on getPackageName().
     */
    private File getActivityImplementorFile(ActivityImplementor implementor) {
        String fileName = implementor.getSimpleName() + IMPL_FILE_EXTENSION;
        return new File(storageDir + "/" + implementor.getPackageName().replace('.', '/') + "/" + fileName);
    }

    /**
     * Only for top-level packages, and relies on getPackageName().
     */
    private File getExternalEventHandlerFile(ExternalEvent eventHandler) {
        String fileName = eventHandler.getSimpleName() + EVT_HANDLER_FILE_EXTENSION;
        return new File(storageDir + "/" + eventHandler.getPackageName().replace('.', '/') + "/" + fileName);
    }

    /**
     * Only for top-level packages, and relies on getPackageName().
     */
    private File getTaskTemplateFile(TaskTemplate taskTemplate) {
        String fileName;
        if (taskTemplate.getVersion() > 0)
            fileName = taskTemplate.getName();  // use asset name
        else
            fileName = taskTemplate.getTaskName() + TASK_TEMPLATE_FILE_EXTENSION;
        return new File(storageDir + "/" + taskTemplate.getPackageName().replace('.', '/') + "/" + fileName);
    }

    public AssetRevision getAssetRevision(Asset asset) {
        AssetRevision rev = new AssetRevision();
        rev.setVersion(asset.getVersion());
        rev.setModDate(new Date());
        rev.setModUser(user);
        rev.setComment(asset.getRevisionComment());
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

    protected void removeObsoleteTaskActivityAttributes(Activity manualTaskActivity) {
        if (manualTaskActivity.getAttribute(TaskActivity.ATTRIBUTE_TASK_TEMPLATE) != null) {
            List<Attribute> attributes = new ArrayList<Attribute>();
            List<String> obsoleteAttributes = Arrays.asList(TaskActivity.ATTRIBUTES_MOVED_TO_TASK_TEMPLATE);
            for (Attribute attribute : manualTaskActivity.getAttributes()) {
                if (!obsoleteAttributes.contains(attribute.getAttributeName()))
                    attributes.add(attribute);
            }
            manualTaskActivity.setAttributes(attributes);
        }
    }

    // loader api methods
    public Package loadPackage(Long packageId, boolean deep) throws DataAccessException {
        try {
            PackageDir pkgDir = getPackageDir(packageId);
            return pkgDir == null ? null : loadPackage(pkgDir, deep);
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    public Package getPackage(String name) throws DataAccessException {
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
        File logicalDir = getVersionControl().getFile(packageId);
        for (PackageDir pkgDir : getPackageDirs()) {
            if (pkgDir.getLogicalDir().equals(logicalDir))
                return pkgDir;
        }
        return null;
    }

    /**
     * this clears the cached package list
     */
    public synchronized List<Package> getPackageList(boolean deep, ProgressMonitor progressMonitor) throws DataAccessException {

        pkgDirs = null;
        versionControl.clear();

        if (progressMonitor != null)
            progressMonitor.progress(10);

        List<Package> packages = new ArrayList<Package>();

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

    public List<Process> getProcessList() throws DataAccessException {
        List<Process> processes = new ArrayList<Process>();
        try {
            for (PackageDir pkgDir : getPackageDirs())
                processes.addAll(loadProcesses(pkgDir, false));
            for (AssetRef ref : AssetRefCache.getAllProcessRefs()) {
                Process proc = AssetRefConverter.getProcess(ref);
                if (proc != null && !processes.contains(proc))
                    processes.add(proc);
            }
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

    public Process loadProcess(Long processId, boolean withSubProcesses) throws DataAccessException {
        File logicalFile = getVersionControl().getFile(processId);
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

    public Process getProcessBase(Long processId) throws DataAccessException {
        return loadProcess(processId, true);
    }

    public Process getProcessBase(String name, int version) throws DataAccessException {
        List<Process> versions = version == 0 ? new ArrayList<Process>() : null;
        String plainName = name;
        String pkgName = null;
        int lastSlash = name.lastIndexOf('/');
        if (lastSlash >= 0) {
            pkgName = plainName.substring(0, lastSlash);
            plainName = plainName.substring(lastSlash + 1);
        }
        for (PackageDir pkgDir : getPackageDirs()) {  // This returns current Pkgs before Archive Pkgs
            if (pkgName == null || pkgName.equals(pkgDir.getPackageName())) {
                for (File procFile : pkgDir.listFiles(procFileFilter)) {
                    String fileName = procFile.getName();
                    if (fileName.substring(0, fileName.length() - PROCESS_FILE_EXTENSION.length()).equals(plainName)) {
                        try {
                            Process process = loadProcess(pkgDir, pkgDir.getAssetFile(procFile), true);
                            if (version == 0) {  // Version 0 means we want the latest version
                                if (!pkgDir.isArchive()) // Assume current asset is latest version
                                    return process;
                                else  // Only applies to deleted processes that only exist in Archive
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
        }
        Process found = null;
        if (version == 0 && !versions.isEmpty()) {
            for (Process proc : versions) {
                if (found == null || found.getVersion() < proc.getVersion())
                    found = proc;
            }
        }
        return found;
    }

    public List<Asset> getAssets() throws DataAccessException {
        List<Asset> assets = new ArrayList<Asset>();
        try {
            for (PackageDir pkgDir : getPackageDirs())
                assets.addAll(loadAssets(pkgDir, false));
            return assets;
        }
        catch (IOException ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
        catch (XmlException ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    public Asset getAsset(Long id) throws DataAccessException {
        try {
            File logicalFile = getVersionControl().getFile(id);
            PackageDir pkgDir = getPackageDir(logicalFile);
            return loadAsset(pkgDir, pkgDir.findAssetFile(logicalFile), true);
        }
        catch (IOException ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
        catch (XmlException ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    public Asset getAsset(String name, String language, int version) throws DataAccessException {
        for (Asset asset : getAssets()) {
            if (asset.getName().equals(name) && (asset.getVersion() == version || version == 0)) {
                try {
                    File logicalFile = getVersionControl().getFile(asset.getId());
                    PackageDir pkgDir = getPackageDir(logicalFile);
                    return loadAsset(pkgDir, pkgDir.findAssetFile(logicalFile), true);
                }
                catch (Exception ex) {
                    throw new DataAccessException(ex.getMessage(), ex);
                }
            }
        }
        return null;
    }

    public Asset getAsset(Long packageId, String name) throws DataAccessException {
        PackageDir pkgDir = getPackageDir(packageId);
        for (File assetFile : pkgDir.listFiles(assetFileFilter)) {
            if (assetFile.getName().equals(name)) {
                try {
                    return loadAsset(pkgDir, pkgDir.getAssetFile(assetFile), true);
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

    public Asset getAssetForOwner(String ownerType, Long ownerId) throws DataAccessException {
        if (ownerType.equals(OwnerType.PACKAGE)) {
            PackageDir pkgDir = getPackageDir(ownerId);
            try {
                Package pkgVO = loadPackage(pkgDir, false);
                if (pkgVO.getMetaContent() != null) {
                    Asset asset = new Asset();
                    asset.setLanguage(Asset.CONFIG);
                    asset.setStringContent(pkgVO.getMetaContent());
                    return asset;
                }
            }
            catch (Exception ex) {
                throw new DataAccessException(ex.getMessage(), ex);
            }
        }
        return null;
    }

    public List<ActivityImplementor> getActivityImplementors() throws DataAccessException {
        try {
            Map<String,ActivityImplementor> impls = new HashMap<>();
            for (PackageDir pkgDir : getPackageDirs()) {
                for (ActivityImplementor impl : loadActivityImplementors(pkgDir)) {
                    if (!impls.containsKey(impl.getImplementorClassName()))
                        impls.put(impl.getImplementorClassName(), impl);
                }
            }
            List<ActivityImplementor> modifiableList = new ArrayList<>();
            modifiableList.addAll(Arrays.asList(impls.values().toArray(new ActivityImplementor[0])));
            return modifiableList;
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    public List<ActivityImplementor> getReferencedImplementors(Package packageVO) throws DataAccessException {
        PackageDir pkgDir = getPackageDir(packageVO.getId());
        List<String> implClasses = new ArrayList<String>();
        try {
            for (Process process : loadProcesses(pkgDir, true)) {
                for (Activity activity : process.getActivities()) {
                    String implClass = activity.getImplementor();
                    if (!implClasses.contains(implClass))
                        implClasses.add(implClass);
                }
            }
            List<ActivityImplementor> referencedImpls = new ArrayList<ActivityImplementor>();
            List<ActivityImplementor> allImpls = getActivityImplementors();  // TODO cache these
            for (String implClass : implClasses) {
                for (ActivityImplementor impl : allImpls) {
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

    public List<ExternalEvent> loadExternalEvents() throws DataAccessException {
        try {
            List<ExternalEvent> evtHandlers = new ArrayList<ExternalEvent>();
            for (PackageDir pkgDir : getPackageDirs())
                evtHandlers.addAll(loadExternalEventHandlers(pkgDir));
            return evtHandlers;
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    public List<TaskTemplate> getTaskTemplates() throws DataAccessException {
        try {
            List<TaskTemplate> tasks = new ArrayList<TaskTemplate>();
            for (PackageDir pkgDir : getPackageDirs()) {
                for (TaskTemplate pkgTask : loadTaskTemplates(pkgDir)) {
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

    public List<VariableType> getVariableTypes() throws DataAccessException {
        List<VariableType> types = baselineData.getVariableTypes();
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
    public List<Process> findCallingProcesses(Process subproc) throws DataAccessException {
        List<Process> callers = new ArrayList<Process>();
        try {
            Pattern singleProcPattern = Pattern.compile("^.*\"processname\": \".*" + subproc.getName() + ".*\"", Pattern.MULTILINE);
            Pattern multiProcPattern = Pattern.compile("^.*\"processmap\": \".*" + subproc.getName() + ".*\"", Pattern.MULTILINE);
            for (PackageDir pkgDir : getPackageDirs()) {
                for (File procFile : pkgDir.listFiles(procFileFilter)) {
                    String json = new String(read(procFile));
                    if (singleProcPattern.matcher(json).find() || multiProcPattern.matcher(json).find()) {
                        Process procVO = loadProcess(pkgDir, pkgDir.getAssetFile(procFile), true);
                        for (Activity activity : procVO.getActivities()) {
                            if (activityInvokesProcess(activity, subproc) && !callers.contains(procVO))
                                callers.add(procVO);
                        }
                        for (Process embedded : procVO.getSubprocesses()) {
                            for (Activity activity : embedded.getActivities()) {
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
    public List<Process> findCalledProcesses(Process mainproc) throws DataAccessException {
        // make sure process is loaded
        mainproc = loadProcess(mainproc.getId(), false);
        return findInvoked(mainproc, getProcessList());
    }

    public List<Process> getProcessListForImplementor(Long implementorId, String implementorClass) throws DataAccessException {
        List<Process> processes = new ArrayList<Process>();
        try {
            String implDecl = "\"implementor\": \"" + implementorClass + "\"";  // crude but fast
            for (PackageDir pkgDir : getPackageDirs()) {
                for (File procFile : pkgDir.listFiles(procFileFilter)) {
                    String json = new String(read(procFile));
                    if (json.contains(implDecl))
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
    public Long persistPackage(Package packageVO, PersistType persistType) throws DataAccessException {
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
                    getVersionControl().clearId(existingTopLevel.getLogicalDir());
                }
                pkgDir = createPackage(packageVO);
            }
            else {
                pkgDir = getTopLevelPackageDir(packageVO.getName());
            }
            pkgDir.setYaml(true);
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
            Package pkg = loadPackage(packageId, false);
            pkg.setName(newName);
            pkg.setVersion(newVersion);
            save(pkg, oldPkgDir, false); // at this point package.xml name doesn't match oldPkgDir
            newPkg = renamePkgDir(oldPkgDir, newPkg);
            getVersionControl().clearId(oldPkgDir.getLogicalDir());
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
            getVersionControl().clearId(pkgDir.getLogicalDir());
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
    public Long persistProcess(Process process, PersistType persistType) throws DataAccessException {
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
        File logicalFile = getVersionControl().getFile(processId);
        PackageDir pkgDir = getPackageDir(logicalFile);
        try {
            File existFile = pkgDir.findAssetFile(logicalFile);
            Process proc = loadProcess(pkgDir, pkgDir.getAssetFile(existFile), true);
            proc.setName(newName);
            proc.setVersion(newVersion);
            save(proc, pkgDir); // update version and name
            File newFile = new File(existFile.getParentFile() + "/" + newName + PROCESS_FILE_EXTENSION);
            delete(existFile);
            getVersionControl().clearId(logicalFile);
            getVersionControl().deleteRev(existFile);
            getVersionControl().setRevision(newFile, getAssetRevision(newVersion, "Renamed from " + existFile));
            // prime the cache
            loadProcess(pkgDir, pkgDir.getAssetFile(newFile), false);
            return proc.getId();
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    public void deleteProcess(Process process) throws DataAccessException {
        deleteProcess(process.getId());
    }

    /**
     * may have already been deleted by removeProcessFromPackage()
     */
    public void deleteProcess(Long processId) throws DataAccessException {
        File logicalFile = getVersionControl().getFile(processId);
        if (logicalFile != null) {
            PackageDir pkgDir = getPackageDir(logicalFile);
            try {
                File procFile = pkgDir.findAssetFile(logicalFile);
                if (procFile.exists()) {
                    delete(procFile);
                    getVersionControl().clearId(logicalFile);
                    getVersionControl().deleteRev(procFile);
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
        File logicalFile = getVersionControl().getFile(processId);
        PackageDir pkgDir = getPackageDir(logicalFile);
        try {
            Process process = loadProcess(pkgDir, pkgDir.findAssetFile(logicalFile), true);
            PackageDir newPkgDir = getPackageDir(packageId);
            File newFile = new File(newPkgDir + "/" + process.getName() + PROCESS_FILE_EXTENSION);
            save(process, newPkgDir);
            getVersionControl().setRevision(newFile, getAssetRevision(process.getVersion(), null));
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
    public Long createAsset(Asset asset) throws DataAccessException {
        try {
          asset.setVersion(1);
          long id = save(asset, getTopLevelPackageDir(asset.getPackageName()));
          asset.setId(id);
          return id;
        }
        catch (IOException ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    /**
     * asset is saved locally; just update the version
     */
    public void updateAsset(Asset asset) throws DataAccessException {
        try {
            getVersionControl().setRevision(getAssetFile(asset), getAssetRevision(asset));
        }
        catch (IOException ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    /**
     * updates the version
     */
    public void updateProcess(Process process) throws DataAccessException {
        try {
            getVersionControl().setRevision(getProcessFile(process), getAssetRevision(process));
        }
        catch (IOException ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    public void renameAsset(Asset asset, String newName) throws DataAccessException  {
        File logicalFile = getVersionControl().getFile(asset.getId());
        PackageDir pkgDir = getPackageDir(logicalFile);
        try {
            File oldFile = pkgDir.findAssetFile(logicalFile);
            File newFile = new File(oldFile.getParentFile() + "/" + newName);
            rename(oldFile, newFile);
            asset.setName(newName);
            getVersionControl().setRevision(getAssetFile(asset), getAssetRevision(asset));
            getVersionControl().clearId(logicalFile);
            getVersionControl().setRevision(newFile, getAssetRevision(asset.getVersion(), "Renamed from " + oldFile));
            Asset newAsset = loadAsset(pkgDir, pkgDir.getAssetFile(newFile), false); // prime
            asset.setId(newAsset.getId());
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    /**
     * may have already been deleted by removeAssetFromPackage()
     */
    public void deleteAsset(Long assetId) throws DataAccessException {
        File logicalFile = getVersionControl().getFile(assetId);
        if (logicalFile != null) {
            PackageDir pkgDir = getPackageDir(logicalFile);
            try {
                File assetFile = pkgDir.findAssetFile(logicalFile);
                if (assetFile.exists()) {
                    delete(assetFile);
                    getVersionControl().clearId(logicalFile);
                    getVersionControl().deleteRev(assetFile);
                }
            }
            catch (IOException ex) {
                throw new DataAccessException(ex.getMessage(), ex);
            }
        }
    }

    /**
     * in move scenario, must be called before removeAssetFromPackage()
     */
    public long addAssetToPackage(Long assetId, Long packageId) throws DataAccessException {
        File logicalFile = getVersionControl().getFile(assetId);
        PackageDir pkgDir = getPackageDir(logicalFile);
        try {
            AssetFile existFile = pkgDir.findAssetFile(logicalFile);
            Asset asset = loadAsset(pkgDir, existFile, true);
            PackageDir newPkgDir = getPackageDir(packageId);
            File newFile = new File(newPkgDir + "/" + existFile.getName());
            save(asset, newPkgDir);
            getVersionControl().setRevision(newFile, getAssetRevision(asset.getVersion(), null));
            return loadAsset(newPkgDir, newPkgDir.getAssetFile(newFile), false).getId();
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    /**
     * file may have been deleted in deleteAsset()
     */
    public void removeAssetFromPackage(Long assetId, Long packageId) throws DataAccessException {
        deleteAsset(assetId);
    }

    /**
     * Saves to top-level package.  Expects getPackageName() to be set.
     */
    public Long createActivityImplementor(ActivityImplementor implementor) throws DataAccessException {
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

    public void updateActivityImplementor(ActivityImplementor implementor) throws DataAccessException {
        createActivityImplementor(implementor);
    }

    /**
     * may have already been deleted due to removeActivityImplFromPackage
     */
    public void deleteActivityImplementor(Long implementorId) throws DataAccessException {
        File logicalFile = getVersionControl().getFile(implementorId);
        if (logicalFile != null) {
            PackageDir pkgDir = getPackageDir(logicalFile);
            try {
                File implFile = pkgDir.findAssetFile(logicalFile);
                if (implFile.exists()) {
                    delete(implFile);
                    getVersionControl().clearId(logicalFile);
                    getVersionControl().deleteRev(implFile);
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
        File logicalFile = getVersionControl().getFile(activityImplId);
        PackageDir pkgDir = getPackageDir(logicalFile);
        try {
            ActivityImplementor implementor = loadActivityImplementor(pkgDir, pkgDir.findAssetFile(logicalFile));
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
    public void createExternalEvent(ExternalEvent eventHandler) throws DataAccessException {
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

    public void updateExternalEvent(ExternalEvent eventHandler) throws DataAccessException {
        createExternalEvent(eventHandler);
    }

    /**
     * may have already been deleted due to removeExternalEventFromPackage()
     */
    public void deleteExternalEvent(Long eventHandlerId) throws DataAccessException {
        File logicalFile = getVersionControl().getFile(eventHandlerId);
        if (logicalFile != null) {
            PackageDir pkgDir = getPackageDir(logicalFile);
            try {
                File evthFile = pkgDir.findAssetFile(logicalFile);
                if (evthFile.exists()) {
                    delete(evthFile);
                    getVersionControl().clearId(logicalFile);
                    getVersionControl().deleteRev(evthFile);
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
        File logicalFile = getVersionControl().getFile(externalEventId);
        PackageDir pkgDir = getPackageDir(logicalFile);
        try {
            ExternalEvent eventHandler = loadExternalEventHandler(pkgDir, pkgDir.findAssetFile(logicalFile));
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
    public void createTaskTemplate(TaskTemplate taskTemplate) throws DataAccessException {
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
        File logicalFile = getVersionControl().getFile(taskId);
        if (logicalFile != null) {
            PackageDir pkgDir = getPackageDir(logicalFile);
            try {
                File taskFile = pkgDir.findAssetFile(logicalFile);
                if (taskFile.exists()) {
                    delete(taskFile);
                    getVersionControl().clearId(logicalFile);
                    getVersionControl().deleteRev(taskFile);
                }
            }
            catch (IOException ex) {
                throw new DataAccessException(ex.getMessage(), ex);
            }
        }
    }

    public void updateTaskTemplate(TaskTemplate taskTemplate) throws DataAccessException {
        createTaskTemplate(taskTemplate);
    }

    /**
     * in move scenario, must be called before removeTaskTemplateFromPackage()
     */
    public long addTaskTemplateToPackage(Long taskId, Long packageId) throws DataAccessException {
        File logicalFile = getVersionControl().getFile(taskId);
        PackageDir pkgDir = getPackageDir(logicalFile);
        try {
            TaskTemplate taskTemplate = loadTaskTemplate(pkgDir, pkgDir.findAssetFile(logicalFile));
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

    public boolean activityInvokesProcess(Activity activity, Process subproc) {
        String procName = activity.getAttribute(WorkAttributeConstant.PROCESS_NAME);
        if (procName != null && (procName.equals(subproc.getName()) || procName.endsWith("/" + subproc.getName()))) {
            String verSpec = activity.getAttribute(WorkAttributeConstant.PROCESS_VERSION);
            try {
                // compatibility
                int ver = Integer.parseInt(verSpec);
                verSpec = Asset.formatVersion(ver);
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
                            verSpec = Asset.formatVersion(ver);
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

    public List<Process> findInvoked(Process caller, List<Process> processes) {
        List<Process> called = new ArrayList<Process>();
        if (caller.getActivities() != null) {
            for (Activity activity : caller.getActivities()) {
                String procName = activity.getAttribute(WorkAttributeConstant.PROCESS_NAME);
                if (procName != null) {
                    String verSpec = activity.getAttribute(WorkAttributeConstant.PROCESS_VERSION);
                    if (verSpec != null) {
                        try {
                            // compatibility
                            int ver = Integer.parseInt(verSpec);
                            verSpec = Asset.formatVersion(ver);
                        }
                        catch (NumberFormatException ex) {}

                        Process latestMatch = null;
                        for (Process process : processes) {
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
                                        verSpec = Asset.formatVersion(ver);
                                    }
                                    catch (NumberFormatException ex) {}

                                Process latestMatch = null;
                                for (Process process : processes) {
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