/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.listener;

import java.util.List;
import java.util.Map;
import java.util.TimerTask;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.Compatibility;
import com.centurylink.mdw.common.cache.impl.PackageVOCache;
import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.service.MdwServiceRegistry;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.translator.SelfSerializable;
import com.centurylink.mdw.common.translator.VariableTranslator;
import com.centurylink.mdw.common.translator.impl.JavaObjectTranslator;
import com.centurylink.mdw.common.utilities.form.CallURL;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.event.EventHandlerException;
import com.centurylink.mdw.event.ExternalEventHandler;
import com.centurylink.mdw.model.data.monitor.LoadBalancedScheduledJob;
import com.centurylink.mdw.model.data.monitor.ScheduledJob;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.variable.DocumentReference;
import com.centurylink.mdw.model.value.variable.DocumentVO;
import com.centurylink.mdw.model.value.variable.VariableInstanceInfo;
import com.centurylink.mdw.model.value.variable.VariableVO;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.services.cache.CacheRegistration;
import com.centurylink.mdw.services.dao.process.cache.ProcessVOCache;
import com.centurylink.mdw.services.messenger.MessengerFactory;
import com.centurylink.mdw.services.pooling.AdapterConnectionPool;
import com.centurylink.mdw.services.pooling.ConnectionPoolRegistration;
import com.centurylink.mdw.xml.XmlPath;

/**
 *
 * DefaultEventHandler
 *
 * This class defines a default external event handler.
 * The handler is invoked when the listeners cannot find any matching
 * external event handlers for an incoming message.
 *
 * @version 1.0
 */
public class DefaultEventHandler implements ExternalEventHandler {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

	/**
     * The handler creates a standard error response message.
     * The status code and status message are set in the following
     * ways:
     * <ul>
     *    <li>If the message cannot be parsed by the generic XML Bean,
     *         the status code is set to -2 and the status message is
     *         status message "failed to parse XML message"</li>
     *    <li>If there is no matching handler found, but the root element
     *         is "ping", the status code is set to 0 and and the
     *         status message is set to the content of the ping in the
     *         request</li>
     *    <li>Otherwise, the status code is set to -3 and the status message
     *         is set to "No event handler has been configured for message"</li>
     * </ul>
     * @param message the request message
     * @param msgdoc XML Bean parsed from request message, or null if the request
     *      message cannot be parsed by the generic XML bean
     * @param metaInfo meta information. The method does not use this.
     * @return the response message
     * @throws EventHandlerException
     * @throws XmlException
     */
    public String handleEventMessage(String message, Object msgdoc, Map<String,String> metaInfo)
            throws EventHandlerException {
        String msg, resp;
        ListenerHelper helper = new ListenerHelper();
        int status;
        if (msgdoc==null) {
            msg = "failed to parse XML message";
            status = ListenerHelper.RETURN_STATUS_NON_XML;
        } else {
            String rootNodeName = XmlPath.getRootNodeName((XmlObject)msgdoc);
            if (rootNodeName!=null && rootNodeName.equals("ping")) {
                msg = XmlPath.evaluate((XmlObject)msgdoc, "ping");
                if (msg==null) msg = "ping is successful";
                status = ListenerHelper.RETURN_STATUS_SUCCESS;
            } else {
                msg = "No event handler has been configured for message";
                status = ListenerHelper.RETURN_STATUS_NO_HANDLER;
                logger.severe(msg + ": " + message);
            }
        }
        resp = helper.createStandardResponse(status, msg,
                metaInfo.get(Listener.METAINFO_REQUEST_ID));
        return resp;
    }

	private String translateJavaObjectValue(EventManager eventMgr, String varValue,
			VariableInstanceInfo varinst, PackageVO pkg) throws DataAccessException {
		DocumentReference docref = (DocumentReference)varinst.getData();
		DocumentVO docvo = eventMgr.getDocumentVO(docref.getDocumentId());
		JavaObjectTranslator translator = new JavaObjectTranslator();
		Object obj = translator.realToObject(docvo.getContent(pkg));
		if (obj instanceof SelfSerializable) {
			((SelfSerializable)obj).fromString(varValue);
			varValue = translator.realToString(obj);
		}
		return varValue;
	}

    public String handleSpecialEventMessage(XmlObject msgdoc)
    	throws EventHandlerException, XmlException {
    	String rootNodeName = XmlPath.getRootNodeName(msgdoc);
    	String response;
    	if (rootNodeName.equals("_mdw_property")) {
    		String propname = XmlPath.getRootNodeValue(msgdoc);
    		response = PropertyManager.getProperty(propname);
    		if (response==null) response = "";
    	} else if (rootNodeName.equals("_mdw_update_variable")) {
    		String varInstId = XmlPath.evaluate(msgdoc, "/_mdw_update_variable/var_inst_id");
    		String varValue = XmlPath.evaluate(msgdoc, "/_mdw_update_variable/var_value");
            String procInstId = XmlPath.evaluate(msgdoc, "/_mdw_update_variable/proc_inst_id");
    		if (varInstId==null || varValue==null) {
        		String varName = XmlPath.evaluate(msgdoc, "/_mdw_update_variable/var_name");
        		if (varName==null || procInstId==null) {
        			response = "ERROR: var_inst_id or var_value is null";
        		} else {
        			try {
    					EventManager eventMgr = ServiceLocator.getEventManager();
    					ProcessInstanceVO procInst = eventMgr.getProcessInstance(new Long(procInstId));
    					VariableInstanceInfo varinst = eventMgr.getVariableInstance(procInst.getId(), varName);
    					if (varinst != null)
    					    throw new Exception("Variable instance is already defined");
    					ProcessVO procdef = ProcessVOCache.getProcessVO(procInst.getProcessId());
    					PackageVO pkg = PackageVOCache.getProcessPackage(procdef.getId());
    					VariableVO vardef = procdef.getVariable(varName);
    					if (vardef==null) throw new Exception("Variable is not defined for the process");
    					if (vardef.getVariableType().equals(Object.class.getName()))
    						varValue = translateJavaObjectValue(eventMgr, varValue, varinst, pkg);
    					if (VariableTranslator.isDocumentReferenceVariable(pkg, vardef.getVariableType())) {
    						Long docid = eventMgr.createDocument(vardef.getVariableType(),
                                    OwnerType.PROCESS_INSTANCE, procInst.getId(), varValue, pkg);
    						varinst = eventMgr.setVariableInstance(procInst.getId(), varName,
    								new DocumentReference(docid));
            	    	    response = "OK:" + varinst.getInstanceId().toString() + ":" + docid.toString();
    					} else {
    						varinst = eventMgr.setVariableInstance(procInst.getId(), varName, varValue);
            	    	    response = "OK:" + varinst.getInstanceId().toString();
    					}
        	    	} catch (Exception e) {
    					response = "ERROR: [" + e.getClass().getName() + "] " + e.getMessage();
    				}
        		}
    		} else {
    			try {
					EventManager eventMgr = ServiceLocator.getEventManager();
					VariableInstanceInfo varinst = eventMgr.getVariableInstance(new Long(varInstId));
					if (varinst==null)
					    throw new Exception("Variable instance does not exist");
					if (varinst.getType().equals(Object.class.getName())) {
                        ProcessInstanceVO procInst = eventMgr.getProcessInstance(new Long(procInstId));
                        ProcessVO procdef = ProcessVOCache.getProcessVO(procInst.getProcessId());
                        PackageVO pkg = PackageVOCache.getProcessPackage(procdef.getId());
						varValue = translateJavaObjectValue(eventMgr, varValue, varinst, pkg);
					}
					eventMgr.updateVariableInstance(varinst.getInstanceId(), varValue);
    	    	    response = "OK";
    	    	} catch (Exception e) {
					response = "ERROR: [" + e.getClass().getName() + "] " + e.getMessage();
				}
    		}
    	} else if (rootNodeName.equals("_mdw_version")) {
    		response = ApplicationContext.getMdwVersion();
    	} else if (rootNodeName.equals("_mdw_run_job")) {
    		String classNameAndArgs = XmlPath.getRootNodeValue(msgdoc);
    		CallURL url = new CallURL(classNameAndArgs);
    		String className = url.getAction();
    		Object aTimerTask = null;
    		try {
                ScheduledJob job = MdwServiceRegistry.getInstance().getScheduledJob(className);
                if (job != null) {
                    if (job instanceof LoadBalancedScheduledJob)
                        ((LoadBalancedScheduledJob)job).runOnLoadBalancedInstance(url);
                    else
                        job.run(url);
                }
                else {
                    className = Compatibility.getEventHandler(className);
                    aTimerTask = Class.forName(className).newInstance();
                    if (aTimerTask instanceof ScheduledJob) {
                        if (aTimerTask instanceof LoadBalancedScheduledJob)
                            ((LoadBalancedScheduledJob)aTimerTask).runOnLoadBalancedInstance(url);
                        else
                            ((ScheduledJob)aTimerTask).run(url);
                    }
                    else
                        ((TimerTask) aTimerTask).run(); // for backward compatibility
                }
    		}
    		catch (Exception ex) {
    		    logger.severeException("Failed to create instance for scheduled job " + className, ex);
    		}
    		// else exception already logged
    		response = "OK";	// not used
    	} else if (rootNodeName.equals("_mdw_refresh")) {
    		String cachename = XmlPath.getRootNodeValue(msgdoc);
    		CacheRegistration.broadcastRefresh(cachename, MessengerFactory.newInternalMessenger());
//    		printServerInfo();
    		response = "OK";
    	} else if (rootNodeName.equals("_mdw_pool_ping")) {
    		String poolname = XmlPath.getRootNodeValue(msgdoc);
    		AdapterConnectionPool pool = ConnectionPoolRegistration.getPool(poolname);
    		pool.ping_and_start();
    		response = "OK";
    	} else if (rootNodeName.equals("_mdw_task_sla")) {
    		try {
				TaskManager taskMgr = ServiceLocator.getTaskManager();
				String taskInstId = XmlPath.evaluate(msgdoc, "/_mdw_task_sla/task_instance_id");
				String isAlert = XmlPath.evaluate(msgdoc, "/_mdw_task_sla/is_alert");
				taskMgr.updateTaskInstanceState(new Long(taskInstId), "true".equalsIgnoreCase(isAlert));
			} catch (Exception e) {
    			logger.severeException("Failed to change task state", e);
    			response = "ERROR: " + e.getMessage();
			}
    		response = "OK";
    	} else if (rootNodeName.equals("_mdw_peer_server_list")) {
    		List<String> servers = ApplicationContext.getRoutingServerList().isEmpty() ? ApplicationContext.getManagedServerList() : ApplicationContext.getRoutingServerList();
    		StringBuffer sb = new StringBuffer();
    		for (String server : servers) {
    			if (sb.length()>0) sb.append(",");
    			sb.append(server);
    		}
    		response = sb.toString();
    	} else if (rootNodeName.equals("_mdw_document_content")) {
    		String documentId = XmlPath.evaluate(msgdoc, "/_mdw_document_content/document_id");
    		String type = XmlPath.evaluate(msgdoc, "/_mdw_document_content/type");
    		try {
    			EventManager eventMgr = ServiceLocator.getEventManager();
    			DocumentVO docvo = eventMgr.getDocumentVO(new Long(documentId));
    			if (type.equals(Object.class.getName())) {
    			    Object obj = VariableTranslator.realToObject(getPackageVO(docvo), "java.lang.Object", docvo.getContent(getPackageVO(docvo)));
	    			response = obj.toString();
    			} else response = docvo.getContent(getPackageVO(docvo));
    		} catch (Exception e) {
    		    logger.severeException(e.getMessage(), e);
    			response = "ERROR: " + e.getClass().getName() + " - " + e.getMessage();
    		}
    	} else {
    		response = "ERROR: unknown internal message " + rootNodeName;
    	}
    	return response;
    }

    // test code to see if we can use MBean to broadcast.
    // The ListenAddress attribute returns empty string (default, so that it can listen to multiple address)
    // tried to access machine but that need WLS admin credential, so have not tried further
    @SuppressWarnings("unused")
	private void printServerInfo() {
    	Context ctx;
    	try{
    		ctx = new InitialContext();
    		MBeanServer server = (MBeanServer)ctx.lookup("java:comp/env/jmx/runtime");
    		ObjectName service = new ObjectName("com.bea:Name=RuntimeService," +
    	      	"Type=weblogic.management.mbeanservers.runtime.RuntimeServiceMBean");
    		ObjectName domainMBean = (ObjectName) server.getAttribute(service,
    			"DomainConfiguration");
    		ObjectName[] servers = (ObjectName[]) server.getAttribute(domainMBean,
    				"Servers");
    		for (ObjectName one : servers) {
    			String name = (String)server.getAttribute(one, "Name");
    			String address = (String)server.getAttribute(one, "ListenAddress");
    			Integer port = (Integer)server.getAttribute(one, "ListenPort");
    			System.out.println("SERVER: " + name + " on " + address + ":" + port);
    			ObjectName machine = (ObjectName)server.getAttribute(one, "Machine");
    			ObjectName nodeManager = (ObjectName)server.getAttribute(machine, "NodeManager");
    			// above line need WLS admin!!!
    			address = (String)server.getAttribute(machine, "ListenAddress");
    			System.out.println(" - hostname: " + address);
    		}
    		if (ctx!=null) return;
    	}catch(Exception ex){
    		ex.printStackTrace();
    	}
    }

    private PackageVO getPackageVO(DocumentVO docVO) throws ServiceException {
        try {
            EventManager eventMgr = ServiceLocator.getEventManager();
            Long procInstId = null;
            if (docVO.getOwnerType().equals(OwnerType.VARIABLE_INSTANCE)) {
                VariableInstanceInfo varInstInf = eventMgr.getVariableInstance(docVO.getOwnerId());
                procInstId = varInstInf.getProcessInstanceId();
            }
            else if (docVO.getOwnerType().equals(OwnerType.PROCESS_INSTANCE)) {
                procInstId = docVO.getOwnerId();
            }
            if (procInstId == null)
                return null;
            ProcessInstanceVO procInstVO = eventMgr.getProcessInstance(procInstId);
            return PackageVOCache.getProcessPackage(procInstVO.getProcessId());
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }



}
