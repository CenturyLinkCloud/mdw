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
package com.centurylink.mdw.timer.cleanup;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.event.InternalEvent;
import com.centurylink.mdw.model.monitor.ScheduledJob;
import com.centurylink.mdw.model.monitor.ServiceLevelAgreement;
import com.centurylink.mdw.model.workflow.ActivityInstance;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.workflow.ProcessInstance;
import com.centurylink.mdw.model.workflow.WorkStatus;
import com.centurylink.mdw.service.data.process.EngineDataAccessDB;
import com.centurylink.mdw.service.data.process.ProcessCache;
import com.centurylink.mdw.services.process.ProcessEngineDriver;
import com.centurylink.mdw.util.CallURL;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.TransactionWrapper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

/**
 * Scheduled job that sweeps for expired process SLAs and triggers the
 *  embedded delay handler for any instances that exceed the SLA.
 *  mdw.timer.task.ProcessSlaScheduledJob.TimerClass=com.centurylink.mdw.timer.cleanup.ProcessSlaScheduledJob
 *  mdw.timer.task.ProcessSlaScheduledJob.Schedule=0,15,30,45 * * * *
 */
public class ProcessSlaScheduledJob implements ScheduledJob {

    private StandardLogger logger;
    public static final String SLA_UNIT = "SLA_UNIT";

    /**
     * Default Constructor
     */
    public ProcessSlaScheduledJob() {
    }

    /**
     * Method that gets invoked periodically by the container
     *
     */
    public void run(CallURL args) {

        logger = LoggerUtil.getStandardLogger();
        logger.info("Calling ProcessSlaScheduledJob");
        TransactionWrapper transaction = null;
        EngineDataAccessDB edao = new EngineDataAccessDB();
        SimpleDateFormat sdfStatDate = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        try {
            transaction = edao.startTransaction();
            //long dbtime = getDatabaseTime();
            long dbtime =DatabaseAccess.getCurrentTime();
            Calendar systcalendar = Calendar.getInstance();
            systcalendar.setTimeInMillis(dbtime);

            for (Process processVO : ProcessCache.getAllProcesses()) {
                int sla = 0;
                processVO = ProcessCache.getProcess(processVO.getId());
                if ((processVO.getAttribute("SLA") != null && processVO.getAttribute("SLA") != "")) {

                    sla = Integer.parseInt(processVO.getAttribute("SLA"));
                    boolean dealyHandlerDefined = false;
                    if (sla > 0) {
                        List <Process> subProcess = processVO.getSubprocesses();
                        for (Process sProcess : subProcess){
                            if(sProcess.getName().contains("Delay Handler")){
                                dealyHandlerDefined = true;
                                if (dealyHandlerDefined) break;
                            }
                        }
                      if (dealyHandlerDefined){
                        ArrayList<ProcessInstance> listProcessVo = getProcessInstances(processVO.getId());

                        for (ProcessInstance tempProcessVO : listProcessVo) {

                            String processStartDate = tempProcessVO.getStartDate();
                            Date date = sdfStatDate.parse(processStartDate);
                            Calendar processCalendar = Calendar.getInstance();
                            processCalendar.setTimeInMillis(date.getTime());

                            if (ServiceLevelAgreement.INTERVAL_MINUTES.equals(processVO.getAttribute(SLA_UNIT))) {
                                processCalendar.add(Calendar.MINUTE, sla);
                            }
                            else if (ServiceLevelAgreement.INTERVAL_HOURS.equals(processVO.getAttribute(SLA_UNIT))) {
                                processCalendar.add(Calendar.HOUR, sla);
                            }
                            else if (ServiceLevelAgreement.INTERVAL_DAYS.equals(processVO.getAttribute(SLA_UNIT))) {
                                processCalendar.add(Calendar.DATE, sla);
                            }
                            boolean eligibleToProcess = processCalendar.compareTo(systcalendar) < 0;

                            if (eligibleToProcess) {
                                List<ActivityInstance> activityList = tempProcessVO
                                        .getActivities();
                                for (ActivityInstance tempActivityInstanceVO : activityList) {
                                    if (tempActivityInstanceVO.getStatusCode() == WorkStatus.STATUS_WAITING
                                            .intValue()
                                            || tempActivityInstanceVO.getStatusCode() == WorkStatus.STATUS_IN_PROGRESS
                                                    .intValue()) {
                                        InternalEvent event = InternalEvent
                                                .createActivityDelayMessage(tempActivityInstanceVO,
                                                        tempProcessVO.getMasterRequestId());
                                        logger.info("SLA clean event=" + event);
                                        ProcessEngineDriver driver = new ProcessEngineDriver();
                                        driver.processEvents(null, event.toXml());
                                    }

                                }
                            }
                        }
                      }
                    }
                }

            }

        }
        catch (Exception exp) {
            logger.severeException(exp.getMessage(), exp);
        }
        finally {
            try {
                edao.stopTransaction(transaction);
            }
            catch (Exception exp) {
                logger.severeException(exp.getMessage(), exp);
            }
        }
    }

    /*
     * Read  all process instances with wait or in progress stage and expired process SLAs
     */
    private ArrayList<ProcessInstance> getProcessInstances(Long process_id) {
        String getProInstances = "select * from PROCESS_INSTANCE where process_id=? and END_DT is null and (status_cd=7 or status_cd=2)"; // Workstatus 2,7

        DatabaseAccess db = new DatabaseAccess(null);
        final ArrayList<ProcessInstance> processVoList = new ArrayList<ProcessInstance>();
        try {
            db.openConnection();
            Object[] args = new Object[1];
            args[0] = process_id;

            ResultSet rs = db.runSelect(getProInstances, args);
            while (rs.next()) {
                ProcessInstance processInstVO = new ProcessInstance();
                processInstVO.setId(rs.getLong(1));
                processInstVO.setProcessId(rs.getLong(2));
                processInstVO.setStartDate(StringHelper.dateToString(rs.getTimestamp(8)));
                processInstVO.setOwnerId(rs.getLong(4));
                processInstVO.setMasterRequestId(rs.getString(15));
                processInstVO.setActivities(getProcessInstanceActivity(rs.getLong(1)));
                processVoList.add(processInstVO);
            }
        }
        catch (SQLException ex) {
            logger.severeException(ex.getMessage(), ex);
        }
        finally {
            db.closeConnection();
        }

        return processVoList;
    }

    private List<ActivityInstance> getProcessInstanceActivity(Long procInstId) {
        List<ActivityInstance> actInstList = new ArrayList<ActivityInstance>();

        DatabaseAccess db = new DatabaseAccess(null);
        String query = "select ACTIVITY_INSTANCE_ID,STATUS_CD,START_DT,END_DT,STATUS_MESSAGE,ACTIVITY_ID"
                + " from ACTIVITY_INSTANCE where PROCESS_INSTANCE_ID=?"
                + " order by ACTIVITY_INSTANCE_ID desc";
        try {

            db.openConnection();
            Object[] args = new Object[1];
            args[0] = procInstId;
            ResultSet rs = db.runSelect(query, args);
            ActivityInstance actInst;
            while (rs.next()) {
                actInst = new ActivityInstance();
                actInst.setId(new Long(rs.getLong(1)));
                actInst.setStatusCode(rs.getInt(2));
                actInst.setStartDate(rs.getTimestamp(3));
                actInst.setEndDate(rs.getTimestamp(4));
                actInst.setMessage(rs.getString(5));
                actInst.setActivityId(new Long(rs.getLong(6)));
                actInst.setProcessInstanceId(procInstId);
                actInstList.add(actInst);
            }
        }
        catch (SQLException ex) {
            logger.severeException(ex.getMessage(), ex);
        }
        finally {
            db.closeConnection();
        }

        return actInstList;

    }

}
