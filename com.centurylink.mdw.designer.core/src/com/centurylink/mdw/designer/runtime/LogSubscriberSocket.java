/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.runtime;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.designer.DesignerCompatibility;
import com.centurylink.mdw.designer.DesignerDataAccess;
import com.centurylink.mdw.service.Action;
import com.centurylink.mdw.service.ActionRequestDocument;
import com.centurylink.mdw.service.ActionRequestDocument.ActionRequest;
import com.centurylink.mdw.service.Parameter;
import com.centurylink.mdw.soccom.SoccomException;
import com.centurylink.mdw.soccom.SoccomServer;

public abstract class LogSubscriberSocket extends SoccomServer {


	private static LogSubscriberSocket onlyOne = null;		// only allow one subcriber at a time

	private boolean oldNamespaces;

	private DesignerDataAccess dao;
	public LogSubscriberSocket(DesignerDataAccess dao) throws IOException {
	    this(dao, Integer.parseInt(LoggerUtil.getStandardLogger().getDefaultPort()), false);
	}

	public LogSubscriberSocket(DesignerDataAccess dao, boolean oldNamespaces) throws IOException {
        this(dao, Integer.parseInt(LoggerUtil.getStandardLogger().getDefaultPort()), oldNamespaces);
    }

    public LogSubscriberSocket(DesignerDataAccess dao, int port, boolean oldNamespaces) throws IOException {
        super(String.valueOf(port), (PrintStream)null);
        if (onlyOne!=null) throw new IOException("There is already a log subscriber running");
        onlyOne = this;
        this.dao = dao;
        this.oldNamespaces = oldNamespaces;
        super.max_threads = 1;
    }

    protected final void request_proc(String threadId, String msgid, byte[] msg,
			int msgsize, OutputStream out) throws IOException, SoccomException {
		String msgstr = new String(msg, 0, msgsize);
		handleMessage(msgstr);
	}

	protected abstract void handleMessage(String message);

    @Override
	public synchronized void shutdown() {
    	if (dao!=null) {	// indicator for not yet shutdown
    		try {
    			notifyServer(false);
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
    		super.shutdown();
    		dao = null;
    	}
		onlyOne = null;
	}

	@Override
	public void start(boolean useNewThread) {
		super.start(useNewThread);
		try {
			notifyServer(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void notifyServer(boolean on) throws Exception {
    	ActionRequestDocument msgdoc = ActionRequestDocument.Factory.newInstance();
		ActionRequest actionRequest = msgdoc.addNewActionRequest();
    	Action action = actionRequest.addNewAction();
    	action.setName("RegressionTest");
    	Parameter param = action.addNewParameter();
    	param.setName("Maintenance");
    	param.setStringValue("Watching");
    	param = action.addNewParameter();
    	param.setName("Server");
    	InetAddress ownIP = InetAddress.getLocalHost();
    	param.setStringValue(ownIP.getHostAddress() + ":" + LoggerUtil.getStandardLogger().getDefaultPort());
    	param = action.addNewParameter();
    	param.setName("Mode");
    	param.setStringValue(on?"on":"off");
    	String request;
    	if (oldNamespaces)
    	    request = DesignerCompatibility.getInstance().getOldActionRequest(msgdoc);
    	else
    	    request = msgdoc.xmlText();
    	dao.engineCall(request);
    }

	public static void killAnyRunning() {
		if (onlyOne!=null) {
			onlyOne.shutdown();
		}
	}

}
