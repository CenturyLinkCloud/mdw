/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.activity.process;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.xmlbeans.XmlObject;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.common.constant.JMSDestinationNames;
import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.constant.ProcessVisibilityConstant;
import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.constant.VariableConstants;
import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.translator.VariableTranslator;
import com.centurylink.mdw.common.utilities.JMSServices;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.TransactionWrapper;
import com.centurylink.mdw.common.utilities.logger.StandardLogger.LogLevel;
import com.centurylink.mdw.common.utilities.timer.Tracked;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.RemoteAccess;
import com.centurylink.mdw.model.data.monitor.CertifiedMessage;
import com.centurylink.mdw.model.data.monitor.ScheduledEvent;
import com.centurylink.mdw.model.value.event.InternalEventVO;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.variable.DocumentReference;
import com.centurylink.mdw.model.value.variable.VariableInstanceInfo;
import com.centurylink.mdw.model.value.variable.VariableVO;
import com.centurylink.mdw.services.ProcessException;
import com.centurylink.mdw.services.dao.process.EngineDataAccessDB;
import com.centurylink.mdw.services.event.CertifiedMessageManager;
import com.centurylink.mdw.services.messenger.IntraMDWMessenger;
import com.centurylink.mdw.services.messenger.MessengerFactory;
import com.centurylink.mdw.services.process.ProcessEngineDriver;
import com.centurylink.mdw.services.process.ProcessExecuter;
import com.centurylink.mdw.services.task.EngineAccess;
import com.qwest.mbeng.DomDocument;
import com.qwest.mbeng.FormatDom;
import com.qwest.mbeng.MbengNode;

/**
 * This activity implementor implements invocation of subprocesses.
 */
@Tracked(LogLevel.TRACE)
public class InvokeSubProcessActivity extends InvokeProcessActivityBase {

    private static final String VARIABLES = "variables";
	private static final String SYNCHRONOUS = "synchronous";
	private boolean subprocIsService = false;

	private static final String ERR_OUTPARA = "Actual parameter for OUTPUT parameter is not a variable";

    public boolean needSuspend() {
    	return (!getEngine().isInService() && subprocIsService)?false:this.isSynchronousCall();
    }

    private boolean isSynchronousCall() {
    	String v = getAttributeValue(SYNCHRONOUS);
    	return (v==null || v.equalsIgnoreCase("TRUE"));
    }

    /**
     * This method returns variable bindings to be passed into subprocess.
     * The method uses the attribute "variables" values as a mapping.
     * The binding of each variable is an expression in the Java Expression Language (beginning with '#'),
     * or the Magic Box rule language (beginning with '$').
     * Example bindings: #{myVar}_suffix, "var1=12*12;var2=$parent_var.LIST.LN"
     * Subclass may override this method to obtain variable binding in other ways.
     *
     * @param childVars variables defined for the child process
     * @param prMgr process manager remote EJB handle
     * @param varMgr variable manager remote EJB handle
     * @return a map (name-value pairs) of variable bindings
     * @throws Exception various types of exceptions
     */
    protected Map<String,String> createVariableBinding(List<VariableVO> childVars,
    		RemoteAccess rao, String myAppName)
    		throws Exception {
        Map<String,String> validParams = new HashMap<String,String>();
        String map = getAttributeValue(VARIABLES);
        if (map==null) map = "";
        String vn, v;
        for (VariableVO childVar : childVars) {
        	if (!allowInput(childVar)) continue;
        	vn = childVar.getVariableName();
        	v = StringHelper.getMapValue(map, vn, ';');
        	if (vn.equals(VariableConstants.REQUEST)) {
        		VariableInstanceInfo varinst = getVariableInstance(VariableConstants.REQUEST);
        		v = varinst==null?null:varinst.getStringValue();
        	} else if (vn.equals(VariableConstants.MASTER_DOCUMENT)) {
        		VariableInstanceInfo varinst = getVariableInstance(VariableConstants.MASTER_DOCUMENT);
        		v = varinst==null?null:varinst.getStringValue();
        	} else if (rao==null) {
        		v = evaluateBindingValue(childVar, v);
        	} else if (childVar.getVariableCategory().intValue()!=VariableVO.CAT_STATIC
        			&& valueIsVariable(v)) {
        		String varName = v.substring(1);
        		VariableInstanceInfo varinst = getVariableInstance(varName);
        		if (varinst==null) {
        			v = null;
        		} else if (varinst.isDocument()) {
        			DocumentReference docref = (DocumentReference)varinst.getData();
        			String server = docref.getServer();
        			if (server==null) server = myAppName;
        			else if (server.equals(rao.getLogicalServerName())) server = null;
        			docref = new DocumentReference(docref.getDocumentId(), server);
        			v = docref.toString();
        		} else {
        			v = varinst.getStringValue();
        		}
        	} else v = evaluateBindingValue(childVar, v);
        	if (v!=null && v.length()>0) validParams.put(vn, v);
        }
        return validParams;
    }

    public void execute() throws ActivityException{
        try{
            String procname = this.getAttributeValue(WorkAttributeConstant.PROCESS_NAME);
            String myAppName=null;
            RemoteAccess rao;
            int k = procname.indexOf(RemoteAccess.REMOTE_NAME_DELIMITER);
            String logicalServerName;
            if (k>0) {
            	logicalServerName = procname.substring(k+1);
				EngineAccess engineAccess = new EngineAccess();
				String dbinfo = engineAccess.getDatabaseCredential(logicalServerName);
                rao = new RemoteAccess(logicalServerName, dbinfo);
                myAppName = getProperty(PropertyNames.APPLICATION_NAME);
            } else {
            	rao = null;
            	logicalServerName = null;
            }
            ProcessVO subprocdef = rao == null ? getSubProcessVO() : getRemoteSubProcessVO(rao);
            if (isLogDebugEnabled())
              logdebug("Invoking subprocess: " + subprocdef.getLabel());
            subprocIsService = subprocdef.getProcessType().equals(ProcessVisibilityConstant.SERVICE);
            List<VariableVO> childVars = subprocdef.getVariables();
            Map<String,String> validParams = createVariableBinding(childVars, rao, myAppName);
            String ownerType;
            if (rao==null) ownerType = OwnerType.PROCESS_INSTANCE;
            else {
            	String engineUrl = MessengerFactory.getEngineUrl();
            	if (engineUrl.contains("/localhost:")) {
            		String ipaddr = java.net.InetAddress.getLocalHost().getHostAddress();
            		engineUrl = engineUrl.replace("localhost", ipaddr);
            	}
            	if (rao.getSchemaVersion()<DataAccess.schemaVersion52) ownerType = myAppName;
            	else ownerType = myAppName + "@" + engineUrl;
            }
            String secondaryOwnerType = OwnerType.ACTIVITY_INSTANCE;
			Long secondaryOwnerId = getActivityInstanceId();
			Long ownerId = this.getProcessInstanceId();
			if (! this.needSuspend()) {
				secondaryOwnerId = null;
				secondaryOwnerType = null;
			}
			//If it is an asynchronous call to a regular process and the
			//parent process is running in a Memory Only thread, then the subprocess won't have any handle
			//to the parent process. So mark the Owner/OwnerId as the root process instance Owner/OwnerId
			if ((!isSynchronousCall() && getEngine().isInMemory()) ||
					(!getEngine().isInService() && getEngine().isInMemory() && subprocIsService)) {
				Object[] rootProcessOwner = getRootProcessOwner(getProcessInstanceOwnerId(), getProcessInstanceOwner());
				ownerId = (Long) rootProcessOwner[0];
				ownerType = (String) rootProcessOwner[1];
			}
			InternalEventVO evMsg = InternalEventVO.createProcessStartMessage(
            		subprocdef.getProcessId(), ownerType,
            		ownerId, getMasterRequestId(), null,
            		secondaryOwnerType, secondaryOwnerId);
            if (rao!=null) {
                logger.info("Invoke remote process " + procname);
                boolean useCertifiedMessage = true;
                boolean useInternalMessageQueue = false;
                evMsg.setParameters(validParams);
                if (rao.getSchemaVersion()<DataAccess.schemaVersion52) {
                	useCertifiedMessage = false;
                	useInternalMessageQueue = true;
                }
                if (useCertifiedMessage) {
	            	DomDocument domdoc = new DomDocument();
	            	FormatDom fmter = new FormatDom();
	            	domdoc.getRootNode().setName("_mdw_remote_process");
	            	MbengNode node = domdoc.newNode("direction", "invoke", "", ' ');
	            	domdoc.getRootNode().appendChild(node);
	            	node = domdoc.newNode("message", evMsg.toXml(), "", ' ');
	            	domdoc.getRootNode().appendChild(node);
	            	String msg = fmter.format(domdoc);
	                DocumentReference docref = this.createDocument(XmlObject.class.getName(), msg,
	                        OwnerType.ADAPTOR_REQUEST, this.getActivityInstanceId(), null, null);
	                CertifiedMessageManager manager = CertifiedMessageManager.getSingleton();
	                Map<String,String> props = new HashMap<String,String>();
	            	props.put(CertifiedMessage.PROP_PROTOCOL, CertifiedMessage.PROTOCOL_MDW2MDW);
	                props.put(CertifiedMessage.PROP_JNDI_URL, logicalServerName);
	                manager.sendTextMessage(props, msg, docref.getDocumentId(), logtag());
                } else if (useInternalMessageQueue) {
                	try {
                		String contextUrl = MessengerFactory.getEngineUrl(logicalServerName);
            			JMSServices.getInstance().sendTextMessage(contextUrl,
            					JMSDestinationNames.PROCESS_HANDLER_QUEUE, evMsg.toXml(), 0, null);
            		} catch (Exception e) {
            			throw new ProcessException(-1, "Failed to send internal event", e);
            		}
                } else {
	            	DomDocument domdoc = new DomDocument();
	            	FormatDom fmter = new FormatDom();
	            	domdoc.getRootNode().setName("_mdw_remote_process");
	            	MbengNode node = domdoc.newNode("direction", "invoke", "", ' ');
	            	domdoc.getRootNode().appendChild(node);
	            	node = domdoc.newNode("message", evMsg.toXml(), "", ' ');
	            	domdoc.getRootNode().appendChild(node);
	            	String msg = fmter.format(domdoc);
	            	IntraMDWMessenger messenger = MessengerFactory.newIntraMDWMessenger(logicalServerName);
	            	messenger.sendMessage(msg);
                }
            } else {
            	ProcessExecuter engine = getEngine();
            	if (engine.isInService()) {
            		if (subprocIsService) {		// call directly
            			ProcessInstanceVO pi = getEngine().createProcessInstance(
                				subprocdef.getProcessId(), OwnerType.PROCESS_INSTANCE,
                				getProcessInstanceId(), secondaryOwnerType, secondaryOwnerId,
                				getMasterRequestId(), validParams);
            			engine.startProcessInstance(pi, 0);
            		} else {					// call externally
            			String msgid = ScheduledEvent.INTERNAL_EVENT_PREFIX + secondaryOwnerId + "startproc" + subprocdef.getProcessId();
            			evMsg.setParameters(validParams);	// TODO this can be large!
                		engine.sendDelayedInternalEvent(evMsg, 0, msgid, false);
                	}
            	} else {
            		if (subprocIsService) {
            			ProcessEngineDriver engineDriver = new ProcessEngineDriver();
            			Map<String,String> params =  engineDriver.invokeServiceAsSubprocess(subprocdef.getProcessId(), ownerId, getMasterRequestId(),
            	    			validParams, subprocdef.getPerformanceLevel());
            	    	this.bindVariables(null, params, true);		// last arg should be true only when perf_level>=9 (DHO:actually 5), but this works
            		} else {
                		int perfLevel = subprocdef.getPerformanceLevel();
                		if (perfLevel==0 || perfLevel==engine.getPerformanceLevel()) {
                			ProcessInstanceVO pi = getEngine().createProcessInstance(
                    				subprocdef.getProcessId(), OwnerType.PROCESS_INSTANCE,
                    				getProcessInstanceId(), secondaryOwnerType, secondaryOwnerId,
                    				getMasterRequestId(), validParams);
                			engine.startProcessInstance(pi, 0);
                		} else {
                			String msgid = ScheduledEvent.INTERNAL_EVENT_PREFIX + secondaryOwnerId
    	    					+ "startproc" + subprocdef.getProcessId();
                			evMsg.setParameters(validParams);	// TODO this can be large!
                    		engine.sendDelayedInternalEvent(evMsg, 0, msgid, false);
                		}
            		}
            	}
            }
        } catch (ActivityException ex) {
        	throw ex;
        }catch(Exception ex){
            logger.severeException("Exception in InvokeSubProcessActivity", ex);
            throw new ActivityException(-1, "Exception in InvokeSubProcessActivity", ex);
        }

    }

	boolean resume_on_process_finish(InternalEventVO msg, Integer status)
		throws ActivityException {
	    try{
            String procname = this.getAttributeValue(WorkAttributeConstant.PROCESS_NAME);
            String remoteName=null;
            Map<String,String> params;
            int k = procname.indexOf(RemoteAccess.REMOTE_NAME_DELIMITER);
            if (k>0) {
                logger.info("*********Got reply from the remote process*********");
                this.createDocument(XmlObject.class.getName(), msg.toXml(),
                        OwnerType.ADAPTOR_RESPONSE, this.getActivityInstanceId(), null, null);
            	remoteName = procname.substring(k+1);
                params = msg.getParameters();
            } else {
            	Long subprocInstId = msg.getWorkInstanceId();
            	params = super.getOutputParameters(subprocInstId, msg.getWorkId());
            }
            this.bindVariables(remoteName, params, false);

            String compcode = msg.getCompletionCode();
            if (compcode!=null && compcode.length()>0) this.setReturnCode(compcode);
            return true;
        } catch (ActivityException ex) {
            throw ex;
        }catch(Exception ex){
            logger.severeException(ex.getMessage(), ex);
            throw new ActivityException(-1, ex.getMessage(), ex);
        }

	}

	private void bindVariables(String remoteName, Map<String, String> params,
			boolean passDocContent) throws ActivityException {
		String myName = getProperty(PropertyNames.APPLICATION_NAME);
		String map = getAttributeValue(VARIABLES);
        if (map==null) map = "";
        if (params!=null) {
        	ProcessVO procdef = getMainProcessDefinition();
        	for (String varname : params.keySet()) {
        		String para = getActualParameterVariable(map, varname);
        		VariableVO var = procdef.getVariable(para);
                if (var == null)
                    throw new ActivityException("Bound variable: '" + var + "' not found in process definition");
        		String varvalue = params.get(varname);
        		Object value;
        		if (passDocContent && VariableTranslator.isDocumentReferenceVariable(getPackage(), var.getVariableType())) {
        			if (StringHelper.isEmpty(varvalue)) value = null;
        			else if (varvalue.startsWith("DOCUMENT:"))
            			value = VariableTranslator.toObject(var.getVariableType(), varvalue);
        			else {
        				DocumentReference docref = super.createDocument(var.getVariableType(),
        						varvalue, OwnerType.PROCESS_INSTANCE, this.getProcessInstanceId(), null, null);
            			value = new DocumentReference(docref.getDocumentId(), null);
        			}
        		} else {
        			value = VariableTranslator.toObject(var.getVariableType(), varvalue);
        		}
        		if (remoteName!=null && value instanceof DocumentReference) {
        			DocumentReference docref = (DocumentReference)value;
        			String server = docref.getServer();
        			if (server==null) server = remoteName;
        			else if (server.equals(myName)) server = null;
        			value = new DocumentReference(docref.getDocumentId(), server);
        		}
        		this.setParameterValue(para, value);
        	}
        }
	}

	/**
	 * Override this as for single process invocation, no need to lock
	 */
	@Override
	protected Integer lockActivityInstance() {
		return null;
	}

	private String getActualParameterVariable(String map, String parameterName)
	        throws ActivityException {
	    String v = StringHelper.getMapValue(map, parameterName, ';');
        if (v==null || v.length()<2 || (v.charAt(0)!='$' && v.charAt(0)!='#'))
            throw new ActivityException(ERR_OUTPARA + ": " + parameterName);
        for (int i=1; i<v.length(); i++) {
            if (!Character.isLetterOrDigit(v.charAt(i))&& v.charAt(i)!='_')
                throw new ActivityException(ERR_OUTPARA + ": " + parameterName);
        }
        return v.substring(1);
	}

	// currently not used - invoke single process does not handle other events
	protected boolean allSubProcessCompleted() throws Exception {
		return true;
	}

	protected Object[] getRootProcessOwner(Long ownerId, String owner) throws DataAccessException {
		if (!OwnerType.PROCESS_INSTANCE.equals(owner)) {
			return new Object[]{ownerId, owner};
		} else {
			ProcessInstanceVO pi = null;
			try {
				pi = getEngine().getProcessInstance(ownerId);
			} catch (ProcessException e) {
				//This shouldn't happen as Engine has to be in memory only mode and Process Exception
				//can get thrown only if a DB call is being made
				logger.severe("InvokeSubprocess->getRootProcessOwner() -> Supposedly unreachable code");
			}
			if (null == pi) {
				//This means that the pi is not present in cache and can be found in DB
				pi = this.getProcInstFromDB(ownerId);
				if (null != pi)
					return getRootProcessOwner(pi.getOwnerId(), pi.getOwner());
				else {
					//Shouldn't happen as pi has to be there either in memory or in DB
					logger.severe("getRootProcessOwner-> pi not found in DB for:" + ownerId);
					return new Object[]{new Long(0), OwnerType.DOCUMENT};
				}
			} else {
				return getRootProcessOwner(pi.getOwnerId(), pi.getOwner());
			}
		}
	}

    /**
     * Method to get the Process Instance from the database
     * @param procInstId
     * @return
     * @throws DataAccessException
     */
	private ProcessInstanceVO getProcInstFromDB(Long procInstId) throws DataAccessException {
		TransactionWrapper transaction = null;
		EngineDataAccessDB edao = new EngineDataAccessDB();
		try {
			transaction = edao.startTransaction();
			return edao.getProcessInstance(procInstId);
		} catch (SQLException e) {
        	logger.severe("InvokeSubProcessActivity -> Failed to load process instance for " + procInstId);
            return null;
		} finally {
			edao.stopTransaction(transaction);
		}
    }

    /**
     * Does not support Smart Subprocess Versioning (except special case of zero).
     */
    protected ProcessVO getRemoteSubProcessVO(RemoteAccess rao) throws Exception {
        String procname = getAttributeValue(WorkAttributeConstant.PROCESS_NAME);
        int k = procname.indexOf(RemoteAccess.REMOTE_NAME_DELIMITER);
        String properProcname = procname.substring(0,k);
        String subproc_version = this.getAttributeValue(WorkAttributeConstant.PROCESS_VERSION);
        ProcessVO subprocdef = rao.getLoader().getProcessBase(properProcname, (subproc_version==null?0:Integer.parseInt(subproc_version)));
        return rao.getLoader().loadProcess(subprocdef.getProcessId(), false);
    }

    protected ProcessVO getSubProcessVO() throws DataAccessException {

        String name = getAttributeValue(WorkAttributeConstant.PROCESS_NAME);
        String verSpec = getAttributeValue(WorkAttributeConstant.PROCESS_VERSION);
        return getSubProcessVO(name, verSpec);
    }



}
