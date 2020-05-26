package com.centurylink.mdw.services.workflow;

import com.centurylink.mdw.cache.CachingException;
import com.centurylink.mdw.cache.asset.AssetHistory;
import com.centurylink.mdw.cli.Hierarchy;
import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.git.VersionControlGit;
import com.centurylink.mdw.model.asset.AssetVersion;
import com.centurylink.mdw.model.asset.AssetVersionSpec;
import com.centurylink.mdw.model.asset.CommitInfo;
import com.centurylink.mdw.model.asset.api.AssetInfo;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.workflow.*;
import com.centurylink.mdw.service.data.activity.ImplementorCache;
import com.centurylink.mdw.service.data.process.ProcessAggregation;
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
import java.util.Map;
import java.util.stream.Collectors;

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
                process.setFile(stagedAsset.getFile());
            }
        } else {
            try {
                process = ProcessCache.getProcess(processName, version);
            } catch (IOException ex) {
                throw new ServiceException(ServiceException.INTERNAL_ERROR, "Error loading " + assetPath, ex);
            }
        }
        if (forUpdate && process != null && version == 0) {
            // load from file
            try {
                byte[] bytes = Files.readAllBytes(Paths.get(process.getFile().getAbsolutePath()));
                process = Process.fromString(new String(bytes));
                process.setName(processName.substring(lastSlash + 1));
                process.setPackageName(processName.substring(0, lastSlash));
            }
            catch (Exception ex) {
                throw new ServiceException(ServiceException.INTERNAL_ERROR, "Error reading process: " + process.getFile());
            }
        }
        if (process == null)
            throw new ServiceException(ServiceException.NOT_FOUND, "Process definition not found: " + assetPath + " v" + AssetVersion.formatVersion(version));

        return process;
    }

    @Override
    public List<Process> getProcessDefinitions(Query query) {
        String find = query.getFind();
        if (find == null) {
            return ProcessCache.getProcesses(true);
        }
        else {
            List<Process> found = new ArrayList<>();
            String findLower = find.toLowerCase();
            for (Process process : ProcessCache.getProcesses(true)) {
                if (process.getName() != null && process.getName().toLowerCase().startsWith(findLower))
                    found.add(process);
                else if (find.indexOf(".") > 0 && process.getPackageName() != null && process.getPackageName().toLowerCase().startsWith(findLower))
                    found.add(process);
            }
            return found;
        }
    }

    @Override
    public Process getProcessDefinition(Long id) throws IOException {
        return ProcessCache.getProcess(id);
    }

    @Override
    public ActivityList getActivityDefinitions(Query query) throws ServiceException {
        String find = query.getFind();
        List<ActivityInstance> activityInstanceList = new ArrayList<>();
        ActivityList found = new ActivityList(ActivityList.ACTIVITY_INSTANCES, activityInstanceList);

        try {
            if (find == null) {
                List<Process> processes = ProcessCache.getProcesses(true);
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
                int max = query.getMax();
                int i = 0;
                String lowerFind = find.toLowerCase();
                for (Process process : ProcessCache.getProcesses(true)) {
                    process = ProcessCache.getProcess(process.getId());
                    List<Activity> activities = process.getActivities();
                    String procNameVer = process.getName() + " v" + process.getVersionString();
                    for (Activity activity : activities) {
                        if (activity.getName() != null
                                && (activity.getName().toLowerCase().startsWith(lowerFind)
                                || (procNameVer + " " + activity.getName() + " (A" + activity.getId() + ")").toLowerCase().startsWith(lowerFind))) {
                            ActivityInstance ai = new ActivityInstance();
                            ai.setId(activity.getId());
                            ai.setName(activity.getName());
                            ai.setDefinitionId(activity.getLogicalId());
                            ai.setProcessId(process.getId());
                            ai.setProcessName(process.getName());
                            ai.setProcessVersion(process.getVersionString());
                            activityInstanceList.add(ai);
                            if (++i >= max) {
                                found.setRetrieveDate(DatabaseAccess.getDbDate());
                                found.setCount(activityInstanceList.size());
                                found.setTotal(activityInstanceList.size());
                                return found;
                            }
                        }
                    }
                }
            }
            found.setRetrieveDate(DatabaseAccess.getDbDate());
            found.setCount(activityInstanceList.size());
            found.setTotal(activityInstanceList.size());
            return found;
        } catch (IOException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, "Error loading process for query: " + query, ex);
        }
    }

    @Override
    public List<ActivityImplementor> getImplementors() {
        return new ArrayList<>(ImplementorCache.getImplementors().values());
    }

    @Override
    public ActivityImplementor getImplementor(String className) throws ServiceException {
        try {
            return ImplementorCache.get(className);
        }
        catch (CachingException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }

    @Override
    public List<Linked<Process>> getProcessHierarchy(Long processId, boolean downward) throws ServiceException {
        try {
            Process process = ProcessCache.getProcess(processId);
            if (process == null)
                throw new ServiceException(ServiceException.NOT_FOUND, "Process not found: " + processId);

            // all must be loaded
            List<Process> processes = new ArrayList<>();
            for (Process proc : ProcessCache.getProcesses(true)) {
                Process loaded = ProcessCache.getProcess(proc.getId());
                if (loaded != null)
                    processes.add(loaded);
            }
            Hierarchy hierarchy = new Hierarchy(process, processes);
            hierarchy.setDownward(downward);
            hierarchy.run();
            return hierarchy.getTopLevelCallers();
        }
        catch (IOException ex) {
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
                assetVersion = new AssetVersion(assetPath, version);
                if (withCommitInfo) {
                    VersionControlGit vcGit = (VersionControlGit) assetServices.getVersionControl();
                    String assetVcPath = vcGit.getRelativePath(currentAsset.getFile().toPath());
                    assetVersion.setCommitInfo(vcGit.getCommitInfo(assetVcPath));
                }
            } else {
                // if older version check history
                AssetVersionSpec spec = new AssetVersionSpec(assetPath, version);
                assetVersion = AssetHistory.getAssetVersion(spec);
                if (assetVersion == null)
                    throw new ServiceException(ServiceException.NOT_FOUND, "Asset ref not found: " + spec);
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
        if (currentAsset != null) {
            JSONObject json = currentAsset.getJson();
            currentVersion = json.optString("version");
        }
        List<AssetVersion> versions = AssetHistory.getAssetVersions(assetPath);
        if (!currentVersion.isEmpty()) { // can be empty if no current version (deleted asset)
            AssetVersion current = new AssetVersion(assetPath, currentVersion);
            if (!versions.contains(current))
                versions.add(current);
        }

        long before = System.currentTimeMillis();
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
                                if (assetVersion.getRef() != null) {
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
            if (logger.isDebugEnabled()) {
                logger.debug("withCommitInfo takes " + (System.currentTimeMillis() - before) + " ms");
            }
            before = System.currentTimeMillis();
        }

        if (assetPath.endsWith(".proc") && query.getBooleanFilter("withInstanceCounts")) {
            try {
                List<Long> processIds = versions.stream().map(v -> v.getId()).collect(Collectors.toList());
                Map<Long,Long> idToCount = new ProcessAggregation().getInstanceCounts(processIds);
                for (AssetVersion version : versions) {
                    Long count = idToCount.get(version.getId());
                    if (count != null)
                        version.setCount(count);
                }
            }
            catch (DataAccessException ex) {
                logger.error(ex.getMessage(), ex);
            }
            if (logger.isDebugEnabled()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("withInstanceCounts takes " + (System.currentTimeMillis() - before) + " ms");
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
