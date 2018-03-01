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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.cache.impl.AssetCache;
import com.centurylink.mdw.common.MdwException;
import com.centurylink.mdw.constant.WorkAttributeConstant;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.asset.AssetVersionSpec;
import com.centurylink.mdw.model.user.Workgroup;
import com.centurylink.mdw.model.workflow.ActivityRuntimeContext;
import com.centurylink.mdw.service.data.task.UserGroupCache;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.util.ExpressionUtil;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.workflow.activity.DefaultActivityImpl;

/**
 * Send email notices using the SendGrid API.
 * Requires a SendGrid account with a registered API key.
 */
@Tracked(LogLevel.TRACE)
public class SendgridActivity extends DefaultActivityImpl {

    @Override
    public Object execute(ActivityRuntimeContext context) throws ActivityException {
        String templateName = getAttributeValueSmart(WorkAttributeConstant.NOTICE_TEMPLATE);
        if (templateName == null)
            throw new ActivityException("Missing attribute: " + WorkAttributeConstant.NOTICE_TEMPLATE);
        String templateVersion = getAttributeValue(WorkAttributeConstant.NOTICE_TEMPLATE + "_assetVersion");
        AssetVersionSpec spec = new AssetVersionSpec(templateName, templateVersion == null ? "0" : templateVersion);
        Asset template = AssetCache.getAsset(spec);
        if (template == null)
            throw new ActivityException("No template asset found: " + spec);
        
        try {
            send(template, context);
        }
        catch (MdwException | IOException ex) {
            logexception(ex.getMessage(), ex);
            if (!"true".equalsIgnoreCase(getAttributeValueSmart(WorkAttributeConstant.CONTINUE_DESPITE_MESSAGING_EXCEPTION))) {
                throw new ActivityException(ex.getMessage(), ex);
            }
        }
        return null;
    }
    
    protected void send(Asset template, ActivityRuntimeContext context) 
    throws ActivityException, MdwException, IOException {
        if (template.getLanguage().equals(Asset.HTML) || template.getLanguage().equals(Asset.TEXT)) {
            String fromEmail = getAttributeValueSmart(WorkAttributeConstant.NOTICE_FROM);
            if (fromEmail == null)
                throw new ActivityException("Missing attribute: " + WorkAttributeConstant.NOTICE_FROM);
            String subject = getAttributeValueSmart(WorkAttributeConstant.NOTICE_SUBJECT);
            if (subject == null)
                throw new ActivityException("Missing attribute: " + WorkAttributeConstant.NOTICE_SUBJECT);

            List<String> recipients = new ArrayList<>();

            String recipEmails = getAttributeValueSmart(WorkAttributeConstant.RECIPIENTS_EXPRESSION);
            if (!StringHelper.isEmpty(recipEmails)) {
                if (recipEmails.indexOf("${") >= 0)
                    recipients.addAll(getRecipientsFromExpression(recipEmails));
                else
                    recipients.addAll(Arrays.asList(recipEmails.split(",")));
            }   
            
            String groups = getAttributeValue(WorkAttributeConstant.NOTICE_GROUPS);
            if (!StringHelper.isEmpty(groups)) {
                for (String groupEmail : getGroupEmails(StringHelper.parseList(groups))) {
                    if (!recipients.contains(groupEmail))
                        recipients.add(groupEmail);
                }
            }
            
            List<String> ccRecipients = new ArrayList<>();
            String ccGroups = getAttributeValue(WorkAttributeConstant.CC_GROUPS);
            if (!StringHelper.isEmpty(ccGroups)) {
                ccRecipients.addAll(getGroupEmails(StringHelper.parseList(groups)));
            }
            
            if (recipients.isEmpty() && ccRecipients.isEmpty()) {
                logwarn("Warning: no email recipients");
            }
            else {
                // build the sendgrid model object
                Email email = new Email();
                email.setFrom(new Address(fromEmail));
                email.setPersonalizations(new ArrayList<>());
                Personalization personalization = new Personalization();
                email.getPersonalizations().add(personalization);
                personalization.setSubject(subject);
                if (!recipients.isEmpty()) {
                    List<Address> to = new ArrayList<>();
                    personalization.setTo(to);
                    for (String recipient : recipients) {
                        to.add(new Address(recipient));
                    }
                }
                if (!ccRecipients.isEmpty()) {
                    List<Address> cc = new ArrayList<>();
                    personalization.setCc(cc);
                    for (String ccRecipient : ccRecipients) {
                        cc.add(new Address(ccRecipient));
                    }
                }
                List<Message> content = new ArrayList<>();
                email.setContent(content);
                Message message = new Message();
                message.setType(template.getContentType());
                String body = template.getStringContent();
                Map<String,String> images = new HashMap<>();
                body = ExpressionUtil.substitute(body, new Object(), images, true);
                if (!images.isEmpty()) {
                    for (String cid : images.keySet()) {
                        String imageFile = images.get(cid);
                        Asset asset = AssetCache.getAsset(imageFile);
                        if (asset.exists()) {
                            Attachment attachment = new Attachment();
                            if (email.getAttachments() == null) {
                                email.setAttachments(new ArrayList<>());
                            }
                            email.getAttachments().add(attachment);
                            attachment.setContent_id(cid);
                            attachment.setFilename(imageFile);
                            attachment.setType(asset.getContentType());
                            attachment.setContent(Base64.getEncoder().encodeToString(asset.getRawContent()));
                        }
                        else {
                            logwarn("Image asset not found: " + imageFile);
                        }
                    }
                }
                message.setValue(context.evaluateToString(body));
                new Sender(email).send();
            }
        }
        else if (template.getLanguage().equals(Asset.JSON)) {
            // Caller has built the message json themselves; simply apply substitutions.
            new Sender(context.evaluateToString(template.getStringContent())).send();
        }
        else {
            throw new ActivityException("Unsupported template format: " + template);
        }
    }
    
    protected List<String> getGroupEmails(List<String> groups) throws ActivityException {
        try {
            return ServiceLocator.getUserServices().getWorkgroupEmails(groups);
        }
        catch (DataAccessException ex) {
            throw new ActivityException(ex.getMessage(), ex);
        }
    }
    
    /**
     * Must resolve to type of String or List<String>.
     * If a value corresponds to a group name, returns users in the group.
     */
    protected List<String> getRecipientsFromExpression(String expression) throws ActivityException {
        Object recip = getValue(expression);
        if (recip == null) {
            logwarn("Warning: Recipient expression '" + expression + "' resolves to null");
        }
        
        List<String> recips = new ArrayList<>();
        if (recip instanceof String) {
            Workgroup group = UserGroupCache.getWorkgroup((String)recip);
            if (group != null)
                recips.addAll(getGroupEmails(Arrays.asList(new String[]{group.getName()})));
            else
                recips.add((String)recip);
        }
        else if (recip instanceof List) {
            for (Object email : recips) {
                Workgroup group = UserGroupCache.getWorkgroup(email.toString());
                if (group != null) {
                    for (String groupEmail : getGroupEmails(Arrays.asList(new String[]{group.getName()}))) {
                        if (!recips.contains(groupEmail))
                            recips.add(groupEmail);
                    }
                }
                else {
                    if (!recips.contains(email))
                        recips.add(email.toString());
                }
            }
        }
        else {
            throw new ActivityException("Recipient expression resolved to unsupported type: " + expression + ": " + recip);
        }
        return recips;
    }
    
}
