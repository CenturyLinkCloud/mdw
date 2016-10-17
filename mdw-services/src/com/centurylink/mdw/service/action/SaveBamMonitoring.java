/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.xmlbeans.XmlOptions;

import com.centurylink.mdw.bpm.MDWStatusMessageDocument;
import com.centurylink.mdw.bpm.MDWStatusMessageDocument.MDWStatusMessage;
import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.XmlService;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.ProcessPersister;
import com.centurylink.mdw.model.value.attribute.AttributeVO;
import com.centurylink.mdw.model.value.event.BamMessageDefinition;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.services.dao.process.cache.ProcessVOCache;
import com.qwest.mbeng.MbengException;

/**
 * <p>
 * Exposes a web service that allows clients to update the BAM Monitoring
 * elements and attributes via POST e.g. <ActionRequest> <Action
 * Name="SaveBamMonitoring"> <Parameter name="ActivityId">7</Parameter>
 * <Parameter name="ProcessName">IainParallel</Parameter> <Parameter
 * name="ProcessVersion">0.1</Parameter> <Parameter
 * name="StartBamMessageDefinition"><![CDATA[<BamMsgDef>
 * <trigger>START</trigger> <name>Start activity</name> <data></data>
 * <realm>MyRealm</realm> <cat>CAT</cat> <subcat>SUBCAT</subcat> <attrs> <attr>
 * <an>Route chosen</an> <av>No Idea at the moment</av> </attr> </attrs>
 * </BamMsgDef>]]></Parameter> <Parameter
 * name="FinishBamMessageDefinition"><![CDATA[<BamMsgDef>
 * <trigger>FINISH</trigger> <name>Start activity</name> <data></data>
 * <realm>MyRealm</realm> <cat>CAT</cat> <subcat>SUBCAT</subcat> <attrs> <attr>
 * <an>Route chosen</an> <av>No Idea at the moment</av> </attr> </attrs>
 * </BamMsgDef>]]></Parameter> </Action> </ActionRequest>
 *
 * </p>
 *
 * @author aa70413
 *
 */
public class SaveBamMonitoring implements XmlService {

    public static final String PARAM_START_BAM_MESSAGE_DEFINITION = "StartBamMessageDefinition";
    public static final String PARAM_FINISH_BAM_MESSAGE_DEFINITION = "FinishBamMessageDefinition";
    public static final String PARAM_ACTIVITY_ID = "ActivityId";
    public static final String PARAM_PROCESS_NAME = "ProcessName";
    public static final String PARAM_PROCESS_VERSION = "ProcessVersion";

    /**
     * <p>
     * In order to figure out which process and activity this BAM update is for,
     * we need:
     * <li>ActivityId</li>
     * <li>ProcessName</li>
     * <li>ProcessVersion</li>
     * </p>
     */
    public String getXml(Map<String, Object> parameters, Map<String, String> metaInfo)
            throws ServiceException {

        // activity id
        Long activityId = parameters.get(PARAM_ACTIVITY_ID) == null ? null : Long
                .valueOf(parameters.get(PARAM_ACTIVITY_ID).toString());
        if (activityId == null)
            throw new ServiceException("Missing parameter: " + PARAM_ACTIVITY_ID);
        // processname
        String processName = parameters.get(PARAM_PROCESS_NAME) == null ? null : parameters.get(
                PARAM_PROCESS_NAME).toString();
        if (processName == null)
            throw new ServiceException("Missing parameter: " + PARAM_PROCESS_NAME);
        // ProcessVersion
        if (parameters.get(PARAM_PROCESS_VERSION) == null)
            throw new ServiceException("Missing parameter: " + PARAM_PROCESS_VERSION);
        int processVersion = ProcessVO.parseVersion(parameters.get(PARAM_PROCESS_VERSION)
                .toString());

        /**
         * Build a map of the start and finish BAM message definitions since
         * this allows us to use the same api for updating BAM definitions
         */
        Map<String, BamMessageDefinition> bamEventDefs = new HashMap<String, BamMessageDefinition>();
        BamMessageDefinition start = getBamMessageDefinition(PARAM_START_BAM_MESSAGE_DEFINITION,
                parameters);
        BamMessageDefinition finish = getBamMessageDefinition(PARAM_FINISH_BAM_MESSAGE_DEFINITION,
                parameters);
        /**
         * The call to this method will only have with the Start or the Finish
         * definition (not both)
         */
        if (start != null) {
            bamEventDefs.put(WorkAttributeConstant.BAM_START_MSGDEF, start);
        }
        else if (finish != null) {
            bamEventDefs.put(WorkAttributeConstant.BAM_FINISH_MSGDEF, finish);
        }
        // Save the Definition and attributes
        saveBamMsgDefs(processName, processVersion, activityId, bamEventDefs);

        return createSuccessResponse("Updated BAM attributes ");
    }
    private String createSuccessResponse(String message) {
        MDWStatusMessageDocument successResponseDoc = MDWStatusMessageDocument.Factory.newInstance();
        MDWStatusMessage statusMessage = successResponseDoc.addNewMDWStatusMessage();
        statusMessage.setStatusCode(0);
        statusMessage.setStatusMessage(message);
        return successResponseDoc.xmlText(getXmlOptions());
    }

     private XmlOptions getXmlOptions() {
        return new XmlOptions().setSavePrettyPrint().setSavePrettyPrintIndent(2);
    }

    /**
     * Takes activity data and a BAM monitoring definition (either Start or
     * Finish) and saves it
     *
     * @param processName
     * @param processVersion
     * @param activityId
     * @param bamEventDefs
     * @throws ServiceException
     */
    private void saveBamMsgDefs(String processName, int processVersion, Long activityId,
            Map<String, BamMessageDefinition> bamEventDefs) throws ServiceException {
        ProcessPersister persister;
        try {
                persister = DataAccess.getDbProcessPersister();
        }
        catch (DataAccessException e) {
            throw new ServiceException("Exception parsing message " + e.getMessage());
        }
        /**
         * Get the correct process definition based on process name and version
         */
        ProcessVO procdef = ProcessVOCache.getProcessVO(processName, processVersion);

        // remove empty bam defs
        List<String> keysToRemove = new ArrayList<String>();
        for (String key : bamEventDefs.keySet()) {

            BamMessageDefinition def = bamEventDefs.get(key);
            def.setAttributes(clearAttributesIfEmpty(def.getAttributes()));
            if (def.isEmpty()) {
                keysToRemove.add(key);

                try {
                    String attrName = WorkAttributeConstant.OVERRIDE_QUALIFIER + activityId + ":" + key;
                    persister.setAttribute(OwnerType.PROCESS, procdef.getId(), attrName, null);
                }
                catch (DataAccessException e) {
                    throw new ServiceException("Exception parsing message " + e.getMessage());
                }
             }

        }
        for (String keyToRemove : keysToRemove)
            bamEventDefs.remove(keyToRemove);

        for (String attrName : bamEventDefs.keySet()) {
            BamMessageDefinition messageDef = bamEventDefs.get(attrName);
            if (messageDef.isDefined()) {
                try {
                    String overrideAttr = WorkAttributeConstant.OVERRIDE_ACTIVITY + activityId + ":" + attrName;
                    persister.setAttribute(OwnerType.PROCESS, procdef.getId(), overrideAttr, messageDef.format());
                }
                catch (MbengException e) {

                    throw new ServiceException("Exception parsing message " + e.getMessage());
                }
                catch (DataAccessException e) {
                    throw new ServiceException("Exception parsing message " + e.getMessage());
                }
            }
            else {
                // no event name
                try {
                    boolean isBlank = messageDef.format().equals(
                            new BamMessageDefinition(attrName
                                    .equals(WorkAttributeConstant.BAM_START_MSGDEF)).format());

                    if (isBlank) {
                        // TODO remove instead of save null
                        String overrideAttrName = WorkAttributeConstant.OVERRIDE_ACTIVITY + activityId + ":" + attrName;
                        persister.setAttribute(OwnerType.PROCESS, procdef.getId(), overrideAttrName, null);
                    }
                }
                catch (MbengException e) {
                    throw new ServiceException("Exception parsing message " + e.getMessage());
                }
                catch (Exception ex) {
                    throw new ServiceException("Exception saving message with blank event name "
                            + ex.getMessage());
                }
            }
        }
    }

    protected List<AttributeVO> clearAttributesIfEmpty(List<AttributeVO> attributes) {
        List<AttributeVO> newAttrList = new ArrayList<AttributeVO>();
        if (attributes != null) {
            for (AttributeVO attr : attributes) {
                if (StringHelper.isEmpty(attr.getAttributeName())
                        && StringHelper.isEmpty(attr.getAttributeValue())) {
                    continue;
                }
                else {
                    newAttrList.add(attr);
                }
            }
        }
        return newAttrList;

    }

    /**
     * Takes an xml BamMsgDef and returns an object BamMessageDefinition
     *
     * @param paramStartBamMessageDefinition
     * @param parameters
     * @return
     * @throws ServiceException
     */
    private BamMessageDefinition getBamMessageDefinition(String paramStartBamMessageDefinition,
            Map<String, Object> parameters) throws ServiceException {
        // determine content
        String bamMessageDefinition = parameters.get(paramStartBamMessageDefinition) == null ? null
                : parameters.get(paramStartBamMessageDefinition).toString();
        if (bamMessageDefinition == null)
            return null;
        BamMessageDefinition def = null;
        try {
            def = new BamMessageDefinition(bamMessageDefinition);
        }
        catch (MbengException e) {
            // TODO Auto-generated catch block
            throw new ServiceException("Exception parsing message " + e.getMessage());
        }
        return def;
    }

    public String getText(Map<String, Object> parameters, Map<String, String> metaInfo)
            throws ServiceException {
        return getXml(parameters, metaInfo);
    }

}
