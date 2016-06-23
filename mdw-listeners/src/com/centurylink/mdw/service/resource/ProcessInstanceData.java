/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.resource;

import java.util.List;
import java.util.Map;

import org.json.JSONException;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.service.JsonService;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.TextService;
import com.centurylink.mdw.common.service.XmlService;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.dataaccess.RuntimeDataAccess;
import com.centurylink.mdw.model.data.work.WorkStatuses;
import com.centurylink.mdw.model.value.activity.ActivityVO;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.variable.VariableInstanceInfo;
import com.centurylink.mdw.model.value.work.ActivityInstanceVO;
import com.centurylink.mdw.services.BamManager;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.bam.BamManagerBean;
import com.centurylink.mdw.services.dao.process.cache.ProcessVOCache;

public class ProcessInstanceData implements TextService, XmlService, JsonService {

    public static final String PARAM_MASTER_REQUEST_ID = "MasterRequestId";
    public static final String PARAM_PROCESS_INSTANCE_ID = "ProcessInstanceId";

    StandardLogger logger = LoggerUtil.getStandardLogger();

    public String getText(Map<String, Object> parameters, Map<String, String> metaInfo)
            throws ServiceException {
        String masterRequestId = (String) parameters.get(PARAM_MASTER_REQUEST_ID);
        String processInstanceId = (String) parameters.get(PARAM_PROCESS_INSTANCE_ID);

        if (masterRequestId != null) {
            try {
                return getProcessDataByMasterRequest(masterRequestId);
            }
            catch (DataAccessException ex) {
                throw new ServiceException(ex.getMessage(), ex);
            }
        }
        else if (processInstanceId != null) {
            try {
                Long processInstanceIdL = new Long(Long.parseLong(processInstanceId));
                return getProcessDataByInstanceId(processInstanceIdL);
            }
            catch (DataAccessException ex) {
                throw new ServiceException(ex.getMessage(), ex);
            }
        }
        else {
            throw new ServiceException("Missing parameter: Either masterRequestId = "
                    + masterRequestId + "or processRequestId = " + processInstanceId
                    + " is required.");
        }
    }

    private String getProcessDataByInstanceId(Long processInstanceId) throws DataAccessException {
        DatabaseAccess db = null;
        String response = null;
        try {
            db = new DatabaseAccess(null);
            RuntimeDataAccess da = DataAccess.getRuntimeDataAccess(db);
            ProcessInstanceVO processInstanceVO = null;
            ProcessVO processVO = null;
            processInstanceVO = da.getProcessInstanceAll(processInstanceId);
            processVO = ProcessVOCache.getProcessVO(processInstanceVO.getProcessName(), 0);

            response = "<ProcessInstanceData><MasterRequestId>"
                    + processInstanceVO.getMasterRequestId() + "</MasterRequestId>"
                    + getProcessInstanceInfo(processInstanceVO, processVO, da)
                    + "</ProcessInstanceData>";
            logger.debug("response=" + response);
        }
        catch (DataAccessException e) {
            logger.severeException("Unable to get instance data for processInstanceId ="
                    + processInstanceId + e.getMessage(), e);
            return null;
        }
        catch (Exception ex) {
            throw new DataAccessException(-1, ex.getMessage(), ex);
        }
        finally {
            if (db != null)
                db.closeConnection();
        }
        return response;
    }

    /**
     * Bypasses cache.
     *
     * @param realm
     */
    private String getProcessDataByMasterRequest(String masterRequestId) throws DataAccessException {

        String response = null;
        try {
            BamManager bamMgr = new BamManagerBean(new DatabaseAccess(null));
            Long processInstanceId = bamMgr.getMainProcessInstanceId(masterRequestId);
            response = getProcessDataByInstanceId(processInstanceId);
        }
        catch (DataAccessException e) {
            logger.severeException("Unable to get instance data for masterRequestId ="
                    + masterRequestId + e.getMessage(), e);
            return null;
        }
        catch (Exception ex) {
            throw new DataAccessException(-1, ex.getMessage(), ex);
        }
        return response;
    }

    public String getProcessInstanceInfo(ProcessInstanceVO processInstanceVO, ProcessVO processVO,
            RuntimeDataAccess da) {
        StringBuffer output = new StringBuffer();
        output.append(
                "<Process><ProcessName>" + replace(processInstanceVO.getProcessName())
                        + "</ProcessName><ProcessStatus>")
                .append(WorkStatuses.getWorkStatuses().get(processInstanceVO.getStatusCode()))
                .append("</ProcessStatus>").append("<ProcessId>").append(processVO.getId())
                .append("</ProcessId>").append("<ProcessInstanceId>")
                .append(processInstanceVO.getId()).append("</ProcessInstanceId>");

        try {
            getActivities(processVO.getName(), processInstanceVO.getId(), output, da);
        }
        catch (Exception e) {
            logger.severeException("Unable to get process instance data " + e.getMessage(), e);
        }
        return output.append("</Process>").toString();
    }

    private String getActivities(String processName, Long processInstanceId, StringBuffer output,
            RuntimeDataAccess da) throws Exception {
        ProcessVO processVO = ProcessVOCache.getProcessVO(processName, 0);
        ProcessInstanceVO process = DataAccess.getRuntimeDataAccess(new DatabaseAccess(null))
                .getProcessInstanceAll(processInstanceId);
        addVariables(output, processInstanceId);

        List<ActivityInstanceVO> acts = process.getActivities();
        for (int i = acts.size() - 1; i >= 0; i--) {
            ActivityInstanceVO actInstance = acts.get(i);

            ActivityVO act = processVO.getActivityVO(actInstance.getDefinitionId());
            // Check for Null as it could be
            if (act != null) {
                output.append("<Activity>");

                String actName = replace(act.getActivityName());
                output.append("<ActivityId>").append(act.getLogicalId()).append("</ActivityId>")
                        .append("<ActivityInstanceId>").append(actInstance.getId())
                        .append("</ActivityInstanceId>").append("<ActivityName>").append(actName)
                        .append("</ActivityName><ActivityStatus>")
                        .append(WorkStatuses.getWorkStatuses().get(actInstance.getStatusCode()))
                        .append("</ActivityStatus>").append("<ActivityImplementor>")
                        .append(act.getImplementorClassName()).append("</ActivityImplementor>");
                if (act.getAttribute("processmap") != null
                        || (act.getAttribute("processname") != null)) {
                    List<ProcessInstanceVO> pInstanceVOList = da.getProcessInstanceList(
                            "PROCESS_INSTANCE", "ACTIVITY_INSTANCE", actInstance.getId(), null);
                    output.append("<SubProcesses>");
                    for (ProcessInstanceVO processInstanceVO : pInstanceVOList) {
                        output.append("<SubProcess>");
                        output.append("<InstanceId>");
                        output.append(processInstanceVO.getId());
                        output.append("</InstanceId>");
                        output.append("<ProcessName>");
                        output.append(processInstanceVO.getProcessName());
                        output.append("</ProcessName>");
                        output.append("<StartDate>");
                        output.append(processInstanceVO.getStartDate());
                        output.append("</StartDate>");
                        output.append("<EndDate>");
                        output.append(processInstanceVO.getEndDate());
                        output.append("</EndDate>");
                        output.append("<StatusCode>");
                        output.append(processInstanceVO.getStatusCode());
                        output.append("</StatusCode>");
                        output.append("</SubProcess>");
                    }
                    output.append("</SubProcesses>");
                }
                output.append("</Activity>");
            }
        }

        return output.toString();

    }

    private void addVariables(StringBuffer output, Long processInstanceId)
            throws DataAccessException {
        EventManager eventMgr = ServiceLocator.getEventManager();
        List<VariableInstanceInfo> vars = eventMgr.getProcessInstanceVariables(processInstanceId);
        output.append("<Variables>");
        for (VariableInstanceInfo var : vars) {
            output.append("<Variable><Name>").append(var.getName());
            output.append("</Name>");
            output.append("<Id>").append(var.getVariableId());
            output.append("</Id>");
            String dataString = var.getStringValue();
            output.append("<Value>").append(dataString);
            output.append("</Value>");
            output.append("<Type>").append(var.getType());
            output.append("</Type></Variable>");
        }
        output.append("</Variables>");

    }

    private String replace(String processName) {
        return processName.replace("\r\n", " ");
    }

    @Override
    public String getJson(Map<String, Object> parameters, Map<String, String> metaInfo)
            throws ServiceException {
        try {
            return org.json.XML.toJSONObject(getText(parameters, metaInfo)).toString();
        }
        catch (JSONException e) {
            throw new ServiceException("Unable to parse XML to Json "+e.getMessage(), e);
        }
    }

    @Override
    public String getXml(Map<String, Object> parameters, Map<String, String> metaInfo)
            throws ServiceException {
        return getText(parameters, metaInfo);
    }
}
