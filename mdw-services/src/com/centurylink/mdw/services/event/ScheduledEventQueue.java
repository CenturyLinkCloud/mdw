/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.event;

import java.sql.SQLRecoverableException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

import org.json.JSONObject;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.cache.CacheEnabled;
import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.provider.CacheService;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.container.ThreadPoolProvider;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.data.monitor.ScheduledEvent;
import com.centurylink.mdw.model.value.event.InternalEventVO;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.cache.CacheRegistration;
import com.centurylink.mdw.services.dao.process.EngineDataAccessDB;
import com.centurylink.mdw.services.messenger.InternalMessenger;
import com.centurylink.mdw.services.messenger.IntraMDWMessenger;
import com.centurylink.mdw.services.messenger.MessengerFactory;
import com.centurylink.mdw.services.process.EventServices;

public class ScheduledEventQueue implements CacheEnabled, CacheService {

	private static ScheduledEventQueue singleton = null;

	private static final boolean processInternalEventInThisJVM = true;
	private static long inMemoryRange = 24*3600*1000L;	// one day

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
        eventQueue = new PriorityQueue<ScheduledEvent>();
        try {
            EventManager eventManager = ServiceLocator.getEventManager();
            List<ScheduledEvent> eventlist = eventManager.getScheduledEventList(cutoffTime);
            for (ScheduledEvent event: eventlist) {
                logger.info("Previously scheduled event " + event.getName() + " at "
                        + StringHelper.dateToString(event.getScheduledTime()) + " (database time)");
                eventQueue.offer(event);
            }
        }
        catch (Exception ex) {
            eventQueue = null;
            logger.severeException(ex.getMessage(), ex);
        }
	}

	public synchronized ScheduledEvent getNextReadyEvent(Date now) {
		if (now.after(cutoffTime)) {
	        cutoffTime = new Date(now.getTime() + inMemoryRange);
	        loadScheduledEvents();
		} else if (eventQueue == null) {
		    loadScheduledEvents();
		}
		if (eventQueue!= null) {
		    ScheduledEvent event = eventQueue.peek();
	        if (event!=null && event.getScheduledTime().compareTo(now)<0)
	            return eventQueue.poll();
		}
		return null;
	}

	public void processEvent(ScheduledEvent event, Date now) {
		try {
			EventManager eventManager = ServiceLocator.getEventManager();
			eventManager.processScheduledEvent(event.getName(), now);
		} catch (Exception e) {
		    if (e.getCause() instanceof SQLRecoverableException) {
		        eventQueue = null;
		    }
            logger.severeException("Failed to process scheduled event " + event.getName(), e);
		}
	}

	public boolean processEventInEjb(String eventName, ScheduledEvent event, Date now, EngineDataAccessDB edao) {
		// when this is called, the database has locked the event, or it is null
		// lock and remove in database, and refresh the copy
		if (event==null) {
			logger.debug("Event has already been processed: " + eventName);
			return false;
		}
		if (event.getScheduledTime().compareTo(now)>0) {
			logger.debug("Event has already been rescheduled: " + eventName
					+ " at " + event.getScheduledTime().toString());
			eventQueue.offer(event);
			return false;
		}
		try {
    		logger.info("EventScheduler processes event " + eventName);
    		if (event.isInternalEvent()) {
    			// long-delayed internal events, form timer/event wait activities
    			if (processInternalEventInThisJVM) {
    			    if (!EventServices.getInstance().sendInternalMessageCheck(ThreadPoolProvider.WORKER_SCHEDULER, null, event.getName(), event.getMessage()))
    			        return false;  // Don't remove event from DB since it couldn't be processed (no thread available)
    			} else {
            		InternalMessenger msgbroker = MessengerFactory.newInternalMessenger();
            		msgbroker.sendMessage(new InternalEventVO(event.getMessage()), edao);
    			}
    		} else if (event.isScheduledJob()) {
    			// timer task
    			// send a message to listener to actually run the task
    			String timerClassAndArgs = event.getName().substring(ScheduledEvent.SCHEDULED_JOB_PREFIX.length());
    			StringBuffer calldoc = new StringBuffer();
    			calldoc.append("<_mdw_run_job>");
    			calldoc.append(timerClassAndArgs.replaceAll("&", "&amp;").replaceAll("<", "&lt;"));
    			calldoc.append("</_mdw_run_job>");
        		IntraMDWMessenger msgbroker = MessengerFactory.newIntraMDWMessenger(null);
        		msgbroker.sendMessage(calldoc.toString());
            } else {		// is scheduled external event
            	if (event.getMessage()==null || !event.getMessage().startsWith("<"))
            		throw new Exception("Scheduled external event message is null or non-XML. Event name " + event.getName());
        		IntraMDWMessenger msgbroker = MessengerFactory.newIntraMDWMessenger(null);
        		msgbroker.sendMessage(event.getMessage());
            }
        } catch (Exception ex) {
            logger.severeException("Failed to process scheduled event " + event.getName(), ex);
        }
    	Date nextDate;
    	if (event.isScheduledJob()) {
			nextDate = getNextDate(event, now, logger);
    	} else nextDate = null;
    	event.setScheduledTime(nextDate);
        if (nextDate!=null) {
        	eventQueue.offer(event);
			logger.info("Reschedules event " + event.getName() + " at "
					+ StringHelper.dateToString(event.getScheduledTime()) + " (database time)");
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

	public void rescheduleInternalEvent(String name, Date time, String message) throws Exception {
    	reschedule(name, time, message);
    	this.broadcastInvalidate(name);
    }

	public void unscheduleEvent(String name) throws Exception {
    	unschedule(name);
    	broadcastInvalidate(name);
    }

	public void scheduleExternalEvent(String name, Date time, String message, String reference) {
		schedule(name, time, message, reference);
	}

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
			EventManager eventManager = ServiceLocator.getEventManager();
			eventManager.offerScheduledEvent(event);
			if (time!=null) {
				eventQueue.offer(event);
				logger.info("Schedules event " + event.getName() + " at "
						+ StringHelper.dateToString(event.getScheduledTime()) + " (database time)");
			} else {
				logger.info("Add unscheduled event " + event.getName());
			}
    	} catch (DataAccessException e) {
    		if (e.getErrorCode()==23000) logger.info("To schedule the event but it is already scheduled: " + event.getName());
    		else logger.severeException("Failed to schedule event " + name, e);
		} catch (Exception e) {
			logger.severeException("Failed to schedule event " + name, e);
		}
    }

    private void removeEvent(String name) {
    	// cannot use eventQueue.remove(event) without setting scheduledTime, as PriorityQueue
    	// uses compareTo() rather than equals() to perform remove,
    	// guess for optimization
    	Iterator<ScheduledEvent> iter = eventQueue.iterator();
    	while (iter.hasNext()) {
    		ScheduledEvent e = iter.next();
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
			EventManager eventManager = ServiceLocator.getEventManager();
			eventManager.updateEventInstance(name, null, null,
					time, message, null, 0);
			removeEvent(name);
			eventQueue.offer(event);
			logger.info("Reschedules event " + event.getName() + " at "
					+ StringHelper.dateToString(event.getScheduledTime()) + " (database time)");
		} catch (Exception e) {
			logger.severeException("Failed to reschedule event " + name, e);
		}
    }

    private synchronized void unschedule(String name) throws Exception {
		EventManager eventManager = ServiceLocator.getEventManager();
    	eventManager.deleteTableRow("EVENT_INSTANCE", "EVENT_NAME", name);
    	removeEvent(name);
    	logger.info("Unchedules event " + name);
    }

    public void scheduleCronJob(String name, String cronExpression) throws Exception {
    	Date now = new Date(DatabaseAccess.getCurrentTime());
    	Date nextTime = this.calculateNextDate(cronExpression, now);
    	if (nextTime!=null) schedule(name, nextTime, cronExpression, null);
    }

    public void rescheduleCronJob(String name, String cronExpression) throws Exception {
    	Date now = new Date(DatabaseAccess.getCurrentTime());
    	Date nextTime = this.calculateNextDate(cronExpression, now);
    	if (nextTime!=null) {
            reschedule(name, nextTime, cronExpression);
    	} else {
    		unschedule(name);
    	}
    	this.broadcastInvalidate(name);
    }

    public void refreshCache() {
        cutoffTime = new Date(DatabaseAccess.getCurrentTime() + inMemoryRange);
        loadScheduledEvents();
    }

    public void clearCache() {
    	eventQueue.clear();
    }

    public void invalidate(String eventName) {
    	this.removeEvent(eventName);
    }

    private void broadcastInvalidate(String eventName) {
		try {
			JSONObject json = new JSONObject();
			json.put("ACTION", "INVALIDATE_EVENT");
			json.put("EVENT_NAME", eventName);
			json.put("FROM", ApplicationContext.getServerHostPort());
			InternalMessenger messenger = MessengerFactory.newInternalMessenger();
			messenger.broadcastMessage(json.toString());
		} catch (Exception e) {
			logger.severeException("Failed to publish event invalidation message", e);
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
		if (schedule==null) nextDate = null;
		else {
			try {
				nextDate = this.calculateNextDate(schedule, event.getScheduledTime());
				while (nextDate!=null && nextDate.before(now)) {
					logger.debug("Skip scheduled event " + event.getName() + " at " +
							StringHelper.dateToString(nextDate));
					nextDate = this.calculateNextDate(schedule, nextDate);
				}
			} catch (Exception e) {
				logger.severeException("Failed to calculated next run time", e);
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
			if (MB!=MA) {	// day overflow, such as Feb 30
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
			if (MB!=MA) {	// day overflow to next month
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

    private class CronField {
    	int increment;
    	boolean ignore;	// ?
    	boolean all;	// *
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
	    		values = new ArrayList<Integer>();
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
    			if (one.intValue()>=from) return one.intValue();
    		}
    		return -1;
    	}
    }

    public static void main(String[] args) throws Exception {
    	ScheduledEventQueue me = new ScheduledEventQueue();
    	Date now = new Date();
    	Date next = me.calculateNextDate("0 12 * 1-12/2 5 *", now);
    	System.out.println("Now : " + now.toString());
    	System.out.println("Next: " + next.toString());
    }

}
