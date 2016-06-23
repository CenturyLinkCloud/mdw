/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.timer.cleanup;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.centurylink.mdw.common.cache.impl.RuleSetCache;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.TransactionWrapper;
import com.centurylink.mdw.common.utilities.form.CallURL;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.data.monitor.ScheduledJob;
import com.centurylink.mdw.model.data.monitor.ServiceLevelAgreement;
import com.centurylink.mdw.model.data.work.WorkStatus;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.event.InternalEventVO;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.work.ActivityInstanceVO;
import com.centurylink.mdw.services.dao.process.EngineDataAccessDB;
import com.centurylink.mdw.services.dao.process.cache.ProcessVOCache;
import com.centurylink.mdw.services.process.ProcessEngineDriver;

/*
 * Scheduled job that sweeps for expired process SLAs and triggers the
 *  embedded delay handler for any instances that exceed the SLA.
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

            for (RuleSetVO ruleSet : RuleSetCache.getRuleSets(RuleSetVO.PROCESS)) {
                ProcessVO processVO = ProcessVOCache.getProcessVO(ruleSet.getId());
                int sla = 0;
                if ((processVO.getAttribute("SLA") != null && processVO.getAttribute("SLA") != "")) {

                    sla = Integer.parseInt(processVO.getAttribute("SLA"));

                    if (sla > 0) {
                        ArrayList<ProcessInstanceVO> listProcessVo = getProcessInstances(ruleSet
                                .getId());

                        for (ProcessInstanceVO tempProcessVO : listProcessVo) {

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
                                List<ActivityInstanceVO> activityList = tempProcessVO
                                        .getActivities();
                                for (ActivityInstanceVO tempActivityInstanceVO : activityList) {
                                    if (tempActivityInstanceVO.getStatusCode() == WorkStatus.STATUS_WAITING
                                            .intValue()
                                            || tempActivityInstanceVO.getStatusCode() == WorkStatus.STATUS_IN_PROGRESS
                                                    .intValue()) {
                                        InternalEventVO event = InternalEventVO
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
    private ArrayList<ProcessInstanceVO> getProcessInstances(Long process_id) {
        String getProInstances = "select * from PROCESS_INSTANCE where process_id=? and END_DT is null and (status_cd=7 or status_cd=2)"; // Workstatus 2,7

        DatabaseAccess db = new DatabaseAccess(null);
        final ArrayList<ProcessInstanceVO> processVoList = new ArrayList<ProcessInstanceVO>();
        try {
            db.openConnection();
            Object[] args = new Object[1];
            args[0] = process_id;

            ResultSet rs = db.runSelect(getProInstances, args);
            while (rs.next()) {
                ProcessInstanceVO processInstVO = new ProcessInstanceVO();
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

    private List<ActivityInstanceVO> getProcessInstanceActivity(Long procInstId) {
        List<ActivityInstanceVO> actInstList = new ArrayList<ActivityInstanceVO>();

        DatabaseAccess db = new DatabaseAccess(null);
        String query = "select ACTIVITY_INSTANCE_ID,STATUS_CD,START_DT,END_DT,STATUS_MESSAGE,ACTIVITY_ID"
                + " from ACTIVITY_INSTANCE where PROCESS_INSTANCE_ID=?"
                + " order by ACTIVITY_INSTANCE_ID desc";
        try {

            db.openConnection();
            Object[] args = new Object[1];
            args[0] = procInstId;
            ResultSet rs = db.runSelect(query, args);
            ActivityInstanceVO actInst;
            while (rs.next()) {
                actInst = new ActivityInstanceVO();
                actInst.setId(new Long(rs.getLong(1)));
                actInst.setStatusCode(rs.getInt(2));
                actInst.setStartDate(StringHelper.dateToString(rs.getTimestamp(3)));
                actInst.setEndDate(StringHelper.dateToString(rs.getTimestamp(4)));
                actInst.setStatusMessage(rs.getString(5));
                actInst.setDefinitionId(new Long(rs.getLong(6)));
                actInst.setOwnerId(procInstId);
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
