/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.attachments;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.faces.context.FacesContext;

import org.apache.commons.codec.binary.Base64;
import org.apache.myfaces.custom.fileupload.UploadedFile;
import org.apache.xmlbeans.XmlObject;

import com.centurylink.mdw.bpm.Attachment;
import com.centurylink.mdw.bpm.MessageDocument;
import com.centurylink.mdw.bpm.MessageDocument.Message;
import com.centurylink.mdw.bpm.ProtocolAttributePair;
import com.centurylink.mdw.common.Compatibility;
import com.centurylink.mdw.common.constant.MiscConstants;
import com.centurylink.mdw.common.exception.PropertyException;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.ListActionController;
import com.centurylink.mdw.web.ui.list.ListItem;
import com.centurylink.mdw.web.util.RemoteLocator;
import com.centurylink.mdw.web.util.WebUtil;

public abstract class AttachmentsActionController implements ListActionController
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  public static final String ACTION_VIEW = "viewAttachments";
  public static final String ACTION_DELETE = "deleteAttachment";
  public static final String ACTION_DOWNLOAD = "downloadAttachment";
  public static final String ACTION_ADD = "addAttachment";
  public static final String ACTION_CONFIRM_DELETE = "confirmDeleteAttachment";
  public static final String ACTION_CANCEL = "cancel";
  private static int ATTRIBUTE_MAX_SIZE = 1;

  /**
   * @return the navigation outcome for arriving at the page
   */
  public abstract String getNavigationOutcome();

  /**
   * @return managed bean name for the in-memory attachment
   */
  public abstract String getManagedBeanName();

  /**
   * @return the instance id of the attachment owner
   */
  public abstract Long getOwnerId() throws UIException;

  public abstract String getOwner() throws PropertyException;

  /**
   * @return the master request id of the attachment owner
   */
  public abstract String getMasterRequestId() throws UIException;

  /**
   * @return managed bean name of this action controller
   */
  public abstract String getActionControllerName();

  public abstract String getAttachmentDocumentOwner() throws PropertyException;

  public abstract Long getAttachmentDocumentOwnerId() throws UIException;

  public abstract String getAttachmentProtocol();

  public abstract String getAttachmentLocationEntityId() throws UIException;

  public abstract Long getProcessInstanceId() throws Exception;

  private AttachmentItem _attachmentItem;

  private String _name = "";
  public String getName() { return _name; }
  public void setName(String name) { this._name = name; }

  private UploadedFile _upFile;
  public UploadedFile getUpFile() { return _upFile; }
  public void setUpFile(UploadedFile upFile) { this._upFile = upFile; }

  public String upload() throws IOException, UIException
  {
    if (_upFile == null)
    {
      return getNavigationOutcome();
    }

    _attachmentItem = (AttachmentItem) FacesVariableUtil.getValue(getManagedBeanName());


    FacesContext facesContext = FacesContext.getCurrentInstance();
    facesContext.getExternalContext().getApplicationMap().put("fileupload_bytes", _upFile.getBytes());
    facesContext.getExternalContext().getApplicationMap().put("fileupload_type", _upFile.getContentType());
    facesContext.getExternalContext().getApplicationMap().put("fileupload_name", _upFile.getName());
    String fileName = _upFile.getName();
    // remove path info from the file name
    int slash = fileName.lastIndexOf('/');
    if (slash >= 0)
      fileName = fileName.substring(slash + 1);
    int backSlash = fileName.lastIndexOf('\\');
    if (backSlash >= 0)
      fileName = fileName.substring(backSlash + 1);

    String filePath = null;
    try
    {
      filePath = WebUtil.getAttachmentLocation();
      if (filePath.startsWith(MiscConstants.DEFAULT_ATTACHMENT_LOCATION)) {
    	  Long documentId = createAttachmentDocument(fileName);
    	  _attachmentItem.setOwnerId(documentId);
    	  filePath = MiscConstants.ATTACHMENT_LOCATION_PREFIX+getAttachmentLocationEntityId();
      } else {
    	  _attachmentItem.setOwnerId(getOwnerId());
    	  if (!filePath.endsWith("/"))
    		  filePath += "/";
    	  if (getMasterRequestId() != null)
    		  filePath += getMasterRequestId() + "/";
    	  if (_attachmentItem.getOwnerId() != null && _attachmentItem.getOwnerId().longValue() > 0) {
    		  filePath += _attachmentItem.getOwnerId() + "/";
    	  }
    	  File ownerDir = new File(filePath);
          ownerDir.mkdirs();
          FileOutputStream outputFile = new FileOutputStream(filePath + fileName);
          outputFile.write(_upFile.getBytes());
          outputFile.close();
      }
    }
    catch (Exception ex)
    {
      logger.severe(ex.getMessage());
      throw new UIException(ex.getMessage(), ex);
    }

    try
    {
      setAction(ACTION_ADD);
      _attachmentItem.setName(fileName);
      _attachmentItem.setLocation(filePath);
      _attachmentItem.setContentType(_upFile.getContentType());
      _attachmentItem.setOwner(getOwner());
      performAction();
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      return "go_error";
    }

    return null;
  }

  protected Long createAttachmentDocument(String fileName) throws Exception {

		EventManager eventMgr = RemoteLocator.getEventManager();
		Long documentId = getOwnerId();
		MessageDocument  emailDoc = null;

		if (documentId == null || documentId.longValue() == 0){
			emailDoc = MessageDocument.Factory.newInstance();
			Message emailMessage = emailDoc.addNewMessage();

			ProtocolAttributePair[] protocolAttributePairArray = new ProtocolAttributePair[ATTRIBUTE_MAX_SIZE];

			setMessageAttribute(Listener.METAINFO_PROTOCOL, getAttachmentProtocol(),protocolAttributePairArray,0);
			emailMessage.setMessageAttributePairArray(protocolAttributePairArray);

			Attachment[] attachmentArray = new Attachment[1];
			attachmentArray[0] = addAttachment(fileName);
			emailMessage.setAttachmentArray(attachmentArray);
			documentId = eventMgr.createDocument(XmlObject.class.getName(), getProcessInstanceId(),
					getAttachmentDocumentOwner(), getAttachmentDocumentOwnerId(), null,
					null, emailDoc.xmlText());
		} else {
			String content = eventMgr.getDocumentVO(documentId).getContent();
			XmlObject xmlObject = XmlObject.Factory.parse(content, Compatibility.namespaceOptions());
			if (xmlObject instanceof MessageDocument) {
				emailDoc = (MessageDocument) xmlObject;
				List<Attachment> attachmentList = emailDoc.getMessage().getAttachmentList();
				if (attachmentList != null ){
					attachmentList.add(addAttachment(fileName));
				} else {
					Attachment[] attachmentArray = new Attachment[1];
					attachmentArray[0] = addAttachment(fileName);
					emailDoc.getMessage().setAttachmentArray(attachmentArray);
				}
				eventMgr.updateDocumentContent(documentId, emailDoc.xmlText());
			}
		}
		return documentId;
  }

  protected Attachment addAttachment(String fileName) throws IOException {
	  Attachment attachment = Attachment.Factory.newInstance();
	  attachment.setAttachment(Base64.encodeBase64(_upFile.getBytes()));
	  attachment.setFileName(fileName);
	  attachment.setContentType(_upFile.getContentType());
	  return attachment;
  }

  public String getDefaultAction()
  {
    return ACTION_VIEW;
  }

  public boolean isUploaded()
  {
    FacesContext facesContext = FacesContext.getCurrentInstance();
    return facesContext.getExternalContext().getApplicationMap().get("fileupload_bytes") != null;
  }

  private String _action = ACTION_VIEW;

  public String getAction()
  {
    return _action;
  }

  public void setAction(String action)
  {
    _action = action;
    if (_action == ACTION_VIEW)
    {
      AttachmentItem attachmentItem = (AttachmentItem) FacesVariableUtil.getValue(getManagedBeanName());
      attachmentItem.clear();
    }
  }

  /**
   * Called from command links in the attachments list.
   *
   * @return the nav destination.
   */
  public String performAction(String action, ListItem listItem)
  {
    setAction(action);
    FacesVariableUtil.setValue(getManagedBeanName(), listItem);
    FacesVariableUtil.setValue(getActionControllerName(), this);
    return performAction();
  }

  /**
   * Called from the command buttons.
   *
   * @return the nav destination
   */
  public String performAction()
  {
    _attachmentItem = (AttachmentItem) FacesVariableUtil.getValue(getManagedBeanName());
    return performActionOnAttachment();
  }

  public String confirmDeleteAttachment()
  {
    setAction(ACTION_CONFIRM_DELETE);
    return performAction();
  }

  public String cancelDeleteAttachment()
  {
    setAction(ACTION_CANCEL);
    return performAction();

  }

  private String performActionOnAttachment()
  {
    String user = FacesVariableUtil.getCurrentUser().getCuid();
    _attachmentItem.setModifiedBy(user);

    Date modDate = new Date();
    _attachmentItem.setModifiedDate(modDate);

    if (logger.isDebugEnabled())
    {
      logger.debug("Attachment Action: " + getAction());
      logger.debug("\nAttachment:\n" + "   ownerId: " + _attachmentItem.getOwnerId() + "   "
          + "user = " + user + "   " + "mod date = " + modDate);
    }

    if (getAction().equals(ACTION_ADD))
    {
      _attachmentItem.add();
      setAction(ACTION_VIEW);
    }
    else if (getAction().equals(ACTION_CANCEL))
    {
      setAction(ACTION_VIEW);
    }
    else if (getAction().equals(ACTION_DOWNLOAD))
    {
      return getNavigationOutcome();
    }
    else if (getAction().equals(ACTION_CONFIRM_DELETE))
    {
      _attachmentItem.delete();
      setAction(ACTION_VIEW);
    }

    return getNavigationOutcome();
  }

  protected void setMessageAttribute(String attributeName,String attributeValue,
			ProtocolAttributePair[] attributeArray, int index) {
		ProtocolAttributePair attributePair = ProtocolAttributePair.Factory.newInstance();
		attributePair.setAttributeName(attributeName);
		attributePair.setAttributeValue(attributeValue);

		if (index < attributeArray.length) {
			attributeArray[index] = attributePair;
		}
  }
}
