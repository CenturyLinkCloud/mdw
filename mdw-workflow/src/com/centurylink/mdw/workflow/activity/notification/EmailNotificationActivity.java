/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.workflow.activity.notification;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.json.JSONObject;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.activity.types.NotificationActivity;
import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.cache.CachingException;
import com.centurylink.mdw.common.MdwException;
import com.centurylink.mdw.config.PropertyException;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.constant.WorkAttributeConstant;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.email.ProcessEmailModel;
import com.centurylink.mdw.email.TemplatedEmail;
import com.centurylink.mdw.model.user.Workgroup;
import com.centurylink.mdw.model.variable.VariableInstance;
import com.centurylink.mdw.service.data.task.UserGroupCache;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.UserException;
import com.centurylink.mdw.services.UserManager;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.file.WildcardFilenameFilter;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.workflow.activity.DefaultActivityImpl;

/**
 * Activity for sending HTML email notifications.
 */
@Tracked(LogLevel.TRACE)
public class EmailNotificationActivity extends DefaultActivityImpl implements NotificationActivity {

    private static final String RECIPIENTS_EXPRESSION = "RecipientsExpression";

    @Override
    public void execute() throws ActivityException {

        String noticeType = getAttributeValue(WorkAttributeConstant.NOTICE_TYPE);
        if (noticeType == null || noticeType.equals(WorkAttributeConstant.NOTICE_TYPE_EMAIL))
            sendNotices();
        else
            throw new ActivityException("Unsupported email notice type: " + noticeType);
    }

    public void sendNotices() throws ActivityException {

        try {
            String groups = getAttributeValue(WorkAttributeConstant.NOTICE_GROUPS);
            String recipEmails = getAttributeValueSmart(RECIPIENTS_EXPRESSION);
            String ccGroups = getAttributeValue(WorkAttributeConstant.CC_GROUPS);

            String fromAddress = getAttributeValueSmart(WorkAttributeConstant.NOTICE_FROM);
            if (fromAddress == null)
                throw new ActivityException("Missing attribute: " + WorkAttributeConstant.NOTICE_FROM);
            String subject = getAttributeValueSmart(WorkAttributeConstant.NOTICE_SUBJECT);
            if (subject == null)
                throw new ActivityException("Missing attribute: " + WorkAttributeConstant.NOTICE_SUBJECT);
            String templateName = getAttributeValueSmart(WorkAttributeConstant.NOTICE_TEMPLATE);
            if (templateName == null)
                throw new ActivityException("Missing attribute: " + WorkAttributeConstant.NOTICE_TEMPLATE);

            Address[] recipAddresses = getRecipients(groups, recipEmails);
            Address[] ccAddresses = getRecipients(ccGroups, null);

            if (recipAddresses.length == 0 && ccAddresses.length == 0) {
                logwarn(" ** Warning: no email recipients found");
                return;
            }

            TemplatedEmail templatedEmail = new TemplatedEmail();
            templatedEmail.setFromAddress(fromAddress);
            templatedEmail.setSubject(subject);
            templatedEmail.setHtml(true);
            templatedEmail.setTemplateName(templateName);
            templatedEmail.setModel(getTemplatedEmailModel());
            templatedEmail.setAttachments(getAttachments());
            templatedEmail.setRecipients(recipAddresses);
            templatedEmail.setCcRecipients(ccAddresses);
            templatedEmail.setRuntimeContext(getRuntimeContext());

            JSONObject emailJson = templatedEmail.buildEmailJson();
            createDocument(JSONObject.class.getName(), emailJson, OwnerType.NOTIFICATION_ACTIVITY, getActivityInstanceId());
            templatedEmail.sendEmail(emailJson);
        }
        catch (MessagingException ex) {
            logexception(ex.getMessage(), ex);
            while (ex.getNextException() != null && ex.getNextException() instanceof MessagingException) {
                ex = (MessagingException) ex.getNextException();
                logexception(ex.getMessage(), ex);
            }
            String continueDespite = getAttributeValue(WorkAttributeConstant.CONTINUE_DESPITE_MESSAGING_EXCEPTION);
            if (continueDespite == null || !Boolean.parseBoolean(continueDespite))
                throw new ActivityException(-1, ex.getMessage(), ex);
        }
        catch (Exception ex) {
            logexception(ex.getMessage(), ex);
            throw new ActivityException(-1, ex.getMessage(), ex);
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
            for (Address address : getGroupEmailAddresses(StringHelper.parseList(groups))) {
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
    protected Address[] getGroupEmailAddresses(List<String> groups)
    throws ActivityException, AddressException {
        try {
            return toMailAddresses(getGroupEmails(groups));
        }
        catch (MdwException e) {
            logger.severeException(e.getMessage(), e);
            throw new ActivityException(-1, e.getMessage(), e);
        }
    }

    public List<String> getGroupEmails(List<String> groups) throws UserException, DataAccessException {
        UserManager userManager = ServiceLocator.getUserManager();
        return userManager.getEmailAddressesForGroups(groups);
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
        VariableInstance var = super.getVariableInstance(varName);
        String recipParamType = var.getType();
        if (recipParamType.equals("java.lang.String")) {
            String recip = (String) recipParam;
            Workgroup group = null;
            try {
                group = UserGroupCache.getWorkgroup(recip);
            }
            catch (CachingException ex) {group=null;}
            if (group != null) {
                return getGroupEmailAddresses(Arrays.asList(new String[]{group.getName()}));
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
                Workgroup group = null;
                try {
                    group = UserGroupCache.getWorkgroup(recip);
                }
                catch (CachingException ex) {group=null;}
                if (group != null) {
                    for (Address address : getGroupEmailAddresses(Arrays.asList(new String[]{group.getName()}))) {
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

            File mriDir = new File(ApplicationContext.getAttachmentsDirectory() + getMasterRequestId());
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
