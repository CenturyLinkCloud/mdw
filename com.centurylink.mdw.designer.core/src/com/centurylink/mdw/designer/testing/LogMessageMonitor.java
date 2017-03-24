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
package com.centurylink.mdw.designer.testing;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.centurylink.mdw.designer.DesignerDataAccess;
import com.centurylink.mdw.designer.runtime.LogSubscriberSocket;
import com.centurylink.mdw.model.data.work.WorkStatus;

public class LogMessageMonitor extends LogSubscriberSocket {

	private Pattern activityPattern;
	private Pattern procPattern;
	private Map<String,Object> waitingObjects;
	private Map<String,String> procInstMasterRequestMap;

	public LogMessageMonitor(DesignerDataAccess dao) throws IOException {
	    this(dao, false);
	}

	public LogMessageMonitor(DesignerDataAccess dao, boolean oldNamespaces) throws IOException {
	    super(dao, oldNamespaces);
        activityPattern = Pattern.compile("\\[\\(.\\)([0-9.:]+) p([0-9]+)\\.([0-9]+) a([0-9]+)\\.([0-9]+)\\] (.*)", 0);
        procPattern = Pattern.compile("\\[\\(.\\)([0-9.:]+) p([0-9]+)\\.([0-9]+) m.([^\\]]+)\\] (.*)", 0);
        waitingObjects = new HashMap<String,Object>();
        procInstMasterRequestMap = new HashMap<String, String>();
    }

    public String createKey(String masterRequestId, Long processId, Long activityId, String status) {
		return masterRequestId + ":" + processId.toString() + ":" + activityId.toString() + ":" + status;
	}

	public void register(Object obj, String key) {
		synchronized (waitingObjects) {
			waitingObjects.put(key, obj);
		}
	}

	public Object remove(String key) {
		synchronized (waitingObjects) {
			return waitingObjects.remove(key);
		}
	}

	private String parseActivityStatus(String msg) {
		String status;
		if (msg.startsWith(WorkStatus.LOGMSG_COMPLETE)) {
			status = WorkStatus.STATUSNAME_COMPLETED;
		} else if (msg.startsWith(WorkStatus.LOGMSG_START)) {
			status = WorkStatus.STATUSNAME_IN_PROGRESS;
		} else if (msg.startsWith(WorkStatus.LOGMSG_FAILED)) {
			status = WorkStatus.STATUSNAME_FAILED;
		} else if (msg.startsWith(WorkStatus.LOGMSG_SUSPEND)) {
			status = WorkStatus.STATUSNAME_WAITING;
		} else if (msg.startsWith(WorkStatus.LOGMSG_HOLD)) {
			status = WorkStatus.STATUSNAME_HOLD;
		} else status = null;
		return status;
	}

	private void checkWaitCondition(String procInstId, String procId, String actId, String status) {
		String masterRequestId = procInstMasterRequestMap.get(procInstId);
		if (masterRequestId==null) return;
		String key = masterRequestId + ":" + procId + ":" + actId + ":" + status;
		Object obj = remove(key);
		if (obj!=null) {
			if (obj instanceof TestCommandRun) {
				((TestCommandRun)obj).reschedule(System.currentTimeMillis());
			} else {
				synchronized (obj) {
					obj.notify();
				}
			}
		}
	}

	protected void handleActivityMatch(Matcher activityMatcher) {
		String msg = activityMatcher.group(6);
		String status = parseActivityStatus(msg);
		if (status!=null) checkWaitCondition(activityMatcher.group(3),
				activityMatcher.group(2), activityMatcher.group(4), status);
	}

	protected void handleProcessMatch(Matcher procMatcher) {
		String processInstId = procMatcher.group(3);
		String msg = procMatcher.group(5);
		if (msg.startsWith(WorkStatus.LOGMSG_PROC_START)) {
			procInstMasterRequestMap.put(processInstId, procMatcher.group(4));
		} else if (msg.startsWith(WorkStatus.LOGMSG_PROC_COMPLETE)) {
			checkWaitCondition(processInstId, procMatcher.group(2), "0", WorkStatus.STATUSNAME_COMPLETED);
		}
	}

	@Override
    protected synchronized void handleMessage(String message) {
		Matcher procMatcher = procPattern.matcher(message);
		Matcher activityMatcher = activityPattern.matcher(message);
		if (activityMatcher.matches()) {
			handleActivityMatch(activityMatcher);
		} else if (procMatcher.matches()) {
			handleProcessMatch(procMatcher);
		}
    }

}
