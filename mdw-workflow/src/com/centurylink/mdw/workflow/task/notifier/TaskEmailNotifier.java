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
package com.centurylink.mdw.workflow.task.notifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import com.centurylink.mdw.cache.impl.AssetCache;
import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.constant.TaskAttributeConstant;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.email.TemplatedEmail;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.task.TaskAction;
import com.centurylink.mdw.model.task.TaskInstance;
import com.centurylink.mdw.model.task.TaskRuntimeContext;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.observer.ObserverException;
import com.centurylink.mdw.observer.task.TemplatedNotifier;
import com.centurylink.mdw.services.user.ContextEmailRecipients;
import com.centurylink.mdw.util.MiniCrypter;
import com.centurylink.mdw.util.ParseException;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class TaskEmailNotifier extends TemplatedNotifier {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private TaskRuntimeContext context;

    public void sendNotice(TaskRuntimeContext runtimeContext, String taskAction, String outcome)
    throws ObserverException {
        this.context = runtimeContext;
        if (taskAction == null ||
                (!taskAction.equals(TaskAction.CLAIM) && !taskAction.equals(TaskAction.RELEASE))) {
            // avoid nuisance notice to claimer and releaser
            sendEmail(runtimeContext, outcome);
        }
    }

    protected String getFromAddress() {
        String fromAddress = getProperty(PropertyNames.TASK_NOTICE_EMAIL_FROM);
        if (fromAddress == null)
            fromAddress = "mdw@example.com";
        return fromAddress;
    }

    protected String getProperty(String name) {
        try {
            Asset template = null;
            if (getTemplateSpec() != null)
                template = AssetCache.getAsset(getTemplateSpec());
            if (template == null || template.getId() == 0L)
                return PropertyManager.getProperty(name);
            else {
                Package pkg = PackageCache.getAssetPackage(template.getId());
                if (pkg == null)
                    return PropertyManager.getProperty(name);
                else
                    return pkg.getProperty(name);
            }
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            return null;
        }
    }

    protected String getSubject(TaskInstance taskInstance, String outcome) {
        return taskInstance.getTaskName() + " ";
    }

    /**
     * Creates the mime message for the e-mail.
     * @param taskInstance the task instance
     * @param outcome the resulting task status or state
     * @return the message
     */
    protected void sendEmail(TaskRuntimeContext context, String outcome)
    throws ObserverException {
        TaskInstance taskInstance = context.getTaskInstance();
        TemplatedEmail templatedEmail = new TemplatedEmail();
        templatedEmail.setFromAddress(getFromAddress());
        templatedEmail.setSubject(getSubject(taskInstance, outcome));
        templatedEmail.setHtml(true);
        templatedEmail.setTemplateAssetVerSpec(getTemplateSpec());
        templatedEmail.setRuntimeContext(context);

        try {
            List<Address> recipients = getRecipientAddresses(outcome);
            List<Address> ccRecipients = getCcRecipientAddresses(outcome);

            if (templatedEmail.getTemplateBody().contains("${taskActionUrl}")) {

                // send individual e-mails
                for (Address recip : recipients) {
                    String cuid = recip.toString().substring(0, recip.toString().indexOf('@'));
                    String userIdentifier = MiniCrypter.encrypt(cuid);
                    taskInstance.setUserIdentifier(userIdentifier);
                    templatedEmail.setRecipients(new Address[]{recip});
                    templatedEmail.setCcRecipients(new Address[0]);
                    try {
                        templatedEmail.sendEmail();
                    }
                    catch (MessagingException ex) {
                        logger.severeException(ex.getMessage(), ex);  // do not rethrow
                    }
                }
                if (ccRecipients != null) {
                    for (Address ccRecip : ccRecipients) {
                        String cuid = ccRecip.toString().substring(0, ccRecip.toString().indexOf('@'));
                        String userIdentifier = MiniCrypter.encrypt(cuid);
                        taskInstance.setUserIdentifier(userIdentifier);
                        templatedEmail.setRecipients(new Address[0]);
                        templatedEmail.setCcRecipients(new Address[]{ccRecip});
                        try {
                            templatedEmail.sendEmail();
                        }
                        catch (MessagingException ex) {
                            logger.severeException(ex.getMessage(), ex);  // do not rethrow
                        }
                    }
                }
            }
            else {
                if ((recipients != null && !recipients.isEmpty()) || (ccRecipients != null && !ccRecipients.isEmpty())) {
                    if (recipients != null)
                        templatedEmail.setRecipients(recipients.toArray(new Address[0]));
                    if (ccRecipients != null)
                        templatedEmail.setCcRecipients(ccRecipients.toArray(new Address[0]));
                    try {
                        templatedEmail.sendEmail();
                    }
                    catch (MessagingException ex) {
                        logger.severeException(ex.getMessage(), ex);  // do not rethrow
                    }
                }
                else {
                    logger.warn("WARNING: No email recipients for task " + context.getTaskInstanceId() + " " + outcome);
                }
            }
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new ObserverException(-1, ex.getMessage(), ex);
        }
    }

    protected List<Address> getRecipientAddresses(String outcome)
    throws ObserverException, AddressException {
        List<Address> addresses = new ArrayList<>();
        for (String email : getRecipients(outcome)) {
            addresses.add(new InternetAddress(email));
        }
        return addresses;
    }

    protected List<Address> getCcRecipientAddresses(String outcome)
    throws ObserverException, AddressException {
        List<Address> addresses = new ArrayList<>();
        for (String email : getCcRecipients(outcome)) {
            addresses.add(new InternetAddress(email));
        }
        return addresses;
    }

    /**
     * Default implementation: If outcome is "Assigned", send only to assignee's email.
     * Otherwise, if Notice Groups attribute is present, the groups so specified
     * are unioned with any recipients resulting from evaluation of the Recipients Expression attribute.
     * If no Notice Groups are specified, any workgroups associated with the task are added to the
     * Recipients Expression outcome.
     */
    protected List<String> getRecipients(String outcome) throws ObserverException {
        if ("Assigned".equals(outcome)) {
            // send e-mail only to assignee
            return Arrays.asList(new String[]{context.getAssignee().getEmail()});
        }
        else {
            try {
                ContextEmailRecipients contextRecipients = new ContextEmailRecipients(context);
                String groups = context.getAttribute(TaskAttributeConstant.NOTICE_GROUPS);
                if (groups != null && !groups.isEmpty() && !groups.equals("[]")) {
                    return contextRecipients.getRecipients(TaskAttributeConstant.NOTICE_GROUPS,
                            TaskAttributeConstant.RECIPIENT_EMAILS);
                }
                else {
                    List<String> emails = contextRecipients.getGroupEmails(context.getTaskInstance().getWorkgroups());
                    for (String email : contextRecipients.getRecipients(null, TaskAttributeConstant.RECIPIENT_EMAILS)) {
                        if (!emails.contains(email))
                            emails.add(email);
                    }
                    return emails;
                }
            }
            catch (DataAccessException | ParseException ex) {
                throw new ObserverException(ex.getMessage(), ex);
            }
        }
    }

    protected List<String> getCcRecipients(String outcome) throws ObserverException {
        if ("Assigned".equals(outcome)) {
            // send e-mail only to assignee
            return Arrays.asList(new String[]{context.getAssignee().getEmail()});
        }
        else {
            try {
                ContextEmailRecipients contextRecipients = new ContextEmailRecipients(context);
                return contextRecipients.getRecipients(TaskAttributeConstant.CC_GROUPS, null);
            }
            catch (DataAccessException | ParseException ex) {
                throw new ObserverException(ex.getMessage(), ex);
            }
        }
    }
}
