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

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.activity.types.NotificationActivity;
import com.centurylink.mdw.annotations.Activity;
import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.cache.impl.AssetCache;
import com.centurylink.mdw.config.PropertyException;
import com.centurylink.mdw.constant.WorkAttributeConstant;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.email.TemplatedEmail;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.asset.AssetVersionSpec;
import com.centurylink.mdw.model.workflow.ActivityRuntimeContext;
import com.centurylink.mdw.services.user.ContextEmailRecipients;
import com.centurylink.mdw.util.ParseException;
import com.centurylink.mdw.util.file.WildcardFilenameFilter;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.workflow.activity.DefaultActivityImpl;
import org.apache.commons.lang.StringUtils;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Activity for sending HTML email notifications.
 */
@Tracked(LogLevel.TRACE)
@Activity(value="Email Notification", category=NotificationActivity.class, icon="com.centurylink.mdw.base/notice.gif",
        pagelet="com.centurylink.mdw.base/emailNotification.pagelet")
public class EmailNotificationActivity extends DefaultActivityImpl implements NotificationActivity {

    @Override
    public Object execute(ActivityRuntimeContext runtimeContext) throws ActivityException {
        sendNotices();
        return null;
    }

    @Override
    public void sendNotices() throws ActivityException {
        try {
            String noticeType = getAttributeValue(WorkAttributeConstant.NOTICE_TYPE);

            String fromAddress = getAttributeValueSmart(WorkAttributeConstant.NOTICE_FROM);
            if (fromAddress == null)
                throw new ActivityException("Missing attribute: " + WorkAttributeConstant.NOTICE_FROM);
            String subject = getAttributeValueSmart(WorkAttributeConstant.NOTICE_SUBJECT);
            if (subject == null)
                throw new ActivityException("Missing attribute: " + WorkAttributeConstant.NOTICE_SUBJECT);
            String templateName = getAttributeValueSmart(WorkAttributeConstant.NOTICE_TEMPLATE);
            if (templateName == null)
                throw new ActivityException("Missing attribute: " + WorkAttributeConstant.NOTICE_TEMPLATE);
            String templateVersion = getAttributeValue(WorkAttributeConstant.NOTICE_TEMPLATE + "_assetVersion");
            AssetVersionSpec spec = new AssetVersionSpec(templateName, templateVersion == null ? "0" : templateVersion);
            Asset template = AssetCache.getAsset(spec);
            if (template == null)
                throw new ActivityException("No template asset found: " + spec);

            String priority = getAttributeValue(WorkAttributeConstant.NOTICE_PRIORITY);

            ActivityRuntimeContext context = getRuntimeContext();
            List<Address> recipAddresses = getRecipientAddresses(context);
            List<Address> ccAddresses = getCcRecipientAddresses(context);
            if (recipAddresses.isEmpty() && ccAddresses.isEmpty()) {
                logWarn("Warning: no email recipients");
                return;
            }

            if (noticeType == null || noticeType.equals("E-Mail") || noticeType.equals(WorkAttributeConstant.EMAIL_NOTICE_SMTP)) {
                TemplatedEmail templatedEmail = new TemplatedEmail();
                templatedEmail.setFromAddress(fromAddress);
                templatedEmail.setSubject(subject);
                templatedEmail.setTemplateAssetVerSpec(spec);
                templatedEmail.setHtml(template.getLanguage().equals(Asset.HTML));
                templatedEmail.setAttachments(getAttachments());
                templatedEmail.setRecipients(recipAddresses.toArray(new Address[0]));
                templatedEmail.setCcRecipients(ccAddresses.toArray(new Address[0]));
                templatedEmail.setRuntimeContext(getRuntimeContext());
                templatedEmail.setPriority(priority);
                templatedEmail.sendEmail();
            }
            else {
                throw new ActivityException("Unsupported email notice type: " + noticeType);
            }

        }
        catch (MessagingException ex) {
            logError(ex.getMessage(), ex);
            while (ex.getNextException() != null && ex.getNextException() instanceof MessagingException) {
                ex = (MessagingException) ex.getNextException();
                logError(ex.getMessage(), ex);
            }
            String continueDespite = getAttributeValue(WorkAttributeConstant.CONTINUE_DESPITE_MESSAGING_EXCEPTION);
            if (continueDespite == null || !Boolean.parseBoolean(continueDespite))
                throw new ActivityException(-1, ex.getMessage(), ex);
        }
        catch (Exception ex) {
            logError(ex.getMessage(), ex);
            throw new ActivityException(-1, ex.getMessage(), ex);
        }
    }

    protected List<Address> getRecipientAddresses(ActivityRuntimeContext context)
    throws MessagingException {
        List<Address> addresses = new ArrayList<>();
        ContextEmailRecipients contextRecipients = new ContextEmailRecipients(context);
        try {
            List<String> emails = contextRecipients.getRecipients(WorkAttributeConstant.NOTICE_GROUPS,
                    WorkAttributeConstant.RECIPIENTS_EXPRESSION);
            for (String email : emails) {
                addresses.add(new InternetAddress(email));
            }
            return addresses;
        }
        catch (DataAccessException | ParseException ex) {
            throw new MessagingException(ex.getMessage(), ex);
        }
    }

    protected List<Address> getCcRecipientAddresses(ActivityRuntimeContext context)
    throws MessagingException {
        List<Address> addresses = new ArrayList<>();
        ContextEmailRecipients contextRecipients = new ContextEmailRecipients(context);
        try {
            List<String> emails = contextRecipients.getRecipients(WorkAttributeConstant.CC_GROUPS, null);
            for (String email : emails) {
                addresses.add(new InternetAddress(email));
            }
            return addresses;
        }
        catch (DataAccessException | ParseException ex) {
            throw new MessagingException(ex.getMessage(), ex);
        }
    }

    protected Map<String,File> getAttachments() throws PropertyException {
        Map<String,File> attachments = new HashMap<>();

        String attachmentPatterns = getAttributeValue(WorkAttributeConstant.NOTICE_ATTACHMENTS);
        if (!StringUtils.isBlank(attachmentPatterns)) {

            // master req id directory and subdirs
            List<File> dirs = new ArrayList<>();

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
