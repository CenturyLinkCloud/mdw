/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.jsf.phase;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.services.dao.task.cache.TaskTemplateCache;
import com.centurylink.mdw.taskmgr.ui.list.ListManager;
import com.centurylink.mdw.taskmgr.ui.tasks.Tasks;
import com.centurylink.mdw.taskmgr.ui.tasks.template.TaskTemplateListController;
import com.centurylink.mdw.taskmgr.ui.workgroups.WorkgroupTree;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.ListItem;

public class TaskTemplatePhaseListener implements PhaseListener
{
  public static final String EXPORT_TASK_TEMPLATES = "exportTaskTemplates";
  public static final String IMPORT_TASK_TEMPLATES = "importTaskTemplates";

  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  public PhaseId getPhaseId()
  {
    return PhaseId.ANY_PHASE;
  }

  public void beforePhase(PhaseEvent phaseEvent)
  {
    if (phaseEvent.getPhaseId() == PhaseId.RESTORE_VIEW)
    {
      FacesContext facesContext = phaseEvent.getFacesContext();
      Map<String,String> paramMap = facesContext.getExternalContext().getRequestParameterMap();
      if (paramMap.containsKey("taskLogicalId"))
      {
        String logicalId = paramMap.get("taskLogicalId");
        TaskVO task = TaskTemplateCache.getTaskTemplate(null, logicalId);
        WorkgroupTree workgroupTree = WorkgroupTree.getInstance();
        workgroupTree.setTaskDetailTemplate(task);
        FacesVariableUtil.setValue("workgroupTree", workgroupTree);
      }
    }
  }

  public void afterPhase(PhaseEvent phaseEvent)
  {
    FacesContext facesContext = phaseEvent.getFacesContext();
    Map<String,String> paramMap = facesContext.getExternalContext().getRequestParameterMap();

    if (phaseEvent.getPhaseId() == PhaseId.RESTORE_VIEW)
    {
      if (containsFlag(paramMap, EXPORT_TASK_TEMPLATES))
      {
        HttpServletResponse response = (HttpServletResponse)facesContext.getExternalContext().getResponse();
        HttpServletRequest request = (HttpServletRequest)facesContext.getExternalContext().getRequest();
        if (request.getMethod().equalsIgnoreCase("get"))
        {
          // we've been redirected -- download now
          try
          {
            TaskTemplateListController controller = (TaskTemplateListController)FacesVariableUtil.getValue(TaskTemplateListController.CONTROLLER_BEAN);
            String xml = controller.doExport();
            DateFormat df = new SimpleDateFormat("MM-dd-yyyy");
            response.setContentType("application/octet-stream");
            String exportFileName = "mdwTaskTemplates_" + df.format(new Date()) + ".xml";
            response.setHeader("Content-Disposition", "attachment;filename=\"" + exportFileName + "\"");
            response.getWriter().print(xml);
            facesContext.responseComplete();
          }
          catch (Exception ex)
          {
            logger.severeException(ex.getMessage(), ex);
            try
            {
              FacesVariableUtil.showError(ex.toString());
            }
            catch (UIException ex2)
            {
              logger.severeException(ex2.getMessage(), ex2);
            }
          }
        }
      }
      else if (containsFlag(paramMap, IMPORT_TASK_TEMPLATES))
      {
        // prevent postback since modal panel handles form submission
        // (see taskList.xhtml and TaskTemplateActionController.java)
        facesContext.responseComplete();
      }
    }
    else if (phaseEvent.getPhaseId() == PhaseId.UPDATE_MODEL_VALUES)
    {
      if (containsFlag(paramMap, EXPORT_TASK_TEMPLATES))
      {
        try
        {
          TaskTemplateListController controller = (TaskTemplateListController)FacesVariableUtil.getValue(TaskTemplateListController.CONTROLLER_BEAN);
          Tasks taskTemplateList = ListManager.getInstance().getTaskTemplateList();
          List<ListItem> selected = taskTemplateList.getMarked();
          if (selected.size() == 0)
          {
            FacesVariableUtil.addMessage("Please select task templates to export.");
            return;
          }
          controller.setExportList(selected);
          facesContext.responseComplete();
        }
        catch (Exception ex)
        {
          logger.severeException(ex.getMessage(), ex);
          try
          {
            FacesVariableUtil.showError(ex.toString());
          }
          catch (UIException ex2)
          {
            logger.severeException(ex2.getMessage(),  ex2);
          }
        }
      }
    }
  }

  private boolean containsFlag(Map<String,String> paramMap, String flag)
  {
    for (Iterator<String> keys = paramMap.keySet().iterator(); keys.hasNext(); )
    {
      String key = keys.next();
      if (key.endsWith(flag))
        return true;
    }
    return false;
  }
}
