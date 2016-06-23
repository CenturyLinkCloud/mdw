/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.artis;

import java.io.Serializable;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.qwest.artis.Artis;
import com.qwest.artis.ArtisFunctionConfiguration;
import com.qwest.artis.ArtisFunctionDescription;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.model.value.activity.ActivityRuntimeContext;
import com.centurylink.mdw.model.value.process.ProcessRuntimeContext;

public class ArtisProcessVariables implements Serializable {

    private static final Log LOG = LogFactory.getLog(ArtisProcessVariables.class);

    private Boolean useArtis = false;
    private String serviceName = "";
    private String functionLabel = "";
    private String startInfo = "";
    private String suppInfo = "";

    private final String STARTINFO_SEP = "|";

    public ArtisProcessVariables() {

    }

    public ArtisProcessVariables(ProcessRuntimeContext context, Artis artis, boolean forceAll,
            boolean forceWithConfig) {
        boolean forceArtis = forceAll;

        String defaultServiceName = context.getProcess().getName();
        String defaultFunctionLabel = context.getProcess().getName();

        ArtisFunctionDescription requestFunction = checkRequestPropertiesForValues(
                defaultServiceName, artis);

        if (requestFunction != null && requestFunction.getServiceName() != null
                && requestFunction.getFunctionLabel() != null) {
            defaultServiceName = requestFunction.getServiceName();
            defaultFunctionLabel = requestFunction.getFunctionLabel();
            if (!forceArtis)
                forceArtis = forceWithConfig;
        }

        setVariables(context.getAttributes(), defaultServiceName, defaultFunctionLabel, forceArtis);
        this.startInfo = context.getMasterRequestId() + STARTINFO_SEP + context.getProcessId()
                + STARTINFO_SEP + context.getProcess().getVersion() + STARTINFO_SEP
                + context.getProcessInstanceId();
    }

    public ArtisProcessVariables(ActivityRuntimeContext context,
            ArtisProcessVariables processVariables, Artis artis, boolean forceAll,
            boolean forceWithConfig) {
        boolean forceArtis = forceAll;

        String defaultServiceName = processVariables.getServiceName();
        String defaultFunctionLabel = context.getActivity().getLogicalId() + "_"
                + context.getActivity().getActivityName();

        ArtisFunctionDescription requestFunction = checkCalloutPropertiesForValues(
                defaultServiceName + "_" + defaultFunctionLabel, artis);

        if (requestFunction != null && requestFunction.getServiceName() != null
                && requestFunction.getFunctionLabel() != null) {
            defaultServiceName = requestFunction.getServiceName();
            defaultFunctionLabel = requestFunction.getFunctionLabel();
            if (!forceArtis)
                forceArtis = forceWithConfig;
        }

        setVariables(context.getAttributes(), defaultServiceName, defaultFunctionLabel, forceArtis);

        this.startInfo = context.getActivity().getLogicalId() + STARTINFO_SEP
                + context.getActivityInstanceId();

        if (this.useArtis) {
            if (!processVariables.getUseArtis()) {
                this.useArtis = false;
                LOG.warn("Activity states to use Artis but is not selected at the process level. Please fix workflow to properly declare Artis variables");
            }
        }
    }

    private void setVariables(Map<String, String> variables, String defaultServiceName,
            String defaultFunctionLabel, boolean forceArtis) {
        LOG.debug("in ArtisProcessVariables.setVariables: defaultServiceName: "
                + defaultServiceName + " defaultFunctionLabel: " + defaultFunctionLabel
                + " forceArtis:" + forceArtis);

        this.useArtis = false;

        if (forceArtis) {
            this.useArtis = true;
        }
        else {
            if (variables.containsKey(MDWArtisConstants.ARTISUSEARTIS)) {
                String useArtis = variables.get(MDWArtisConstants.ARTISUSEARTIS);
                if (useArtis.equalsIgnoreCase("true"))
                    this.useArtis = true;
            }
        }

        // Default to using the Designer Variables for this
        this.serviceName = (String) variables.get(MDWArtisConstants.ARTISSERVICENAME);
        this.functionLabel = (String) variables.get(MDWArtisConstants.ARTISFUNCTIONLABEL);

        if (this.useArtis) {
            if (StringHelper.isEmpty(this.serviceName)) {
                if (StringHelper.isEmpty(defaultServiceName)) {
                    LOG.warn("ServiceName is empty, Disabling Artis. Please fix workflow to properly declare Artis variables");
                    this.useArtis = false;
                }
                else {
                    this.serviceName = defaultServiceName;
                }
            }

            if (StringHelper.isEmpty(this.functionLabel)) {
                if (StringHelper.isEmpty(defaultFunctionLabel)) {
                    LOG.warn("ServiceName or FunctionLabel is empty, Disabling Artis. Please fix workflow to properly declare Artis variables");
                    this.useArtis = false;
                }
                else {
                    this.functionLabel = defaultFunctionLabel;
                }
            }
        }
    }

    private ArtisFunctionDescription checkRequestPropertiesForValues(String key, Artis artis) {
        LOG.debug("Finding Values in Request Functions based on " + key);
        ArtisFunctionConfiguration requestFunctions = artis.getRequestsFunctions();

        ArtisFunctionDescription requestFunction = requestFunctions.findExactFunction(key);

        if (requestFunction != null) {
            LOG.debug("Exact: " + requestFunction.getFunctionLabel() + " - "
                    + requestFunction.getServiceName());
        }
        else {
            LOG.debug("Nothing Found, using passed in defaults");
        }

        return requestFunction;
    }

    private ArtisFunctionDescription checkCalloutPropertiesForValues(String key, Artis artis) {
        LOG.debug("Finding Values in Callout Functions based on " + key);
        ArtisFunctionConfiguration calloutFunctions = artis.getCalloutsFunctions();

        ArtisFunctionDescription calloutFunction = calloutFunctions.findExactFunction(key);

        if (calloutFunction != null) {
            LOG.debug("Exact: " + calloutFunction.getFunctionLabel() + " - "
                    + calloutFunction.getServiceName());
        }
        else {
            LOG.debug("Nothing Found, using passed in defaults");
        }

        return calloutFunction;
    }

    public Boolean getUseArtis() {
        return useArtis;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getFunctionLabel() {
        return functionLabel;
    }

    public String getStartInfo() {
        return startInfo;
    }

    public String getSuppInfo() {
        return suppInfo;
    }

    @Override
    public String toString() {
        return "ArtisProcessVariables [useArtis=" + useArtis + ", serviceName=" + serviceName
                + ", functionLabel=" + functionLabel + ", startInfo=" + startInfo + ", suppInfo="
                + suppInfo + "]";
    }
}
