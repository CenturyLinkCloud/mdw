package com.centurylink.mdw.services.workflow;

import com.centurylink.mdw.cache.impl.AssetRefCache;
import com.centurylink.mdw.cli.Hierarchy;
import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.dataaccess.AssetRef;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.dataaccess.file.VersionControlGit;
import com.centurylink.mdw.model.asset.AssetInfo;
import com.centurylink.mdw.model.asset.AssetVersion;
import com.centurylink.mdw.model.asset.CommitInfo;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.workflow.*;
import com.centurylink.mdw.service.data.activity.ImplementorCache;
import com.centurylink.mdw.service.data.process.ProcessCache;
import com.centurylink.mdw.services.AssetServices;
import com.centurylink.mdw.services.DesignServices;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DesignServicesImpl implements DesignServices {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    @Override
    public Process getProcessDefinition(String assetPath, Query query) throws ServiceException {
        int lastSlash = assetPath.lastIndexOf('/');
        if (lastSlash <= 0)
            throw new ServiceException(ServiceException.BAD_REQUEST, "Bad asset path: " + assetPath);
        String processName = assetPath; //.substring(lastSlash + 1);
        if (assetPath.endsWith(".proc"))
            processName = processName.substring(0, processName.length() - ".proc".length());

        int version = query == null ? 0 : query.getIntFilter("version");
        if (version < 0)
            version = 0;
        boolean forUpdate = query == null ? false : query.getBooleanFilter("forUpdate");
        String stagingCuid = query == null ? null : query.getFilter("stagingUser");
        Process process = null;
        if (stagingCuid != null) {
            if (!assetPath.endsWith(".proc"))
                assetPath += ".proc";
            AssetInfo stagedAsset = ServiceLocator.getStagingServices().getStagedAsset(stagingCuid, assetPath);
            if (stagedAsset != null) {
                process = new Process();
                process.setRawFile(stagedAsset.getFile());
            }
        } else {
            process = ProcessCache.getProcess(processName, version);
        }
        if (forUpdate && process != null && version == 0) {
            // load from file
            try {
                byte[] bytes = Files.readAllBytes(Paths.get(process.getRawFile().getAbsolutePath()));
                process = Process.fromString(new String(bytes));
                process.setName(processName.substring(lastSlash + 1));
                process.setPackageName(processName.substring(0, lastSlash));
            }
            catch (Exception ex) {
                throw new ServiceException(ServiceException.INTERNAL_ERROR, "Error reading process: " + process.getRawFile());
            }
        }
        if (process == null)
            throw new ServiceException(ServiceException.NOT_FOUND, "Process definition not found: " + assetPath);

        return process;
    }

    @Override
    public List<Process> getProcessDefinitions(Query query) throws ServiceException {
        try {
            String find = query.getFind();
            if (find == null) {
                return ProcessCache.getAllProcesses();
            }
            else {
                List<Process> found = new ArrayList<>();
                String findLower = find.toLowerCase();
                for (Process processVO : ProcessCache.getAllProcesses()) {
                    if (processVO.getName() != null && processVO.getName().toLowerCase().startsWith(findLower))
                        found.add(processVO);
                    else if (find.indexOf(".") > 0 && processVO.getPackageName() != null && processVO.getPackageName().toLowerCase().startsWith(findLower))
                        found.add(processVO);
                }
                return found;
            }
        }
        catch (DataAccessException ex) {
            throw new ServiceException(500, ex.getMessage(), ex);
        }
    }

    @Override
    public Process getProcessDefinition(Long id) {
        return ProcessCache.getProcess(id);
    }

    @Override
    public ActivityList getActivityDefinitions(Query query) throws ServiceException {
        try {
            String find = query.getFind();
            List<ActivityInstance> activityInstanceList = new ArrayList<>();
            ActivityList found = new ActivityList(ActivityList.ACTIVITY_INSTANCES, activityInstanceList);

            if (find == null) {
                List<Process> processes = ProcessCache.getAllProcesses();
                for (Process process : processes) {
                    process = ProcessCache.getProcess(process.getId());
                    List<Activity> activities = process.getActivities();
                    for (Activity activityVO : activities) {
                        if (activityVO.getName() != null && activityVO.getName().startsWith(find)) {
                            ActivityInstance ai = new ActivityInstance();
                            ai.setId(activityVO.getId());
                            ai.setName(activityVO.getName());
                            ai.setDefinitionId(activityVO.getLogicalId());
                            ai.setProcessId(process.getId());
                            ai.setProcessName(process.getName());
                            ai.setProcessVersion(process.getVersionString());
                            activityInstanceList.add(ai);
                        }
                    }
                }
            } else {
                String lowerFind = find.toLowerCase();
                for (Process process : ProcessCache.getAllProcesses()) {
                    process = ProcessCache.getProcess(process.getId());
                    List<Activity> activities = process.getActivities();
                    for (Activity activityVO : activities) {
                        if (activityVO.getName() != null && activityVO.getName().toLowerCase().startsWith(lowerFind)) {
                            ActivityInstance ai = new ActivityInstance();
                            ai.setId(activityVO.getId());
                            ai.setName(activityVO.getName());
                            ai.setDefinitionId(activityVO.getLogicalId());
                            ai.setProcessId(process.getId());
                            ai.setProcessName(process.getName());
                            ai.setProcessVersion(process.getVersionString());
                            activityInstanceList.add(ai);
                        }
                    }
                }
            }
            found.setRetrieveDate(DatabaseAccess.getDbDate());
            found.setCount(activityInstanceList.size());
            found.setTotal(activityInstanceList.size());
            return found;
        } catch (DataAccessException ex) {
            throw new ServiceException(500, ex.getMessage(), ex);
        }
    }

    @Override
    public List<ActivityImplementor> getImplementors() {
        return new ArrayList<>(ImplementorCache.getImplementors().values());
    }

    @Override
    public ActivityImplementor getImplementor(String className) throws ServiceException {
        ActivityImplementor implementor =  ImplementorCache.get(className);
        if (implementor.getPagelet() == null) {
            try {
                for (ActivityImplementor impl : DataAccess.getProcessLoader().getActivityImplementors()) {
                    if (impl.getImplementorClass().equals(implementor.getImplementorClass()))
                        return impl; // loaded from .impl file
                }
            } catch (DataAccessException ex) {
                throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage());
            }
        }
        return implementor; // loaded from annotation or not found
    }

    @Override
    public List<Linked<Process>> getProcessHierarchy(Long processId, boolean downward) throws ServiceException {
        Process process = ProcessCache.getProcess(processId);
        if (process == null)
            throw new ServiceException(ServiceException.NOT_FOUND, "Process not found: " + processId);

        try {
            // all must be loaded
            List<Process> processes = new ArrayList<>();
            for (Process proc : ProcessCache.getAllProcesses()) {
                Process loaded = ProcessCache.getProcess(proc.getId());
                if (loaded != null)
                    processes.add(loaded);
            }
            Hierarchy hierarchy = new Hierarchy(process, processes);
            hierarchy.setDownward(downward);
            hierarchy.run();
            return hierarchy.getTopLevelCallers();
        }
        catch (DataAccessException | IOException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, "Hierarchy error for " + processId, ex);
        }
    }

    public AssetVersion getAsset(String assetPath, String version, boolean withCommitInfo) throws ServiceException {
        String assetName = assetPath.substring(assetPath.indexOf('/') + 1);
        AssetServices assetServices = ServiceLocator.getAssetServices();
        AssetInfo currentAsset = assetServices.getAsset(assetPath);
        try {
            AssetVersion assetVersion;
            if (currentAsset != null && version.equals(currentAsset.getJson().optString("version"))) {
                // if current version just return asset from file system
                JSONObject json = currentAsset.getJson();
                assetVersion = new AssetVersion(json.optLong("id"), assetPath, version);
                if (withCommitInfo) {
                    VersionControlGit vcGit = (VersionControlGit) assetServices.getVersionControl();
                    String assetVcPath = vcGit.getRelativePath(currentAsset.getFile().toPath());
                    assetVersion.setCommitInfo(vcGit.getCommitInfo(assetVcPath));
                }
            } else {
                // if older version check in AssetRefCache
                AssetRef assetRef = AssetRefCache.getRef(assetPath, version);
                if (assetRef == null)
                    throw new ServiceException(ServiceException.NOT_FOUND, "Asset ref not found: " + assetPath + " v" + version);
                assetVersion = new AssetVersion(assetRef);
                VersionControlGit vcGit = (VersionControlGit) assetServices.getVersionControl();
                CommitInfo refCommit = vcGit.getCommitInfoForRef(assetVersion.getRef());
                if (refCommit != null) {
                    String assetVcPath = vcGit.getRelativePath(currentAsset.getFile().toPath());
                    // actual commit is last one before or same as refCommit
                    for (CommitInfo commit : vcGit.getCommits(assetVcPath)) {
                        if (commit.getDate().before(refCommit.getDate()) || commit.getDate().equals(refCommit.getDate())) {
                            assetVersion.setCommitInfo(commit);
                            break;
                        }
                    }
                }
            }

            assetVersion.setName(assetName);
            return assetVersion;
        }
        catch (Exception ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }

    @Override
    public List<AssetVersion> getAssetVersions(String assetPath, Query query) throws ServiceException {
        AssetInfo currentAsset = ServiceLocator.getAssetServices().getAsset(assetPath);
        String currentVersion = "";
        Long currentId = 0L;
        if (currentAsset != null) {
            JSONObject json = currentAsset.getJson();
            currentVersion = json.optString("version");
            currentId = json.optLong("id");
        }
        List<AssetVersion> versions = new ArrayList<>();
        boolean includesCurrent = false;
        for (AssetRef assetRef : AssetRefCache.getRefs(assetPath)) {
            AssetVersion version = new AssetVersion(assetRef);
            versions.add(version);
            if (version.getVersion().equals(currentVersion))
                includesCurrent = true;
        }
        if (!includesCurrent) {
            AssetVersion current = new AssetVersion(currentId, assetPath, currentVersion);
            versions.add(current);
        }

        if (query.getBooleanFilter("withCommitInfo")) {
            AssetServices assetServices = ServiceLocator.getAssetServices();
            try {
                VersionControlGit versionControl = (VersionControlGit) assetServices.getVersionControl();
                if (versionControl != null && PropertyManager.getProperty(PropertyNames.MDW_GIT_USER) != null) {
                    AssetInfo assetInfo = assetServices.getAsset(assetPath, false);
                    if (assetInfo != null) {
                        String assetVcPath = versionControl.getRelativePath(assetInfo.getFile().toPath());
                        try {
                            List<CommitInfo> commits = versionControl.getCommits(assetVcPath);
                            for (int i = 0; i < versions.size(); i++) {
                                AssetVersion assetVersion = versions.get(i);
                                try {
                                    CommitInfo refCommit = versionControl.getCommitInfoForRef(assetVersion.getRef());
                                    if (refCommit == null && i == 0)
                                        refCommit = versionControl.getCommitInfoForRef(versionControl.getCommit());
                                    if (refCommit != null) {
                                        // actual commit is last one before or same as refCommit
                                        for (CommitInfo commit : commits) {
                                            if (commit.getDate().before(refCommit.getDate()) || commit.getDate().equals(refCommit.getDate())) {
                                                commit.setUrl(getCommitUrl(commit));
                                                assetVersion.setCommitInfo(commit);
                                                break;
                                            }
                                        }
                                    }
                                }
                                catch (Exception ex) {
                                    logger.error("Error reading commit for " + assetVersion, ex);
                                }
                            }
                        }
                        catch (Exception ex) {
                            logger.error("Error reading commits for " + assetInfo.getFile().toPath());
                        }
                    }
                }
            }
            catch (IOException ex) {
                throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
            }
        }

        if (assetPath.endsWith(".proc") && query.getBooleanFilter("withInstanceCounts")) {
            for (AssetVersion version : versions) {
                Long processId = version.getId();
                if (processId != null) {
                    Query processQuery = new Query();
                    processQuery.setFilter("processId", processId);
                    try {
                        long count = ServiceLocator.getWorkflowServices().getProcessCount(processQuery);
                        if (count > 0)
                            version.setCount(count);
                    }
                    catch (ServiceException ex) {
                        logger.error(ex.getMessage(), ex);
                    }
                }
            }
        }

        Collections.sort(versions);

        return versions;
    }

    private String getCommitUrl(CommitInfo commitInfo) {
        if (commitInfo.getCommit() != null) {
            String gitUrl = PropertyManager.getProperty(PropertyNames.MDW_GIT_REMOTE_URL);
            if (gitUrl != null && gitUrl.endsWith(".git")) {
                // TODO this doesn't work for BitBucket
                return gitUrl.substring(0, gitUrl.length() - 4) + "/commit/" + commitInfo.getCommit();
            }
        }
        return null;
    }
}
