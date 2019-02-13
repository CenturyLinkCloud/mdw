package com.centurylink.mdw.microservice;

import java.util.List;

import org.json.JSONException;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.config.PropertyException;
import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.Status;
import com.centurylink.mdw.model.event.EventType;
import com.centurylink.mdw.model.variable.DocumentReference;
import com.centurylink.mdw.model.workflow.WorkStatus;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.workflow.activity.event.EventWaitActivity;

public class DependenciesWaitActivity extends EventWaitActivity {

    private static String MICROSERVICE_NAMES = "MICROSERVICE_NAMES";

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    @Override
    public void execute() throws ActivityException {
        if (dependenciesMet()) {
            setReturnCode(WorkStatus.STATUSNAME_COMPLETED + "::FINISH");
        }
        else {
            super.execute();
        }
    }

    @Override
    protected boolean handleCompletionCode() throws ActivityException {
        Integer exitStatus = WorkStatus.STATUS_COMPLETED;
        if (!dependenciesMet()) {
            logger.info(getActivityName() + "  *** not met, setting to waiting");
            exitStatus = WorkStatus.STATUS_WAITING;
            setActivityWaitingOnExit();
        }
        String compCode = this.getReturnCode();
        if (compCode != null
                && (compCode.length() == 0 || compCode.equals(EventType.EVENTNAME_FINISH)))
            compCode = null;
        String actInstStatusName;
        if (exitStatus == null)
            actInstStatusName = null;
        else if (exitStatus.equals(WorkStatus.STATUS_CANCELLED))
            actInstStatusName = WorkStatus.STATUSNAME_CANCELLED;
        else if (exitStatus.equals(WorkStatus.STATUS_WAITING))
            actInstStatusName = WorkStatus.STATUSNAME_WAITING;
        else if (exitStatus.equals(WorkStatus.STATUS_HOLD))
            actInstStatusName = WorkStatus.STATUSNAME_HOLD;
        else
            actInstStatusName = null;
        if (actInstStatusName != null) {
            if (compCode == null)
                compCode = actInstStatusName + "::";
            else
                compCode = actInstStatusName + "::" + compCode;
        }
        setReturnCode(compCode);
        if (WorkStatus.STATUS_WAITING.equals(exitStatus)) {
            try {
                registerWaitEvents(true, true);
            }
            catch (Exception e) {
                logger.info("Error in registerWaitEvents - " + e.getMessage());
                e.printStackTrace();
            }
            if (compCode
                    .startsWith(WorkStatus.STATUSNAME_WAITING + "::" + EventType.EVENTNAME_CORRECT)
                    || compCode.startsWith(
                            WorkStatus.STATUSNAME_WAITING + "::" + EventType.EVENTNAME_ABORT)
                    || compCode.startsWith(
                            WorkStatus.STATUSNAME_WAITING + "::" + EventType.EVENTNAME_ERROR))
                return true;
            else
                return false;
        }
        else
            return true;
    }

    /**
     * @return a table of microservices and expressions
     */
    public List<String[]> getMicroserviceAttributes() {
        String attVal = this.getAttributeValue(MICROSERVICE_NAMES);
        return StringHelper.parseTable(attVal, ',', ';', 3);
    }

    public boolean dependenciesMet() throws ActivityException {
        ServiceSummary serviceSummary = getServiceSummary(true); // Force read from DB to have latest
        if (serviceSummary == null) {
            // No service Summary, so throw exception since we shouldn't proceed
            // if we can't determine if dependencies are met
            logger.severe("Service summary not found");
            throw new ActivityException("Unable to determine if dependencies are met, "
                    + "service summary variable not found");
        }
        // if microservice is checked and not successful invocation, then
        // dependenciesMet=false and bunk out
        boolean dependenciesMet = true;
        List<String[]> microservices = getMicroserviceAttributes();
        int tag = 0;

        try {
            for (String[] microservice : microservices) {
                logger.info(getActivityName() + "  *** microservice[2] " + microservice[2]);
                Object expResult = getValueSmart(microservice[2], String.valueOf(tag++));
                String expString = null;
                if (expResult instanceof String) {
                    expString = (String) expResult;
                    logger.info(getActivityName() + "  *** 1st " + expString);
                    if (expString.isEmpty()) {
                        expResult = Boolean.TRUE;
                    }
                    else {
                        expResult = Boolean.parseBoolean(expString);
                    }
                }
                if (!(expResult instanceof Boolean)) {
                    throw new ActivityException("MicroserviceDependenciesWaitActivity - expression "
                            + microservice[2] + " does not evaluate to Boolean");
                }

                Boolean expResultBool = (Boolean) expResult;
                logger.info(getActivityName() + "  *** expString " + (String) expString + " is "
                        + expResultBool);
                if (Boolean.parseBoolean(microservice[0])) {
                    // check it
                    // if microservice isn't populated then do the check based
                    // on
                    // the expression
                    if ((StringHelper.isEmpty(microservice[1]) && !expResultBool.booleanValue())
                            || (expResultBool.booleanValue()
                                    && !StringHelper.isEmpty(microservice[1])
                                    && microServiceSuccess(microservice[1],
                                            serviceSummary) == null)) {
                        // service has been checked, but no successful response
                        // was
                        // received
                        dependenciesMet = false;
                        break;
                    }
                }
            }
            logger.info(getActivityName() + "  *** dependenciesMet " + dependenciesMet);
            return dependenciesMet;
        }
        catch (PropertyException ex) {
            throw new ActivityException(ex.getMessage(), ex);
        }

    }

    /**
     * <p>
     * Looks through the service summary for a successful completion for a
     * certain microservice:
     * <li>1. First checks the invocations for a 200 response</li>
     * <li>2. If none found then checks the updates for a 200 Completion
     * response</li>
     * </p>
     *
     * @param microservice
     *            to check for successful completion
     * @param serviceSummary
     *            object to check for successful completions
     * @see ServiceSummary
     * @return a Jsonable as a future-proof in case we need to know which
     *         invocation/update was successful
     * @throws JSONException
     */
    public Jsonable microServiceSuccess(String microservice, ServiceSummary serviceSummary) {
        MicroserviceList microserviceList = serviceSummary.getMicroservices(microservice);
        if (microserviceList != null) {
            for (MicroserviceInstance instance : microserviceList.getInstances()) {
                List<Invocation> invocations = instance.getInvocations();
                if (invocations != null) {
                    for (Invocation invocation : invocations) {
                        if (invocation.getStatus().getCode() == Status.OK.getCode()) {
                            return invocation;
                        }
                    }
                }
                List<Update> updates = instance.getUpdates();
                if (updates != null) {
                    for (Update update : updates) {
                        if (update.getStatus().getCode() == Status.OK.getCode()) {
                            return update;
                        }
                    }
                }
            }
        }
        // Now check all children ServiceSummaries
        if (serviceSummary.getChildServiceSummaryList() != null) {
            for (ServiceSummary child : serviceSummary.getChildServiceSummaryList()) {
                Jsonable match = microServiceSuccess(microservice, child);
                if (match != null)
                    return match;
            }
        }
        return null;
    }

    protected ServiceSummary getServiceSummary(boolean forUpdate) throws ActivityException {
        DocumentReference docRef = (DocumentReference)getParameterValue(getServiceSummaryVariableName());
        if (docRef == null)
            return null;
        if (forUpdate)
            return (ServiceSummary) getDocumentForUpdate(docRef, Jsonable.class.getName());
        else
            return (ServiceSummary) getDocument(docRef, Jsonable.class.getName());
    }

    /**
     * You'd need a custom .impl asset to set this through designer
     */
    protected String getServiceSummaryVariableName() {
        return getAttribute("serviceSummaryVariable", "serviceSummary");
    }
}
