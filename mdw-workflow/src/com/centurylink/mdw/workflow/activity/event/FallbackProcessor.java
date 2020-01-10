package com.centurylink.mdw.workflow.activity.event;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.constant.WorkAttributeConstant;
import com.centurylink.mdw.dataaccess.DbAccess;
import com.centurylink.mdw.model.attribute.Attribute;
import com.centurylink.mdw.model.event.EventInstance;
import com.centurylink.mdw.model.event.EventType;
import com.centurylink.mdw.model.event.InternalEvent;
import com.centurylink.mdw.model.monitor.ScheduledEvent;
import com.centurylink.mdw.model.workflow.Activity;
import com.centurylink.mdw.model.workflow.ActivityInstance;
import com.centurylink.mdw.model.workflow.ProcessRuntimeContext;
import com.centurylink.mdw.service.data.process.EngineDataAccess;
import com.centurylink.mdw.service.data.process.EngineDataAccessCache;
import com.centurylink.mdw.service.data.process.EngineDataAccessDB;
import com.centurylink.mdw.services.ProcessException;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.WorkflowServices;
import com.centurylink.mdw.services.messenger.InternalMessenger;
import com.centurylink.mdw.services.messenger.MessengerFactory;
import com.centurylink.mdw.services.process.ActivityLogger;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FallbackProcessor {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private Activity activity;
    private ActivityInstance activityInstance;
    private String eventNamesAttribute = WorkAttributeConstant.WAIT_EVENT_NAMES;
    private Map<String,String> events;

    public FallbackProcessor(Activity activity, ActivityInstance activityInstance) {
        this.activity = activity;
        this.activityInstance = activityInstance;
    }

    public FallbackProcessor(Activity activity, ActivityInstance activityInstance, String eventNamesAttribute) {
        this(activity, activityInstance);
        this.eventNamesAttribute = eventNamesAttribute;
    }

    private static final String EVENT_WAIT_SQL = "select event_wait_instance_id from EVENT_WAIT_INSTANCE\n"
            + "where event_name = ? and event_wait_instance_owner = ? and event_wait_instance_owner_id = ?";

    /**
     * Inserts EVENT_WAIT_INSTANCE rows if missing for the activity instance.
     */
    public void insertNeededEventWaits() throws ServiceException {
        Map<String, String> events = getEvents();
        for (String eventName : events.keySet()) {
            // find out if there's an EVENT_WAIT_INSTANCE
            try (DbAccess dbAccess = new DbAccess()) {
                ResultSet rs = dbAccess.runSelect(EVENT_WAIT_SQL, eventName, OwnerType.ACTIVITY_INSTANCE, activityInstance.getId());
                if (!rs.next()) {
                    EngineDataAccessDB edao = new EngineDataAccessDB();
                    try {
                        edao.getDatabaseAccess().openConnection();
                        edao.createEventWaitInstance(activityInstance.getId(), eventName, events.get(eventName));
                        String msg = "Inserted missing EVENT_WAIT_INSTANCE for activity " + activityInstance.getId() + ": " + eventName;
                        logger.info(msg);
                        ActivityLogger.persist(activityInstance.getProcessInstanceId(), activityInstance.getId(), StandardLogger.LogLevel.INFO, msg, null);
                    } finally {
                        edao.getDatabaseAccess().closeConnection();
                    }
                }
            }
            catch (SQLException ex) {
                throw new ServiceException(ex.getMessage(), ex);
            }
        }
    }

    /**
     * Replublish event notices for the activity instance.  This triggers them to evaluate whether to
     * continue waiting.
     */
    public void republishEvents() throws ServiceException {
        Map<String,String> events = getEvents();
        WorkflowServices workflowServices = ServiceLocator.getWorkflowServices();
        for (String eventName : events.keySet()) {
            int res = workflowServices.notify(eventName, null, 2);
            String msg = "Notified activity instance " + activityInstance.getId() + ": " + eventName + " with result = " + res;
            logger.info(msg);
            StandardLogger.LogLevel logLevel = (res == EventInstance.RESUME_STATUS_NO_WAITERS ? StandardLogger.LogLevel.DEBUG : StandardLogger.LogLevel.INFO);
            ActivityLogger.persist(activityInstance.getProcessInstanceId(), activityInstance.getId(), logLevel, msg, null);
        }
    }

    /**
     * returns a map of event name to compCode
     */
    public Map<String,String> getEvents() throws ServiceException {
        if (events == null) {
            events = new HashMap<>();
            List<String[]> eventSpecs = Attribute.parseTable(activity.getAttribute(eventNamesAttribute), ',', ';', 3);
            ProcessRuntimeContext runtimeContext = getWorkflowServices().getContext(activityInstance.getProcessInstanceId());
            for (String[] eventSpec : eventSpecs) {
                String eventName = runtimeContext.evaluateToString(eventSpec[0]);
                String compCode = eventSpec[1];
                if (compCode == null || compCode.isEmpty())
                    compCode = EventType.EVENTNAME_FINISH;
                events.put(eventName, compCode);
            }
        }
        return events;
    }

    public void sendTimerEvent() throws ServiceException {
        InternalEvent message = InternalEvent.createActivityNotifyMessage(activityInstance,
                EventType.RESUME, activityInstance.getMasterRequestId(), null);
        // edao with no persist
        EngineDataAccess edao = EngineDataAccessCache.getInstance(true, 9);
        String eventName = ScheduledEvent.INTERNAL_EVENT_PREFIX + activityInstance.getId() + "timer";
        try {
            getInternalMessenger().sendDelayedMessage(message, 0, eventName, false, edao);
        }
        catch (ProcessException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    private InternalMessenger internalMessenger;
    private InternalMessenger getInternalMessenger() {
        if (internalMessenger == null) {
            internalMessenger = MessengerFactory.newInternalMessenger();
            internalMessenger.setCacheOption(InternalMessenger.CACHE_ONLY);
        }
        return internalMessenger;
    }

    private WorkflowServices workflowServices;
    private WorkflowServices getWorkflowServices() {
        if (workflowServices == null) {
            workflowServices = ServiceLocator.getWorkflowServices();
        }
        return workflowServices;
    }
}
