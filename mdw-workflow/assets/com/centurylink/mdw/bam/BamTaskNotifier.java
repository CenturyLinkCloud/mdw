/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.bam;

import java.util.Calendar;

import com.centurylink.bam.AttributeListT;
import com.centurylink.bam.AttributeT;
import com.centurylink.bam.CustomEventDocument;
import com.centurylink.bam.CustomEventT;
import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.exception.ObserverException;
import com.centurylink.mdw.common.exception.PropertyException;
import com.centurylink.mdw.model.value.task.TaskRuntimeContext;
import com.centurylink.mdw.observer.task.TaskNotifier;
import com.centurylink.mdw.services.messenger.IntraMDWMessenger;
import com.centurylink.mdw.services.messenger.MessengerFactory;

/**
 * Dynamic Java workflow asset.
 */
@RegisteredService(com.centurylink.mdw.observer.task.TaskNotifier.class)
public class BamTaskNotifier implements TaskNotifier {

    /**
     * Notifies when an action has been performed on a task instance.
     * @param TaskRuntimeContext
     * @param outcome
       BAM Message:
        <bus:CustomEvent xmlns:bus="http://www.centurylink.com/bam">
          <bus:MasterRequestId>12932</bus:MasterRequestId>
          <bus:Realm>WFMT</bus:Realm>
          <bus:EventName>Manual Acknowledgement</bus:EventName>
          <bus:EventTime>2013-11-18T13:16:54.430-07:00</bus:EventTime>
          <bus:EventCategory>Task</bus:EventCategory>
          <bus:EventSubCategory>Open</bus:eventSubCategory>
          <bus:Attributes>
            <bus:Attribute>
              <bus:Name>Name</bus:Name>
              <bus:Value>Manual Acknowledgement</bus:Value>
            </bus:Attribute>
            <bus:Attribute>
              <bus:Name>Instance Id</bus:Name>
              <bus:Value>12941</bus:Value>
            </bus:Attribute>
            <bus:Attribute>
              <bus:Name>Due Date</bus:Name>
              <bus:Value>11/18/2013</bus:Value>
            </bus:Attribute>
            <bus:Attribute>
              <bus:Name>WorkGroup(s)</bus:Name>
              <bus:Value>Dons Group</bus:Value>
            </bus:Attribute>
          </bus:Attributes>
          <bus:SourceSystem>MDWFramework</bus:SourceSystem>
        </bus:CustomEvent>
    */
    public void sendNotice(TaskRuntimeContext runtimeContext, String taskAction, String outcome) throws ObserverException {
        String bamUrl = null;
        String bamRealm = null;
        String msg = null;
        try {
            bamUrl = runtimeContext.getProperty(PropertyNames.MDW_BAM_URL);
            if (bamUrl == null)
                throw new PropertyException("Missing property: " + PropertyNames.MDW_BAM_URL);
            bamRealm = runtimeContext.getProperty(PropertyNames.MDW_BAM_REALM);
            if (bamRealm == null)
                throw new PropertyException("Missing property: " + PropertyNames.MDW_BAM_REALM);
            CustomEventDocument msginstdoc = CustomEventDocument.Factory.newInstance();
            CustomEventT msginst = msginstdoc.addNewCustomEvent();
            msginst.setMasterRequestId(runtimeContext.getMasterRequestId());
            msginst.setRealm(bamRealm);
            msginst.setEventName(runtimeContext.getTaskName());
            msginst.setEventTime(Calendar.getInstance());
            msginst.setEventCategory("Task");
            msginst.setSubCategory(outcome);
            msginst.setSourceSystem(ApplicationContext.getApplicationName());
            AttributeListT xmlattrlist = msginst.addNewAttributes();
            AttributeT xmlattr = xmlattrlist.addNewAttribute();
            xmlattr.setName("Name");
            xmlattr.setValue(runtimeContext.getTaskName());
            xmlattr = xmlattrlist.addNewAttribute();
            xmlattr.setName("Instance Id");
            xmlattr.setValue("" + runtimeContext.getInstanceId());
            if (runtimeContext.getAssignee() !=null && runtimeContext.getAssignee().length()>0){
                xmlattr = xmlattrlist.addNewAttribute();
                xmlattr.setName("Assignee");
                xmlattr.setValue(runtimeContext.getAssignee());
            }
            if (runtimeContext.getFormattedDueDate() !=null && runtimeContext.getFormattedDueDate().length()>0 ){
                xmlattr = xmlattrlist.addNewAttribute();
                xmlattr.setName("Due Date");
                xmlattr.setValue(runtimeContext.getFormattedDueDate().toString());
            }
            if (runtimeContext.getTaskInstanceVO().getWorkgroupsString() !=null && runtimeContext.getTaskInstanceVO().getWorkgroupsString().length()>0 ){
                xmlattr = xmlattrlist.addNewAttribute();
                xmlattr.setName("WorkGroup(s)");
                xmlattr.setValue(runtimeContext.getTaskInstanceVO().getWorkgroupsString());
            }

            msg =  msginstdoc.xmlText();
            if (msg == null || msg.isEmpty())
                return;

            if ("log".equals(bamUrl)) {
                runtimeContext.logInfo("BAM Message:\n" + msg);
            }
            else {
                IntraMDWMessenger msgbroker = MessengerFactory.newIntraMDWMessenger(bamUrl);
                msgbroker.sendMessage(msg);
            }
        } catch (Exception ex) {
            runtimeContext.logException(ex.getMessage() + " (bamUrl=" + bamUrl + ", bamRealm=" + bamRealm  + ")", ex);
            if (msg != null)
                runtimeContext.logDebug("Failed to send BAM Message:\n " + msg);
        }
    }

}