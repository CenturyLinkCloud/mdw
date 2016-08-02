/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.ListenerList;

import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.designer.display.Node;
import com.centurylink.mdw.model.value.activity.ActivityVO;
import com.centurylink.mdw.model.value.attribute.AttributeVO;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.task.TaskInstanceVO;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;
import com.centurylink.mdw.model.value.work.ActivityInstanceVO;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

/**
 * Wraps a Designer workflow Node.
 */
public class Activity extends WorkflowElement implements AttributeHolder
{
  // used to identify activity type in new wizard actions
  public class StartActivity extends Activity {};
  public class AdapterActivity extends Activity {};
  public class EvaluatorActivity extends Activity {};

  private Node node;
  public Node getNode() { return node; }

  public List<AttributeVO> getAttributes()
  {
    return node.getAttributes();
  }

  private WorkflowProcess process;
  public WorkflowProcess getProcess() { return process; }

  public WorkflowPackage getPackage()
  {
    return process.getPackage();
  }

  public WorkflowProject getProject()
  {
    return process.getProject();
  }

  private ActivityImpl activityImpl;
  public ActivityImpl getActivityImpl() { return activityImpl; }
  public void setActivityImpl(ActivityImpl activityImpl)
  {
    this.activityImpl = activityImpl;
    if (activityImpl != null)
      node.nodet.setImplementorClassName(activityImpl.getImplClassName());
  }

  public Entity getActionEntity()
  {
    if (hasInstanceInfo())
      return Entity.ActivityInstance;
    else
      return Entity.Activity;
  }

  private ProcessInstanceVO processInstance;
  public ProcessInstanceVO getProcessInstance() { return processInstance; }
  public void setProcessInstance(ProcessInstanceVO processInstance) { this.processInstance = processInstance; }

  private List<ActivityInstanceVO> instances;
  public List<ActivityInstanceVO> getInstances() { return instances; }
  public void setInstances(List<ActivityInstanceVO> instances) { this.instances = instances; }
  public boolean hasInstanceInfo()
  {
    return instances != null && instances.size() > 0;
  }

  /**
   * true if the activity is displayed within a process
   * (still might not have instance info if flow has not reached here yet)
   */
  public boolean isForProcessInstance()
  {
    return processInstance != null;
  }

  private List<ProcessInstanceVO> subProcessInstances;
  public List<ProcessInstanceVO> getSubProcessInstances() { return subProcessInstances; }
  public void setSubProcessInstances(List<ProcessInstanceVO> subProcessInstances) { this.subProcessInstances = subProcessInstances; }
  public boolean hasSubProcessInstances()
  {
    return subProcessInstances != null && subProcessInstances.size() > 0;
  }

  private List<TaskInstanceVO> taskInstances;
  public List<TaskInstanceVO> getTaskInstances() { return taskInstances; }
  public void setTaskInstances(List<TaskInstanceVO> taskInstances) { this.taskInstances = taskInstances; }
  public boolean hasTaskInstances() { return taskInstances != null && !taskInstances.isEmpty(); }

  private List<TaskInstanceVO> subTaskInstances;
  public List<TaskInstanceVO> getSubTaskInstances() { return subTaskInstances; }
  public void setSubTaskInstances(List<TaskInstanceVO> subTaskInstances) { this.subTaskInstances = subTaskInstances; }
  public boolean hasSubTaskInstances() { return subTaskInstances != null && !subTaskInstances.isEmpty(); }

  public Activity() { }

  public Activity(Node node, WorkflowProcess processVersion, ActivityImpl activityImpl)
  {
    this.node = node;
    Collections.<AttributeVO>sort(node.getAttributes());
    this.process = processVersion;
    this.activityImpl = activityImpl;
    if (isScriptExecutor() && getScriptLanguage() == null)
    {
      if (getId() < 0)  // new
        setScriptLanguage("Groovy");
      else
        setScriptLanguage("MagicBox");
    }
    else if (isTransformActivity() && getTransformLanguage() == null)
    {
      setTransformLanguage("GPath");
    }
  }

  public String getTitle()
  {
    return "Activity";
  }

  public Long getId()
  {
    return node.getId();
  }

  public int getSequenceId()
  {
    return node.getSequenceId();
  }

  public boolean isReadOnly()
  {
    return process.isReadOnly();
  }

  @Override
  public String getLabel()
  {
    String logicalId = getAttribute(WorkAttributeConstant.LOGICAL_ID);
    return getName() + (logicalId == null ? "" : " " + logicalId);
  }

  @Override
  public String getFullPathLabel()
  {
    return getPath() + (getProcess() == null ? "" : getProcess().getName() + "/") + getLabel();
  }

  public String getName()
  {
    return node.getName();
  }

  public void setName(String name)
  {
    node.setName(name);
  }

  @Override
  public String getIcon()
  {
    return "element.gif";
  }

  public String getDescription()
  {
    if (getProcess().isInRuleSet())
      node.setDescription(getAttribute("DESCRIPTION"));
    return node.getDescription();
  }

  public void setDescription(String description)
  {
    node.setDescription(description);
    if (getProcess().isInRuleSet())
      setAttribute("DESCRIPTION", description);
  }

  public String getScriptLanguage()
  {
    return getAttribute("SCRIPT");
  }

  public void setScriptLanguage(String language)
  {
    setAttribute("SCRIPT", language);
  }

  public String getTransformLanguage()
  {
    return getAttribute("Transform Language");
  }

  public void setTransformLanguage(String language)
  {
    setAttribute("Transform Language", language);
  }

  public String getReturnCode()
  {
    return getAttribute("RETURN_CODE");
  }

  public void setReturnCode(String returnCode)
  {
    setAttribute("RETURN_CODE", returnCode);
  }

  public String getLogicalId()
  {
    return getAttribute("LOGICAL_ID");
  }

  public void setLogicalId(String logicalId)
  {
    setAttribute("LOGICAL_ID", logicalId);
  }

  public boolean isAdapter()
  {
    return getActivityImpl().getActivityImplVO().isAdapter();
  }

  public boolean isLdapAdapter()
  {
    return getActivityImpl().getActivityImplVO().isLdapAdapter();
  }

  public boolean isEventWait()
  {
    return getActivityImpl().getActivityImplVO().isEventWait();
  }

  public boolean isStart()
  {
    return getActivityImpl().getActivityImplVO().isStart();
  }

  public boolean isScript()
  {
    return getActivityImpl().getActivityImplVO().isScript();
  }

  public boolean isRule()
  {
    return getActivityImpl().getActivityImplVO().isRule();
  }

  public boolean isTransformActivity()
  {
    return getActivityImpl().getActivityImplVO().isTransformActivity();
  }

  public boolean isScriptExecutor()
  {
    return getActivityImpl().getActivityImplVO().isScriptExecutor();
  }

  public boolean isExpressionEval()
  {
    return getActivityImpl().getActivityImplVO().isExpressionEval();
  }

  public boolean isNotification()
  {
    return getActivityImpl().getActivityImplVO().isNotification();
  }

  public boolean isProcessStart()
  {
    return node.getBaseClassName() != null
      && node.getBaseClassName().endsWith("StartActivity");
  }

  public boolean isSubProcessInvoke()
  {
    return node.getBaseClassName() != null
      && node.getBaseClassName().endsWith("InvokeProcessActivity");
  }

  public boolean isOldMultipleSubProcInvoke()
  {
    return getActivityImpl().getImplClassName().endsWith("InvokeMultipleProcessActivity");
  }

  public boolean isHeterogeneousSubProcInvoke()
  {
    return isSubProcessInvoke() && getActivityImpl().getImplClassName().contains("Heterogeneous");
  }

  public boolean isManualTask()
  {
    return node.getBaseClassName() != null
      && node.getBaseClassName().endsWith("TaskActivity");
  }

  public boolean isAutoFormManualTask()
  {
    return isManualTask()
      && getActivityImpl().getImplClassName().endsWith("AutoFormManualTaskActivity");
  }

  public boolean isDynamicJava()
  {
    return node.getBaseClassName() != null
        && getActivityImpl().getImplClassName().endsWith("JavaActivity");
  }

  public boolean isOsgiAdapter()
  {
    return node.getBaseClassName() != null
        && getActivityImpl().getImplClassName().endsWith("OsgiServiceAdapter");
  }

  public boolean canWriteOutputDocs()
  {
    String implClass = getActivityImpl().getImplClassName();
    return implClass != null
      && !implClass.endsWith("ScriptEvaluator");
  }

  /**
   * Sets the value of the specified attribute.
   * @param attrName
   * @param attrValue
   */
  public void setAttribute(String attrName, String attrValue)
  {
    for (AttributeVO attribute : node.getAttributes())
    {
      if (attribute.getAttributeName().equals(attrName))
      {
        attribute.setAttributeValue(attrValue);
        fireAttributeValueChanged(attrName, attrValue);
        return;
      }
    }
    // not found, so add
    AttributeVO attrVO = new AttributeVO(attrName, attrValue);
    node.getAttributes().add(attrVO);
    fireAttributeValueChanged(attrName, attrValue);
  }

  /**
   * Removes the specified attribute.
   * @param attrName
   * @param attrValue
   */
  public boolean removeAttribute(String attrName)
  {
    boolean success = false;
    for (AttributeVO attribute : node.getAttributes())
    {
      if (attribute.getAttributeName().equals(attrName))
      {
        success = node.getAttributes().remove(attribute);
        fireAttributeValueChanged(attrName, null);
        return success;
      }
    }
    fireAttributeValueChanged(attrName, null);
    return success;
  }

  private ListenerList attributeValueChangeListeners = new ListenerList();
  public void addAttributeValueChangeListener(AttributeValueChangeListener listener)
  {
    attributeValueChangeListeners.add(listener);
  }
  public void removeAttributeValueChangeListener(AttributeValueChangeListener listener)
  {
    attributeValueChangeListeners.remove(listener);
  }
  public void fireAttributeValueChanged(String attrName, String newValue)
  {
    for (int i = 0; i < attributeValueChangeListeners.getListeners().length; ++i)
    {
      AttributeValueChangeListener listener
        = (AttributeValueChangeListener) attributeValueChangeListeners.getListeners()[i];
      if (listener.getAttributeName().equals(attrName))
        listener.attributeValueChanged(newValue);
    }
  }

  public List<String> getUpstreamActivityNames()
  {
    List<String> upstreamActivityNames = new ArrayList<String>();
    ProcessVO processVO = process.getProcessVO();
    for (ActivityVO activityVO : processVO.getUpstreamActivities(getId()))
    {
      upstreamActivityNames.add(activityVO.getActivityName());
    }
    Collections.sort(upstreamActivityNames);
    return upstreamActivityNames;
  }

  private List<String> scriptLanguages;
  public List<String> getScriptLanguages() { return scriptLanguages; }
  public void setScriptLanguages(String languages)
  {
    scriptLanguages = new ArrayList<String>();
    if (languages == null || languages.trim().length() == 0)
    {
      // backward compatibility
      scriptLanguages.add("Groovy");
      scriptLanguages.add("JavaScript");
      scriptLanguages.add("MagicBox");
    }
    else
    {
      // dynamic language support
      String[] langs = languages.split(",");
      for (String lang : langs)
        scriptLanguages.add(lang.trim());
    }
  }

  private static List<String> transformLanguages;
  public List<String> getTransformLanguages()
  {
    if (transformLanguages == null)
    {
      transformLanguages = new ArrayList<String>();
      transformLanguages.add("GPath");
      transformLanguages.add("XSLT");
    }
    return transformLanguages;
  }

  private static List<String> rulesLanguages;
  public List<String> getRulesLanguages()
  {
    if (rulesLanguages == null)
    {
      rulesLanguages = new ArrayList<String>();
      rulesLanguages.add("Drools");
      rulesLanguages.add("Guided");
    }
    return rulesLanguages;
  }

  @Override
  public Long getProcessId()
  {
    return getProcess().getId();
  }

  @Override
  public boolean overrideAttributesApplied()
  {
    return getProcess().overrideAttributesApplied();
  }
  @Override
  public boolean isOverrideAttributeDirty(String prefix)
  {
    return getProcess().isAttributeOwnerDirty(prefix, OwnerType.ACTIVITY, getId());
  }
  @Override
  public void setOverrideAttributeDirty(String prefix, boolean dirty)
  {
    getProcess().setAttributeOwnerDirty(prefix, OwnerType.ACTIVITY, getId(), dirty);
  }

}
