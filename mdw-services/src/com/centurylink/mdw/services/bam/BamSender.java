/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.bam;

import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.exception.PropertyException;
import com.centurylink.mdw.model.value.event.BamMessageDefinition;
import com.centurylink.mdw.model.value.process.ProcessRuntimeContext;
import com.centurylink.mdw.services.messenger.IntraMDWMessenger;
import com.centurylink.mdw.services.messenger.MessengerFactory;

public class BamSender {

    private ProcessRuntimeContext runtimeContext;

    public BamSender(ProcessRuntimeContext runtimeContext) {
        this.runtimeContext = runtimeContext;
    }

    public void sendMessage(String event, boolean includeLiveViewData) {
        String bamUrl = null;
        String msg = null;
        try {
            bamUrl = runtimeContext.getProperty(PropertyNames.MDW_BAM_URL);
            if (bamUrl == null)
                throw new PropertyException("Missing property: " + PropertyNames.MDW_BAM_URL);
            String msgdef = runtimeContext.evaluateToString(runtimeContext.getAttribute(event));
            BamMessageDefinition bammsg = new BamMessageDefinition(msgdef);
            if (includeLiveViewData)
                msg = bammsg.getMessageInstanceBlv(runtimeContext);
            else
                msg = bammsg.getMessageInstance(runtimeContext.getMasterRequestId());
            if ("log".equals(bamUrl)) {
                runtimeContext.logInfo("BAM Message:\n" + msg);
            }
            else {
                IntraMDWMessenger msgbroker = MessengerFactory.newIntraMDWMessenger(bamUrl);
                // TODO spawn a separate thread
                msgbroker.sendMessage(msg);
            }
        } catch (Exception ex) {
            runtimeContext.logException(ex.getMessage() + " (" + bamUrl + ")", ex);
            if (msg != null)
              runtimeContext.logDebug("Failed to send BAM Message:\n " + msg);
        }
    }
}
