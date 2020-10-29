package com.centurylink.mdw.services.user;

import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.user.Workgroup;
import com.centurylink.mdw.model.workflow.RuntimeContext;
import com.centurylink.mdw.service.data.user.UserGroupCache;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.util.ParseException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ContextEmailRecipients {

    private RuntimeContext context;
    public ContextEmailRecipients(RuntimeContext context) {
        this.context = context;
    }

    /**
     * Default behavior returns the UNION of addresses specified via the expression attribute
     * along with those specified by the designated workgroups attribute.
     */
    public List<String> getRecipients(String workgroupsAttr, String expressionAttr)
    throws DataAccessException, ParseException {
        List<String> recipients = new ArrayList<>();
        if (workgroupsAttr != null) {
            List<String> groupList = context.getAttributes().getList(workgroupsAttr);
            for (String groupEmail : getGroupEmails(groupList)) {
                if (!recipients.contains(groupEmail))
                    recipients.add(groupEmail);
            }
        }
        if (expressionAttr != null) {
            String expression = context.getAttribute(expressionAttr);
            if (expression != null && !expression.isEmpty()) {
                List<String> expressionEmails;
                if (expression.indexOf("${") >= 0)
                    expressionEmails = getRecipientsFromExpression(expression);
                else
                    expressionEmails = Arrays.asList(expression.split(","));
                for (String expressionEmail : expressionEmails) {
                    if (!recipients.contains(expressionEmail))
                        recipients.add(expressionEmail);
                }
            }
        }
        return recipients;
    }

    /**
     * Must resolve to type of String or List<String>.
     * If a value corresponds to a group name, returns users in the group.
     */
    public List<String> getRecipientsFromExpression(String expression)
    throws DataAccessException, ParseException {
        Object recip = context.evaluate(expression);
        if (recip == null) {
            context.logWarn("Warning: Recipient expression '" + expression + "' resolves to null");
        }

        List<String> recips = new ArrayList<>();
        if (recip instanceof String) {
            if (!((String)recip).isEmpty()) {  // null list evaluates to empty string (why?)
                Workgroup group = UserGroupCache.getWorkgroup((String)recip);
                if (group != null)
                    recips.addAll(getGroupEmails(Arrays.asList(new String[]{group.getName()})));
                else
                    recips.add((String)recip);
            }
        }
        else if (recip instanceof List) {
            for (Object email : (List)recip) {
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

    public List<String> getGroupEmails(List<String> groups) throws DataAccessException {
        return ServiceLocator.getUserServices().getWorkgroupEmails(groups);
    }
}
