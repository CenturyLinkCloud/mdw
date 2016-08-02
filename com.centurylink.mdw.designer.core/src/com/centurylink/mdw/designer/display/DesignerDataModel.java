/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.display;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.ProcessImporter;
import com.centurylink.mdw.dataaccess.RemoteAccess;
import com.centurylink.mdw.designer.DesignerDataAccess;
import com.centurylink.mdw.designer.runtime.ProcessInstanceTreeModel;
import com.centurylink.mdw.designer.runtime.ProcessInstanceTreeModel.ProcessInstanceTreeNode;
import com.centurylink.mdw.designer.utils.ImportItem;
import com.centurylink.mdw.designer.utils.NodeMetaInfo;
import com.centurylink.mdw.model.data.task.TaskCategory;
import com.centurylink.mdw.model.value.activity.ActivityImplementorVO;
import com.centurylink.mdw.model.value.attribute.AttributeVO;
import com.centurylink.mdw.model.value.attribute.CustomAttributeVO;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.event.ExternalEventVO;
import com.centurylink.mdw.model.value.process.LaneVO;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.model.value.user.UserGroupVO;
import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.model.value.user.UserVO;
import com.centurylink.mdw.model.value.variable.VariableTypeVO;

public class DesignerDataModel {

    // data loaded from database
    private List<VariableTypeVO> variableTypes;
    private List<ProcessVO> processes;      // public processes - only latest version
    private List<ProcessVO> privateProcesses;   // used prior to MDW 4.1, also for remote processes
    private Map<String,UserGroupVO> groups;
    private List<String> roleNames;
    private List<PackageVO> packages;
    private List<ExternalEventVO> externalEvents;
    private List<TaskVO> taskTemplates;
    private List<ActivityImplementorVO> activityImplementors;
    private List<TaskCategory> taskCategories;
    private Map<String,RuleSetVO> rulesets;
    private Map<Long,RuleSetVO> rulesetsById;
    private UserVO user;
    private NodeMetaInfo nodeMetaInfo;
    private Map<String,CustomAttributeVO> rulesetCustomAttributes;

    // session data
    private List<Graph> processGraphs;
    private List<ProcessInstanceTreeModel> instanceTrees;

    // others
    private int databaseSchemaVersion;
    private int nextNewId = -10000;

    //AK..added on 02/27/2011
    private static boolean alreadyCollectedObjectsGlobalFlag = false;

    private static boolean allowAnyUserToRead = true;

    public DesignerDataModel() {
        packages = null;
        variableTypes = new ArrayList<VariableTypeVO>(0);
        processes = new ArrayList<ProcessVO>(0);
        privateProcesses = new ArrayList<ProcessVO>(0);
        processGraphs = new ArrayList<Graph>();
        instanceTrees = new ArrayList<ProcessInstanceTreeModel>();
        user = null;
        nodeMetaInfo = new NodeMetaInfo();
    }

    public void clear() {
        packages = null;
        variableTypes = new ArrayList<VariableTypeVO>(0);
        processes = new ArrayList<ProcessVO>(0);
        privateProcesses = new ArrayList<ProcessVO>(0);
        processGraphs = new ArrayList<Graph>();
        instanceTrees = new ArrayList<ProcessInstanceTreeModel>();
        user = null;
    }

    public int getDatabaseSchemaVersion() {
        return databaseSchemaVersion;
    }

    public void setDatabaseSchemaVersion(int databaseSchemaVersion) {
        this.databaseSchemaVersion = databaseSchemaVersion;
    }

    public boolean schemaVersionAllowEdit() {
//        return databaseSchemaVersion==DataAccess.currentSchemaVersion;
        return databaseSchemaVersion>=DataAccess.schemaVersion4;
    }

    public List<VariableTypeVO> getVariableTypes() {
        return variableTypes;
    }
    public void setVariableTypes(List<VariableTypeVO> variableTypes) {
        this.variableTypes = variableTypes;
    }
    public List<ProcessVO> getProcesses() {
        return processes;
    }
    private void insertOrderedByVersion(ProcessVO head, ProcessVO one) {
        ProcessVO next = head;
        while (next.getPrevVersion()!=null) {
            if (one.getVersion()>=next.getPrevVersion().getVersion()) break;
            next = next.getPrevVersion();
        }
        if (next.getPrevVersion()!=null) {
            next.getPrevVersion().setNextVersion(one);
        }
        one.setPrevVersion(next.getPrevVersion());
        one.setNextVersion(next);
        next.setPrevVersion(one);
    }
    private void addProcess(List<ProcessVO> proclist, ProcessVO procdef) {
        int i, n = proclist.size();
        String procname = procdef.getProcessName().toUpperCase();
        for (i=0; i<n; i++) {
            ProcessVO one = proclist.get(i);
            String oneName = one.getProcessName().toUpperCase();
            int d = procname.compareTo(oneName);
            if (d==0) d = procdef.getProcessName().compareTo(one.getProcessName());
            if (d<0) {
                procdef.setPrevVersion(null);
                procdef.setNextVersion(null);
                proclist.add(i,procdef);
                return;
            } else if (d==0) {
                if (procdef.getVersion()>=one.getVersion()) {
                    procdef.setNextVersion(null);
                    procdef.setPrevVersion(one);
                    one.setNextVersion(procdef);
                    proclist.set(i, procdef);
                } else insertOrderedByVersion(one, procdef);
                return;
            }
        }
        procdef.setPrevVersion(null);
        procdef.setNextVersion(null);
        proclist.add(procdef);
    }
    public void addProcess(ProcessVO procdef) {
        addProcess(processes, procdef);
    }
    public void addPrivateProcess(ProcessVO procdef) {
        addProcess(privateProcesses, procdef);
    }
    public void setProcesses(List<ProcessVO> processList) {
        processes = new ArrayList<ProcessVO>();
        privateProcesses = new ArrayList<ProcessVO>();
        for (ProcessVO one : processList) {
             addProcess(processes, one);
        }
    }
    public List<ProcessVO> getPrivateProcesses() {
        return privateProcesses;
    }

    /**
     * Return the list of group names that the current user can play
     * the role, including groups inherited the role.
     * @param role
     * @return
     */
    public List<String> getGroupNames(String role) {
        List<String> groups = new ArrayList<String>();
//        if (role.equals(UserRole.PROCESS_DESIGN)) {
            if (userHasRole(UserGroupVO.COMMON_GROUP,role)) groups.add(UserGroupVO.COMMON_GROUP);
//        }
        for (String name : this.groups.keySet()) {
            if (!name.equals(UserGroupVO.SITE_ADMIN_GROUP)) {
                if (userHasRole(name, role)) groups.add(name);
            }
        }
        return groups;
    }

    public List<UserGroupVO> getGroups() {
        List<UserGroupVO> groups = new ArrayList<UserGroupVO>();
        for (String name : this.groups.keySet()) {
            groups.add(this.groups.get(name));
        }
        return groups;
    }
    public void setGroups(List<UserGroupVO> groups) {
        this.groups = new HashMap<String,UserGroupVO>();
        for (UserGroupVO group : groups) {
            if (user.belongsToGroup(group.getName())) {
                group.setRoles(user.getRoles(group.getName()));
            } else {
                boolean canSee;
                if (user.belongsToGroup(UserGroupVO.SITE_ADMIN_GROUP)) canSee = true;
                else {
                    canSee = false;
                    String parentName = group.getParentGroup();
                    while (parentName!=null && !canSee) {
                        canSee = user.belongsToGroup(parentName);
                        if (!canSee) {
                            parentName = null;
                            for (UserGroupVO g : groups) {
                                if (g.getName().equals(parentName)) {
                                    parentName = g.getParentGroup();
                                    break;
                                }
                            }
                        }
                    }
                }
                if (!canSee) continue;
                List<String> noRoles = new ArrayList<String>();
                group.setRoles(noRoles);
            }
            this.groups.put(group.getName(), group);
        }
    }
    public List<String> getRoleNames() {
        return roleNames;
    }
    public void setRoleNames(List<String> roleNames) {
        this.roleNames = roleNames;
    }
//    public PackageListTreeModel getPackageListTree() {
//        return packageListTree;
//    }
    public List<PackageVO> getPackages() {
        return packages;
    }
    public void setPackages(List<PackageVO> packages) {
        this.packages = packages;
    }

    public List<ExternalEventVO> getExternalEvents() {
        return externalEvents;
    }
    public void setExternalEvents(List<ExternalEventVO> externalEvents) {
        this.externalEvents = externalEvents;
    }
    public List<TaskVO> getTaskTemplates() {
        return taskTemplates;
    }
    public void setTaskTemplates(List<TaskVO> taskTemplates) {
        this.taskTemplates = taskTemplates;
    }
    public List<ActivityImplementorVO> getActivityImplementors() {
        return activityImplementors;
    }
    public void setActivityImplementors(List<ActivityImplementorVO> activityImplementors) {
        this.activityImplementors = activityImplementors;
    }
    public List<TaskCategory> getTaskCategories() {
        return taskCategories;
    }
    public void setTaskCategories(List<TaskCategory> taskCategories) {
        this.taskCategories = taskCategories;
    }
    public void reloadProcesses(DesignerDataAccess dao) throws DataAccessException, RemoteException {
        setProcesses(dao.getProcessList(0));
        // packages = null;    // invalidate it
    }
    public void reloadActivityImplementors(DesignerDataAccess dao) throws DataAccessException, RemoteException {
        activityImplementors = dao.getActivityImplementors();
        nodeMetaInfo.init(activityImplementors, databaseSchemaVersion);
        // packages = null;    // invalidate it
    }
    public void reloadExternalEvents(DesignerDataAccess dao) throws DataAccessException, RemoteException {
        externalEvents = dao.getExternalEvents();
        // packages = null;    // invalidate it
    }
    public void reloadTaskTemplates(DesignerDataAccess dao) throws DataAccessException, RemoteException {
        taskTemplates = dao.getTaskTemplates();
    }

    public void reloadGroups(DesignerDataAccess dao) throws DataAccessException, RemoteException {
        setGroups(dao.getAllGroups());
    }

    public void reloadRoleNames(DesignerDataAccess dao) throws DataAccessException, RemoteException {
        roleNames = dao.getRoleNames();
    }

    public void reloadVariableTypes(DesignerDataAccess dao) throws DataAccessException, RemoteException {
        variableTypes = dao.getVariableTypes();
    }

    public void reloadRuleSets(DesignerDataAccess dao) throws DataAccessException, RemoteException {
        rulesets = new HashMap<String,RuleSetVO>();
        rulesetsById = new HashMap<Long,RuleSetVO>();
        if (dao==null) return;
        List<RuleSetVO> flatList = dao.getRuleSets();
        rulesetCustomAttributes = new HashMap<String,CustomAttributeVO>();
        for (RuleSetVO one : flatList) {
            String key = getResourceKey(one);
            RuleSetVO last = rulesets.get(key);
            if (last!=null) {
                last.setPrevVersion(one);
                one.setNextVersion(last);
            } else {
                one.setNextVersion(null);
                if (one.getLanguage()!=null && !rulesetCustomAttributes.containsKey(one.getLanguage())) {
                      CustomAttributeVO custAttrVO = dao.getCustomAttribute("RULE_SET", one.getLanguage());
                      rulesetCustomAttributes.put(one.getLanguage(), custAttrVO);
                }
                  rulesets.put(key, one);
            }
            rulesetsById.put(one.getId(), one);
            one.setPrevVersion(null);
        }
    }

    public void reloadPackages(DesignerDataAccess dao, boolean populateTree)
            throws DataAccessException, RemoteException {
        if (getDatabaseSchemaVersion()>=DataAccess.schemaVersion4) {
            packages = dao.getPackageList(false);
            replaceAliasesInPackages(packages);
        } else packages = new ArrayList<PackageVO>(1);
    }

    public void reloadTaskCategories(DesignerDataAccess dao) throws DataAccessException, RemoteException {
        taskCategories = dao.getTaskCategories();
    }

    public String collectUnsavedObjects(String cuid, String question) {
        StringBuffer sb = new StringBuffer();
        if (rulesets!=null) {
            for (String key : rulesets.keySet()) {
                RuleSetVO one = rulesets.get(key);
                if (cuid.equals(one.getModifyingUser())) {
                    if (sb.length()==0) sb.append(question);
                    String type = one.getLanguage().toLowerCase();
                    sb.append("- " + type + " " + one.getName() + "\n");
                }
            }
        }
        int i=0; //AK..added on 02/27/2011
        for (Graph g : processGraphs) {
            //AK..commented out on 02/25/2011 -- looks like a bug....if (g.dirtyLevel!=Graph.CLEAN ||
                    //cuid.equals(g.getProcessVO().getModifyingUser())) {
            if (g.dirtyLevel!=Graph.CLEAN && //AK..changed "||" to "&&" on 02/25/2011
                      cuid.equals(g.getProcessVO().getModifyingUser())) {
                if (sb.length()==0) sb.append(question);
                sb.append(++i + ". Process: " + g.getName() + "\n"); //AK..modified on 02/27/2011
            }
        }
        //AK..added 02/27/2011
        this.setAlreadyCollectedObjectsGlobalFlag(true);

        // TODO also check unsaved packages
        return sb.length()==0?null:sb.toString();
    }

    public List<Graph> getProcessGraphs() {
        return processGraphs;
    }
    public void addProcessGraph(Graph g) {
        processGraphs.add(g);
    }
    public void removeProcessGraph(Graph g) {
        processGraphs.remove(g);
    }
    public void removeAllProcessGraphs() {
        processGraphs.clear();
    }
    public Graph findProcessGraph(String procname, int version) {
        if (version>0) {
            for (Graph g : processGraphs) {
                if (procname.equals(g.getName())
                       && version==g.getVersion())
                    return g;
            }
            return null;
        } else {    // find the newest version;
            Graph ret = null;
            int v1;
            for (Graph g : processGraphs) {
                if (procname.equals(g.getName())) {
                    v1 = g.getVersion();
                    if (version==0 || v1>version) {
                        version = v1;
                        ret = g;
                    }
                }
            }
            return ret;
        }
    }

    public Graph findProcessGraph(Long processId, String server) {
        for (Graph g : processGraphs) {
            if (processId.equals(g.getId())) {
                if (server==null&&g.getProcessVO().getRemoteServer()==null ||
                        server!=null&&server.equals(g.getProcessVO().getRemoteServer())) return g;
            }
        }
        return null;
    }

    public ProcessVO findLoadedProcessVO(Long processId, String server) {
        Graph g = findProcessGraph(processId, server);
        if (g == null)
          return null;
        else
          return g.getProcessVO();
    }

    public Graph findProcessGraph(ProcessVO proc) {
        for (Graph g : processGraphs) {
            if (g.getProcessVO()==proc) return g;
        }
        return null;
    }

    public ProcessInstanceTreeModel findInstanceTree(String modelId) {
        for (ProcessInstanceTreeModel pi : instanceTrees) {
            if (pi.getId().equals(modelId)) return pi;
        }
        return null;
    }

    public ProcessInstanceTreeModel findInstanceTreeAndNode(Long procInstId, String server, String masterId) {
        if (masterId == null) return null;
        ProcessInstanceTreeNode node;
        for(int i =0;i<instanceTrees.size();i++){
            ProcessInstanceTreeModel model = instanceTrees.get(i);
            if (!masterId.equals(model.getMasterRequestId())) continue;
            node = model.find(procInstId, server);
            if (node!=null) {
                model.setCurrentProcess(node);
                return model;
            }
        }
        return null;
    }

    public ProcessInstanceTreeModel findInstanceTreeAndNode(ProcessInstanceVO processInstance) {
        return findInstanceTreeAndNode(processInstance.getId(), processInstance.getRemoteServer(),
                processInstance.getMasterRequestId());
    }
    public List<ProcessInstanceTreeModel> getInstanceTrees() {
        return this.instanceTrees;
    }
    public void addInstanceTree(ProcessInstanceTreeModel model) {
        instanceTrees.add(model);
    }
    public int removeInstanceTree(ProcessInstanceTreeModel model) {
        for(int i =0;i<instanceTrees.size();i++){
            ProcessInstanceTreeModel one = instanceTrees.get(i);
            if (one.equals(model)) {
                instanceTrees.remove(i);
                return i;
            }
        }
        return -1;
    }
    public int removeInstanceTree(String modelId) {
        for(int i =0;i<instanceTrees.size();i++){
            ProcessInstanceTreeModel one = instanceTrees.get(i);
            if (one.getId().equals(modelId)) {
                instanceTrees.remove(i);
                return i;
            }
        }
        return -1;
    }
    public void removeAllInstanceTrees() {
        instanceTrees.clear();
    }

    private ProcessVO findProcessDefinition(List<ProcessVO> proclist,
            Long procid, String server) {
        for (ProcessVO pd1 : proclist) {
            if (pd1.equals(procid,server)) return pd1;
            ProcessVO prev = pd1.getPrevVersion();
            while (prev!=null) {
                if (prev.equals(procid,server)) return prev;
                prev = prev.getPrevVersion();
            }
        }
        return null;
    }
    private ProcessVO findProcessDefinition(List<ProcessVO> proclist,
            String processName, int version) {
        String remoteServer = null;
        int k = processName.indexOf(RemoteAccess.REMOTE_NAME_DELIMITER);
        if (k>0) {
            remoteServer = processName.substring(k+1);
            processName = processName.substring(0,k);
        }
        for (ProcessVO pd1 : proclist) {
            if (pd1.getProcessName().equals(processName) &&
                (remoteServer==null&&pd1.getRemoteServer()==null
                        ||remoteServer!=null&&remoteServer.equals(pd1.getRemoteServer()))) {
                if (version==0 || version==pd1.getVersion()) {
                    return pd1;
                }
                ProcessVO prev = pd1.getPrevVersion();
                while (prev!=null) {
                    if (prev.getVersion()==version) return prev;
                    prev = prev.getPrevVersion();
                }
            }
        }
        return null;
    }
    public ProcessVO findProcessDefinition(Long procid, String server) {
        ProcessVO proc = findProcessDefinition(processes, procid, server);
        if (proc==null) proc = findProcessDefinition(privateProcesses, procid, server);
        return proc;
    }
    public ProcessVO findProcessDefinition(String processName, int version) {
        ProcessVO proc = findProcessDefinition(processes, processName, version);
        if (proc==null) proc = findProcessDefinition(privateProcesses, processName, version);
        return proc;
    }

    public int findProcessIndex(String procname) {
        for (int i=0; i<processes.size(); i++) {
            if (procname.equals(processes.get(i).getProcessName())) return i;
        }
        return -1;
    }

    public ActivityImplementorVO findActivityImplementorVO(String implClass) {
        if ( activityImplementors != null) {
            for (ActivityImplementorVO activityImplementorVO :activityImplementors) {
                if (activityImplementorVO.getImplementorClassName().equals(implClass))
                    return activityImplementorVO;
            }
        }
        return null;

    }

    public ExternalEventVO findExternalEvent(String pattern) {
        if (externalEvents != null) {
            for (ExternalEventVO externalEventVO:  externalEvents) {
                if (externalEventVO.getEventName().equals(pattern)) return externalEventVO;
            }
        }
        return null;
    }

    public PackageVO findPackage(String packageName, int version) {
        for (PackageVO pd1 : packages) {
            if (pd1.getPackageName().equals(packageName)) {
                if (version==0 || version==pd1.getVersion()) {
                    return pd1;
                }
            }
        }
        return null;
    }

    public PackageVO findPackageForProcess(Long processId) {
        for (PackageVO pd1 : packages) {
            for (ProcessVO proc : pd1.getProcesses()) {
                if (proc.getProcessId().equals(processId)) return pd1;
            }
        }
        return null;
    }

    public List<PackageVO> getPackagesForProcess(String procname) {
        List<PackageVO> ret = new ArrayList<PackageVO>();
        String lastPackageName = "";
        for (PackageVO pkg : packages) {
            if (pkg.getPackageName().equals(lastPackageName)) continue;
            lastPackageName = pkg.getPackageName();
            if (pkg.getProcesses()==null) continue;    // package is shallow-loaded
            for (ProcessVO proc : pkg.getProcesses()) {
                if (proc.getProcessName().equals(procname)) {
                    ret.add(pkg);
                    break;
                }
            }
        }
        return ret;
    }

    private void replaceAliasesInPackage(PackageVO pkg) {
        List<ProcessVO> newProcs = new ArrayList<ProcessVO>(pkg.getProcesses().size());
        for (ProcessVO proc : pkg.getProcesses()) {
            ProcessVO proc1 = this.findProcessDefinition(processes, proc.getProcessId(), null);
            if (proc1!=null) newProcs.add(proc1);
            else newProcs.add(proc);
        }
        pkg.setProcesses(newProcs);
        List<ActivityImplementorVO> newImpls =
            new ArrayList<ActivityImplementorVO>(pkg.getImplementors().size());
        for (ActivityImplementorVO impl : pkg.getImplementors()) {
            ActivityImplementorVO impl1 = findActivityImplementorVO(impl.getImplementorClassName());
            if (impl1!=null) newImpls.add(impl1);
            else newImpls.add(impl);
        }
        pkg.setImplementors(newImpls);
        List<ExternalEventVO> newHandlers =
            new ArrayList<ExternalEventVO>(pkg.getExternalEvents().size());
        for (ExternalEventVO hdl : pkg.getExternalEvents()) {
            ExternalEventVO hdl1 = findExternalEvent(hdl.getEventName());
            if (hdl1!=null) newHandlers.add(hdl1);
            else newHandlers.add(hdl);
        }
        pkg.setExternalEvents(newHandlers);
        if (pkg.getRuleSets()!=null) {
            List<RuleSetVO> newRuleSets =
                new ArrayList<RuleSetVO>(pkg.getRuleSets().size());
            for (RuleSetVO hdl : pkg.getRuleSets()) {
                RuleSetVO hdl1 = findRuleSet(hdl.getId());
                if (hdl1!=null) newRuleSets.add(hdl1);
                else newRuleSets.add(hdl);
            }
            pkg.setRuleSets(newRuleSets);
        }
    }

    private void replaceAliasesInPackages(List<PackageVO> packages) {
        for (PackageVO pkg : packages) {
            if (pkg.getProcesses()!=null)    // loaded
                replaceAliasesInPackage(pkg);
        }
    }

    public void addImplementor(ActivityImplementorVO impl) {
        activityImplementors.add(impl);
    }
    public void removeImplementor(ActivityImplementorVO impl) {
        activityImplementors.remove(impl);
    }

    public void addExternalEvent(ExternalEventVO impl) {
        externalEvents.add(impl);
    }
    public void removeExternalEvent(ExternalEventVO impl) {
        externalEvents.remove(impl);
    }

    public void addTaskTemplate(TaskVO task) {
        taskTemplates.add(task);
    }
    public void removeTaskTemplate(TaskVO task) {
        taskTemplates.remove(task);
    }

    public void addRuleSet(RuleSetVO impl) {
        String key = getResourceKey(impl);
        RuleSetVO found = rulesets.get(key);
        impl.setNextVersion(null);
        if (found==null) {
            impl.setPrevVersion(null);
        } else {
            impl.setPrevVersion(found);
            found.setNextVersion(impl);
        }
        rulesets.put(key, impl);
        rulesetsById.put(impl.getId(), impl);
    }

    private String getResourceKey(RuleSetVO ruleSetVO) {
        return getResourceKey(ruleSetVO.getName(),ruleSetVO.getLanguage());
    }

    private String getResourceKey(String name, String language) {
        return name + "/" + language;
    }

    public void removeRuleSet(RuleSetVO ruleSetVO, boolean allVersions) {
        String key = getResourceKey(ruleSetVO);
        RuleSetVO lead = rulesets.get(key);
        if (lead != null) { // null for task templates
            if (allVersions) {
                rulesets.remove(key);
                while (lead!=null) {
                    rulesetsById.remove(lead.getId());
                    lead = lead.getPrevVersion();
                }
            } else {    // remove latest version
                rulesetsById.remove(lead.getId());
                if (lead.getPrevVersion()==null) rulesets.remove(key);
                else {
                    lead = lead.getPrevVersion();
                    lead.setNextVersion(null);
                    rulesets.put(key, lead);
                }
            }
        }
    }

    public void addPackage(PackageVO pkg) {
        int i, n = packages.size();
        for (i=0; i<n; i++) {
            PackageVO p = packages.get(i);
            if (p.getPackageName().compareTo(pkg.getPackageName())>=0) {
                packages.add(i, pkg);
                return;
            }
        }
        packages.add(pkg);
    }

    public void removePackage(PackageVO pkg) {
        packages.remove(pkg);
    }

    public Long getNewId() {
        return new Long(nextNewId--);
    }

    private boolean packageReferences(PackageVO pkg, ActivityImplementorVO obj) {
        if (pkg.getImplementors()==null) return false;    // this is not good, but load every package takes too much time
        return pkg.getImplementors().contains(obj);
    }

    private boolean packageReferences(PackageVO pkg, ExternalEventVO obj) {
        if (pkg.getExternalEvents()==null) return false;    // this is not good, but load every package takes too much time
        return pkg.getExternalEvents().contains(obj);
    }

    public List<Object> getReferences(ActivityImplementorVO obj) {
        List<Object> result = new ArrayList<Object>();
        for (PackageVO pkg : packages) {
            if (packageReferences(pkg, obj)) result.add(pkg);
        }
        return result;
    }

    public List<Object> getReferences(ExternalEventVO obj) {
        List<Object> result = new ArrayList<Object>();
        for (PackageVO pkg : packages) {
            if (packageReferences(pkg, obj)) result.add(pkg);
        }
        return result;
    }

    /**
     * Load package from a xml string. The result package is not added to the package
     * list, which will be done by the caller.
     * @param xmlstring
     * @param isXPDL
     * @return imported package in memory
     * @throws Exception
     */
    public PackageVO loadPackageFromXml(String xmlstring, boolean isXPDL) throws Exception {
        ProcessImporter importer = DataAccess.getProcessImporter(DataAccess.currentSchemaVersion);
        return importer.importPackage(xmlstring);
    }

    private boolean same_handler(ExternalEventVO newone, ExternalEventVO existing) {
        return newone.getEventHandler().equals(existing.getEventHandler());
    }

    private boolean sameString(String one, String two) {
        if (one==null) one = "";
        if (two==null) two = "";
        one = one.replaceAll("\r", "").trim();
        two = two.replaceAll("\r", "").trim();
        return one.equals(two);
    }

    private boolean same_implementor(ActivityImplementorVO newone, ActivityImplementorVO existing) {
        if (!sameString(newone.getBaseClassName(),existing.getBaseClassName())) return false;
        if (!sameString(newone.getIconName(),existing.getIconName())) return false;
        if (!sameString(newone.getAttributeDescription(),existing.getAttributeDescription())) return false;
        if (!sameString(newone.getLabel(),existing.getLabel())) return false;
        return true;

    }

    public List<ImportItem> collectImportItems(PackageVO pkg) {
        List<ImportItem> importItems = new ArrayList<ImportItem>();
        // check for package
        ImportItem item;
        int status;
        if (pkg.getVersion()!=0 && getDatabaseSchemaVersion()>=DataAccess.schemaVersion4) {
            status = ImportItem.STATUS_NEW_VERSION;
            for (PackageVO one : packages) {
                if (one.getPackageName().equals(pkg.getPackageName())) {
                    if (pkg.getVersion()<one.getVersion()) {
                        status = ImportItem.STATUS_OLD_VERSION;
                    } else if (pkg.getVersion()==one.getVersion()) {
                        if (status==ImportItem.STATUS_NEW_VERSION)
                            status = ImportItem.STATUS_SAME_VERSION;
                    }
                }
            }
        } else status = ImportItem.STATUS_NOT_PACKAGE;
        item = new ImportItem(pkg.getPackageName(), ImportItem.TYPE_PACKAGE, status);
        importItems.add(item);
        // check for implementors
        for (ActivityImplementorVO a : pkg.getImplementors()) {
            ActivityImplementorVO a1 = findActivityImplementorVO(a.getImplementorClassName());
            if (a1==null) status = ImportItem.STATUS_NEW;
            else if (same_implementor(a, a1)) status = ImportItem.STATUS_SAME;
            else status = ImportItem.STATUS_DIFFERENT;
            item = new ImportItem(a.getImplementorClassName(), ImportItem.TYPE_IMPLEMENTOR, status);
            importItems.add(item);
        }
        for (ExternalEventVO e : pkg.getExternalEvents()) {
            ExternalEventVO e1 = findExternalEvent(e.getEventName());
            if (e1==null) status = ImportItem.STATUS_NEW;
            else if (same_handler(e, e1)) status = ImportItem.STATUS_SAME;
            else status = ImportItem.STATUS_DIFFERENT;
            item = new ImportItem(e.getEventName(), ImportItem.TYPE_HANDLER, status);
            importItems.add(item);
        }
        for (RuleSetVO e : pkg.getRuleSets()) {
            RuleSetVO e1 = findRuleSet(e.getName(), null);
            if (e1==null || e.getVersion()>e1.getVersion()) status = ImportItem.STATUS_NEW_VERSION;
            else if (e.getVersion()==0) {
                status = ImportItem.STATUS_NEW_VERSION;
                e.setVersion(e1.getVersion()+1);
            }
            else if (e.getVersion()==e1.getVersion()) status = ImportItem.STATUS_SAME_VERSION;
            else status = ImportItem.STATUS_OLD_VERSION;
            item = new ImportItem(e.getName(), ImportItem.TYPE_RULESET, status);
            importItems.add(item);
        }
        for (ProcessVO p : pkg.getProcesses()) {
            ProcessVO p1 = findProcessDefinition(p.getProcessName(), 0);
            if (p1==null || p.getVersion()>p1.getVersion() || p.getVersion()==0) status = ImportItem.STATUS_NEW_VERSION;
            else if (p.getVersion()==p1.getVersion()) status = ImportItem.STATUS_SAME_VERSION;
            else status = ImportItem.STATUS_OLD_VERSION;
            item = new ImportItem(p.getProcessName(), ImportItem.TYPE_PROCESS, status);
            importItems.add(item);
        }
        return importItems;
    }

    private ImportItem findImportItem(List<ImportItem> importItems, int type, String name) {
        for (int i=0; i<importItems.size(); i++) {
            if (importItems.get(i).getType()==type && importItems.get(i).getName().equals(name))
                return importItems.get(i);
        }
        return null;
    }


    public void loadImportedPackage(PackageVO pkg, List<ImportItem> importItems) {
        ImportItem item;
//        PackageListTreeModel pkgTree = getPackageListTree();
//        GroupingNode pkgGrpNode;
//        PackageNode pkgNode;
        boolean loadPackageItself;

        // load package
        item = findImportItem(importItems, ImportItem.TYPE_PACKAGE, pkg.getPackageName());
        loadPackageItself = item.isSelected();
        if (item.isSelected()) {
//            pkg.setPackageId(model.getNewId());
//            pkgGrpNode = pkgTree.findPackageGroupNode(pkg.getPackageName());
//            if (pkgGrpNode==null) pkgGrpNode = pkgTree.addPackageGroup(pkg.getPackageName());
//            pkgNode = pkgTree.addPackage(pkgGrpNode, pkg);
            addPackage(pkg);
//            pkgNode.setObject(pkg);
//            pkgNode.setStatus(BaseTreeNode.STATUS_NEW);
//            pkgNode.clean();
        } else {
            pkg.setVersion(0);
//            pkgNode = pkgTree.getDefaultPackageNode();
        }

        // add implementors
        List<ActivityImplementorVO> pkgImplementors = new ArrayList<ActivityImplementorVO>();
        for (ActivityImplementorVO a : pkg.getImplementors()) {
            item = findImportItem(importItems, ImportItem.TYPE_IMPLEMENTOR, a.getImplementorClassName());
            if (!item.isSelected()) {
                if (loadPackageItself) {
                    if (item.getStatus()==ImportItem.STATUS_SAME
                            || item.getStatus()==ImportItem.STATUS_DIFFERENT) {
                        pkgImplementors.add(findActivityImplementorVO(a.getImplementorClassName()));
//                        pkgNode.addImplementor(a, BaseTreeNode.STATUS_CLEAN);
                    }
                }
            } else if (item.getStatus()==ImportItem.STATUS_DIFFERENT) {
                ActivityImplementorVO impl = findActivityImplementorVO(a.getImplementorClassName());
                impl.setIconName(a.getIconName());
                impl.setBaseClassName(a.getBaseClassName());
                impl.setAttributeDescription(a.getAttributeDescription());
                impl.setLabel(a.getLabel());
                impl.setShowInToolbox(!StringHelper.isEmpty(a.getAttributeDescription())
                        &&!StringHelper.isEmpty(impl.getBaseClassName())
                        && !StringHelper.isEmpty(impl.getIconName()));
                pkgImplementors.add(impl);
//                if (loadPackageItself) {
//                    pkgNode.addImplementor(a, BaseTreeNode.STATUS_CHANGED);
//                }

            } else {        // staus==STATUS_NEW
                a.setImplementorId(getNewId());
                addImplementor(a);
                a.setShowInToolbox(!StringHelper.isEmpty(a.getAttributeDescription())
                        &&!StringHelper.isEmpty(a.getBaseClassName())
                        && !StringHelper.isEmpty(a.getIconName()));
                pkgImplementors.add(a);
//                if (loadPackageItself) {
//                    pkgNode.addImplementor(a, BaseTreeNode.STATUS_NEW);
//                }
            }
        }
        nodeMetaInfo.init(activityImplementors, databaseSchemaVersion);
        pkg.setImplementors(pkgImplementors);

        // add event handlers
        List<ExternalEventVO> pkgHandlers = new ArrayList<ExternalEventVO>();
        for (ExternalEventVO e : pkg.getExternalEvents()) {
            item = findImportItem(importItems, ImportItem.TYPE_HANDLER, e.getEventName());
            if (!item.isSelected()) {
                if (loadPackageItself) {
                    if (item.getStatus()==ImportItem.STATUS_SAME
                            || item.getStatus()==ImportItem.STATUS_DIFFERENT) {
                        pkgHandlers.add(findExternalEvent(e.getEventName()));
//                        pkgNode.addHandler(e, BaseTreeNode.STATUS_CLEAN);
                    }
                }
            } else if (item.getStatus()==ImportItem.STATUS_DIFFERENT) {
                ExternalEventVO vo = findExternalEvent(e.getEventName());
                vo.setEventHandler(e.getEventHandler());
                pkgHandlers.add(vo);
//                if (loadPackageItself) {
//                    pkgNode.addHandler(e, BaseTreeNode.STATUS_CHANGED);
//                }
            } else {        // staus==STATUS_NEW
                e.setId(getNewId());
                addExternalEvent(e);
                pkgHandlers.add(e);
//                if (loadPackageItself) {
//                    pkgNode.addHandler(e, BaseTreeNode.STATUS_NEW);
//                }
            }
        }
        pkg.setExternalEvents(pkgHandlers);

        // add ruleset
        List<RuleSetVO> pkgRuleSets = new ArrayList<RuleSetVO>();
        for (RuleSetVO e : pkg.getRuleSets()) {
            item = findImportItem(importItems, ImportItem.TYPE_RULESET, e.getName());
            if (!item.isSelected()) {
                if (loadPackageItself) {
                    if (item.getStatus()==ImportItem.STATUS_SAME_VERSION) {
                        pkgRuleSets.add(findRuleSet(e.getName(),e.getLanguage()));
                    }
                }
            } else {
                e.setId(getNewId());
                addRuleSet(e);
                pkgRuleSets.add(e);
            }
        }
        pkg.setRuleSets(pkgRuleSets);

        // TODO import participants

        // load processes
        List<ProcessVO> pkgProcesses = new ArrayList<ProcessVO>();
        for (ProcessVO p : pkg.getProcesses()) {
            item = findImportItem(importItems, ImportItem.TYPE_PROCESS, p.getProcessName());
            if (item.isSelected()) {        // status must be STATUS_NEW_VERSION
                // p.setProcessId(model.getNewId());       // set IDs for all???
                if (p.getVersion()==0) {
                    ProcessVO p1 = findProcessDefinition(p.getProcessName(), 0);
                    if (p1!=null) p.setVersion(p1.getVersion()+1);
                    else p.setVersion(1);
                }
                pkgProcesses.add(p);
                addProcess(p);
                // pkgNode.addProcess(p, BaseTreeNode.STATUS_NEW);  done by above
            } else if (item.getStatus()==ImportItem.STATUS_SAME_VERSION || item.getStatus()==ImportItem.STATUS_OLD_VERSION) {
                ProcessVO p1 = findProcessDefinition(p.getProcessName(), p.getVersion());
                if (p1!=null) {
                    p.setActivities(null);    // indicating to include in package but do not import
                    p.setProcessId(p1.getProcessId());
                    pkgProcesses.add(p);
                }    // else new process but not import - excluded entirely
            }
        }
        pkg.setProcesses(pkgProcesses);
//        return pkgNode;
    }

    public PackageVO copyPackage(PackageVO curPkg, String newname, int newversion) {
        PackageVO newPkg = new PackageVO();
        newPkg.setPackageName(newname!=null?newname:curPkg.getPackageName());
        newPkg.setPackageDescription(curPkg.getPackageDescription());
        newPkg.setExported(false);
//        newPkg.setPools(pools)
//        newPkg.setVariables(variables)
        newPkg.setSchemaVersion(DataAccess.currentSchemaVersion);
        newPkg.setPackageId(getNewId());
        newPkg.setVersion(newversion);
        newPkg.setVoXML(curPkg.getVoXML());
        List<ProcessVO> processes = new ArrayList<ProcessVO>();
        newPkg.setProcesses(processes);
        if (curPkg.getProcesses()!=null) {
            for (ProcessVO p : curPkg.getProcesses()) {
                processes.add(p);
            }
        }
        if (curPkg.getImplementors()!=null) {
            List<ActivityImplementorVO> impls = new ArrayList<ActivityImplementorVO>();
            newPkg.setImplementors(impls);
            for (ActivityImplementorVO a : curPkg.getImplementors()) {
                impls.add(a);
            }
        }
        if (curPkg.getExternalEvents()!=null) {
            List<ExternalEventVO> handlers = new ArrayList<ExternalEventVO>();
            newPkg.setExternalEvents(handlers);
            for (ExternalEventVO a : curPkg.getExternalEvents()) {
                handlers.add(a);
            }
        }
        if (curPkg.getParticipants()!=null) {
            List<LaneVO> participants = new ArrayList<LaneVO>();
            newPkg.setParticipants(participants);
            for (LaneVO a : curPkg.getParticipants()) {
                participants.add(a);
            }
        }
        if (curPkg.getRuleSets()!=null) {
            List<RuleSetVO> rulesets = new ArrayList<RuleSetVO>();
            newPkg.setRuleSets(rulesets);
            for (RuleSetVO a : curPkg.getRuleSets()) {
                rulesets.add(a);
            }
        }
        if (curPkg.getAttributes()!=null) {
            List<AttributeVO> attrs = new ArrayList<AttributeVO>();
            newPkg.setAttributes(attrs);
            for (AttributeVO a : curPkg.getAttributes()) {
                attrs.add(a);
            }
        }

        addPackage(newPkg);

        return newPkg;
    }

    public List<RuleSetVO> getRuleSets() {
        List<RuleSetVO> ret = new ArrayList<RuleSetVO>();
        for (String key : rulesets.keySet()) {
            RuleSetVO ruleset = rulesets.get(key);
            ret.add(ruleset);
        }
        return ret;
    }

    public List<RuleSetVO> getAllRuleSets() {
        return new ArrayList<RuleSetVO>(rulesetsById.values());
    }

    public List<RuleSetVO> getRuleSets(String language) {
        List<RuleSetVO> ret = new ArrayList<RuleSetVO>();
        for (String key : rulesets.keySet()) {
            RuleSetVO ruleset = rulesets.get(key);
            if (language.equals(ruleset.getLanguage())) ret.add(ruleset);
        }
        return ret;
    }

    public List<RuleSetVO> getRuleSets(String[] languages) {
        List<RuleSetVO> ret = new ArrayList<RuleSetVO>();
        for (String key : rulesets.keySet()) {
            RuleSetVO ruleset = rulesets.get(key);
            for (int i=0;i<languages.length;i++) {
                if (languages[i].equalsIgnoreCase(ruleset.getLanguage())) {
                    ret.add(ruleset);
                    break;
                }
            }
        }
        return ret;
    }

    public RuleSetVO getRuleSet(DesignerDataAccess dao, Long id)
            throws DataAccessException,RemoteException {
        RuleSetVO found = findRuleSet(id);
        if (found!=null) {
            if (!found.isLoaded()) {
                RuleSetVO loaded = dao.getRuleSet(id);
                found.setLanguage(loaded.getLanguage());
                found.setRuleSet(loaded.getRuleSet());
                found.setVersion(loaded.getVersion());
                found.setModifyingUser(loaded.getModifyingUser());
                found.setModifyDate(loaded.getModifyDate());
            }
            return found;
        }
        throw new DataAccessException("Rule Set with ID " + id + " does not exist");
    }

    public RuleSetVO findRuleSet(Long id) {
        return rulesetsById.get(id);
//        for (String key : rulesets.keySet()) {
//            RuleSetVO ruleset = rulesets.get(key);
//            if (ruleset.getId().equals(id)) {
//                return ruleset;
//            } else {
//                for (RuleSetVO old=ruleset.getPrevVersion();
//                        old!=null; old=old.getPrevVersion()) {
//                    if (old.getId().equals(id)) return old;
//                }
//            }
//        }
//        return null;
    }

    public RuleSetVO findRuleSet(String name, String language) {
        if (language==null) {
            for (String key : rulesets.keySet()) {
                RuleSetVO ruleset = rulesets.get(key);
                if (ruleset.getName().equals(name)) return ruleset;
            }
            return null;
        } else {
            return rulesets.get(getResourceKey(name,language));
        }
    }

    public RuleSetVO findRuleSet(String name, String language, int version) {
        RuleSetVO ruleset = findRuleSet(name, language);
        if (version!=0) {
            while (ruleset!=null && ruleset.getVersion()!=version) {
                ruleset = ruleset.getPrevVersion();
            }
        }
        return ruleset;
    }

    public List<LaneVO> getParticipants() {
        return null;    // TODO implement this
    }

    public boolean canExecuteProcess(ProcessVO procdef) {
        return userHasRole(null, UserRoleVO.PROCESS_EXECUTION);
    }

    public boolean canDesignProcess(ProcessVO procdef) {
        return userHasRole(null, UserRoleVO.PROCESS_DESIGN);
    }

    // check user role in a group with consideration of roles in super groups
    public boolean userHasRole(String group, String role) {
        if (StringHelper.isEmpty(group)) group = UserGroupVO.COMMON_GROUP;
        while (group!=null) {
            if (user.hasRole(group, role)) return true;
            UserGroupVO g = groups.get(group);
            if (group.equals(UserGroupVO.SITE_ADMIN_GROUP)) group = null;
            else if (g==null) {
                if (group.equals(UserGroupVO.COMMON_GROUP)) group = UserGroupVO.SITE_ADMIN_GROUP;
                else group = null;
            }
            else if (g.getParentGroup()==null) group = UserGroupVO.SITE_ADMIN_GROUP;
            else group = g.getParentGroup();
        }
        return false;
    }

    public boolean userHasRoleInAnyGroup(String role) {
        if (userHasRole(UserGroupVO.COMMON_GROUP,role)) return true;
        for (UserGroupVO group : groups.values()) {
            if (user.hasRole(group.getName(), role)) return true;
        }
        return false;
    }

    public String getPrivileges() {
        return user.getGroupsAndRolesAsString();
    }

    public void reloadPriviledges(DesignerDataAccess dao, String cuid) throws DataAccessException, RemoteException {
        if (dao.noDatabase()) {
            user = new UserVO();
            user.setCuid(cuid);
            user.setName(cuid);
            user.addRoleForGroup(UserGroupVO.COMMON_GROUP, UserRoleVO.PROCESS_DESIGN);
            user.addRoleForGroup(UserGroupVO.SITE_ADMIN_GROUP, UserRoleVO.PROCESS_DESIGN);
        } else {
            user = dao.getUser(cuid);
            if (user==null) {
                if (allowAnyUserToRead) {
                    user = new UserVO();
                    user.setCuid(cuid);
                    user.setName(cuid);
                    if (UserGroupVO.DEFAULT_ALL_ROLES)
                        user.addRoleForGroup(UserGroupVO.COMMON_GROUP, UserRoleVO.VIEW_ONLY);
                } else {
                    throw new DataAccessException("You are not authorized to access this site");
                }
            }
        }
    }

    public List<String> getWorkgroupNames()
    {
      List<String> workgroupNames = new ArrayList<String>();
      for (UserGroupVO group : getGroups())
        workgroupNames.add(group.getName());
      Collections.sort(workgroupNames);
      return workgroupNames;
    }

    public boolean belongsToGroup(String group)
    {
      for (String workgroup : getWorkgroupNames())
      {
        if (workgroup.equals(group))
          return true;
      }
      return false;
    }

    public NodeMetaInfo getNodeMetaInfo() {
        return nodeMetaInfo;
    }

    public CustomAttributeVO getRuleSetCustomAttribute(String language) {
        return rulesetCustomAttributes.get(language);
    }

    public void setRuleSetCustomAttribute(CustomAttributeVO customAttribute) {
        rulesetCustomAttributes.put(customAttribute.getCategorizer(), customAttribute);
    }

    //AK..added 02/27/2011
    public boolean getAlreadyCollectedObjectsGlobalFlag() {
        return alreadyCollectedObjectsGlobalFlag;
    }

    //AK..added 02/27/2011
    public void setAlreadyCollectedObjectsGlobalFlag(boolean alreadyCollectedObjectsLocalFlag) {
        alreadyCollectedObjectsGlobalFlag = alreadyCollectedObjectsLocalFlag;
    }
}
