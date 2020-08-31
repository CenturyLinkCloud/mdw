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
package com.centurylink.mdw.sync;

import com.centurylink.mdw.cache.asset.PackageCache;
import com.centurylink.mdw.model.variable.VariableInstance;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import org.apache.commons.jexl.Expression;
import org.apache.commons.jexl.ExpressionFactory;
import org.apache.commons.jexl.JexlContext;
import org.apache.commons.jexl.JexlHelper;

import java.util.List;
import java.util.Map;

/**
 * Synchronization expressions use <a href="http://commons.apache.org/jexl/reference/syntax.html">Java Expression Language syntax</a>.
 * Evaluation is based on activity statuses and can include process variables as well.  Expression should resolve
 * to a boolean value indicating whether process flow can proceed.
 */
public class SyncExpressionEvaluator {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private Package pkg;

    private String[] syncedActivityIds;
    public String[] getSyncedActivityIds() { return syncedActivityIds; }

    private String syncExpression;
    public String getSyncExpression() { return syncExpression; }

    private Map<String,String> idToEscapedName;

    /**
     * Creates a sync expression involving the specified activities.
     * @param pkg workflow package
     * @param syncedActivityIds the logical IDs of activities involved in the sync
     * @param syncExpression exampleFormat: <pre>A38 && (A12 || A18)</pre>
     */
    public SyncExpressionEvaluator(Package pkg, String[] syncedActivityIds,
            String syncExpression, Map<String,String> idToEscapedName) {
        this.syncedActivityIds = syncedActivityIds;
        this.idToEscapedName = idToEscapedName;
        if (syncExpression == null || syncExpression.trim().length() == 0)
            this.syncExpression = getDefaultSyncExpression();
        else
            this.syncExpression = syncExpression;

        if (pkg == null)
            this.pkg = PackageCache.getMdwBasePackage();
        else
            this.pkg = pkg;
    }

    /**
     * Checks whether the synchronization criteria can be considered to be met
     * by evaluating the expression versus the list of completed activities.
     * @param completedActivities logical ids of completed activities
     * @param variableInstances variable instances to use in evaluation
     * @return result of the evaluation
     */
    @SuppressWarnings("unchecked")
    public boolean evaluate(List<String> completedActivities, List<VariableInstance> variableInstances)
    throws SynchronizationException {
        if (syncedActivityIds == null || syncedActivityIds.length == 0)
            return true;

        try {
            Expression e = ExpressionFactory.createExpression(syncExpression);
            // create a context
            JexlContext jc = JexlHelper.createContext();
            if (syncedActivityIds != null && completedActivities != null) {
                // set the sync values
                for (String syncedActivityId : syncedActivityIds) {
                    Boolean isCompleted = completedActivities.contains(syncedActivityId);
                    jc.getVars().put(syncedActivityId, isCompleted);
                    // the following is for backward compatibility where escaped activity names are used
                    String escapedName = idToEscapedName.get(syncedActivityId);
                    if (escapedName!=null) jc.getVars().put(escapedName, isCompleted);
                }
            }
            if (variableInstances != null) {
                // set the variables
                for (VariableInstance variableInstance : variableInstances) {
                    jc.getVars().put(variableInstance.getName(), variableInstance.getData(pkg));
                }
            }

            // evaluate the expression
            Boolean b = (Boolean) e.evaluate(jc);
            return b.booleanValue();
        }
        catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            throw new SynchronizationException(ex.getMessage(), ex);
        }
    }

    /**
     * Create the default sync expression based on the synced activity logical IDs.
     */
    public String getDefaultSyncExpression() {
        String syncExpression = "";
        for (int i = 0; i < syncedActivityIds.length; i++) {
            if (i > 0)
                syncExpression += " && ";
            syncExpression += syncedActivityIds[i];
        }
        return syncExpression;
    }

}
