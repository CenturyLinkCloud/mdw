/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.servlet;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.xmlbeans.XmlException;

import com.centurylink.mdw.bpm.Attachment;
import com.centurylink.mdw.bpm.MessageDocument;
import com.centurylink.mdw.common.Compatibility;
import com.centurylink.mdw.common.constant.MiscConstants;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.task.TaskInstanceVO;
import com.centurylink.mdw.model.value.variable.DocumentVO;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.web.util.RemoteLocator;
import com.centurylink.mdw.web.util.WebUtil;

public class AttachmentDownload extends HttpServlet
{
	private static final long serialVersionUID = 420L;
	private static StandardLogger logger = LoggerUtil.getStandardLogger();

	public void init(ServletConfig config) throws ServletException {
		super.init(config);
	}

    public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException
      {
        doGet(request, response);
      }


	public void doGet(HttpServletRequest request, HttpServletResponse response)
	  throws ServletException, IOException
    {
      InputStream is = null;
      OutputStream os = null;

      try
      {
    	String attachmentId = request.getParameter("attachmentId");
        String ownerId = request.getParameter("ownerId");
        String fileName = request.getParameter("fileName");
        String masterRequestId = request.getParameter("masterRequestId");
    	com.centurylink.mdw.model.data.common.Attachment attachment = null;
    	String attachPropLocation = WebUtil.getAttachmentLocation();
    	TaskManager taskMgr = RemoteLocator.getTaskManager();
    	if (!StringHelper.isEmpty(attachmentId)) {
    		attachment = taskMgr.getAttachment(new Long(attachmentId));
    	} else if (!StringHelper.isEmpty(masterRequestId) && !StringHelper.isEmpty(fileName)) {
    		attachment = getAttachmentByOrderId(fileName, masterRequestId,
    				attachPropLocation, taskMgr);
    	} else if (!StringHelper.isEmpty(ownerId) && !StringHelper.isEmpty(fileName)){
    		attachment = getAttachmentByTaskId(ownerId, fileName,attachPropLocation, taskMgr);
    	}
    	if (attachment != null) {
    		String attachmentLocation = attachment.getAttachmentLocation();
    		if (attachmentLocation.startsWith(MiscConstants.ATTACHMENT_LOCATION_PREFIX)) {
    			DocumentVO documentVO = taskMgr.getAttachmentDocument(attachment);
    			if (documentVO != null && documentVO.getContent() != null) {
    				is = getAttachmentStream(documentVO.getContent(), fileName);
    			}
    		} else {
    			String filePath = attachment.getAttachmentLocation();
    			if (!filePath.endsWith("/"))
    	               filePath += "/";
    			is = new FileInputStream(new File(filePath, fileName));
    		}
    	}
        os = response.getOutputStream();
        response.setContentType("application/octet-stream");
        String encodedFileName = URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename=\"" + encodedFileName + "\"");

        int read = 0;
        byte[] bytes = new byte[1024];
        while((read = is.read(bytes)) != -1)
        {
           os.write(bytes,0,read);
        }

        os.flush();
      }
      catch (Exception ex)
      {
        logger.severeException(ex.getMessage(), ex);
        throw new ServletException(ex);
      }
      finally
      {
        if (is != null)
          is.close();
        if (os != null)
          os.close();
      }
    }

	protected com.centurylink.mdw.model.data.common.Attachment getAttachmentByTaskId(
			String ownerId, String fileName, String attachPropLocation,
			TaskManager taskMgr) throws DataAccessException {
		com.centurylink.mdw.model.data.common.Attachment attachment;
		String location = null;
		if (MiscConstants.DEFAULT_ATTACHMENT_LOCATION.equals(attachPropLocation)) {
			location = MiscConstants.ATTACHMENT_LOCATION_PREFIX + ownerId;
		} else {
			TaskInstanceVO taskInstanceVO = taskMgr.getTaskInstance(new Long(ownerId));
			location = attachPropLocation + taskInstanceVO.getMasterRequestId() + "/" + ownerId;
		}
		attachment = taskMgr.getAttachment(fileName,location);
		return attachment;
	}

	protected com.centurylink.mdw.model.data.common.Attachment getAttachmentByOrderId(
			String fileName, String masterRequestId,
			String attachPropLocation, TaskManager taskMgr)
			throws DataAccessException {
		String location = null;
		com.centurylink.mdw.model.data.common.Attachment attachment;
		if (MiscConstants.DEFAULT_ATTACHMENT_LOCATION.equals(attachPropLocation)) {
			location = MiscConstants.ATTACHMENT_LOCATION_PREFIX + masterRequestId;
		} else {
			location = attachPropLocation + masterRequestId;
		}
		attachment = taskMgr.getAttachment(fileName,location);

		return attachment;
	}

	protected InputStream getAttachmentStream(String emailXml,String fileName)
			throws XmlException, IOException {
		InputStream input = null;
		MessageDocument emailDoc = MessageDocument.Factory.parse(emailXml, Compatibility.namespaceOptions());
		List<Attachment> attchmentList = emailDoc.getMessage().getAttachmentList();
		if (! attchmentList.isEmpty()) {
			for (Attachment attachment : attchmentList ) {
				if (fileName != null && fileName.equals(attachment.getFileName())) {
					byte[] decodedByte = Base64.decodeBase64(attachment.getAttachment());
					input = new ByteArrayInputStream(decodedByte);
					break;
				}
			}
		}
		return input;
	}

 }