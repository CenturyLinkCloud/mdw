/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.activity.process;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.activity.types.InvokeProcessActivity;
import com.centurylink.mdw.common.constant.VariableConstants;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.translator.VariableTranslator;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.TransactionWrapper;
import com.centurylink.mdw.model.value.attribute.AssetVersionSpec;
import com.centurylink.mdw.model.value.event.EventWaitInstanceVO;
import com.centurylink.mdw.model.value.event.InternalEventVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.variable.VariableInstanceInfo;
import com.centurylink.mdw.model.value.variable.VariableVO;
import com.centurylink.mdw.services.ProcessException;
import com.centurylink.mdw.services.dao.process.cache.ProcessVOCache;
import com.centurylink.mdw.workflow.activity.AbstractWait;

/**
 * This abstract activity implementor implements the common funciton
 * for invocation of sub processes, whether it is single, multiple or heterogeneous
 *
 *
 */

public abstract class InvokeProcessActivityBase extends AbstractWait
		implements InvokeProcessActivity {

	public final boolean resumeWaiting(InternalEventVO msg)
			throws ActivityException {
		TransactionWrapper transaction = null;
		try {
			transaction = startTransaction();
			lockActivityInstance();
			if (allSubProcessCompleted()) {
				return true;
			} else {
				EventWaitInstanceVO received = registerWaitEvents(true, true);
				if (received!=null) {
					this.setReturnCode(received.getCompletionCode());
					processOtherMessage(getExternalEventInstanceDetails(received.getMessageDocumentId()));
					handleEventCompletionCode();
					return true;
				} else return false;
			}
		} catch (Exception e) {
			throw new ActivityException(-1, e.getMessage(), e);
		} finally {
			stopTransaction(transaction);
		}
	}

	abstract protected boolean allSubProcessCompleted() throws Exception;

    /**
     * This method is invoked to process a received event (other than subprocess termination).
     * You will need to override this method to customize processing of the event.
     *
     * The default method does nothing.
     *
     * The status of the activity after processing the event is configured in the designer, which
     * can be either Hold or Waiting.
     *
     * When you override this method, you can optionally set different completion
     * code from those configured in the designer by calling setReturnCode().
     *
     * @param messageString the entire message content of the external event (from document table)
     * @throws ActivityException
     */
    protected void processOtherMessage(String msg)
    	throws ActivityException {
    }

    /**
     * This method is a hook for custom processing, to be called
     * when subprocesses are completed. The default method
     * does nothing.
     * @throws ActivityException
     */
    protected void onFinish() throws ActivityException {
    }

    boolean resume_on_other_event(String msg, String compCode)
			throws ActivityException {
    	TransactionWrapper transaction = null;
    	try {
    		transaction = startTransaction();
    		lockActivityInstance();
    		this.setReturnCode(compCode);
    		processOtherMessage(msg);
    		handleEventCompletionCode();
    		return true;
		} finally {
    		stopTransaction(transaction);
    	}
    }

    abstract boolean resume_on_process_finish(InternalEventVO msg, Integer status)
	throws ActivityException;

    public final boolean resume(InternalEventVO msg)
	    throws ActivityException {
    	TransactionWrapper transaction = null;
		try {
			transaction = startTransaction();
			Integer status = lockActivityInstance();
			if (msg.isProcess()) {
				boolean done = resume_on_process_finish(msg, status);
				if (done) onFinish();
				return done;
			} else {
		     	String messageString = this.getMessageFromEventMessage(msg);
		     	this.setReturnCode(msg.getCompletionCode());
	    		processOtherMessage(messageString);
	    		handleEventCompletionCode();
	    		return true;
			}
		} finally {
			stopTransaction(transaction);
		}
	}

    protected boolean allowInput(VariableVO childVar) {
    	int varCat = childVar.getVariableCategory().intValue();
    	if (varCat==VariableVO.CAT_INPUT || varCat==VariableVO.CAT_INOUT
    			|| varCat==VariableVO.CAT_STATIC) return true;
    	else return false;
    }

    protected String evaluateBindingValue(VariableVO childVar, String v) {
    	if (v!=null && v.length()>0) {
    		int varCat = childVar.getVariableCategory().intValue();
			if (varCat!=VariableVO.CAT_STATIC) {
				if (valueIsVariable(v)) {
					VariableInstanceInfo varinst = this.getVariableInstance(v.substring(1));
					v = varinst==null?null:varinst.getStringValue();
				} else {
					try {
					    if (valueIsMagicBoxExpression(v)) {
						    Object obj = evaluateExpression(getActivityId().toString(), MAGIC_BOX, v);
						    v = obj == null ? null : obj.toString();
					    }
					    else if (valueIsJavaExpression(v)) {
                            Object obj = evaluateExpression(getActivityId().toString(), JAVA_EL, v);
                            v = obj == null ? null : obj.toString();
					    }
					} catch (Exception e) {
						logger.warnException("Failed to evaluate the expression '" + v + "'", e);
						// treat v as it is
					}
				}
			} // else v is static value
		}
    	return v;
    }

    protected Map<String,String> getOutputParameters(Long subprocInstId, Long subprocId)
    	throws SQLException, ProcessException, DataAccessException {
    	ProcessVO subprocDef = ProcessVOCache.getProcessVO(subprocId);
    	Map<String,String> params = null;
    	for (VariableVO var : subprocDef.getVariables()) {
    		if (var.getVariableCategory().intValue()==VariableVO.CAT_OUTPUT
    				|| var.getVariableCategory().intValue()==VariableVO.CAT_INOUT) {
    			VariableInstanceInfo vio = getEngine().getVariableInstance(subprocInstId, var.getVariableName());
    			if (vio!=null) {
    				if (params==null) params = new HashMap<String,String>();
    				params.put(var.getVariableName(), vio.getStringValue());
    			}
    		}
    	}
    	return params;
    }

    @SuppressWarnings("unchecked")
	protected void addInternalVariable(Map<String, String> params, String key,
			String value) {
		Map<String, String> mdwUtilMap = null;
		if (StringHelper.isEmpty(value) || StringHelper.isEmpty(key))
			return;
		String mdwUtilMapString = params.get(VariableConstants.MDW_UTIL_MAP);
		if (!StringHelper.isEmpty(mdwUtilMapString))
			mdwUtilMap = (Map<String, String>) VariableTranslator.toObject(
					"java.util.Map", mdwUtilMapString);
		else
			mdwUtilMap = new HashMap<String, String>();
		mdwUtilMap.put(key, value);
		params.put(VariableConstants.MDW_UTIL_MAP,
				VariableTranslator.toString("java.util.Map", mdwUtilMap));
	}

    /**
     * TODO: Smart subprocess versioning for federated workflow.
     * TODO: Allow expressions that resolve to a version/spec.
     */
    protected ProcessVO getSubProcessVO(String name, String verSpec) throws DataAccessException {
        ProcessVO match = ProcessVOCache.getProcessVOSmart(new AssetVersionSpec(name, verSpec));
        if (match == null)
            throw new DataAccessException("Unable to find process definition for " + name + " v" + verSpec);
        return match;
    }
}
