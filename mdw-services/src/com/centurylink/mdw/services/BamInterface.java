/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.xmlbeans.XmlObject;

import com.centurylink.bam.AttributeListT;
import com.centurylink.bam.AttributeT;
import com.centurylink.bam.ComponentRelationT;
import com.centurylink.bam.ComponentT;
import com.centurylink.bam.CustomEventDocument;
import com.centurylink.bam.CustomEventT;
import com.centurylink.bam.MasterRequestDocument;
import com.centurylink.bam.MasterRequestT;
import com.centurylink.bam.ProcessEventDocument;
import com.centurylink.bam.ProcessEventT;
import com.centurylink.bam.impl.CustomEventDocumentImpl;
import com.centurylink.mdw.common.Compatibility;
import com.centurylink.mdw.common.utilities.ListDiffer;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.data.bam.Attribute;
import com.centurylink.mdw.model.data.bam.Component;
import com.centurylink.mdw.model.data.bam.ComponentRelation;
import com.centurylink.mdw.model.data.bam.Event;
import com.centurylink.mdw.model.data.bam.MasterRequest;
import com.centurylink.mdw.services.dao.bam.BamDataAccess;
import com.centurylink.mdw.services.dao.bam.BamDataAccessDao;

public class BamInterface {

    private static StandardLogger _logger = LoggerUtil.getStandardLogger();
    private DatabaseAccess db;

    public BamInterface(DatabaseAccess db) {
        this.db = db;
    }

    public String handleEventMessage(String msg, XmlObject msgdoc, Map<String, String> metainfo) {
        try {
            _logger.mdwDebug("Class loader for CustomEventDocument=" + CustomEventDocument.class.getClassLoader().toString() + ", Class loader for CustomEventDocumentImpl=" + CustomEventDocumentImpl.class.getClassLoader().toString());
            String eventRootType = metainfo.get("EventRootType");
            if (eventRootType.equals("MasterRequest")) {
                MasterRequestDocument reqxmldoc = MasterRequestDocument.Factory.parse(msg, Compatibility.namespaceOptions());
                handlerMasterRequest(reqxmldoc);
            } else if (eventRootType.equals("CustomEvent")) {
                CustomEventDocument reqxmldoc = CustomEventDocument.Factory.parse(msg, Compatibility.namespaceOptions());
                handlerCustomEvent(reqxmldoc);
            } else if (eventRootType.equals("ProcessEvent")) {
                ProcessEventDocument reqxmldoc = ProcessEventDocument.Factory.parse(msg, Compatibility.namespaceOptions());
                handleProcessEvent(reqxmldoc);
            } else {
                _logger.severe("Unknown message type: " + msg);
            }
        } catch (Exception e) {
            _logger.severeException(e.getMessage(), e);
        }
        return null;
    }


    private void handlerMasterRequest(MasterRequestDocument reqxmldoc) throws Exception {
        BamDataAccess dbhelper = new BamDataAccessDao();
        MasterRequestT msgroot = reqxmldoc.getMasterRequest();
        int version = msgroot.getVersion();
        MasterRequest masterRequest;
        if (version>1) {
            masterRequest = dbhelper.loadMasterRequest(db, msgroot.getMasterRequestId(), msgroot.getRealm(), 2);
        } else masterRequest = null;
        List<Attribute> attributes = convertAttribute(msgroot.getAttributeList());
        List<Component> components = convertComponent(msgroot.getComponentList());
        List<ComponentRelation> componentRelations =
            convertComponentRelation(msgroot.getComponentRelationList(), masterRequest);
        if (masterRequest==null) {
            masterRequest = new MasterRequest();
            masterRequest.setMasterRequestId(msgroot.getMasterRequestId());
            masterRequest.setCreateTime(msgroot.getRequestTime().getTime());
            masterRequest.setRealm(msgroot.getRealm());
            dbhelper.persistMasterRequest(db, masterRequest);
            // put a standard event Submit automatically
            Long eventRowId = autoEvent(dbhelper, masterRequest, null, Event.EVENT_NAME_SUBMIT, "Version " + version);
            masterRequest.setAttributes(attributes);
            if (attributes!=null)
                dbhelper.persistAttributes(db, masterRequest.getRowId(), eventRowId, null, attributes);
            masterRequest.setComponents(components);
            if (components!=null) {
                dbhelper.persistComponents(db, masterRequest.getRowId(),eventRowId, components);
            }
            masterRequest.setComponentRelations(componentRelations);
            if (componentRelations!=null)
                dbhelper.persistComponentRelations(db, masterRequest.getRowId(), componentRelations);
        } else {
            // put a standard event Submit automatically
            Long eventRowId = autoEvent(dbhelper, masterRequest, null, Event.EVENT_NAME_SUBMIT, "Version " + version);
            // update master request attributes (update/insert, no delete)
            if (attributes!=null) {
                if (attributes!=null) {
                    dbhelper.persistAttributes(db, masterRequest.getRowId(), eventRowId, null, attributes);
                }
            }
            // update components (insert, no update/delete but record events)
            if (components!=null) {
                List<Component> updateList = new ArrayList<Component>();
                List<Component> insertList = new ArrayList<Component>();
                List<Component> deleteList = new ArrayList<Component>();
                ListDiffer<Component> differ = new ListDiffer<Component>();
                differ.diff(masterRequest.getComponents(), components, insertList, updateList, deleteList);
                if (insertList.size()>0)
                    dbhelper.persistComponents(db, masterRequest.getRowId(), eventRowId, insertList);
                for (Component c : insertList) {
                    // put a standard event NewComponent automatically
                    autoEvent(dbhelper, masterRequest, c.getRowId(),
                            Event.EVENT_NAME_NEW_COMPONENT, c.getComponentId());
                }
                for (Component c: deleteList) {
                    // put a standard event CancelComponent automatically
                    autoEvent(dbhelper, masterRequest, c.getRowId(),
                            Event.EVENT_NAME_CANCEL_COMPONENT, c.getComponentId());
                }
            }
            // update component relations (replace)
            if (masterRequest.getComponentRelations()!=null)
                dbhelper.deleteComponentRelations(db, masterRequest.getComponents());
            if (componentRelations!=null)
                dbhelper.persistComponentRelations(db, masterRequest.getRowId(), componentRelations);
        }
    }

    private Long autoEvent(BamDataAccess dbhelper, MasterRequest masterRequest, Long componentRowId,
            String eventName, String eventData) throws SQLException {
        Event event = new Event();
        event.setEventCategory(Event.EVENT_CAT_STANDARD);
        event.setEventName(eventName);
        event.setEventData(eventData);
        event.setSourceSystem(masterRequest.getRealm());
        event.setEventTime(masterRequest.getCreateTime());
        return dbhelper.persistEvent(db, masterRequest.getRowId(), componentRowId, event);
    }

    private void handlerCustomEvent(CustomEventDocument reqxmldoc) throws Exception {
        BamDataAccess dbhelper = new BamDataAccessDao();
        CustomEventT msgroot = reqxmldoc.getCustomEvent();
        Event event = new Event();
        event.setEventCategory(msgroot.getEventCategory());
        event.setEventName(msgroot.getEventName());
        event.setEventData(msgroot.getEventData());
        String realm = msgroot.getRealm();
        event.setSourceSystem(msgroot.getSourceSystem());
        event.setEventId(msgroot.getSourceEventId());
        event.setSubCategory(msgroot.getSubCategory());
        event.setEventTime(msgroot.getEventTime().getTime());
        String masterRequestId = msgroot.getMasterRequestId();
        String componentId = msgroot.getComponentId();

        MasterRequest masterRequest = dbhelper.loadMasterRequest(db, masterRequestId, realm, 0, true);

        if (masterRequest==null) {
            masterRequest = new MasterRequest();
            masterRequest.setMasterRequestId(msgroot.getMasterRequestId());
            masterRequest.setCreateTime(msgroot.getEventTime().getTime());
            masterRequest.setRealm(msgroot.getRealm());
            try{
                dbhelper.persistMasterRequest(db, masterRequest);
            }
            catch (SQLIntegrityConstraintViolationException ex){
                _logger.severe("duplicate master request with ID " + masterRequestId + " in realm " + realm + " - message:\n");
            }
            finally{
                db.getConnection().commit();
            }
        _logger.debug("Going to fetch master request Id for realm "+ msgroot.getRealm());
        masterRequest = dbhelper.loadMasterRequest(db, masterRequestId, realm, 0);

        _logger.debug("master request Id is "+masterRequest.getMasterRequestId());
        }

        Component component = componentId==null?null:dbhelper.loadComponent(db, masterRequest, componentId);
        Long componentRowId = component==null?null:component.getRowId();
        Long eventRowId = dbhelper.persistEvent(db, masterRequest.getRowId(), componentRowId, event);
        List<Attribute> attributes = convertAttribute(msgroot.getAttributes());
        if (attributes!=null) {
            dbhelper.persistAttributes(db, masterRequest.getRowId(), eventRowId, componentRowId, attributes);
        }
    }

    private void handleProcessEvent(ProcessEventDocument reqxmldoc) throws Exception {
        BamDataAccess dbhelper = new BamDataAccessDao();
        ProcessEventT msgroot = reqxmldoc.getProcessEvent();
        Event event = new Event();
        String realm = msgroot.getRealm();
        event.setSourceSystem(msgroot.getSourceSystem());
        String masterRequestId = msgroot.getMasterRequestId();
        String processId = msgroot.getProcessId();
        String processInstId = msgroot.getProcessInstanceId();
        String activityId = msgroot.getActivityId();
        String activityInstId = msgroot.getActivityInstanceId();
        if (activityId!=null && activityId.length()>0) {
            event.setEventCategory("ActivityEvent");
            event.setEventName("ActivityEvent");
            event.setEventId("p"+processId+"."+processInstId
                    +" a"+activityId+"."+activityInstId);
        } else {
            event.setEventId("p"+processId+"."+processInstId);
            event.setEventCategory("ProcessEvent");
            event.setEventName("ProcessEvent");
        }
        event.setEventTime(msgroot.getEventTime().getTime());
        event.setEventData(msgroot.getStatus());
        MasterRequest masterRequest = dbhelper.loadMasterRequest(db, masterRequestId, realm, 0);
        if (masterRequest==null) {
            _logger.severe("Cannot find master request with ID " +
                    masterRequestId + " in realm " + realm + " - message:\n" + reqxmldoc.xmlText());
            return;
        }
        dbhelper.persistEvent(db, masterRequest.getRowId(), null, event);
    }


    private List<Attribute> convertAttribute(AttributeListT xmlattrlist) {
        if (xmlattrlist==null) return null;
        List<AttributeT> xmlattrs = xmlattrlist.getAttributeList();
        if (xmlattrs==null) return null;
        List<Attribute> attrs = new ArrayList<Attribute>(xmlattrs.size());
        for (AttributeT xmlattr : xmlattrlist.getAttributeList()) {
            Attribute attr = new Attribute();
            attr.setAttributeName(xmlattr.getName());
            String value = xmlattr.getValue();
            if (value != null)
                value = value.replaceAll("&amp;", "&").replaceAll("&#39;", "'");
            attr.setAttributeValue(value);
            attrs.add(attr);
        }
        return attrs;
    }

    private List<Component> convertComponent(List<ComponentT> xmlCompList) {
        if (xmlCompList==null) return null;
        List<Component> compList = new ArrayList<Component>(xmlCompList.size());
        for (ComponentT xmlComp : xmlCompList) {
            Component comp = new Component();
            comp.setComponentId(xmlComp.getComponentId());
            comp.setComponentType(xmlComp.getComponentType());
            comp.setAttributes(convertAttribute(xmlComp.getAttributeList()));
            comp.setEvents(null);
            compList.add(comp);
        }
        return compList;
    }

    private List<ComponentRelation> convertComponentRelation(List<ComponentRelationT> xmlCompList,
            MasterRequest masterRequest) {
        if (xmlCompList==null) return null;
        List<ComponentRelation> compList = new ArrayList<ComponentRelation>(xmlCompList.size());
        for (ComponentRelationT xmlComp : xmlCompList) {
            ComponentRelation comp = new ComponentRelation();
            comp.setComponentARowid(findComponentRowId(xmlComp.getComponentId1(),masterRequest));
            comp.setComponentBRowid(findComponentRowId(xmlComp.getComponentId2(),masterRequest));
            comp.setRelationType(xmlComp.getRelationType());
            compList.add(comp);
        }
        return compList;
    }

    private Long findComponentRowId(String componentId, MasterRequest masterRequest) {
        for (Component component : masterRequest.getComponents()) {
            if (component.getComponentId().equals(componentId)) return component.getRowId();
        }
        return null;
    }


}
