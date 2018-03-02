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
package com.centurylink.mdw.services.user;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.user.Workgroup;
import com.centurylink.mdw.model.workflow.RuntimeContext;
import com.centurylink.mdw.service.data.task.UserGroupCache;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.util.ParseException;
import com.centurylink.mdw.util.StringHelper;

public class ContextEmailRecipients {

    private RuntimeContext context;
    public ContextEmailRecipients(RuntimeContext context) {
        this.context = context;
    }

    /**
     * Default behavior returns the UNION of addresses specified via the expression attribute
     * along with those specified by the designated workgroups attribute.
     */
    public List<String> getRecipients(String expressionAttr, String workgroupsAttr)
    throws DataAccessException, ParseException {
        List<String> recipients = new ArrayList<>();
        String expression = context.getAttribute(expressionAttr);
        if (!StringHelper.isEmpty(expression)) {
            if (expression.indexOf("${") >= 0)
                recipients.addAll(getRecipientsFromExpression(expression));
            else
                recipients.addAll(Arrays.asList(expression.split(",")));
        }
        if (workgroupsAttr != null) {
            String workgroups = context.getAttribute(workgroupsAttr);
            if (workgroups != null) {
                for (String groupEmail : getGroupEmails(StringHelper.parseList(workgroups))) {
                    if (!recipients.contains(groupEmail))
                        recipients.add(groupEmail);
                }
            }
        }
        return recipients;
    }

    /**
     * Must resolve to type of String or List<String>.
     * If a value corresponds to a group name, returns users in the group.
     */
    protected List<String> getRecipientsFromExpression(String expression)
    throws DataAccessException, ParseException {
        Object recip = context.evaluate(expression);
        if (recip == null) {
            context.logWarn("Warning: Recipient expression '" + expression + "' resolves to null");
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
            throw new ParseException("Recipient expression resolved to unsupported type: " + expression + ": " + recip);
        }
        return recips;
    }

    protected List<String> getGroupEmails(List<String> groups) throws DataAccessException {
        return ServiceLocator.getUserServices().getWorkgroupEmails(groups);
    }
}
