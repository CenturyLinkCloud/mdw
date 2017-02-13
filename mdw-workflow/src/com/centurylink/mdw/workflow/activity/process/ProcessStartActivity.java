/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.activity.process;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.activity.types.StartActivity;
import com.centurylink.mdw.bpm.Attachment;
import com.centurylink.mdw.bpm.MessageDocument;
import com.centurylink.mdw.bpm.MessageDocument.Message;
import com.centurylink.mdw.config.PropertyException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.config.PropertyUtil;
import com.centurylink.mdw.constant.MiscConstants;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.script.ExecutionException;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.translator.VariableTranslator;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.workflow.activity.DefaultActivityImpl;
import com.centurylink.mdw.xml.XmlPath;

/**
 * Base class for all the ProcessStart Controlled Activity
 * This class will be extended by the custom ProcessStart activity
 */
@Tracked(LogLevel.TRACE)
public class ProcessStartActivity extends DefaultActivityImpl implements StartActivity {

    private static final String PARAMETERS = "Parameters";

    /**
     * Default constructor with params
     */
    public ProcessStartActivity(){
        super();
    }

    @Override
    public void execute() throws ActivityException {
        String parameters_spec = this.getAttributeValue(PARAMETERS);
        try {
            if (parameters_spec != null) {
                Map<String,String>parameters = StringHelper.parseMap(parameters_spec);
                for (String key : parameters.keySet()) {
                    String one = parameters.get(key);
                    if(valueIsJavaExpression(one)){
                        Object request = getParameterValue("request");
                        if (request != null) {
                            String value = getRuntimeContext().evaluateToString(one);
                            // do not override input values set explicitly with null ones from xpath (eg: HandleOrder demo)
                            if (value != null){
                                Variable variable = getProcessDefinition().getVariable(key);
                                if (VariableTranslator.isDocumentReferenceVariable(getPackage(), variable.getVariableType()))
                                    setParameterValueAsDocument(key, variable.getVariableType(), value);
                                else
                                    setParameterValue(key, value);
                            }
                        }
                    }
                }
            }
        }catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
        }

    }

    protected void addDocumentAttachment(XmlObject msgdoc) throws ActivityException {

        try {
            if (msgdoc instanceof MessageDocument) {
                TaskManager taskMgr = ServiceLocator.getTaskManager();
                EventManager eventMgr = ServiceLocator.getEventManager();
                Long documentId = this.getProcessInstanceOwnerId();
                Message emailMessage = ((MessageDocument) msgdoc).getMessage();
                FileOutputStream outputFile = null;
                List<Attachment> attachments = emailMessage.getAttachmentList();
                String filePath = getAttachmentLocation();
                if (filePath.equals(MiscConstants.DEFAULT_ATTACHMENT_LOCATION)) {
                    for (Attachment attachment : attachments) {
                        taskMgr.addAttachment(attachment.getFileName(),
                                MiscConstants.ATTACHMENT_LOCATION_PREFIX+getMasterRequestId(),attachment.getContentType(), "SYSTEM",OwnerType.DOCUMENT,documentId);
                    }
                } else { // download file to directory location
                    if (getMasterRequestId() != null) {
                          filePath += getMasterRequestId() + "/";
                    }
                      File ownerDir = new File(filePath);
                    ownerDir.mkdirs();
                    Long attachmentId = null;
                    for (Attachment attachment : attachments) {
                        outputFile = new FileOutputStream(filePath + attachment.getFileName());
                        outputFile.write(Base64.decodeBase64(attachment.getAttachment()));
                        outputFile.close();
                        attachmentId = taskMgr.addAttachment(attachment.getFileName(),
                                filePath,attachment.getContentType(), "SYSTEM",OwnerType.DOCUMENT,documentId);
                        attachment.unsetAttachment();
                        attachment.setAttachmentId(attachmentId.longValue());
                    }
                      eventMgr.updateDocumentContent(documentId, msgdoc.xmlText(), XmlObject.class.getName());
                }
            }
        } catch (Exception e) {
            logger.severeException("Exception occured to while adding attachment",e);
            throw new ActivityException("Exception occured to while adding attachment"+e.getMessage());
        }
    }

    private String evaluate(XmlObject /* Document */ doc, String expr) {
        String value;
        if (expr.startsWith("xpath:")) {
            value = XmlPath.evaluate(doc, expr.substring(6));
        } else value = expr;
        return value;
    }

    public String getAttachmentLocation() throws PropertyException
    {
        PropertyManager propMgr = PropertyUtil.getInstance().getPropertyManager();
        String filePath = propMgr.getStringProperty("MDWFramework.TaskManagerWeb", "attachments.storage.location");
        if (StringHelper.isEmpty(filePath) || MiscConstants.DEFAULT_ATTACHMENT_LOCATION.equalsIgnoreCase(filePath)) {
            filePath = MiscConstants.DEFAULT_ATTACHMENT_LOCATION;
        } else if (! filePath.endsWith("/")){
            filePath +="/";
        }
        return filePath;
    }

}
