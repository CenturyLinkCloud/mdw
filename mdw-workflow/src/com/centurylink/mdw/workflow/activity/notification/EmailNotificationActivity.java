/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.activity.notification;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.json.JSONObject;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.common.email.ProcessEmailModel;
import com.centurylink.mdw.common.email.TemplatedEmail;
import com.centurylink.mdw.common.exception.CachingException;
import com.centurylink.mdw.common.exception.MDWException;
import com.centurylink.mdw.common.exception.PropertyException;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.file.WildcardFilenameFilter;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.logger.StandardLogger.LogLevel;
import com.centurylink.mdw.common.utilities.timer.Tracked;
import com.centurylink.mdw.model.value.user.UserGroupVO;
import com.centurylink.mdw.model.value.variable.VariableInstanceInfo;
import com.centurylink.mdw.services.dao.user.cache.UserGroupCache;
import com.centurylink.mdw.services.task.TaskManagerAccess;
import com.centurylink.mdw.workflow.activity.DefaultActivityImpl;

/**
 * Activity for sending HTML email notifications.
 */
@Tracked(LogLevel.TRACE)
public class EmailNotificationActivity extends DefaultActivityImpl {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    @Override
    public void execute() throws ActivityException {

        String noticeType = getAttributeValue(WorkAttributeConstant.NOTICE_TYPE);

        if (noticeType == null || noticeType.equals(WorkAttributeConstant.NOTICE_TYPE_EMAIL)) {
            String groups = getAttributeValue(WorkAttributeConstant.NOTICE_GROUPS);
            String recipEmails = getAttributeValue(WorkAttributeConstant.NOTICE_RECIP_EMAILS);
            if (!StringHelper.isEmpty(recipEmails) && recipEmails.indexOf('@') < 0 && !recipEmails.startsWith("$")) {
                if (recipEmails.startsWith("prop:")) {
                    try {
                        recipEmails = getAttributeValueSmart(WorkAttributeConstant.NOTICE_RECIP_EMAILS);
                    }
                    catch (PropertyException e) {
                        throw new ActivityException("Notification activity requires recipient emails or groups attribute: " + this.getActivityName()
                                + "Property not found  = recipEmails" + recipEmails);
                    }
                }
                else
                    recipEmails = "$" + recipEmails;
            }
            if (StringHelper.isEmpty(groups) && StringHelper.isEmpty(recipEmails)) {
                throw new ActivityException("Notification activity requires recipient emails or groups attribute: " + this.getActivityName());
            }

            try {
                String fromAddress = getAttributeValueSmart(WorkAttributeConstant.NOTICE_FROM);
                if (fromAddress == null)
                    throw new ActivityException("Missing attribute: " + WorkAttributeConstant.NOTICE_FROM);
                String subject = getAttributeValueSmart(WorkAttributeConstant.NOTICE_SUBJECT);
                if (subject == null)
                    throw new ActivityException("Missing attribute: " + WorkAttributeConstant.NOTICE_SUBJECT);
                String templateName = getAttributeValueSmart(WorkAttributeConstant.NOTICE_TEMPLATE);
                if (templateName == null)
                    throw new ActivityException("Missing attribute: " + WorkAttributeConstant.NOTICE_TEMPLATE);

                TemplatedEmail templatedEmail = new TemplatedEmail();
                templatedEmail.setFromAddress(fromAddress);
                templatedEmail.setSubject(subject);
                templatedEmail.setHtml(true);
                templatedEmail.setTemplateName(templateName);
                templatedEmail.setModel(getTemplatedEmailModel());
                templatedEmail.setAttachments(getAttachments());
                templatedEmail.setRecipients(getRecipients(groups, recipEmails));
                templatedEmail.setCcRecipients(getCcRecipients());
                templatedEmail.setRuntimeContext(this.getRuntimeContext());

                JSONObject emailJson = templatedEmail.buildEmailJson();
                createDocument(JSONObject.class.getName(), emailJson, OwnerType.ACTIVITY_INSTANCE_REQUEST, getActivityInstanceId(), null, null);
                templatedEmail.sendEmail(emailJson);
            }
            catch (MessagingException ex) {
                logger.severeException(ex.getMessage(), ex);
                while (ex.getNextException() != null && ex.getNextException() instanceof MessagingException) {
                    ex = (MessagingException) ex.getNextException();
                    logger.severeException(ex.getMessage(), ex);
                }
                String continueDespite = getAttributeValue(WorkAttributeConstant.CONTINUE_DESPITE_MESSAGING_EXCEPTION);
                if (continueDespite == null || !Boolean.parseBoolean(continueDespite))
                    throw new ActivityException(-1, ex.getMessage(), ex);
            }
            catch (Exception ex) {
                logger.severeException(ex.getMessage(), ex);
                throw new ActivityException(-1, ex.getMessage(), ex);
            }
        }
    }

    /**
     * Default behavior returns the UNION of addresses specified by groups
     * along with those specified by the recipient variable value;
     * @throws AddressException
     */
    protected Address[] getRecipients(String groups, String recipEmails)
    	throws ActivityException, CachingException, AddressException {
        List<Address> recipients = new ArrayList<Address>();

        if (!StringHelper.isEmpty(groups)) {
            for (Address address : getGroupEmailAddresses(groups.split("#"))) {
                if (!recipients.contains(address))
                    recipients.add(address);
            }
        }
        if (!StringHelper.isEmpty(recipEmails)) {
        	if (recipEmails.startsWith("$")) {
        		for (Address address : getRecipientsFromVariable(recipEmails.substring(1).trim())) {
        			if (!recipients.contains(address))
        				recipients.add(address);
        		}
        	} else {
        		String[] emails = recipEmails.split("[;,] *");
        		for (String one : emails) {
        			try {
						Address address = new InternetAddress(one);
						if (!recipients.contains(address))
	        				recipients.add(address);
					} catch (AddressException e) {
						logger.severeException("Illegal email address - " + one, e);
					}
        		}
        	}
        }

        return recipients.toArray(new Address[0]);
    }

    /**
     * Default behavior is CC recipients driven by attributes.
     * @throws AddressException
     */
    protected Address[] getCcRecipients()
    	throws ActivityException, CachingException, AddressException {
        String groups = getAttributeValue(WorkAttributeConstant.CC_GROUPS);
        String recipEmails = getAttributeValue(WorkAttributeConstant.CC_EMAILS);
        if (!StringHelper.isEmpty(recipEmails) && recipEmails.indexOf('@') < 0 && !recipEmails.startsWith("$"))
        	recipEmails = "$" + recipEmails;
        if (StringHelper.isEmpty(groups) && StringHelper.isEmpty(recipEmails)) {
            return null;
        }
        else {
            return getRecipients(groups, recipEmails);
        }
    }

    private Address[] toMailAddresses(List<String> addressList) throws AddressException {
    	Address[] addresses = new Address[addressList.size()];
    	for (int i=0; i<addresses.length; i++) {
    		addresses[i] = new InternetAddress(addressList.get(i));
    	}
    	return addresses;
    }

    /**
     * Finds the relevant users for the specified group names
     * @return an array with the valid e-mail addresses
     * @throws AddressException
     */
    protected Address[] getGroupEmailAddresses(String[] groups)
    throws ActivityException, AddressException {
        try {
            return toMailAddresses(TaskManagerAccess.getInstance().getGroupEmailAddresses(groups));
        }
        catch (MDWException e) {
            logger.severeException(e.getMessage(), e);
            throw new ActivityException(-1, e.getMessage(), e);
        }
    }

    /**
     * Supports variable type of String or String[].
     * If a value corresponds to a group name, returns users in the group.
     * @throws AddressException
     */
    protected Address[] getRecipientsFromVariable(String varName)
    throws ActivityException, CachingException, AddressException {
        Object recipParam = getParameterValue(varName);
        if (recipParam == null) {
            throw new ActivityException("Recipient parameter '" + varName + "' value is null");
        }
//        String recipParamType = getParameterType(varName);
        VariableInstanceInfo var = super.getVariableInstance(varName);
        String recipParamType = var.getType();
        if (recipParamType.equals("java.lang.String")) {
            String recip = (String) recipParam;
            UserGroupVO group = null;
            try {
                group = UserGroupCache.getWorkgroup(recip);
            }
            catch (CachingException ex) {group=null;}
            if (group != null) {
                return getGroupEmailAddresses(new String[] { group.getName() });
            }
            else {
                int atIdx = recip.indexOf('@');
                try {
                  return new Address[]{new InternetAddress(atIdx > 0 ? recip : recip + "@centurylink.com")};
                }
                catch (AddressException ex) {
                    logger.severeException(ex.getMessage(), ex);
                    return new Address[0];
                }
            }
        }
        else if (recipParamType.equals("java.lang.String[]")) {
            List<Address> addresses = new ArrayList<Address>();
            String[] recips = (String[]) recipParam;
            for (String recip : recips) {
                UserGroupVO group = null;
                try {
                    group = UserGroupCache.getWorkgroup(recip);
                }
                catch (CachingException ex) {group=null;}
                if (group != null) {
                    for (Address address : getGroupEmailAddresses(new String[] { group.getName() })) {
                        if (!addresses.contains(address))
                            addresses.add(address);
                    }
                }
                else {
                    int atIdx = recip.indexOf('@');
                    try {
                      Address address = new InternetAddress(atIdx > 0 ? recip : recip + "@centurylink.com");
                      if (!addresses.contains(address))
                        addresses.add(address);
                    }
                    catch (AddressException ex) {
                        logger.severeException(ex.getMessage(), ex);
                    }
                }
            }
            return addresses.toArray(new Address[0]);
        }
        else {
            throw new ActivityException("Unsupported variable type for recipient address(es): " + recipParamType);
        }
    }

    protected ProcessEmailModel getTemplatedEmailModel() throws ActivityException {
        return new ProcessEmailModel(getProcessInstance(), new HashMap<String,Object>() {
            private static final long serialVersionUID = 1L;
            @Override
            public Object get(Object key) {
                try {
                    return getVariableValue(key.toString());
                }
                catch (ActivityException ex) {
                    logexception(ex.getMessage(), ex);
                    return null;
                }
            }
        });
    }

    protected Map<String,File> getAttachments() throws PropertyException {
        Map<String,File> attachments = new HashMap<String,File>();

        String attachmentPatterns = getAttributeValue(WorkAttributeConstant.NOTICE_ATTACHMENTS);
        if (!StringHelper.isEmpty(attachmentPatterns)) {

            // master req id directory and subdirs
            List<File> dirs = new ArrayList<File>();

            String rootDir = getProperty(PropertyNames.ATTACHMENTS_STORAGE_LOCATION);
            if (!rootDir.endsWith("/"))
                rootDir += "/";
            File mriDir = new File(rootDir + getMasterRequestId());
            if (mriDir.exists() && mriDir.isDirectory()) {
                dirs.add(mriDir);
                for (File subFile : mriDir.listFiles()) {
                    if (subFile.isDirectory())
                        dirs.add(subFile);
                }
            }

            String[] patterns = attachmentPatterns.split("~");
            for (String pattern : patterns) {
                for (File dir : dirs) {
                    File[] files = dir.listFiles(new WildcardFilenameFilter(pattern));
                    for (File attachment : files)
                        attachments.put(attachment.getName(), attachment);
                }
            }
        }

        return attachments;
    }
}
