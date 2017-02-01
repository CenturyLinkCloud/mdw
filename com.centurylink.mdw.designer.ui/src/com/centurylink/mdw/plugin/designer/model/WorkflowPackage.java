/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.designer.display.DesignerDataModel;
import com.centurylink.mdw.model.value.activity.ActivityImplementorVO;
import com.centurylink.mdw.model.value.attribute.AttributeVO;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.event.ExternalEventVO;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;
import com.centurylink.mdw.model.value.user.UserGroupVO;
import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.PluginUtil;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent.ChangeType;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;
import com.centurylink.mdw.plugin.project.model.WorkflowProject.PersistType;

/**
 * Wraps a PackageVO.
 */
public class WorkflowPackage extends WorkflowElement implements Versionable, AttributeHolder, Comparable<WorkflowPackage>
{
  public static final String STATUS_EXPORTED = "Exported";
  public static final String STATUS_NOT_EXPORTED = "NotExported";

  private PackageVO packageVO;
  public PackageVO getPackageVO() { return packageVO; }
  public void setPackageVO(PackageVO pkgVO) { this.packageVO = pkgVO; }

  public Entity getActionEntity()
  {
    return Entity.Package;
  }

  public void setProject(WorkflowProject workflowProject)
  {
    super.setProject(workflowProject);
  }

  public WorkflowPackage getPackage()
  {
    return this;
  }

  private List<WorkflowPackage> descendantPackageVersions;
  public List<WorkflowPackage> getDescendantPackageVersions() { return descendantPackageVersions; }
  public void setDescendantPackageVersions(List<WorkflowPackage> dpvs) { descendantPackageVersions = dpvs; }
  public boolean hasDescendantPackageVersions()
  {
    return descendantPackageVersions != null && descendantPackageVersions.size() > 0;
  }
  public void addDescendantPackageVersion(WorkflowPackage packageVersion)
  {
    if (descendantPackageVersions == null)
      descendantPackageVersions = new ArrayList<WorkflowPackage>();
    descendantPackageVersions.add(packageVersion);
    Collections.sort(descendantPackageVersions);
  }
  public void removeDescendantPackageVersion(WorkflowPackage packageVersion)
  {
    descendantPackageVersions.remove(packageVersion);
    Collections.sort(descendantPackageVersions);
  }

  private List<WorkflowProcess> processes = new ArrayList<WorkflowProcess>();
  public List<WorkflowProcess> getProcesses() { return processes; }
  public void setProcesses(List<WorkflowProcess> processes) { this.processes = processes; }
  public boolean hasProcesses()
  {
    return processes != null && processes.size() > 0;
  }
  public void addProcess(WorkflowProcess processVersion)
  {
    if (isDefaultPackage())
    {
      WorkflowProcess alreadyIn = getProcess(processVersion.getName());
      if (alreadyIn != null)
      {
        if (processVersion.getVersionString().compareTo(alreadyIn.getVersionString()) > 0)
        {
          // becomes the new top level
          removeProcess(alreadyIn);
          processes.add(processVersion);
          addProcessToVO(processVersion.getProcessVO());
          processVersion.setPackage(this);
        }
      }
      else
      {
        processes.add(processVersion);
        addProcessToVO(processVersion.getProcessVO());
        processVersion.setPackage(this);
      }
    }
    else
    {
      processes.add(processVersion);
      processVersion.setPackage(this);
      addProcessToVO(processVersion.getProcessVO());
    }
    Collections.sort(processes);
  }
  public void removeProcess(WorkflowProcess processVersion)
  {
    processes.remove(processVersion);
    removeProcessFromVO(processVersion.getProcessVO());
    Collections.sort(processes);
  }
  public WorkflowProcess findMatchingProcess(WorkflowProcess toCheck)
  {
    for (WorkflowProcess processVersion : processes)
    {
      if (processVersion.getName().equals(toCheck.getName())
          && processVersion.getVersion() == toCheck.getVersion())
      {
        return processVersion;
      }
    }
    return null;  // not found
  }
  private void addProcessToVO(ProcessVO processVO)
  {
    if (packageVO.getProcesses() == null)
      packageVO.setProcesses(new ArrayList<ProcessVO>());
    packageVO.getProcesses().add(processVO);
  }
  private void removeProcessFromVO(ProcessVO processVO)
  {
    if (packageVO.getProcesses() != null)
      packageVO.getProcesses().remove(processVO);
  }

  private List<ExternalEvent> externalEvents = new ArrayList<ExternalEvent>();
  public List<ExternalEvent> getExternalEvents() { return externalEvents; }
  public void setExternalEvents(List<ExternalEvent> externalEvents) { this.externalEvents = externalEvents; }
  public boolean hasExternalEvents()
  {
    return externalEvents != null && externalEvents.size() > 0;
  }
  public void addExternalEvent(ExternalEvent externalEvent)
  {
    externalEvents.add(externalEvent);
    externalEvent.setPackage(this);
    addExternalEventToVO(externalEvent.getExternalEventVO());
    Collections.sort(externalEvents);
  }
  public void removeExternalEvent(ExternalEvent externalEvent)
  {
    externalEvents.remove(externalEvent);
    removeExternalEventFromVO(externalEvent.getExternalEventVO());
    Collections.sort(externalEvents);
  }
  private void addExternalEventToVO(ExternalEventVO externalEventVO)
  {
    if (packageVO.getExternalEvents() == null)
      packageVO.setExternalEvents(new ArrayList<ExternalEventVO>());
    packageVO.getExternalEvents().add(externalEventVO);
  }
  private void removeExternalEventFromVO(ExternalEventVO externalEventVO)
  {
    if (packageVO.getExternalEvents() != null)
      packageVO.getExternalEvents().remove(externalEventVO);
  }

  private List<TaskTemplate> taskTemplates = new ArrayList<TaskTemplate>();
  public List<TaskTemplate> getTaskTemplates() { return taskTemplates; }
  public void setTaskTemplates(List<TaskTemplate> taskTemplates) { this.taskTemplates = taskTemplates; }
  public boolean hasTaskTemplates()
  {
    return taskTemplates != null && taskTemplates.size() > 0;
  }
  public void addTaskTemplate(TaskTemplate taskTemplate)
  {
    taskTemplates.add(taskTemplate);
    taskTemplate.setPackage(this);
    addTaskTemplateToVO(taskTemplate.getTaskVO());
    Collections.sort(taskTemplates);
  }
  public void removeTaskTemplate(TaskTemplate taskTemplate)
  {
    taskTemplates.remove(taskTemplate);
    removeTaskTemplateFromVO(taskTemplate.getTaskVO());
    Collections.sort(taskTemplates);
  }
  private void addTaskTemplateToVO(TaskVO taskVO)
  {
    if (packageVO.getTaskTemplates() == null)
      packageVO.setTaskTemplates(new ArrayList<TaskVO>());
    packageVO.getTaskTemplates().add(taskVO);
  }
  private void removeTaskTemplateFromVO(TaskVO taskVO)
  {
    if (packageVO.getTaskTemplates() != null)
      packageVO.getTaskTemplates().remove(taskVO);
  }

  private List<ActivityImpl> activityImpls = new ArrayList<ActivityImpl>();
  public List<ActivityImpl> getActivityImpls() { return activityImpls; }
  public void setActivityImpls(List<ActivityImpl> activityImpls) { this.activityImpls = activityImpls; }
  public boolean hasActivityImpls()
  {
    return activityImpls != null && activityImpls.size() > 0;
  }
  public void addActivityImpl(ActivityImpl activityImpl)
  {
    activityImpls.add(activityImpl);
    activityImpl.setPackage(this);
    addActivityImplToVO(activityImpl.getActivityImplVO());
    Collections.sort(activityImpls);
  }
  public void removeActivityImpl(ActivityImpl activityImpl)
  {
    activityImpls.remove(activityImpl);
    removeActivityImplFromVO(activityImpl.getActivityImplVO());
    Collections.sort(activityImpls);
  }
  private void addActivityImplToVO(ActivityImplementorVO activityImplVO)
  {
    if (packageVO.getImplementors() == null)
      packageVO.setImplementors(new ArrayList<ActivityImplementorVO>());
    packageVO.getImplementors().add(activityImplVO);
  }
  private void removeActivityImplFromVO(ActivityImplementorVO activityImplVO)
  {
    if (packageVO.getImplementors() != null)
      packageVO.getImplementors().remove(activityImplVO);
  }

  private List<WorkflowAsset> assets = new ArrayList<WorkflowAsset>();
  public List<WorkflowAsset> getAssets() { return assets; }
  public void setAssets(List<WorkflowAsset> assets) { this.assets = assets; }
  public boolean hasAssets()
  {
    return assets != null && assets.size() > 0;
  }
  public void addAsset(WorkflowAsset asset)
  {
    assets.add(asset);
    asset.setPackage(this);
    addRuleSetToVO(asset.getRuleSetVO());
    Collections.sort(assets);
  }
  public void removeAsset(WorkflowAsset asset)
  {
    assets.remove(asset);
    removeRuleSetFromVO(asset.getRuleSetVO());
    Collections.sort(assets);
  }
  private void addRuleSetToVO(RuleSetVO ruleSetVO)
  {
    if (packageVO.getRuleSets() == null)
      packageVO.setRuleSets(new ArrayList<RuleSetVO>());
    packageVO.getRuleSets().add(ruleSetVO);
  }
  private void removeRuleSetFromVO(RuleSetVO ruleSetVO)
  {
    if (packageVO.getRuleSets() != null)
      packageVO.getRuleSets().remove(ruleSetVO);
  }

  private WorkflowPackage topLevelVersion;
  public WorkflowPackage getTopLevelVersion() { return topLevelVersion; }
  public void setTopLevelVersion(WorkflowPackage topLevelVersion) { this.topLevelVersion = topLevelVersion; }
  public boolean isTopLevel()
  {
    // only top-level versions don't have a topLevelVersion
    return topLevelVersion == null;
  }

  public String getMetaContent()
  {
    return packageVO.getMetaContent();
  }
  public void setMetaContent(String metaContent)
  {
    packageVO.setMetaContent(metaContent);
  }

  public Date getModifyDate()
  {
    return packageVO.getModifyDate();
  }
  public void setModifyDate(Date modDate)
  {
    packageVO.setModifyDate(modDate);
  }
  public String getFormattedModifyDate()
  {
    if (getModifyDate() == null)
      return "";
    return PluginUtil.getDateFormat().format(getModifyDate());
  }

  private boolean archived;
  public boolean isArchived() { return archived; }
  public void setArchived(boolean archived) { this.archived = archived; }

  public boolean isReadOnly() { return isArchived(); }

  public boolean hasInstanceInfo() { return false; }

  private Folder archivedFolder;
  public Folder getArchivedFolder() { return archivedFolder; }
  public void setArchivedFolder(Folder folder) { this.archivedFolder = folder; }

  private List<Folder> childFolders = new ArrayList<Folder>();
  public List<Folder> getChildFolders() { return childFolders; }
  public void setChildFolders(List<Folder> folders) { this.childFolders = folders; }
  public boolean hasChildFolders()
  {
    return childFolders != null && childFolders.size() > 0;
  }

  /**
   * Synchronizes the process list on the PackageVO with the local list.
   */
  public void syncProcessVos()
  {
    List<ProcessVO> processVOs = new ArrayList<ProcessVO>();
    for (WorkflowProcess processVersion : processes)
    {
      processVOs.add(processVersion.getProcessVO());
    }
    packageVO.setProcesses(processVOs);
  }

  public void syncProcesses()
  {
    List<WorkflowProcess> processVersions = new ArrayList<WorkflowProcess>();
    for (ProcessVO processVO : packageVO.getProcesses())
    {
      WorkflowProcess processVersion = new WorkflowProcess(getProject(), processVO);
      processVersion.setPackage(this);
      processVersions.add(processVersion);
    }
    this.setProcesses(processVersions);
  }

  public WorkflowPackage(WorkflowProject wfProject, PackageVO packageVO)
  {
    this.packageVO = packageVO;
    setProject(wfProject);
    if (packageVO.getSchemaVersion() == 0)
      packageVO.setSchemaVersion(wfProject.getDesignerProxy().getPluginDataAccess().getSchemaVersion());
  }

  public WorkflowPackage(WorkflowPackage cloneFrom)
  {
    this(cloneFrom.getProject(), cloneFrom.getPackageVO());
  }

  public WorkflowPackage()
  {
    packageVO = new PackageVO();
    packageVO.setRuleSets(new ArrayList<RuleSetVO>());
  }

  @Override
  public String getTitle()
  {
    return "Package";
  }

  @Override
  public Long getId()
  {
    return packageVO.getPackageId();
  }

  public String getIdLabel()
  {
    if (getProject().getPersistType() == PersistType.Git)
      return getId() + " (" + getHexId() + ")";
    else
      return String.valueOf(getId());
  }

  public String getName()
  {
    return packageVO.getPackageName();
  }
  public void setName(String name)
  {
    packageVO.setPackageName(name);
  }

  public boolean isDefaultPackage()
  {
    return getName().equals(PackageVO.DEFAULT_PACKAGE_NAME);
  }

  public WorkflowProcess getProcess(String processName)
  {
    for (WorkflowProcess processVersion : processes)
    {
      if (processVersion.getName().equals(processName))
        return processVersion;
    }
    return null;
  }

  public WorkflowAsset getAsset(String name)
  {
    for (WorkflowAsset asset : assets)
    {
      if (asset.getName().equals(name))
        return asset;
    }
    return null;
  }

  public String getVersionString()
  {
    return packageVO.getVersionString();
  }

  public int getVersion()
  {
    return packageVO.getVersion();
  }

  public void setVersion(int v)
  {
    packageVO.setVersion(v);
  }

  /**
   * TODO: Not tested, not used.  Added for Versionable Interface
   */
  public int getNextMajorVersion()
  {
    return (packageVO.getVersion()/1000 + 1) * 1000;
  }

  /**
   * TODO: Not tested, not used.  Added for Versionable Interface
   */
  public int getNextMinorVersion()
  {
    return packageVO.getVersion()/1000 + (packageVO.getVersion()%1000 + 1) * 100;
  }


  public int parseVersion(String versionString) throws NumberFormatException
  {
    return PackageVO.parseVersion(versionString);
  }

  public String formatVersion(int version)
  {
    return PackageVO.formatVersion(version);
  }

  public int getSchemaVersion()
  {
    return packageVO.getSchemaVersion();
  }

  public void setSchemaVersion(int version)
  {
    packageVO.setSchemaVersion(version);
  }

  @Override
  public String getLabel()
  {
    if (isDefaultPackage())
    {
      return getName();
    }
    else
    {
      return getName() + " " + getVersionLabel();
    }
  }

  public String getFullPathLabel()
  {
    return getProjectPrefix() + getLabel();
  }

  @Override
  public String getIcon()
  {
    return "package.gif";
  }

  public String getVersionLabel()
  {
    if (isDefaultPackage())
      return "";
    else
      return "v" + getVersionString();
  }

  public boolean isExported()
  {
    return packageVO.isExported();
  }

  public void setExported(boolean exported)
  {
    packageVO.setExported(exported);
  }

  public String getDescription()
  {
    return packageVO.getPackageDescription();
  }
  public void setDescription(String description)
  {
    packageVO.setPackageDescription(description);
  }

  public AttributeVO getAttributeVO(String name)
  {
    if (packageVO.getAttributes() != null)
    {
      for (AttributeVO attribute : packageVO.getAttributes())
      {
        if (attribute.getAttributeName().equals(name))
          return attribute;
      }
    }
    return null;
  }

  public String getAttribute(String name)
  {
    AttributeVO attr = getAttributeVO(name);
    return attr == null ? null : attr.getAttributeValue();
  }

  public void removeAttribute(String name)
  {
    if (packageVO.getAttributes() != null)
    {
      AttributeVO toRemove = null;
      for (AttributeVO attribute : packageVO.getAttributes())
      {
        if (attribute.getAttributeName().equals(name))
        {
          toRemove = attribute;
          break;
        }
      }
      if (toRemove != null)
        packageVO.getAttributes().remove(toRemove);
    }
  }

  public void setAttribute(String name, String value)
  {
    if (packageVO.getAttributes() == null)
      packageVO.setAttributes(new ArrayList<AttributeVO>());
    AttributeVO attr = null;
    for (AttributeVO attribute : packageVO.getAttributes())
    {
      if (attribute.getAttributeName().equals(name))
      {
        attr = attribute;
        break;
      }
    }
    if (attr == null)
    {
      attr = new AttributeVO(name, value);
      packageVO.getAttributes().add(attr);
    }
    else
    {
      attr.setAttributeValue(value);
    }
  }

  public String getVcsAssetPath()
  {
    if (!getProject().isFilePersist())
      throw new UnsupportedOperationException("Only for VCS Assets");

    String projPath = getProject().getVcsAssetPath();

    if (isArchived())
      return projPath + "/Archive/" + getLabel();
    else
      return projPath + "/" + getName().replace('.', '/');
  }

  public List<String> getVcsAssetMetaPaths()
  {
    if (!getProject().isFilePersist())
      throw new UnsupportedOperationException("Only for VCS Assets");

    List<String> metaPaths = new ArrayList<String>();
    String metaPkgPath = getVcsAssetPath() + "/.mdw";
    metaPaths.add(metaPkgPath + "/package.xml");
    metaPaths.add(metaPkgPath + "/versions");
    return metaPaths;
  }

  public IFolder getMetaFolder()
  {
    assert getProject().isFilePersist();
    return getFolder(".mdw");
  }

  public IFile getMetaFile()
  {
    IFile metaFile = getMetaFolder().getFile("package.json");
    if (!metaFile.exists())
      metaFile = getMetaFolder().getFile("package.xml");
    return metaFile;
  }

  public String getTags()
  {
    return getAttribute(WorkAttributeConstant.VERSION_TAG);
  }

  public void setTags(String tags)
  {
    setAttribute(WorkAttributeConstant.VERSION_TAG, tags);
  }

  public boolean workflowAssetNameExists(String name)
  {
    if (isDefaultPackage())
      return getProject().workflowAssetNameExists(name);

    for (WorkflowAsset asset : assets)
    {
      if (asset.getName().equals(name))
        return true;
    }

    return false;
  }

  public boolean isLatest()
  {
    for (WorkflowPackage pkg : getAllPackageVersions())
    {
      if (pkg.getVersion() > getVersion())
        return false;
    }
    return true;
  }

  public List<WorkflowPackage> getAllPackageVersions()
  {
    List<WorkflowPackage> allVersions = new ArrayList<WorkflowPackage>();
    for (WorkflowPackage pv : getProject().getTopLevelPackages())
    {
      if (pv.getName().equals(getName()))
        allVersions.add(pv);
    }
    for (WorkflowElement we : getProject().getArchivedPackageFolder().getChildren())
    {
      WorkflowPackage pv = (WorkflowPackage) we;
      if (pv.getName().equals(getName()) && pv.getDescendantPackageVersions() != null)
        allVersions.addAll(pv.getDescendantPackageVersions());
    }

    Collections.sort(allVersions);
    Collections.reverse(allVersions);
    return allVersions;
  }

  public int compareTo(WorkflowPackage other)
  {
    int res = this.getName().compareToIgnoreCase(other.getName());
    if (res != 0)
      return res;
    // versions sorted in descending order
    return this.getVersion() - other.getVersion();
  }

  public boolean isHomogeneous(WorkflowElement we)
  {
    if (!super.isHomogeneous(we))
      return false;

    if (!(we instanceof WorkflowPackage))
      return false;

    WorkflowPackage other = (WorkflowPackage) we;
    return (this.isTopLevel() == other.isTopLevel()
            && this.isArchived() == other.isArchived());
  }

  public String getWelcomePagePath()
  {
    // TODO get welcome page from package properties
    String welcomePage = "index.jsf";
    return "/" + getName() + "/" + welcomePage;
  }

  // Versionable methods not currently supported
  public String getLockingUser()
  {
    return null;
  }
  public void setLockingUser(String lockUser)
  {
  }
  public String getExtension()
  {
    return null;
  }

  private String group;
  public String getGroup()
  {
    return packageVO == null ? group : packageVO.getGroup();
  }

  public void setGroup(String group)
  {
    if (packageVO == null)
      this.group = group;
    else
      packageVO.setGroup(group);
  }

  public DesignerDataModel getDesignerDataModel()
  {
    return getProject().getDesignerDataModel();
  }

  public boolean isVisible()
  {
    boolean permission = StringHelper.isEmpty(getGroup()) || getGroup().equals(UserGroupVO.COMMON_GROUP) || getDesignerDataModel().belongsToGroup(getGroup());
    if (permission)
    {
      // check for VCS prefix filters
      if (getProject().isFilePersist() && getProject().getMdwVcsRepository().getPkgPrefixes() != null)
      {
        // further restrict
        for (String prefix : getProject().getMdwVcsRepository().getPkgPrefixes())
        {
          if (getName().startsWith(prefix))
            return true;
        }
        return false;
      }
      else
      {
        return true;
      }
    }
    else
    {
      return false;
    }
  }

  public boolean isUserAuthorized(String role)
  {
    if (getProject().isFilePersist() && UserRoleVO.ASSET_DESIGN.equals(role))
    {
      if (getProject().isRemote())
      {
        return false;
      }
      else
      {
        return true;
      }
    }
    return getDesignerDataModel().userHasRole(getGroup(), role);
  }

  public List<String> getTestCaseStringList()
  {
    List<String> lst = new ArrayList<String>();
    for (AutomatedTestCase testCase : getTestCases())
      lst.add(testCase.getPath());
    return lst;
  }

  public List<AutomatedTestCase> getTestCases()
  {
    List<AutomatedTestCase> testCases = new ArrayList<AutomatedTestCase>();
    for (WorkflowAsset asset : getAssets())
    {
      if (asset instanceof AutomatedTestCase)
        testCases.add((AutomatedTestCase)asset);
    }
    return testCases;
  }

  public IFolder getFolder()
  {
    assert getProject().isFilePersist();
    return getProject().getSourceProject().getFolder("/" + getVcsAssetPath());
  }

  public IFolder getFolder(String path)
  {
    return getFolder().getFolder(path);
  }

  public void refreshFolder()
  {
    try
    {
      getFolder().refreshLocal(IResource.DEPTH_INFINITE, null);
    }
    catch (CoreException ex)
    {
      PluginMessages.log(ex);
    }
  }
  /**
   * Updated this to use a WorkspaceJob. See mdw issues 44
   */
  public void refreshMdwMetaFolder()
  {
    scheduleRefresh(getMetaFolder());
  }

  @Override
  public void fireElementChangeEvent(ChangeType changeType, Object newValue)
  {
    super.fireElementChangeEvent(changeType, newValue);
    if (getProject().isFilePersist() && (changeType == ChangeType.ELEMENT_CREATE ||
        changeType == ChangeType.ELEMENT_DELETE || changeType == ChangeType.RENAME))
      getProject().refreshProject();
  }

  public WorkflowAsset getAsset(IFile file)
  {
    assert getProject().isFilePersist();
    for (WorkflowAsset asset : getAssets())
    {
      if (file.equals(asset.getFile()))
        return asset;
    }
    return null;
  }

  public TaskTemplate getTaskTemplate(IFile file)
  {
    assert getProject().isFilePersist();
    for (TaskTemplate taskTemplate : getTaskTemplates())
    {
      if (file.equals(taskTemplate.getFile()))
        return taskTemplate;
    }
    return null;
  }

  @Override
  public String toString()
  {
    return getLabel();
  }
}
