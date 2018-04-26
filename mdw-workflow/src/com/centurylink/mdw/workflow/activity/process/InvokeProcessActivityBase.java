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
package com.centurylink.mdw.workflow.activity.process;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.activity.types.InvokeProcessActivity;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.asset.AssetVersionSpec;
import com.centurylink.mdw.model.event.EventWaitInstance;
import com.centurylink.mdw.model.event.InternalEvent;
import com.centurylink.mdw.model.variable.VariableInstance;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.service.data.process.ProcessCache;
import com.centurylink.mdw.services.ProcessException;
import com.centurylink.mdw.util.TransactionWrapper;
import com.centurylink.mdw.workflow.activity.AbstractWait;

/**
 * This abstract activity implementor implements the common funciton
 * for invocation of sub processes, whether it is single, multiple or heterogeneous
 *
 *
 */

public abstract class InvokeProcessActivityBase extends AbstractWait
        implements InvokeProcessActivity {

    public final boolean resumeWaiting(InternalEvent msg)
            throws ActivityException {
        TransactionWrapper transaction = null;
        try {
            transaction = startTransaction();
            lockActivityInstance();
            if (allSubProcessCompleted()) {
                return true;
            } else {
                EventWaitInstance received = registerWaitEvents(true, true);
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

    @Deprecated
    boolean resume_on_other_event(String msg, String compCode) throws ActivityException {
        return resumeOnOtherEvent(msg, compCode);
    }

    protected boolean resumeOnOtherEvent(String msg, String compCode)
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

    protected abstract boolean resumeOnProcessFinish(InternalEvent msg, Integer status)
    throws ActivityException;

    public final boolean resume(InternalEvent msg)
        throws ActivityException {
        TransactionWrapper transaction = null;
        try {
            transaction = startTransaction();
            Integer status = lockActivityInstance();
            if (msg.isProcess()) {
                boolean done = resumeOnProcessFinish(msg, status);
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

    protected boolean allowInput(Variable childVar) {
        int varCat = childVar.getVariableCategory().intValue();
        if (varCat==Variable.CAT_INPUT || varCat==Variable.CAT_INOUT
                || varCat==Variable.CAT_STATIC) return true;
        else return false;
    }

    protected String evaluateBindingValue(Variable childVar, String v) {
        if (v != null && v.length() > 0) {
            int varCat = childVar.getVariableCategory().intValue();
            if (varCat!=Variable.CAT_STATIC) {
                if (valueIsVariable(v)) {
                    VariableInstance varinst = this.getVariableInstance(v.substring(1));
                    v = varinst==null?null:varinst.getStringValue();
                }
                else if (v.startsWith("${") && v.endsWith("}") && v.indexOf('.') == -1 && v.indexOf('[') == -1) {
                    VariableInstance varinst = this.getVariableInstance(v.substring(2, v.length() - 1));
                    v = varinst==null?null:varinst.getStringValue();
                }
                else {
                    try {
                        if (valueIsJavaExpression(v)) {
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
        Process subprocDef = ProcessCache.getProcess(subprocId);
        Map<String,String> params = null;
        for (Variable var : subprocDef.getVariables()) {
            if (var.getVariableCategory().intValue()==Variable.CAT_OUTPUT
                    || var.getVariableCategory().intValue()==Variable.CAT_INOUT) {
                VariableInstance vio = getEngine().getVariableInstance(subprocInstId, var.getName());
                if (vio!=null) {
                    if (params==null) params = new HashMap<String,String>();
                    params.put(var.getName(), vio.getStringValue());
                }
            }
        }
        return params;
    }

    /**
     * TODO: Allow expressions that resolve to a version/spec.
     */
    protected Process getSubprocess(String name, String verSpec) throws DataAccessException {
        Process match = ProcessCache.getProcessSmart(new AssetVersionSpec(name, verSpec));
        if (match == null)
            throw new DataAccessException("Unable to find process definition for " + name + " v" + verSpec);
        return match;
    }
}
