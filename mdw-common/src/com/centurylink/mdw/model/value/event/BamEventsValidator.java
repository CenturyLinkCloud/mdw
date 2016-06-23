/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.value.event;

import java.util.List;
import java.util.Map;

import com.centurylink.mdw.model.value.attribute.AttributeVO;

public class BamEventsValidator {

    private static final int MAX_REALM_CODE_SIZE = 32; //to match size in DB
    private static final int MAX_EVENT_NAME_SIZE = 40; //to match size in DB
    private static final int MAX_EVENT_CATEGORY_SIZE = 40; //to match size in DB
    private static final int MAX_EVENT_DATA_SIZE = 4000; //to match size in DB
    private static final int MAX_EVENT_SUB_CATEGORY_SIZE = 40; //to match size in DB
    private static final int MAX_ATTR_NAME_SIZE = 64; //to match size in DB
    private static final int MAX_ATTR_VALUE_SIZE = 4000; //to match size in DB

    public BamEventsValidator() {
        super();
    }

    public String validateBamEventAttributes(Map<String,BamMessageDefinition> bamEventDefs) {
        int i=0;
        StringBuffer msgBuf = new StringBuffer();
        String realm = "";
        String eventName = "";
        String eventData = "";
        String eventCategory = "";
        String eventSubCategory = "";

        for (String attrName : bamEventDefs.keySet()) {
            BamMessageDefinition messageDef = bamEventDefs.get(attrName);
            messageDef = bamEventDefs.get(attrName);

            //Check Realm
            realm = messageDef.getRealm();
            if ((realm == null) || (realm.length() == 0)) {
                msgBuf.append(++i + ". Realm cannot be null for " + attrName + " event\n");
            }
            else if (realm.trim().length() > MAX_REALM_CODE_SIZE) {
                msgBuf.append(++i + ". Realm, '" + realm + "' cannot exceed " + MAX_REALM_CODE_SIZE
                        + " characters for " + attrName + " event\n");
            }

            //Check Event Name
            eventName = messageDef.getEventName();
            if ((eventName == null) || (eventName.length() == 0)) {
                msgBuf.append(++i + ". Event Name cannot be null for " + attrName + " event\n");
            }
            else if (eventName.trim().length() > MAX_EVENT_NAME_SIZE) {
                msgBuf.append(++i + ". Event Name, '" + eventName + "' cannot exceed " + MAX_EVENT_NAME_SIZE
                        + " characters for " + attrName + " event\n");
            }

            //Check Event Category
            eventCategory = messageDef.getEventCategory();
            if ((eventCategory != null) && (eventCategory.trim().length() > MAX_EVENT_CATEGORY_SIZE)) {
                msgBuf.append(++i + ". Event Category, '" + eventCategory + "' cannot exceed " + MAX_EVENT_CATEGORY_SIZE
                        + " characters for for event: " + attrName + " \n");
            }

            //Check Event Name
            eventSubCategory = messageDef.getEventSubCategory();
            if ((eventSubCategory != null) && (eventSubCategory.trim().length() > MAX_EVENT_SUB_CATEGORY_SIZE)) {
                msgBuf.append(++i + ". Event Sub-Category, '" + eventSubCategory + "' cannot exceed "
                        + MAX_EVENT_SUB_CATEGORY_SIZE + " characters for for event: " + attrName + " \n");
            }

            //Check Event Data
            eventData = messageDef.getEventData();

            if ((eventData != null) && (eventData.trim().length() > MAX_EVENT_DATA_SIZE)) {
                msgBuf.append(++i + ". Event Data, '" + eventData + "' cannot exceed " + MAX_EVENT_DATA_SIZE
                        + " characters for " + attrName + " event\n");
            }

            //Check Attributes List
            List<AttributeVO> attrList = messageDef.getAttributes();
            int attrNo=1;
            for (AttributeVO attrVO : attrList) {
                if ((attrVO.getAttributeName() == null || attrVO.getAttributeName().length() == 0)
                ||  (attrVO.getAttributeValue() == null || attrVO.getAttributeValue().length() == 0))
                {
                    msgBuf.append(++i + ". Both Name and Value fields must be populated in Attribute# " + attrNo + " for " + attrName + " event\n");
                }
                if (attrVO.getAttributeName().length() > MAX_ATTR_NAME_SIZE)
                {
                    msgBuf.append(++i + ". Attribute Name '" + attrVO.getAttributeName() + "' cannot exceed " + MAX_ATTR_NAME_SIZE + " characters for " + attrName + " event\n");
                }
                if (attrVO.getAttributeValue().length() > MAX_ATTR_VALUE_SIZE)
                {
                    msgBuf.append(++i + ". Attribute Name '" + attrVO.getAttributeValue() + "' cannot exceed " + MAX_ATTR_VALUE_SIZE + " characters for " + attrName + " event\n");
                }
                attrNo++;
            }

        }

        String msg = "";
        if (msgBuf.length() > 0)
            msg = msgBuf.toString();

        return msg;
    }
}
