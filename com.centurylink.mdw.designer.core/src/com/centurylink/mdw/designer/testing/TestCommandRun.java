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

import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * This class represents one scheduled execution of a test command.
 * Note a test command can be executed multiple times in case of load testing.
 * 
 *
 */
public class TestCommandRun implements Comparable<TestCommandRun> {
	
	private TestCaseRun run;
	private TestFileLine commandline;
	private int lineno;
	private long time;		// real time in milliseconds
	private PriorityBlockingQueue<TestCommandRun> queue;

	public TestCommandRun(TestCaseRun run, TestFileLine command, int lineno,
			PriorityBlockingQueue<TestCommandRun> queue) {
		this.run = run;
		this.commandline = command;
		this.lineno = lineno;
		this.queue = queue;
	}
	
	public void schedule(long time) {
		try {
			if (commandline.getCommand().equals(TestCase.SLEEP)) {
				int seconds = Integer.parseInt(commandline.getWord(1));
				time += seconds*1000;
			} else if (commandline.getCommand().equals(TestCase.WAIT)) {
				time += 1000*run.executeWaitRegister(this);
			}
			this.time = time;
			queue.offer(this);
		} catch (Exception e) {
			run.finishExecution(e);
		}
	}
	
	public int getRunNumber() {
		return run.getRunNumber();
	}
	
	public long getScheduledTime() {
		return time;
	}
	
	public TestFileLine getCommandLine() {
		return commandline;
	}
	
	public String getMasterRequestId() {
		return run.getMasterRequestId();
	}
	
	public TestCaseRun getTestCaseRun() {
		return run;
	}
	
	@Override
	public int compareTo(TestCommandRun o) {
		return (int)(this.time-o.time);
	}
	
	public TestCommandRun getNextCommand() {
		String status = run.getTestCase().getStatus();
    	if (status.equals(TestCase.STATUS_STOP)) return null;
    	int next_lineno = lineno + 1;
    	TestFileLine nextline = null;
    	List<TestFileLine> lines = run.getTestCase().getCommands().getLines();
    	while (next_lineno < lines.size()) {
    		nextline = lines.get(next_lineno);
    		if (status.equals(TestCase.STATUS_ERROR)) {
    			if (!nextline.getCommand().equalsIgnoreCase(TestCase.VERIFY_PROCESS)) {
    				next_lineno++;
    				nextline = null;
    			} else break;
    		} else break;
    	}
    	if (nextline==null) {
    		run.finishExecution(null);
    		return null;
    	} else {
    		TestCommandRun next_command = new TestCommandRun(run,
    				nextline, next_lineno, queue);
    		return next_command;
    	}
	}
	
	public void reschedule(long time) {
		boolean removed = queue.remove(this);
		if (removed) {
//			System.out.println("+++++++++++wait command is satisfied++++++++++++");
			this.time = time;
			queue.offer(this);
		} // else the command has already been executed;
	}
	
	public void execute_command() throws Exception {    
		run.executeCommand(getCommandLine());
	}

}
