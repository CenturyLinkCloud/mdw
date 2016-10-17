/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.listener.email;

import static com.centurylink.mdw.common.constant.MessageAttributeConstant.MESSAGE_RECIPIENTS;
import static com.centurylink.mdw.common.constant.MessageAttributeConstant.MESSAGE_SENDER;
import static com.centurylink.mdw.common.constant.MessageAttributeConstant.MESSAGE_SENT_DATE;
import static com.centurylink.mdw.common.constant.MessageAttributeConstant.MESSAGE_SUBJECT;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Part;

import org.bouncycastle.util.encoders.Base64;

import com.centurylink.mdw.bpm.Attachment;
import com.centurylink.mdw.bpm.MessageDocument;
import com.centurylink.mdw.bpm.ProtocolAttributePair;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.listener.ListenerHelper;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.listener.ListenerException;

public class MDWEmailListener {

	protected static StandardLogger logger = LoggerUtil.getStandardLogger();
	private static int ATTRIBUTE_MAX_SIZE = 5;

	public void process(Message[] messages) throws ListenerException {
		try {
			for (Message message : messages){
				String emailXml = createEmailXml(message);
				if (! StringHelper.isEmpty(emailXml)) {
					ListenerHelper helper = new ListenerHelper();
					Map<String,String> metaInfo = new HashMap<String,String>();
					metaInfo.put(Listener.METAINFO_PROTOCOL, Listener.METAINFO_PROTOCOL_EMAIL);
					metaInfo.put(Listener.METAINFO_SERVICE_CLASS, this.getClass().getName());
					helper.processEvent(emailXml, metaInfo);
					// Set Delete flag as true sothat this email will be marked as read or deleted based on property values
					message.setFlag(Flags.Flag.DELETED,true);
				}
			}
		} catch (Exception ex) {
			logger.severeException("Exception in MDWEmailListener "+ex.getMessage(), ex);
			throw new ListenerException("Exception in MDWEmailListener " + ex.getMessage());
		}
	}


	protected String createEmailXml(Message message) throws Exception {
		MessageDocument  emailDoc = MessageDocument.Factory.newInstance();
		MessageDocument.Message emailMessage = emailDoc.addNewMessage();

		ProtocolAttributePair[] protocolAttributePairArray = new ProtocolAttributePair[ATTRIBUTE_MAX_SIZE];

		int i = 0;
		setMessageAttribute(Listener.METAINFO_PROTOCOL, Listener.METAINFO_PROTOCOL_EMAIL,protocolAttributePairArray,i);
		setMessageAttribute(MESSAGE_SUBJECT, message.getSubject(),protocolAttributePairArray,++i);

		String recipients = getAddressAsString(message.getAllRecipients());
		setMessageAttribute(MESSAGE_RECIPIENTS, recipients,protocolAttributePairArray,++i);

		String sender = getAddressAsString(message.getFrom());
		setMessageAttribute(MESSAGE_SENDER, sender,protocolAttributePairArray,++i);

		if(message.getSentDate() != null) {
			setMessageAttribute(MESSAGE_SENT_DATE, message.getSentDate().toString(),protocolAttributePairArray,++i);
		} else {
			setMessageAttribute(MESSAGE_SENT_DATE, Calendar.getInstance().getTime().toString(),protocolAttributePairArray,++i);
		}

		emailMessage.setMessageAttributePairArray(protocolAttributePairArray);


		Map<String,byte[]> attachmentMap = new HashMap<String,byte[]>();
		String emailBody = null;
		if (message.isMimeType("multipart/*")) {
			emailBody = handleMimeMessages(message,attachmentMap);
		} else {
			emailBody = (String)message.getContent();
		}
		if (! StringHelper.isEmpty(emailBody)) {
			emailMessage.setBody(emailBody);
		}

		if ( ! attachmentMap.isEmpty()){
			Attachment[] attachmentArray = new Attachment[attachmentMap.size()];
			int j = 0;
			for (String keyName : attachmentMap.keySet()) {
				Attachment attachment = Attachment.Factory.newInstance();
				String[] keyArray = keyName.split("~");
				attachment.setFileName(keyArray[0]);
				attachment.setAttachment(attachmentMap.get(keyName));
				if (keyArray.length > 1) {
					attachment.setContentType(keyArray[1]);
				}
				attachmentArray[j++] = attachment;
			}
			emailMessage.setAttachmentArray(attachmentArray);
		}

		return emailDoc.xmlText();
	}


	private static String getAddressAsString(Address[] addresses) {
		StringBuffer addresssb = new StringBuffer();
		for (Address address : addresses){
			String addressstr = address.toString();
			int startIndex = addressstr.indexOf("<");
			int endIndex = addressstr.indexOf(">");
			if (startIndex != -1 || endIndex != -1) {
				addressstr = addressstr.substring(startIndex+1, endIndex);
			}
			addresssb.append(addressstr);
			addresssb.append(";");
		}
		return addresssb.toString();
	}


	protected String handleMimeMessages(Message message,Map<String,byte[]> attachmentMap)
			throws Exception{

		String emailBody = null;
		Multipart multipart = (Multipart) message.getContent();
		for (int j = 0, m = multipart.getCount(); j < m; j++) {
			Part part = multipart.getBodyPart(j);
			String disposition = part.getDisposition();
			boolean isAttachment = false;
			if (disposition != null && (disposition.equals(Part.ATTACHMENT)
					|| (disposition.equals(Part.INLINE)))) {
				isAttachment = true;
				InputStream input = part.getInputStream();
				String fileName = part.getFileName();
				String contentType = part.getContentType();
				ByteArrayOutputStream output = new ByteArrayOutputStream();
				if (saveAttachment(input, output)) {
					byte[] encodedByte = Base64.encode(output.toByteArray());
					if (encodedByte != null && fileName != null && contentType != null) {
						attachmentMap.put(fileName+"~"+contentType, encodedByte);
					}
				}
			}
			if (!isAttachment && part.getContentType().startsWith("text/")){
				emailBody = (String) part.getContent();
			}
		}
		return emailBody;
	}

	protected boolean saveAttachment(InputStream input,OutputStream output) {
		byte[] buffer = new byte[4096];
		boolean isSuccessful = false;
		try {
			int buffread;
			while ((buffread = input.read(buffer)) != -1) {
				  output.write(buffer, 0, buffread);
			}
			output.flush();
			isSuccessful =true;
		} catch (Exception ex) {
			logger.severe(ex.getMessage());
		} finally {
			if (output != null) {
				try {
					output.close();
				} catch (IOException e) {
					logger.severe(e.getMessage());
				}
			}
		}
		return isSuccessful;
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
