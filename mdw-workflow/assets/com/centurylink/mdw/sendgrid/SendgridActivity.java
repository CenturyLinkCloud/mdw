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
import java.util.List;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.cache.impl.AssetCache;
import com.centurylink.mdw.common.MdwException;
import com.centurylink.mdw.constant.WorkAttributeConstant;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.asset.AssetVersionSpec;
import com.centurylink.mdw.model.workflow.ActivityRuntimeContext;
import com.centurylink.mdw.services.user.ContextEmailRecipients;
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

            try {
                ContextEmailRecipients contextRecipients = new ContextEmailRecipients(context);
                List<String> recipients = contextRecipients.getRecipients(WorkAttributeConstant.NOTICE_GROUPS,
                        WorkAttributeConstant.RECIPIENTS_EXPRESSION);
                List<String> ccRecipients = contextRecipients.getRecipients(WorkAttributeConstant.CC_GROUPS, null);
                if (recipients.isEmpty() && ccRecipients.isEmpty()) {
                    logwarn("Warning: no email recipients");
                }
                else {
                    Email email = new EmailBuilder(template, context)
                            .from(fromEmail)
                            .subject(subject)
                            .to(recipients.toArray(new String[0]))
                            .cc(ccRecipients.toArray(new String[0]))
                            .create();
                    new Sender(email).send();
                }
            }
            catch (DataAccessException ex) {
                throw new ActivityException(ex.getMessage(), ex);
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
}
