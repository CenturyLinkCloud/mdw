/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.testing;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

import javax.swing.JTable;

import com.centurylink.mdw.designer.DesignerDataAccess;
import com.centurylink.mdw.model.data.work.WorkStatus;

public class LoadTestLogMonitor extends LogMessageMonitor {

	private JTable table;	// for repaint only
	private Map<String,TestCaseRun> masterRequestRunMap;
	private Map<String,String> masterRequestMainProcessMap;
	private SimpleDateFormat df;
	private int numberOfActivityStarted;
	private int numberOfActivityCompleted;
	private int numberOfProcesses;

	public LoadTestLogMonitor(DesignerDataAccess dao,
			Map<String,TestCaseRun> masterRequestRunMap, JTable table)
		throws IOException {
		super(dao);
		this.table = table;
		this.masterRequestRunMap = masterRequestRunMap;
		masterRequestMainProcessMap = new HashMap<String,String>();
		numberOfActivityStarted = 0;
		numberOfActivityCompleted = 0;
		numberOfProcesses = 0;
		df = new SimpleDateFormat("yyyyMMdd.HH:mm:ss.SSS");
	}

	@Override
	protected void handleActivityMatch(Matcher activityMatcher) {
		super.handleActivityMatch(activityMatcher);
		String msg = activityMatcher.group(6);
		if (msg.startsWith(WorkStatus.LOGMSG_COMPLETE))
			numberOfActivityCompleted++;
		else if (msg.startsWith(WorkStatus.LOGMSG_START))
			numberOfActivityStarted++;
		if (table != null)
		  table.repaint();
	}

	@Override
	protected void handleProcessMatch(Matcher procMatcher) {
		super.handleProcessMatch(procMatcher);
		String time = procMatcher.group(1);
		String processInstId = procMatcher.group(3);
		String masterRequestId = procMatcher.group(4);
		String msg = procMatcher.group(5);
		TestCaseRun run = masterRequestRunMap.get(masterRequestId);
		TestCase case1 = run.getTestCase();
		String mainProcInstId = masterRequestMainProcessMap.get(masterRequestId);
		if (msg.startsWith(WorkStatus.LOGMSG_PROC_START)) {
			numberOfProcesses++;
			if (mainProcInstId==null) {
				masterRequestMainProcessMap.put(masterRequestId, processInstId);
				case1.setNumberStarted(case1.getNumberStarted()+1);
				case1.setStatus(TestCase.STATUS_RUNNING);
				try {
					case1.setStartDate(df.parse(time));
				} catch (ParseException e) {
				}
			}
		} else if (msg.startsWith(WorkStatus.LOGMSG_PROC_COMPLETE)) {
			if (processInstId.equals(mainProcInstId)) {
				masterRequestMainProcessMap.put(masterRequestId, processInstId);
				case1.setNumberCompleted(case1.getNumberCompleted()+1);
				if (case1.getNumberCompleted()>=case1.getNumberPrepared()) {
					case1.setStatus(TestCase.STATUS_PASS);
					try {
						case1.setEndDate(df.parse(time));
					} catch (ParseException e) {
					}
				}
			}
		}
		if (table != null)
          table.repaint();
	}

	public int getNumberOfActivityStarted() {
		return numberOfActivityStarted;
	}

	public int getNumberOfActivityCompleted() {
		return numberOfActivityCompleted;
	}

	public int getNumberOfProcesses() {
		return numberOfProcesses;
	}

}