/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.value.event;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.centurylink.bam.AttributeListT;
import com.centurylink.bam.AttributeT;
import com.centurylink.bam.CustomEventDocument;
import com.centurylink.bam.CustomEventT;
import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.dataaccess.RuntimeDataAccess;
import com.centurylink.mdw.model.data.event.EventType;
import com.centurylink.mdw.model.value.activity.ActivityRuntimeContext;
import com.centurylink.mdw.model.value.activity.ActivityVO;
import com.centurylink.mdw.model.value.attribute.AttributeVO;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessRuntimeContext;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.work.ActivityInstanceVO;
import com.qwest.mbeng.DomDocument;
import com.qwest.mbeng.MbengException;
import com.qwest.mbeng.MbengNode;

public class BamMessageDefinition {

    // CONSTANTS

    private String trigger;
    private String componentId;
    private String eventName;
    private String eventData;
    private String eventCategory;
    private String eventSubCategory;
    private String realm;
    private List<AttributeVO> attributes;
    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public BamMessageDefinition(boolean isStart) {
        trigger = isStart ? EventType.EVENTNAME_START : EventType.EVENTNAME_FINISH;
        attributes = new ArrayList<AttributeVO>();
    }

    public BamMessageDefinition(String string) throws MbengException {
        string = string.replaceAll("&", "&amp;").replaceAll("'", "&#39;");
        DomDocument doc = new DomDocument(string);
        trigger = doc.getValue("trigger");
        eventName = doc.getValue("name");
        eventData = doc.getValue("data");
        realm = doc.getValue("realm");
        eventCategory = doc.getValue("cat");
        eventSubCategory = doc.getValue("subcat");
        componentId = doc.getValue("component");
        MbengNode attrs = doc.findNode("attrs");
        attributes = new ArrayList<AttributeVO>();
        for (MbengNode anode = attrs.getFirstChild(); anode != null; anode = anode.getNextSibling()) {
            AttributeVO attr = new AttributeVO();
            attr.setAttributeName(doc.getValue(anode, "an"));
            attr.setAttributeValue(doc.getValue(anode, "av"));
            attributes.add(attr);
        }
    }

    public String format() throws MbengException {
        DomDocument doc = new DomDocument();
        doc.getRootNode().setName("BamMsgDef");
        doc.setValue("trigger", trigger, "X");
        doc.setValue("name", eventName, "X");
        doc.setValue("data", eventData, "X");
        doc.setValue("realm", realm, "X");
        if (eventCategory != null && eventCategory.length() > 0)
            doc.setValue("cat", eventCategory, "X");
        if (eventSubCategory != null && eventSubCategory.length() > 0)
            doc.setValue("subcat", eventSubCategory, "X");
        if (componentId != null && componentId.length() > 0)
            doc.setValue("component", componentId, "X");
        doc.setValue("attrs", null, "X");
        for (AttributeVO attr : attributes) {
            MbengNode anode = doc.addValue("attrs.attr", null, "X");
            doc.setValue(anode, "an", attr.getAttributeName(), "X");
            doc.setValue(anode, "av", attr.getAttributeValue(), "X");
        }
        return doc.xmlText();
    }

    public String getTrigger() {
        return trigger;
    }

    public void setTrigger(String trigger) {
        this.trigger = trigger;
    }

    public String getComponentId() {
        return componentId;
    }

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getEventData() {
        return eventData;
    }

    public void setEventData(String eventData) {
        this.eventData = eventData;
    }

    public String getEventCategory() {
        return eventCategory;
    }

    public void setEventCategory(String eventCategory) {
        this.eventCategory = eventCategory;
    }

    public String getEventSubCategory() {
        return eventSubCategory;
    }

    public void setEventSubCategory(String eventSubCategory) {
        this.eventSubCategory = eventSubCategory;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public List<AttributeVO> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<AttributeVO> attributes) {
        this.attributes = attributes;
    }

    public boolean isDefined() {
        return eventName != null && eventName.length() > 0;
    }

    public boolean isEmpty() {

        return StringHelper.isEmpty(eventName) && StringHelper.isEmpty(realm)
                && StringHelper.isEmpty(eventCategory) && StringHelper.isEmpty(eventSubCategory)
                && StringHelper.isEmpty(componentId) && StringHelper.isEmpty(eventData)
                && attributes.isEmpty();
    }

    public void clear() {
        eventName = null;
        realm = null;
        eventCategory = null;
        eventSubCategory = null;
        componentId = null;
        eventData = null;
        attributes = new ArrayList<AttributeVO>();
    }

    public String getMessageInstance(String masterRequestId) {
        CustomEventDocument msginstdoc = CustomEventDocument.Factory.newInstance();
        CustomEventT msginst = msginstdoc.addNewCustomEvent();
        msginst.setMasterRequestId(masterRequestId);
        msginst.setRealm(this.realm);
        msginst.setEventName(eventName);
        msginst.setEventTime(Calendar.getInstance());
        if (componentId != null && componentId.length() > 0)
            msginst.setComponentId(this.componentId);
        if (eventCategory != null && eventCategory.length() > 0)
            msginst.setEventCategory(this.eventCategory);
        if (eventSubCategory != null && eventSubCategory.length() > 0)
            msginst.setSubCategory(this.eventSubCategory);
        if (eventData != null && eventData.length() > 0)
            msginst.setEventData(this.eventData);
        msginst.setSourceSystem(ApplicationContext.getApplicationName());
        if (attributes != null) {
            AttributeListT xmlattrlist = msginst.addNewAttributes();
            for (AttributeVO attr : attributes) {
                AttributeT xmlattr = xmlattrlist.addNewAttribute();
                xmlattr.setName(attr.getAttributeName());
                xmlattr.setValue(attr.getAttributeValue());
            }
        }
        return msginstdoc.xmlText();
    }

    public String getMessageInstanceBlv(ProcessRuntimeContext runtimeContext)
            throws DataAccessException {
        CustomEventDocument msginstdoc = CustomEventDocument.Factory.newInstance();
        CustomEventT msginst = msginstdoc.addNewCustomEvent();
        msginst.setMasterRequestId(runtimeContext.getMasterRequestId());
        msginst.setRealm(this.realm);
        msginst.setEventName(eventName);
        msginst.setEventTime(Calendar.getInstance());
        if (componentId != null && componentId.length() > 0)
            msginst.setComponentId(this.componentId);
        if (eventCategory != null && eventCategory.length() > 0)
            msginst.setEventCategory(this.eventCategory);
        if (eventSubCategory != null && eventSubCategory.length() > 0)
            msginst.setSubCategory(this.eventSubCategory);
        if (eventData != null && eventData.length() > 0)
            msginst.setEventData(this.eventData);
        msginst.setSourceSystem(ApplicationContext.getApplicationName());
        AttributeListT xmlattrlist = null;
        if (attributes != null) {
            xmlattrlist = msginst.addNewAttributes();
            for (AttributeVO attr : attributes) {
                AttributeT xmlattr = xmlattrlist.addNewAttribute();
                xmlattr.setName(attr.getAttributeName());
                xmlattr.setValue(attr.getAttributeValue());
            }
        }
        Long activityId = 0L;

        if (runtimeContext instanceof ActivityRuntimeContext) {
            activityId = ((ActivityRuntimeContext) runtimeContext).getActivityId();
            if (activityId != null) {
                msginst.setActivityId("" + activityId);
                msginst.setSourceEventId("" + activityId);
            }
        }
        Long processId = runtimeContext.getProcessId();
        if (processId != null)
            msginst.setProcessId("" + processId);
        String processName = runtimeContext.getProcess().getName();
        if (processName != null && processName.length() > 0) {
            msginst.setProcessName(processName);
            msginst.setEventData(processName);
        }
        String processVer = runtimeContext.getProcess().getVersionString();
        if (processVer != null && processVer.length() > 0)
            msginst.setProcessVersion(processVer);
        Long processInstanceId = runtimeContext.getProcessInstanceId();
        msginst.setProcessInstanceId("" + processInstanceId);

        xmlattrlist = msginst.getAttributes();
        if (xmlattrlist == null)
            xmlattrlist = msginst.addNewAttributes();
        AttributeT xmlattr = xmlattrlist.addNewAttribute();
        xmlattr.setName("ProcessInstanceId");
        xmlattr.setValue("" + processInstanceId);

        xmlattr = xmlattrlist.addNewAttribute();
        xmlattr.setName("ProcessName");
        xmlattr.setValue(processName);

        xmlattr = xmlattrlist.addNewAttribute();
        xmlattr.setName("ProcessId");
        xmlattr.setValue("" + processId);
        if (runtimeContext instanceof ActivityRuntimeContext) {
            xmlattr = xmlattrlist.addNewAttribute();
            xmlattr.setName("ActivityId");
            xmlattr.setValue("" + activityId);
            // Add Caller Activity id and Process name
            RuntimeDataAccess da = DataAccess.getRuntimeDataAccess(new DatabaseAccess(null));
            ProcessInstanceVO processInstanceVO = da
                    .getProcessInstanceForCalling(processInstanceId);
            if (logger.isDebugEnabled()) {
                logger.debug("getMessageInstanceBlv - processName=" + processName + " eventname=" + eventName + " owner="
                    + processInstanceVO.getOwner() + " ownerid=" + processInstanceVO.getOwnerId()
                    + " secowner=" + processInstanceVO.getSecondaryOwner() + " secownerid="
                    + processInstanceVO.getSecondaryOwnerId());
            }
            if (processInstanceVO.getSecondaryOwnerId() != null
                    && OwnerType.ACTIVITY_INSTANCE.equals(processInstanceVO.getSecondaryOwner())) {
                ProcessInstanceVO callingProcessInstance = da
                        .getProcessInstanceAll(processInstanceVO.getOwnerId());
                ActivityInstanceVO callingActivityInstance = getActivityInstance(
                        callingProcessInstance.getActivities(),
                        processInstanceVO.getSecondaryOwnerId());

                // Find the activity instance with
                // instanceid=processInstanceVO.getSecondaryOwnerId()
                if (callingProcessInstance != null && callingActivityInstance != null) {
                    ProcessVO callingProcessVO = DataAccess.getProcessLoader().loadProcess(
                            callingProcessInstance.getProcessId(), false);
                    // Got the calling Process and Activity
                    xmlattr = xmlattrlist.addNewAttribute();
                    xmlattr.setName("CallingProcessName");
                    xmlattr.setValue("" + callingProcessVO.getProcessName());

                    ActivityVO callingActivity = callingProcessVO
                            .getActivityVO(callingActivityInstance.getDefinitionId());
                    xmlattr = xmlattrlist.addNewAttribute();
                    xmlattr.setName("CallingActivity");
                    xmlattr.setValue("" + callingActivity.getLogicalId());

                }

            }
        }
        return msginstdoc.xmlText();
    }

    /**
     * @param activities
     * @param secondaryOwnerId
     * @return
     */
    private ActivityInstanceVO getActivityInstance(List<ActivityInstanceVO> activities,
            Long secondaryOwnerId) {
        ActivityInstanceVO activityInstanceVO = null;
        for (ActivityInstanceVO act : activities) {
            if (act.getId().equals(secondaryOwnerId)) {
                // Found the activity instance, so break
                activityInstanceVO = act;
                break;
            }
        }
        return activityInstanceVO;
    }
}
