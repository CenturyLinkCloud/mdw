/*
 * Copyright (C) 2018 CenturyLink, Inc.
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
package com.centurylink.mdw.sendgrid;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.cache.impl.AssetCache;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.constant.TaskAttributeConstant;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.task.TaskAction;
import com.centurylink.mdw.model.task.TaskRuntimeContext;
import com.centurylink.mdw.observer.ObserverException;
import com.centurylink.mdw.observer.task.TemplatedNotifier;
import com.centurylink.mdw.services.user.ContextEmailRecipients;
import com.centurylink.mdw.util.ParseException;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

@RegisteredService(com.centurylink.mdw.observer.task.TaskNotifier.class)
public class TaskNotifier extends TemplatedNotifier {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private TaskRuntimeContext context;
    private Asset template;

    @Override
    public void sendNotice(TaskRuntimeContext context, String taskAction, String outcome)
            throws ObserverException {
        // avoid nuisance notice to claimer and releaser
        if (TaskAction.CLAIM.equals(taskAction) || TaskAction.RELEASE.equals(taskAction)) {
            return;
        }

        this.context = context;
        this.template = AssetCache.getAsset(getTemplateSpec());
        if (template == null)
            throw new ObserverException("Template asset not found: " + getTemplateSpec());

        try {
            if (template.getLanguage().equals(Asset.HTML) || template.getLanguage().equals(Asset.TEXT)) {

                List<String> recipients = getRecipients(outcome);
                List<String> ccRecipients = getCcRecipients(outcome);
                if (recipients.isEmpty() && ccRecipients.isEmpty()) {
                    logger.warn("Warning: no email recipients for task: " + context.getTaskLogicalId());
                }
                else {
                    Email email = new EmailBuilder(template, context)
                            .from(getFrom())
                            .subject(getSubject(outcome))
                            .to(recipients.toArray(new String[0]))
                            .cc(ccRecipients.toArray(new String[0]))
                            .create();
                    new Sender(email).send();
                }
            }
            else if (template.getLanguage().equals(Asset.JSON)) {
                // Caller has built the message json themselves; simply apply substitutions.
                new Sender(context.evaluateToString(template.getStringContent())).send();
            }
            else {
                throw new ObserverException("Unsupported template format: " + template);
            }
        }
        catch (IOException ex) {
            throw new ObserverException("SendGrid error for task: " + context.getTaskInstanceId(), ex);
        }

    }

    protected String getProperty(String name) {
        return PropertyManager.getProperty(name);
    }

    protected String getSubject(String action) {
        return context.getTaskName();
    }

    protected String getFrom() {
        String from = getProperty(PropertyNames.TASK_NOTICE_EMAIL_FROM);
        if (from == null)
            from = "mdw@example.com";
        return from;
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
