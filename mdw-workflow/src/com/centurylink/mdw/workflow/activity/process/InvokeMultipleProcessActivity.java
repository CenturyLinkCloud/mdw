/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.activity.process;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.common.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.common.translator.VariableTranslator;
import com.centurylink.mdw.common.utilities.logger.StandardLogger.LogLevel;
import com.centurylink.mdw.common.utilities.timer.Tracked;
import com.centurylink.mdw.groovy.DynaRow;
import com.centurylink.mdw.model.data.work.WorkStatus;
import com.centurylink.mdw.model.value.event.EventWaitInstanceVO;
import com.centurylink.mdw.model.value.event.InternalEventVO;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.variable.DocumentReference;
import com.centurylink.mdw.model.value.variable.VariableVO;
import com.centurylink.mdw.services.dao.process.cache.ProcessVOCache;
import com.centurylink.mdw.services.process.ProcessExecuter;
import com.qwest.mbeng.MbengException;
import com.qwest.mbeng.MbengTable;
import com.qwest.mbeng.MbengTableArray;
import com.qwest.mbeng.MbengTableSchema;

/**
 * This activity implementor implements invocation of sub processes.
 * @deprecated @see InvokeHeterogeneousProcessActivity
 */
@Tracked(LogLevel.TRACE)
@Deprecated
public class InvokeMultipleProcessActivity extends InvokeProcessActivityBase {

	private static final String TABLE_VARIABLE = "ParameterTable";
	private static final String KEY_COLUMN = "KeyColumn";
	private static final String RETURN_CODE_COLUMN = "ReturnCodeColumn";
	private static final String DELAY_BETWEEN = "DELAY_BETWEEN";
    private static final String SYNCHRONOUS = "synchronous";

    // TODO instance var
    private int rowCount = 0;

    public boolean needSuspend() {
        String v = getAttributeValue(SYNCHRONOUS);
        return ((v==null || v.equalsIgnoreCase("TRUE")) && rowCount > 0);
    }

    private String getTableValue(MbengTable table, Object row, String name)
            throws MbengException
    {
        MbengTableSchema schema = table.getSchema();
        for (int j=0; j<schema.getColumnCount(); j++) {
            if (schema.getColumnName(j).equals(name)) return table.getValue(row, j);
        }
        return null;
    }

    private void setTableValue(MbengTable table, Object row, String name, String value)
        throws MbengException
    {
        MbengTableSchema schema = table.getSchema();
        for (int j=0; j<schema.getColumnCount(); j++) {
            if (schema.getColumnName(j).equals(name)) {
                table.setValue(row, j, value);
                return;
            }
        }
    }

    /**
     * This method returns variable bindings to be passed into subprocess.
     * The method uses the attribute "variables" values as a mapping.
     * The binding of each variable is an expression in the Magic Box rule language.
     * Example bindings: "var1=12*12;var2=$parent_var.LIST.LN"
     * Subclass may override this method to obtain variable binding in other ways.
     *
     * @param childVars variables defined for the child process
     * @param prMgr process manager remote EJB handle
     * @param varMgr variable manager remote EJB handle
     * @return a map (name-value pairs) of variable bindings
     * @throws Exception various types of exceptions
     */
    private Map<String,String> createVariableBinding(List<VariableVO> childVars,
    			MbengTable table, Object row)
    		throws Exception {
        Map<String,String> validParams = new HashMap<String,String>();

        String vn, v;
        for (VariableVO childVar : childVars) {
        	if (!allowInput(childVar)) continue;
        	vn = childVar.getVariableName();
        	v = evaluateBindingValue(childVar, getTableValue(table, row, vn));
        	if (v!=null && v.length()>0) validParams.put(vn, v);
        }
        return validParams;
    }

    /**
     * Creates the bindings based on DynaRows.  Currently does not support expressions.
     */
    private Map<String, String> createVariableBinding(List<VariableVO> childVars, DynaRow dynaRow) throws Exception {
        Map<String, String> validParams = new HashMap<String, String>();
        for (VariableVO childVar : childVars) {
        	if (!allowInput(childVar)) continue;
            String vn = childVar.getVariableName();
            String varType = childVar.getVariableType();
            Object v = dynaRow.getProperty(vn);
            if (v != null) {
                if (VariableTranslator.isDocumentReferenceVariable(varType)) {
                    Object doc = DocumentReferenceTranslator.realToObject(varType, (String)v);
                    DocumentReference docRef = createDocument(varType, doc, OwnerType.PROCESS_INSTANCE, this.getProcessInstanceId(), null, null);
                    validParams.put(vn, docRef.toString());
                }
                else if (v instanceof String) {
                    validParams.put(vn, (String)v);
                }
                else {
                    validParams.put(vn, VariableTranslator.toString(varType, v));
                }
            }
        }
        return validParams;
    }

    /**
     * By default, the implementor assumes the control table is already
     * initialized and bound to the variable specified in the attribute "Parameter Table".
     * You can override this method to create the table here instead.
     * The implementor will bind the table to the specified variable
     * @return default method returns null, which implies parameter table variable is already bound.
     *     Overriding method needs to return desired table.
     */
    protected MbengTable getBindingTable() throws ActivityException {
        return null;
    }

    private ProcessVO getSubProcessVO() throws Exception {
        String procname = this.getAttributeValue(WorkAttributeConstant.PROCESS_NAME);
        String subproc_version = this.getAttributeValue(WorkAttributeConstant.PROCESS_VERSION);
        int version = (subproc_version==null?0:Integer.parseInt(subproc_version));
        return ProcessVOCache.getProcessVO(procname, version);
    }

    public void execute() throws ActivityException{

        try{
        	ProcessVO subprocdef = getSubProcessVO();
            if (isLogDebugEnabled())
                logdebug("Invoking subprocess: " + subprocdef.getLabel());

	        long activityInstanceId = this.getActivityInstanceId().longValue();
	        List<VariableVO> childVars = subprocdef.getVariables();
            MbengTable table = getBindingTable();
            List<?> dynaRows = null;
            String table_varname = getAttributeValue(TABLE_VARIABLE);
            DocumentReference docref;
            List<ProcessInstanceVO> procInstList = new ArrayList<ProcessInstanceVO>();
            if (table==null) {
                Object binding = getParameterValue(table_varname);
                if (binding==null || ! (binding instanceof DocumentReference))
                    throw new ActivityException("InvokeMultipleProcess: control variable is not bound.");
                docref = (DocumentReference)binding;
                binding = getDocumentForUpdate(docref, getParameterType(table_varname));
                if (binding instanceof MbengTable)
                    table = (MbengTable)binding;
                else if (binding instanceof List<?>)
                    dynaRows = (List<?>)binding;
                else
                    throw new ActivityException("InvokeMultipleProcess: control variable is not bound to a table or list");
            } else {
                docref = setParameterValueAsDocument(table_varname, MbengTableArray.class.getName(), table);
            }
//            String key_column = (String)getAttributeValue(KEY_COLUMN);
            String return_code_column = getAttributeValue(RETURN_CODE_COLUMN);
            String delayStr = getAttributeValue(DELAY_BETWEEN);
            int pDelay = (delayStr==null)?0:Integer.parseInt(delayStr);
            rowCount = dynaRows == null ? table.getRowCount() : dynaRows.size();
            Map<String,String> validParams = null;
            ProcessExecuter engine = getEngine();
            for (int i=0; i<rowCount; i++) {
                if (dynaRows != null) {
                    DynaRow dynaRow = (DynaRow) dynaRows.get(i);
                    dynaRow.setProperty(return_code_column, "START");
                    validParams = createVariableBinding(childVars, dynaRow);
                }
                else {
                    Object row = table.getRow(i);
                    setTableValue(table, row, return_code_column, "START");
                    validParams = createVariableBinding(childVars, table, row);
                }
//
//            	InternalEventVO evMsg = InternalEventVO.createProcessStartMessage(
//            			subprocdef.getProcessId(), OwnerType.PROCESS_INSTANCE, this.getProcessInstanceId(),
//            			this.getMasterRequestId(), null, OwnerType.ACTIVITY_INSTANCE, activityInstanceId);
        		ProcessInstanceVO pi = engine.createProcessInstance(subprocdef.getProcessId(),
        				OwnerType.PROCESS_INSTANCE, this.getProcessInstanceId(),
        				OwnerType.ACTIVITY_INSTANCE, activityInstanceId, getMasterRequestId(), validParams);
        		procInstList.add(pi);
            }
            if (dynaRows != null)
                updateDocumentContent(docref, dynaRows, Object.class.getName());
            else
                updateDocumentContent(docref, table, MbengTableArray.class.getName());
            EventWaitInstanceVO received = registerWaitEvents(false, true);
            if (received!=null)
            	resume_on_other_event(getExternalEventInstanceDetails(received.getMessageDocumentId()), received.getCompletionCode());
    		for (int i=0; i<procInstList.size(); i++) {
    			engine.startProcessInstance(procInstList.get(i), pDelay*i);
    		}
        } catch (ActivityException ex) {
        	throw ex;
        }catch(Exception ex){
            logger.severeException(ex.getMessage(), ex);
            throw new ActivityException(-1, ex.getMessage(), ex);
        }

    }

    private boolean isDynaRows() throws ActivityException {
        String tableVar = getAttributeValue(TABLE_VARIABLE);
        if (tableVar == null)
            return false;
        return getParameterType(tableVar).equals(Object.class.getName());
    }

    boolean resume_on_process_finish(InternalEventVO msg, Integer status)
    	throws ActivityException {
    	boolean done;
        try {
            String key_column = getAttributeValue(KEY_COLUMN);
            String table_varname = getAttributeValue(TABLE_VARIABLE);

            Long subprocInstId = msg.getWorkInstanceId();
            Map<String,String> params = getOutputParameters(subprocInstId, msg.getWorkId());
            String key = null;
            if (params!=null) {
            	// the key must be INOUT, not just INPUT
	            for (String param_name : params.keySet()) {
	            	if (param_name.equals(key_column)) {
	            		key = params.get(param_name);
	            		break;
	            	}
	            }
            }
            if (key==null) throw new ActivityException(
        		"Cannot find key variable of subprocess instance in InvokeMultipleProcess");

            DocumentReference docref = (DocumentReference)getParameterValue(table_varname);
            if (isDynaRows())
                done = updateForDynaRows(docref, params, key_column, key);
            else
                done = updateForMbengTable(docref, params, key_column, key);
        } catch (Exception ex) {
            logger.severeException("InvokeMultipleProcessActivity: cannot get variable instance", ex);
            throw new ActivityException(-1, ex.getMessage(), ex);
        }
        if (done && status.equals(WorkStatus.STATUS_HOLD)) done = false;
		if (done) super.deregisterEvents();
        return done;
    }

    private boolean updateForMbengTable(DocumentReference docRef, Map<String,String> params, String key_column, String key) throws Exception {
        String ret_column = getAttributeValue(RETURN_CODE_COLUMN);
        MbengTable table = (MbengTable) getDocumentForUpdate(docRef, MbengTableArray.class.getName());
        Object row = null;
        int completed = 0;
        for (int i=0; i<table.getRowCount(); i++) {
            Object r = table.getRow(i);
            if (row==null && getTableValue(table, r, key_column).equals(key)) {
                row = r;
            }
            if (!getTableValue(table, r, ret_column).equals("START"))
                completed++;
        }
        if (row==null) throw new ActivityException(
            "Cannot find table row matching the subprocess key in InvokeMultipeProcess");
        this.setTableValue(table, row, ret_column, "FINISH");
        completed++;
        for (String varname : params.keySet()) {
            this.setTableValue(table, row, varname, params.get(varname));
        }
        updateDocumentContent(docRef, table, MbengTableArray.class.getName());
        return (completed>=table.getRowCount());
	}

    private boolean updateForDynaRows(DocumentReference docRef, Map<String,String> params, String key_column, String key) throws Exception {
        String ret_column = getAttributeValue(RETURN_CODE_COLUMN);
        List<?> dynaRows = (List<?>) getDocumentForUpdate(docRef, Object.class.getName());

        int completed = 0;

        DynaRow row = null;
        for (int i = 0; i < dynaRows.size(); i++) {
            DynaRow dynaRow = (DynaRow) dynaRows.get(i);
            if (row == null && dynaRow.getProperty(key_column).equals(key))
                row = dynaRow;
            if (!dynaRow.getProperty(ret_column).equals("START"))
                completed++;
        }

        if (row == null)
            throw new ActivityException("Cannot find dynarow matching the subprocess key in InvokeMultipeProcess");

        row.setProperty(ret_column, "FINISH");
        completed++;
        String[] propNames = row.getPropertyNamesSorted();
        for (String varname : params.keySet()) {
            if (!varname.equals(ret_column) && Arrays.binarySearch(propNames, varname) >= 0) {
                Object paramVal = params.get(varname);
                if (paramVal != null) {
                    if (((String)paramVal).startsWith("DOCUMENT:")) {
                    	DocumentReference docref = new DocumentReference(new Long(((String)paramVal).substring(9)), null);
                        paramVal = getDocument(docref, Object.class.getName());
                    }
                    row.setProperty(varname, paramVal);
                }
            }
        }
        updateDocumentContent(docRef, dynaRows, Object.class.getName());
        return completed >= dynaRows.size();
    }

    protected boolean allSubProcessCompleted() throws ActivityException, MbengException {
        String ret_column = getAttributeValue(RETURN_CODE_COLUMN);
        String table_varname = getAttributeValue(TABLE_VARIABLE);

        DocumentReference docref = (DocumentReference)this.getParameterValue(table_varname);
        MbengTable table = (MbengTable)getDocumentForUpdate(docref, MbengTableArray.class.getName());
        int completed = 0;
        for (int i=0; i<table.getRowCount(); i++) {
            Object r = table.getRow(i);
            if (!getTableValue(table, r, ret_column).equals("START"))
                completed++;
        }
        completed++;
        return (completed>=table.getRowCount());
    }

}
