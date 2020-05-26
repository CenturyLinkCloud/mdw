package com.centurylink.mdw.services.process;

import com.centurylink.mdw.model.workflow.TransitionInstance;
import com.centurylink.mdw.util.log.StandardLogger;

public class EngineLogger {

    private StandardLogger logger;

    private int performanceLevel;
    public int getPerformanceLevel() { return performanceLevel; }
    public void setPerformanceLevel(int performanceLevel) { this.performanceLevel = performanceLevel; }

    EngineLogger(StandardLogger logger, int performanceLevel) {
        this.logger = logger;
        this.performanceLevel = performanceLevel;
    }

    void info(String tag, Long processInstanceId, Long activityInstanceId, String message) {
        if (logger.isInfoEnabled()) {
            logger.info(tag, message);
            if (performanceLevel < 9)
                ActivityLogger.persist(processInstanceId, activityInstanceId, StandardLogger.LogLevel.INFO, message);
        }
    }

    void info(String tag, Long processInstanceId, String message) {
        if (logger.isInfoEnabled()) {
            logger.info(tag, message);
            if (performanceLevel < 9)
                ActivityLogger.persist(processInstanceId, null, StandardLogger.LogLevel.INFO, message);
        }
    }

    void info(Long processId, Long processInstanceId, Long activityId, Long activityInstanceId, String message) {
        String tag = logtag(processId, processInstanceId, activityId, activityInstanceId);
        if (performanceLevel < 9)
            info(tag, processInstanceId, activityInstanceId, message);
    }

    void info(Long processId, Long processInstanceId, String masterRequestId, String message) {
        String tag = logtag(processId, processInstanceId, masterRequestId);
        if (performanceLevel < 9)
            info(tag, processInstanceId, null, message);
    }

    void info(Long processInstanceId, Long activityInstanceId, String message) {
        if (logger.isInfoEnabled()) {
            logger.info(message);
            if (performanceLevel < 9)
                ActivityLogger.persist(processInstanceId, activityInstanceId, StandardLogger.LogLevel.INFO, message);
        }
    }

    void error(String tag, Long processInstanceId, Long activityInstanceId, String message) {
        logger.error(tag, message);
        if (performanceLevel < 9)
            ActivityLogger.persist(processInstanceId, activityInstanceId, StandardLogger.LogLevel.ERROR, message);
    }

    void error(String tag, Long processInstanceId, Long activityInstanceId, String message, Throwable t) {
        logger.error(tag, message, t);
        if (performanceLevel < 9)
            ActivityLogger.persist(processInstanceId, activityInstanceId, StandardLogger.LogLevel.ERROR, message, t);
    }

    void error(Long processId, Long processInstanceId, Long activityId, Long activityInstanceId, String message, Throwable t) {
        String tag = logtag(processId, processInstanceId, activityId, activityInstanceId);
        error(tag, processInstanceId, activityInstanceId, message, t);
    }

    void error(Long processId, Long processInstanceId, String masterRequestId, String message) {
        String tag = logtag(processId, processInstanceId, masterRequestId);
        error(tag, processInstanceId, null, message);
    }

    void error(Long processId, Long processInstanceId, String masterRequestId, String message, Throwable t) {
        String tag = logtag(processId, processInstanceId, masterRequestId);
        error(tag, processInstanceId, null, message, t);
    }

    static String logtag(Long procId, Long procInstId, Long actId, Long actInstId) {
        return "p" + procId + "." + procInstId + " a" + actId + "." + actInstId;
    }

    static String logtag(Long procId, Long procInstId, String masterRequestId) {
        return "p" + procId + "." + procInstId + " m." + masterRequestId;
    }

    static String logtag(Long procId, Long procInstId, TransitionInstance transInst) {
        return "p" + procId + "." + procInstId + " t" + transInst.getTransitionID() + "." + transInst.getTransitionInstanceID();
    }
}
