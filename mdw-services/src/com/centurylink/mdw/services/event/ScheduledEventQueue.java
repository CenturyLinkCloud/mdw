package com.centurylink.mdw.services.event;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.cache.CacheService;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.JMSDestinationNames;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.container.ThreadPoolProvider;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.event.EventInstance;
import com.centurylink.mdw.model.monitor.ScheduledEvent;
import com.centurylink.mdw.model.monitor.ScheduledJob;
import com.centurylink.mdw.service.data.process.EngineDataAccessDB;
import com.centurylink.mdw.services.EventServices;
import com.centurylink.mdw.services.MessageServices;
import com.centurylink.mdw.services.ProcessException;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.cache.CacheRegistration;
import com.centurylink.mdw.services.messenger.InternalMessenger;
import com.centurylink.mdw.services.messenger.MessengerFactory;
import com.centurylink.mdw.util.CallURL;
import com.centurylink.mdw.util.DateHelper;
import com.centurylink.mdw.util.JMSServices;
import com.centurylink.mdw.util.TransactionWrapper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import org.json.JSONObject;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLRecoverableException;
import java.util.*;

public class ScheduledEventQueue implements CacheService {

    private static ScheduledEventQueue singleton = null;

    private static long inMemoryRange = 24*3600*1000L;    // one day

    private PriorityQueue<ScheduledEvent> eventQueue;
    private StandardLogger logger;
    private Date cutoffTime;

    private ScheduledEventQueue() {
        inMemoryRange = PropertyManager.getIntegerProperty(PropertyNames.SCHEDULED_EVENTS_MEMORY_RANGE, 1440) *60*1000L;
        eventQueue = null;
        logger = LoggerUtil.getStandardLogger();
        cutoffTime = new Date(DatabaseAccess.getCurrentTime() + inMemoryRange);
        loadScheduledEvents();
    }

    private synchronized void loadScheduledEvents() {
        eventQueue = new PriorityQueue<>();
        try {
            List<ScheduledEvent> eventlist = getScheduledEventList(cutoffTime);
            for (ScheduledEvent event: eventlist) {
                logger.info("Previously scheduled event " + event.getName() + " at "
                        + DateHelper.dateToString(event.getScheduledTime()) + " (database time)");
                eventQueue.offer(event);
            }
        }
        catch (Exception ex) {
            eventQueue = null;
            logger.error(ex.getMessage(), ex);
        }
    }

    /**
     * Load all internal event and scheduled jobs before cutoff time.
     * If cutoff time is null, load only unscheduled events
     */
    private List<ScheduledEvent> getScheduledEventList(Date cutoffTime) throws DataAccessException {
        TransactionWrapper transaction = null;
        EngineDataAccessDB edao = new EngineDataAccessDB();
        try {
            transaction = edao.startTransaction();
            return edao.getScheduledEventList(cutoffTime);
        } catch (SQLException e) {
            throw new DataAccessException(-1, "Failed to get scheduled event list", e);
        } finally {
            edao.stopTransaction(transaction);
        }
    }

    /**
     * Returns the next event that needs to be processed
     * @param now current time
     * @return SecheduledEvent next event
     */
    public synchronized ScheduledEvent getNextReadyEvent(Date now) {
        if (now.after(cutoffTime)) {
            cutoffTime = new Date(now.getTime() + inMemoryRange);
            loadScheduledEvents();
        } else if (eventQueue == null) {
            loadScheduledEvents();
        }
        if (eventQueue != null) {
            ScheduledEvent event = eventQueue.peek();
            if (event != null && event.getScheduledTime().compareTo(now) < 0)
                return eventQueue.poll();
        }
        return null;
    }

    /**
     * Processes the event with the current time.
     *
     * @param event the scheduled event.
     * @param now the current time
     *
     */
    public void processEvent(ScheduledEvent event, Date now) {
        try {
            processScheduledEvent(event.getName(), now);
        } catch (Exception e) {
            if (e.getCause() instanceof SQLRecoverableException) {
                eventQueue = null;
            }
            logger.error("Failed to process scheduled event " + event.getName(), e);
        }
    }

    public void processScheduledEvent(String eventName, Date now) throws DataAccessException {
        TransactionWrapper transaction = null;
        EngineDataAccessDB edao = new EngineDataAccessDB();
        try {
            transaction = edao.startTransaction();
            ScheduledEvent event = edao.lockScheduledEvent(eventName);
            Date currentScheduledTime = event == null ? null : event.getScheduledTime();
            ScheduledEventQueue queue = ScheduledEventQueue.getSingleton();
            boolean processed = queue.processEvent(eventName, event, now);
            if (event != null && processed)  {
                if (event.isScheduledJob()) {
                    edao.recordScheduledJobHistory(event.getName(), currentScheduledTime,
                            ApplicationContext.getServer().toString());
                }
                if (event.getScheduledTime() == null) {
                    edao.deleteEventInstance(event.getName());
                }
                else {
                    edao.updateEventInstance(event.getName(), null, null,
                            event.getScheduledTime(), null, null, 0, null);
                }
            }     // else do nothing - may be processed by another server
        } catch (Exception e) {
            throw new DataAccessException(-1, "Failed to process scheduled event", e);
        } finally {
            edao.stopTransaction(transaction);
        }
    }

    /**
     * Processes the event and reschedules with the next data.
     * @param eventName the scheduled event name.
     * @param event the scheduled event.
     * @param now the current time
     */
    public boolean processEvent(String eventName, ScheduledEvent event, Date now) {
        // when this is called, the database has locked the event, or it is null
        // lock and remove in database, and refresh the copy
        if (event == null) {
            logger.debug("Event has already been processed: " + eventName);
            return false;
        }
        if (event.getScheduledTime().compareTo(now) > 0) {
            logger.debug("Event has already been rescheduled: " + eventName + " at " + event.getScheduledTime().toString());
            eventQueue.offer(event);
            return false;
        }
        try {
            logger.info("EventScheduler processes event " + eventName);
            if (event.isInternalEvent()) {
                // long-delayed internal events, for timer/event wait activities
                if (!MessageServices.getInstance().sendInternalMessageCheck(ThreadPoolProvider.WORKER_SCHEDULER,
                        null, event.getName(), event.getMessage())) {
                    return false;  // Don't remove event from DB since it couldn't be processed (no thread available)
                }
            } else if (event.isScheduledJob()) {
                if (EventInstance.STATUS_SCHEDULED_JOB_RUNNING.equals(event.getStatus())) {
                    logger.error("Scheduled job still in progress, so execution skipped at " + DateHelper.dateToString(now) + ": " + event.getName());
                }
                else {
                    // send message to listener to run the job
                    String jobClassAndArgs = event.getName().substring(ScheduledEvent.SCHEDULED_JOB_PREFIX.length());
                    String calldoc = "<_mdw_run_job>" +
                            jobClassAndArgs.replaceAll("&", "&amp;").replaceAll("<", "&lt;") +
                            "</_mdw_run_job>";
                    try {
                        JMSServices.getInstance().sendTextMessage(null,
                                JMSDestinationNames.EXTERNAL_EVENT_HANDLER_QUEUE, calldoc, 0, null);
                    } catch (Exception ex) {
                        throw new ProcessException(-1, ex.getMessage(), ex);
                    }
                }
            } else {
                // is scheduled external event
                if (event.getMessage() == null || !event.getMessage().startsWith("<"))
                    throw new Exception("Scheduled external event message is null or non-XML. Event name " + event.getName());
                try {
                    JMSServices.getInstance().sendTextMessage(null,
                            JMSDestinationNames.EXTERNAL_EVENT_HANDLER_QUEUE, event.getMessage(), 0, null);
                } catch (Exception ex) {
                    throw new ProcessException(-1, ex.getMessage(), ex);
                }
            }
        } catch (Exception ex) {
            logger.error("Failed to process scheduled event " + event.getName(), ex);
        }
        Date nextDate;
        if (event.isScheduledJob()) {
            nextDate = getNextDate(event, now, logger);
        } else {
            nextDate = null;
        }
        event.setScheduledTime(nextDate);
        if (nextDate != null) {
            eventQueue.offer(event);
            logger.info("Reschedules event " + event.getName() + " at "
                    + DateHelper.dateToString(event.getScheduledTime()) + " (database time)");
        } // else event manager will delete the database entry
        return true;
    }

    /**
     * Schedule a timer task, or a delayed event
     * @param name event name
     * @param time this must be database time (such as new Date(DatabaseAccess.getCurrentTime()+difference_in_milliseconds)
     * @param message message content for delayed event; null o/w
     */
    public void scheduleInternalEvent(String name, Date time, String message, String reference) {
        schedule(name, time, message, reference);
    }


    /**
     * Reschedule a timer task, or a delayed event
     * @param name event name
     * @param time this must be database time (such as new Date(DatabaseAccess.getCurrentTime()+difference_in_milliseconds)
     * @param message message content for delayed event; null o/w
     */
    public void rescheduleInternalEvent(String name, Date time, String message) throws Exception {
        reschedule(name, time, message);
        this.broadcastInvalidate(name);
    }

    /**
     * Unschedule a timer task, or a delayed event
     * @param name event name
     */
    public void unscheduleEvent(String name) throws Exception {
        unschedule(name);
        broadcastInvalidate(name);
    }

    /**
     * Schedule external event
     * @param name event name
     * @param time time
     * @param message event message
     * @param reference reference
     */
    public void scheduleExternalEvent(String name, Date time, String message, String reference) {
        schedule(name, time, message, reference);
    }

    /**
     * Reschedule external event
     * @param name event name
     * @param time time
     * @param message event message
     */
    public void rescheduleExternalEvent(String name, Date time, String message) {
        reschedule(name, time, message);
        this.broadcastInvalidate(name);
    }

    private synchronized void schedule(String name, Date time, String message,
            String reference) {
        ScheduledEvent event = new ScheduledEvent();
        event.setName(name);
        event.setScheduledTime(time);
        event.setMessage(message);
        event.setReference(reference);
        try {
            offerScheduledEvent(event);
            if (time != null) {
                eventQueue.offer(event);
                logger.info("Schedules event " + event.getName() + " at "
                        + DateHelper.dateToString(event.getScheduledTime()) + " (database time)");
            } else {
                logger.info("Add unscheduled event " + event.getName());
            }
        } catch (DataAccessException e) {
            if (e.getCode() == DataAccessException.INTEGRITY_CONSTRAINT_VIOLATION)
                logger.info("To schedule the event but it is already scheduled: " + event.getName());
            else
                logger.error("Failed to schedule event " + name, e);
        } catch (Exception e) {
            logger.error("Failed to schedule event " + name, e);
        }
    }

    /**
     * Run a ScheduledJob so that there is no overlap between running instances.
     * For example, if scheduled to run every hour and the 2:00 o'clock execution
     * is still running at 3:00, the 3:00 o'clock execution will be skipped and the
     * next event is rescheduled to occur at 4:00.
     */
    public void runScheduledJobExclusively(ScheduledJob job, CallURL params) throws DataAccessException {
        String eventName = ScheduledEvent.SCHEDULED_JOB_PREFIX + params;
        TransactionWrapper transaction = null;
        EngineDataAccessDB edao = new EngineDataAccessDB();
        try {
            transaction = edao.startTransaction();
            ScheduledEvent event = edao.lockScheduledEvent(eventName);
            if (event == null) {
                logger.error("ScheduledJob not found: " + eventName);
            } else
                edao.updateEventInstance(event.getName(), null, EventInstance.STATUS_SCHEDULED_JOB_RUNNING,
                        event.getScheduledTime(), null, null, 0, null);
        } catch (SQLException e) {
            throw new DataAccessException(-1, "Failed to update scheduled job", e);
        } finally {
            edao.stopTransaction(transaction);
        }
        job.run(params, (Integer status) -> {
            try {
                completeScheduledJob(eventName);
            }
            catch (DataAccessException ex) {
                logger.error(ex.getMessage(), ex);
            }
        });
    }

    /**
     * Update scheduled job to non-running status.
     */
    public void completeScheduledJob(String eventName) throws DataAccessException {
        TransactionWrapper transaction = null;
        EngineDataAccessDB edao = new EngineDataAccessDB();
        try {
            transaction = edao.startTransaction();
            ScheduledEvent event = edao.lockScheduledEvent(eventName);
            if (event != null) {
                edao.updateEventInstance(event.getName(), null, EventInstance.STATUS_SCHEDULED_JOB,
                        event.getScheduledTime(), null, null, 0, null);
            }
        } catch (SQLException e) {
            throw new DataAccessException(-1, "Failed to complete scheduled job", e);
        } finally {
            edao.stopTransaction(transaction);
        }
    }

    private void removeEvent(String name) {
        // cannot use eventQueue.remove(event) without setting scheduledTime, as PriorityQueue
        // uses compareTo() rather than equals() to perform remove,
        // guess for optimization
        for (ScheduledEvent e : eventQueue) {
            if (e.getName().equals(name)) {
                eventQueue.remove(e);
                return;
            }
        }
    }

    private synchronized void reschedule(String name, Date time, String message) {
        ScheduledEvent event = new ScheduledEvent();
        event.setName(name);
        event.setScheduledTime(time);
        event.setMessage(message);
        try {
            EventServices eventManager = ServiceLocator.getEventServices();
            eventManager.updateEventInstance(name, null, null,
                    time, message, null, 0);
            removeEvent(name);
            eventQueue.offer(event);
            logger.info("Reschedules event " + event.getName() + " at "
                    + DateHelper.dateToString(event.getScheduledTime()) + " (database time)");
        } catch (Exception e) {
            logger.error("Failed to reschedule event " + name, e);
        }
    }

    private synchronized void unschedule(String name) throws Exception {
        deleteTableRow("EVENT_INSTANCE", "EVENT_NAME", name);
        removeEvent(name);
        logger.info("Unchedules event " + name);
    }

    /**
     * Delete row of the table given
     * @param tableName name of the table
     * @param fieldName name of the column
     * @param fieldValue value of the column
     * @return int deleted row id.
     * @throws DataAccessException data access exception
     */

    public int deleteTableRow(String tableName, String fieldName, Object fieldValue) throws DataAccessException {
        TransactionWrapper transaction = null;
        EngineDataAccessDB edao = new EngineDataAccessDB();
        try {
            transaction = edao.startTransaction();
            return edao.deleteTableRow(tableName, fieldName, fieldValue);
        } catch (SQLException e) {
            throw new DataAccessException(-1, "Failed to delete " + fieldValue.toString() + " from " + tableName, e);
        } finally {
            edao.stopTransaction(transaction);
        }
    }

    /**
     * Schedule cron job
     * @param name cron job name
     * @param cronExpression cron expression
     */
    public void scheduleCronJob(String name, String cronExpression) throws Exception {
        Date now = new Date(DatabaseAccess.getCurrentTime());
        Date nextTime = calculateNextDate(cronExpression, now);
        if (nextTime != null)
            schedule(name, nextTime, cronExpression, null);
    }

    /**
     * Reschedule cron job
     * @param name cron job name
     * @param cronExpression cron expression
     */
    public void rescheduleCronJob(String name, String cronExpression) throws Exception {
        Date now = new Date(DatabaseAccess.getCurrentTime());
        Date nextTime = calculateNextDate(cronExpression, now);
        if (nextTime != null) {
            reschedule(name, nextTime, cronExpression);
        } else {
            unschedule(name);
        }
        this.broadcastInvalidate(name);
    }

    /**
     * Refresh schedule event cache
     */
    public void refreshCache() {
        cutoffTime = new Date(DatabaseAccess.getCurrentTime() + inMemoryRange);
        loadScheduledEvents();
    }

    /**
     * Clears schedule event cache
     */
    public void clearCache() {
        eventQueue.clear();
    }

    /**
     * Removes the schedule event
     * @param eventName event name.
     */
    public void invalidate(String eventName) {
        this.removeEvent(eventName);
    }

    private void broadcastInvalidate(String eventName) {
        try {
            JSONObject json = new JsonObject();
            json.put("ACTION", "INVALIDATE_EVENT");
            json.put("EVENT_NAME", eventName);
            json.put("FROM", ApplicationContext.getServer().toString());
            InternalMessenger messenger = MessengerFactory.newInternalMessenger();
            messenger.broadcastMessage(json.toString());
        } catch (Exception e) {
            logger.error("Failed to publish event invalidation message", e);
        }
    }

    public static ScheduledEventQueue getSingleton() {
        if (singleton==null) {
            singleton = new ScheduledEventQueue();
            (new CacheRegistration()).registerCache(ScheduledEventQueue.class.getName(), singleton);
        }
        return singleton;
    }

    private Date getNextDate(ScheduledEvent event, Date now, StandardLogger logger) {
        Date nextDate;
        String schedule = event.getMessage();
        if (schedule == null)
            nextDate = null;
        else {
            try {
                nextDate = calculateNextDate(schedule, event.getScheduledTime());
                while (nextDate != null && nextDate.before(now)) {
                    logger.debug("Skip scheduled event " + event.getName() + " at " +
                            DateHelper.dateToString(nextDate));
                    nextDate = calculateNextDate(schedule, nextDate);
                }
            } catch (Exception e) {
                logger.error("Failed to calculated next run time", e);
                nextDate = null;
            }
        }
        return nextDate;
    }

    private void getNextAllowableYear(Calendar cal, CronField[] cf) {
        if (cf.length==6) {
            int Y0 = cal.get(Calendar.YEAR);
            int Y1 = cf[5].getNextAllowed(Y0);
            if (Y1==Y0) return;
            if (Y1<0) Y1 = 2101;
            cal.set(Calendar.YEAR, Y1);
            cal.set(Calendar.MONTH, 0);
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
        }
    }

    private void getNextAllowableMonth(Calendar cal, CronField[] cf) {
        int M0 = cal.get(Calendar.MONTH)+1;
        int M1 = cf[3].getNextAllowed(M0);
        if (M0==M1) return;
        if (M1<0) {
            cal.set(Calendar.YEAR, cal.get(Calendar.YEAR)+1);
            getNextAllowableYear(cal, cf);
            M1 = cf[3].getNextAllowed(1);
        }
        cal.set(Calendar.MONTH, M1-1);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
    }

    private void getNextAllowableDay(Calendar cal, CronField[] cf) {
        int D0 = cal.get(Calendar.DAY_OF_MONTH);
        if (cf[4].ignore||cf[4].all) {
            int D1 = cf[2].getNextAllowed(D0);
            if (D0==D1) return;
            if (D1<0) {
                cal.set(Calendar.MONTH, cal.get(Calendar.MONTH)+1);
                getNextAllowableMonth(cal, cf);
                D1 = cf[2].getNextAllowed(1);
            }
            int MA = cal.get(Calendar.MONTH);
            cal.set(Calendar.DAY_OF_MONTH, D1);
            int MB = cal.get(Calendar.MONTH);
            if (MB!=MA) {    // day overflow, such as Feb 30
                cal.set(Calendar.MONTH, MA+1);
                getNextAllowableMonth(cal, cf);
                D1 = cf[2].getNextAllowed(1);
                cal.set(Calendar.DAY_OF_MONTH, D1);
            }
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
        } else {
            int d0 = cal.get(Calendar.DAY_OF_WEEK)-1;
            int d1 = cf[4].getNextAllowed(d0);
            if (d0==d1) return;
            if (d1<0) d1 = cf[4].getNextAllowed(0) + 7;
            int D1 = D0 + (d1-d0);
            int MA = cal.get(Calendar.MONTH);
            cal.set(Calendar.DAY_OF_MONTH, D1);
            int MB = cal.get(Calendar.MONTH);
            if (MB!=MA) {    // day overflow to next month
                cal.set(Calendar.MONTH, MA+1);
                getNextAllowableMonth(cal, cf);
                cal.set(Calendar.DAY_OF_MONTH, 1);
                d0 = cal.get(Calendar.DAY_OF_WEEK)-1;
                d1 = cf[4].getNextAllowed(d0);
                if (d1<0) d1 = cf[4].getNextAllowed(0) + 7;
                D1 = 1 + (d1-d0);
                cal.set(Calendar.DAY_OF_MONTH, D1);
            }
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
        }
    }

    private void getNextAllowableHour(Calendar cal, CronField[] cf) {
        int h0 = cal.get(Calendar.HOUR_OF_DAY);
        int h1 = cf[1].getNextAllowed(h0);
        if (h0==h1) return;
        if (h1<0) {
            cal.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DAY_OF_MONTH)+1);
            getNextAllowableDay(cal, cf);
            h1 = cf[1].getNextAllowed(0);
        }
        cal.set(Calendar.HOUR_OF_DAY, h1);
    }

    private void getNextAllowableMinute(Calendar cal, CronField[] cf) {
        int m0 = cal.get(Calendar.MINUTE);
        int m1 = cf[0].getNextAllowed(m0);
        if (m0==m1) return;
        if (m1<0) {
            cal.set(Calendar.HOUR, cal.get(Calendar.HOUR)+1);
            getNextAllowableHour(cal, cf);
            m1 = cf[0].getNextAllowed(0);
        }
        cal.set(Calendar.MINUTE, m1);
    }

    private Date calculateNextDate(String cronExpression, Date from) throws Exception {
        String[] vs = cronExpression.split("\\s+");
        if (vs.length<5 || vs.length>6) throw new Exception("Invalid cron expression " + cronExpression);
        CronField[] cf = new CronField[vs.length];
        for (int j=0; j<vs.length; j++) {
            cf[j] = new CronField(vs[j]);
        }
        if (!(cf[2].ignore || cf[2].all) &&
            !(cf[4].ignore || cf[4].all))
            throw new Exception("Invalid cron expression " + cronExpression);
        Calendar cal = Calendar.getInstance();
        cal.setTime(from);
        this.getNextAllowableYear(cal, cf);
        this.getNextAllowableMonth(cal, cf);
        this.getNextAllowableDay(cal, cf);
        this.getNextAllowableHour(cal, cf);
        if (cal.getTimeInMillis()>from.getTime()) {
            cal.set(Calendar.MINUTE, 0);
        } else {
            cal.set(Calendar.MINUTE, cal.get(Calendar.MINUTE)+1);
        }
        this.getNextAllowableMinute(cal, cf);
        cal.set(Calendar.SECOND, 0);
        if (cal.get(Calendar.YEAR)>2100) return null;
        return cal.getTime();
    }

    public void offerScheduledEvent(ScheduledEvent event)
            throws DataAccessException {
        TransactionWrapper transaction = null;
        EngineDataAccessDB edao = new EngineDataAccessDB();
        try {
            transaction = edao.startTransaction();
            edao.offerScheduledEvent(event);
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new DataAccessException(DataAccessException.INTEGRITY_CONSTRAINT_VIOLATION,
                    "The event is already scheduled", e);
        } catch (SQLException e) {
            if (e.getSQLState().equals("23000")) {
                throw new DataAccessException(DataAccessException.INTEGRITY_CONSTRAINT_VIOLATION,
                        "The event is already scheduled", e);
                // for unknown reason (may be because of different Oracle driver - ojdbc14),
                // when running under Tomcat, contraint violation does not throw SQLIntegrityConstraintViolationException
                // 23000 is ANSI/SQL standard SQL State for constraint violation
                // Alternatively, we can use e.getErrorCode()==1 for Oracle (ORA-00001)
                // or e.getErrorCode()==1062 for MySQL
            } else {
                throw new DataAccessException(-1, "Failed to create scheduled event", e);
            }
        } finally {
            edao.stopTransaction(transaction);
        }
    }

    private class CronField {
        int increment;
        boolean ignore;    // ?
        boolean all;    // *
        ArrayList<Integer> values;
        CronField(String spec) throws Exception {
            ignore = spec.equals("?");
            all = spec.equals("*");
            if (!ignore && !all) {
                int slash = spec.indexOf('/');
                if (slash>0) {
                    increment = Integer.parseInt(spec.substring(slash+1));
                    spec = spec.substring(0,slash);
                } else increment = 1;
                values = new ArrayList<>();
                String[] vs = spec.split(",");
                for (String v : vs) {
                    int dash = v.indexOf("-");
                    if (dash>0) {
                        String vf = v.substring(0,dash);
                        String vt = v.substring(dash+1);
                        int f = Integer.parseInt(vf);
                        int t = Integer.parseInt(vt);
                        for (int k=f; k<=t; k+=increment) values.add(k);
                    } else {
                        values.add(Integer.parseInt(v));
                    }
                }
                Collections.sort(values);
            }
        }
        int getNextAllowed(int from) {
            if (ignore || all) return from;
            for (Integer one : values) {
                if (one >=from) return one;
            }
            return -1;
        }
    }

}
