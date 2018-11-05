package com.centurylink.mdw.base;

import com.centurylink.mdw.annotations.Monitor;
import com.centurylink.mdw.constant.WorkAttributeConstant;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.event.InternalEvent;
import com.centurylink.mdw.model.monitor.ScheduledEvent;
import com.centurylink.mdw.model.workflow.ProcessRuntimeContext;
import com.centurylink.mdw.monitor.MonitorAttributes;
import com.centurylink.mdw.monitor.ProcessMonitor;
import com.centurylink.mdw.service.data.process.EngineDataAccess;
import com.centurylink.mdw.service.data.process.EngineDataAccessDB;
import com.centurylink.mdw.services.messenger.InternalMessenger;
import com.centurylink.mdw.services.messenger.MessengerFactory;
import com.centurylink.mdw.util.TransactionWrapper;
import org.json.JSONArray;

import java.util.Map;

@Monitor(value="Due Date Monitor", category=ProcessMonitor.class)
public class ProcessDueDateMonitor implements ProcessMonitor {

    public Map<String,Object> onStart(ProcessRuntimeContext context) {
        EngineDataAccess db = new EngineDataAccessDB();
        TransactionWrapper tw = null;
        try {
            JSONArray monitor = new MonitorAttributes(context.getAttribute(WorkAttributeConstant.MONITORS)).getRow(getClass().getName());
            int delay = Integer.parseInt(monitor.getString(3));
            InternalEvent event = InternalEvent.createProcessDelayMessage(context.getProcessInstance());
            InternalMessenger msgBroker = MessengerFactory.newInternalMessenger();
            tw = db.startTransaction();
            msgBroker.sendDelayedMessage(event, delay, ScheduledEvent.INTERNAL_EVENT_PREFIX + context.getProcessInstanceId() + ".delay", false, db);
        }
        catch (Exception ex) {
            context.logException(ex.getMessage(), ex);
        }
        finally {
            try {
                db.stopTransaction(tw);
            } catch (DataAccessException e) {
                context.logException(e.getMessage(), e);
            }
        }
        return null;
    }
}
