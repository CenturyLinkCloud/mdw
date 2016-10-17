/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.task.notifier;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.json.JSONObject;

import com.centurylink.mdw.common.cache.impl.PackageVOCache;
import com.centurylink.mdw.common.cache.impl.TemplateCache;
import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.constant.TaskAttributeConstant;
import com.centurylink.mdw.common.email.TaskEmailModel;
import com.centurylink.mdw.common.email.Template;
import com.centurylink.mdw.common.email.TemplatedEmail;
import com.centurylink.mdw.common.exception.CachingException;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.exception.MDWException;
import com.centurylink.mdw.common.exception.ObserverException;
import com.centurylink.mdw.common.utilities.CryptUtil;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.model.data.task.TaskAction;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.task.TaskInstanceVO;
import com.centurylink.mdw.model.value.task.TaskRuntimeContext;
import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.model.value.user.UserGroupVO;
import com.centurylink.mdw.model.value.variable.DocumentReference;
import com.centurylink.mdw.model.value.variable.DocumentVO;
import com.centurylink.mdw.model.value.variable.VariableInstanceInfo;
import com.centurylink.mdw.observer.task.TemplatedNotifier;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.services.dao.process.cache.ProcessVOCache;
import com.centurylink.mdw.services.dao.task.cache.TaskTemplateCache;
import com.centurylink.mdw.services.dao.user.cache.UserGroupCache;
import com.centurylink.mdw.services.task.TaskManagerAccess;

public class TaskEmailNotifier extends TemplatedNotifier {
    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public TaskEmailNotifier() {
    }

    public void sendNotice(TaskRuntimeContext runTimeContext, String taskAction, String outcome)
    throws ObserverException {
        if (taskAction == null || (!taskAction.equals(TaskAction.CLAIM) && !taskAction.equals(TaskAction.RELEASE)))  // avoid nuisance notice to claimer and releaser
        sendEmail(runTimeContext, outcome);
    }

    protected String getFromAddress() {
        String fromAddress = getProperty(PropertyNames.NOTIFICATION_EMAIL_FROM_ADDRESS);
        if (fromAddress == null) fromAddress = "mdw@centurylink.com";
        return fromAddress;
    }

    protected String getProperty(String name) {
        try {
            Template template = null;
            if (getTemplateSpec() != null)
                template = TemplateCache.getTemplate(getTemplateSpec());
            if (template == null)
                template = TemplateCache.getTemplate(getTemplate());
            if (template == null || template.getAssetId() == 0L)
                return PropertyManager.getProperty(name);
            else {
                PackageVO pkg = PackageVOCache.getRuleSetPackage(template.getAssetId());
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

    protected String getSubject(TaskInstanceVO taskInstance, String outcome) {
        return "Task: \"" + taskInstance.getTaskName() + "\" " + outcome + " Notice";
    }

    protected JSONObject createEmailJson(TemplatedEmail templatedEmail, TaskInstanceVO taskInstance)
    throws MessagingException, DataAccessException {
        JSONObject emailJson = templatedEmail.buildEmailJson();
		EventManager eventManager = ServiceLocator.getEventManager();
        Long procInstId = taskInstance.getOwnerId();
        eventManager.createDocument(JSONObject.class.getName(), procInstId, OwnerType.TASK_INSTANCE, taskInstance.getTaskInstanceId(), null, null, emailJson);
        return emailJson;
    }

    /**
     * Creates the mime message for the e-mail.
     * @param taskInstance the task instance
     * @param outcome the resulting task status or state
     * @return the message
     */
    protected void sendEmail(TaskRuntimeContext runTimeContext, String outcome)
    throws ObserverException {
        TaskInstanceVO taskInstance = runTimeContext.getTaskInstanceVO();
        TaskEmailModel emailModel = new TaskEmailModel(taskInstance, new VariablesModel(taskInstance.getOwnerId()));
        TemplatedEmail templatedEmail = new TemplatedEmail();
        templatedEmail.setFromAddress(getFromAddress());
        templatedEmail.setSubject(getSubject(taskInstance, outcome));
        templatedEmail.setHtml(true);
        templatedEmail.setTemplateName(getTemplate());
        templatedEmail.setTemplateAssetVerSpec(getTemplateSpec());
        TaskVO taskVO = TaskTemplateCache.getTaskTemplate(taskInstance.getTaskId());
        if (taskVO != null)
            emailModel.setDescription(taskVO.getComment());
        templatedEmail.setModel(emailModel);
        templatedEmail.setRuntimeContext(runTimeContext);

        try {
            Address[] recipients = getRecipients(taskInstance, outcome, emailModel);
            Address[] ccRecipients = getCcRecipients(taskInstance, outcome, emailModel);

            if (templatedEmail.getTemplateBody().contains("${taskActionUrl}") || templatedEmail.getTemplateBody().contains("#{taskActionUrl}")) {

                // send individual e-mails (not stored)
                for (Address recip : recipients) {
                    String cuid = recip.toString().substring(0, recip.toString().indexOf('@'));
                    String userIdentifier = CryptUtil.encrypt(cuid);
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
                        String userIdentifier = CryptUtil.encrypt(cuid);
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
                templatedEmail.setRecipients(recipients);
                templatedEmail.setCcRecipients(ccRecipients);
                try {
              	    templatedEmail.sendEmail(createEmailJson(templatedEmail, taskInstance));
                }
                catch (MessagingException ex) {
                    logger.severeException(ex.getMessage(), ex);  // do not rethrow
                }
            }
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new ObserverException(-1, ex.getMessage(), ex);
        }
    }

    /**
     * Default logic: If outcome is 'Assigned' send e-mail to assignee only,
     * otherwise send to the UNION of Notice Group members along with users
     * specified by the designated recipient process variable value.
     * (Notice Groups are task workgroup members unless overridden by the NoticeGroup attribute).
     *
     * @param taskInstance task instance vo
     * @param outcome task action
     * @return array of recipient addresses
     * @throws AddressException
     */
    protected Address[] getRecipients(TaskInstanceVO taskInstance, String outcome, TaskEmailModel emailModel)
    throws ObserverException, AddressException {
        if ("Assigned".equals(outcome)) {
            // send e-mail only to assignee
            try {
                return new Address[] { getAssigneeEmailAddress(taskInstance) };
            }
            catch (AddressException ex) {
                logger.severeException(ex.getMessage(), ex);
                return new Address[0];
            }
        }
        else {
            List<Address> recipients = new ArrayList<Address>();

            // if the Notice Groups attribute is set on the task definition,
            // use that for the e-mail recipients, otherwise default to task workgroups
            String noticeGroups;
            String recipientEmails;
        	TaskVO task = TaskTemplateCache.getTaskTemplate(taskInstance.getTaskId());
            if (task!=null) {
            	noticeGroups = task.getAttribute(TaskAttributeConstant.NOTICE_GROUPS);
                recipientEmails = task.getAttribute(TaskAttributeConstant.RECIPIENT_EMAILS);
                if (!StringHelper.isEmpty(recipientEmails) && recipientEmails.indexOf('@') < 0 && !recipientEmails.startsWith("$"))
                	recipientEmails = "$" + recipientEmails;
            } else {	// should not happen even for shadow task instances, but just in case
            	noticeGroups = null;
            	recipientEmails = null;
            }
            Address[] groupRecipients = getNoticeGroupsEmailAddresses(taskInstance, noticeGroups);
            if (groupRecipients == null) {
                if (recipientEmails == null)
                    groupRecipients = getTaskUserEmailAddresses(taskInstance);
                else
                    groupRecipients = new Address[0];  // recip var overrides workflow groups
            }
            for (Address address : groupRecipients) {
                if (!recipients.contains(address))
                    recipients.add(address);
            }

            // variable-specified recipients
            if (recipientEmails != null) {
            	if (recipientEmails.startsWith("$")) {
            		Object recipVarValue = emailModel.getVariables().get(recipientEmails.substring(1).trim());
            		if (recipVarValue != null) {
            			try {
            				for (Address address : getRecipientsFromVariable(recipVarValue)) {
            					if (!recipients.contains(address))
            						recipients.add(address);
            				}
            			} catch (CachingException ex) {
            				throw new ObserverException(-1, ex.getMessage(), ex);
            			}
            		}
            	} else {
            		String[] emails = recipientEmails.split("[;,] *");
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
    }

    protected Address[] getRecipientsFromVariable(Object recipVarValue)
    throws ObserverException, CachingException, AddressException {
        if (recipVarValue instanceof String) {
            String recip = (String) recipVarValue;
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
        else if (recipVarValue instanceof String[]) {
            List<Address> addresses = new ArrayList<Address>();
            String[] recips = (String[]) recipVarValue;
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
            throw new ObserverException("Unsupported recipient variable type: " + recipVarValue.getClass().getName());
        }
    }


    /**
     * Default logic: Send to the UNION of Notice Group members along with users
     * specified by the designated recipient process variable value.
     * @throws AddressException
     */
    protected Address[] getCcRecipients(TaskInstanceVO taskInstance, String outcome, TaskEmailModel emailModel)
    throws ObserverException, AddressException {
        List<Address> recipients = new ArrayList<Address>();

        // if the Notice Groups attribute is set on the task definition,
        // use that for the e-mail recipients, otherwise default to task workgroups
        String ccNoticeGroups;
        String ccRecipientEmails;
        TaskVO task = TaskTemplateCache.getTaskTemplate(taskInstance.getTaskId());
        if (task!=null) {
        	ccNoticeGroups = task.getAttribute(TaskAttributeConstant.CC_GROUPS);
            ccRecipientEmails = task.getAttribute(TaskAttributeConstant.CC_EMAILS);
            if (!StringHelper.isEmpty(ccRecipientEmails) && ccRecipientEmails.indexOf('@') < 0 && !ccRecipientEmails.startsWith("$"))
            	ccRecipientEmails = "$" + ccRecipientEmails;
        } else {	// should not happen even for shadow task instances, but just in case
        	ccNoticeGroups = null;
        	ccRecipientEmails = null;
        }
        Address[] groupRecipients = getNoticeGroupsEmailAddresses(taskInstance, ccNoticeGroups);
        if (groupRecipients != null) {
            for (Address address : groupRecipients) {
                if (!recipients.contains(address))
                    recipients.add(address);
            }
        }

        // variable-specified recipients
        if (ccRecipientEmails != null) {
        	if (ccRecipientEmails.startsWith("$")) {
        		Object recipVarValue = emailModel.getVariables().get(ccRecipientEmails.substring(1).trim());
        		if (recipVarValue != null) {
        			try {
        				for (Address address : getRecipientsFromVariable(recipVarValue)) {
        					if (!recipients.contains(address))
        						recipients.add(address);
        				}
        			} catch (CachingException ex) {
        				throw new ObserverException(-1, ex.getMessage(), ex);
        			}
        		}
        	} else {
        		String[] emails = ccRecipientEmails.split("[;,] *");
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

        if (recipients.isEmpty())
            return null;
        else
            return recipients.toArray(new Address[0]);
    }

    private SimpleDateFormat dateFormat;
    protected SimpleDateFormat getDateFormat() {
        if (dateFormat == null)
          dateFormat = new SimpleDateFormat("MMM-dd-yyyy");
        return dateFormat;
    }

    private Address[] toMailAddresses(List<String> addressList) throws AddressException {
        Address[] addresses = new Address[addressList.size()];
        List<Address> toRecipients = new ArrayList<Address>(); //List to just keep the non malformed  addresses
        for (int i = 0; i < addresses.length; i++) {
            try {
                toRecipients.add(new InternetAddress(addressList.get(i)));
            }
            catch (AddressException e) {
                logger.severeException("Illegal email address - " + addressList.get(i), e);
            }
        }
        return toRecipients.toArray(new Address[0]);
    }

    protected Address[] getNoticeGroupsEmailAddresses(TaskInstanceVO taskInstanceVO, String noticeGroups)
    throws ObserverException {
        if (noticeGroups == null)
            return null;
        try {
        	return toMailAddresses(TaskManagerAccess.getInstance()
        			.getGroupEmailAddresses(noticeGroups.split("#")));
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new ObserverException(-1, ex.getMessage(), ex);
        }
    }

    /**
     * Finds the relevant users for a task instance and returns their e-mail addresses
     * @param taskInstanceVO
     * @return an array with the valid e-mail addresses
     */
    protected Address[] getTaskUserEmailAddresses(TaskInstanceVO taskInstanceVO)
    throws ObserverException {
        try {
			TaskManager taskManager = ServiceLocator.getTaskManager();
            List<String> groups;
            if (taskInstanceVO.isTemplateBased()) groups = taskInstanceVO.getGroups();
            else groups = taskManager.getGroupsForTaskInstance(taskInstanceVO);
            return toMailAddresses(TaskManagerAccess.getInstance()
            		.getGroupEmailAddresses(groups.toArray(new String[groups.size()])));
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new ObserverException(-1, ex.getMessage(), ex);
        }
    }

    protected Address getAssigneeEmailAddress(TaskInstanceVO taskInstance) throws AddressException {
        return new InternetAddress(taskInstance.getTaskClaimUserCuid() + "@centurylink.com");
    }

    /**
     * Supports variable type of String or String[].
     * If a value corresponds to a group name, returns users in the group.
     * @throws AddressException
     */
    protected Address[] getGroupEmailAddresses(String[] groups) throws ObserverException, AddressException {
        try {
        	return toMailAddresses(TaskManagerAccess.getInstance().getGroupEmailAddresses(groups));
        }
        catch (MDWException e) {
            logger.severeException(e.getMessage(), e);
            throw new ObserverException(-1, e.getMessage(), e);
        }
    }

    protected class VariablesModel extends HashMap<String,Object> {
		private static final long serialVersionUID = 1L;
		private ProcessVO processVO = null;
		private ProcessInstanceVO procInstVO = null;
		private Long procInstId;
		public VariablesModel(Long procInstId) { this.procInstId = procInstId; }
		@Override
		public Object get(Object key) {
			Object result = super.get(key);
			if (result!=null) return result;
			try {
				EventManager eventMgr = ServiceLocator.getEventManager();
				if (procInstVO == null || processVO == null) {
					procInstVO = eventMgr.getProcessInstance(procInstId);
					processVO = ProcessVOCache.getProcessVO(procInstVO.getProcessId());
				}
				Long procInstanceId = procInstId;
				VariableInstanceInfo vi = null;
                if (processVO.isEmbeddedProcess() || procInstVO.isNewEmbedded()) {
                    procInstanceId = procInstVO.getOwnerId();
                    procInstVO = eventMgr.getProcessInstance(procInstanceId);
                    processVO = ProcessVOCache.getProcessVO(procInstVO.getProcessId());
                }

                vi = eventMgr.getVariableInstance(procInstanceId, (String)key);
                PackageVO packageVO = PackageVOCache.getProcessPackage(processVO.getProcessId());

				if (vi!=null) {
					result = vi.getData();
					if (result instanceof DocumentReference) {
					    DocumentVO docvo = eventMgr.getDocumentVO(((DocumentReference)result).getDocumentId());
					    result = docvo==null?null:docvo.getObject(vi.getType(), packageVO);
					}
					if (result!=null) this.put((String)key, result);
				} else result = null;
			} catch (Exception e) {
				logger.severeException(e.getMessage(), e);
				result = null;
		    }
			return result;
		}
    }
}
