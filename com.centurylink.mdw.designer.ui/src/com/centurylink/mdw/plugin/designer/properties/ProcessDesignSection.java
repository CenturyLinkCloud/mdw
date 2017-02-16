/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.viewers.IFilter;
import org.eclipse.swt.widgets.Composite;

import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.TimeInterval;
import com.centurylink.mdw.plugin.designer.properties.editor.TimeInterval.Units;
import com.centurylink.mdw.plugin.designer.properties.editor.ValueChangeListener;
import com.centurylink.mdw.plugin.designer.properties.editor.WorkflowAssetEditor;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.common.constant.WorkAttributeConstant;

public class ProcessDesignSection extends PropertySection implements IFilter {
    private WorkflowProcess process;

    public WorkflowProcess getProcess() {
        return process;
    }

    private PropertyEditor syncPropertyEditor;
    private PropertyEditor slaPropertyEditor;
    private PropertyEditor performancePropertyEditor;
    private PropertyEditor transitionOutcomePropertyEditor;
    private PropertyEditor transitionRetryPropertyEditor;
    private PropertyEditor startPagePropertyEditor;
    private PropertyEditor processConfigHelpLinkEditor;
    private PropertyEditor customPageRenderingEditor;
    private PropertyEditor customPageHelpLinkEditor;
    private PropertyEditor renderingInfoEditor;

    public void setSelection(WorkflowElement selection) {
        process = (WorkflowProcess) selection;

        syncPropertyEditor.setElement(process);
        syncPropertyEditor.setValue(process.isSynchronous());
        syncPropertyEditor.setEditable(!process.isReadOnly());

        slaPropertyEditor.setElement(process);
        String slaAttr = process.getAttribute(WorkAttributeConstant.SLA);
        if (slaAttr != null) {
            int slaVal = Integer.parseInt(slaAttr);
            if (slaVal > 0) {
                String unitsAttr = process.getAttribute(WorkAttributeConstant.SLA_UNIT);
                Units units = unitsAttr == null ? Units.Minutes : Units.valueOf(unitsAttr);
                slaPropertyEditor.setValue(slaAttr, units);
            }
        }
        slaPropertyEditor.setEditable(!process.isReadOnly());

        performancePropertyEditor.setElement(process);
        int perfLevel = process.getPerformanceLevel();
        for (String opt : getPerformanceLevelOptions()) {
            if (opt.startsWith(perfLevel + " -"))
                performancePropertyEditor.setValue(opt);
        }
        performancePropertyEditor.setEditable(!process.isReadOnly());

        transitionOutcomePropertyEditor.setElement(process);
        String transOutcome = process.getEmptyTransitionOutcome();
        for (String opt : getTransitionOutcomeOptions()) {
            if (opt.startsWith(transOutcome))
                transitionOutcomePropertyEditor.setValue(opt);
        }
        transitionOutcomePropertyEditor.setEditable(!process.isReadOnly());

        transitionRetryPropertyEditor.setElement(process);
        int limit = process.getDefaultTransitionRetryLimit();
        for (String opt : getTransitionRetryOptions()) {
            if (opt.indexOf("(" + limit + ")") > 0)
                transitionRetryPropertyEditor.setValue(opt);
        }
        transitionRetryPropertyEditor.setEditable(!process.isReadOnly());

        processConfigHelpLinkEditor.setElement(process);
        processConfigHelpLinkEditor.setValue("/MDWHub/doc/process.html");

        startPagePropertyEditor.setElement(process);
        startPagePropertyEditor.setValue(process.getStartPage());
        startPagePropertyEditor.setEditable(!process.isReadOnly());

        customPageRenderingEditor.setElement(process);
        String rendering = process.getRenderingEngine();
        for (String opt : getPageRenderingOptions()) {
            if (opt.equals(rendering))
                customPageRenderingEditor.setValue(opt);
        }
        customPageRenderingEditor.setEditable(!process.isReadOnly());

        renderingInfoEditor.setElement(process);
        renderingInfoEditor.setValue(" Blank rendering defers to mdw.default.rendering.engine");

        customPageHelpLinkEditor.setElement(process);
        customPageHelpLinkEditor.setValue("/MDWHub/doc/customWeb.html");
    }

    public void drawWidgets(Composite composite, WorkflowElement selection) {
        process = (WorkflowProcess) selection;

        // sla timer field
        slaPropertyEditor = new PropertyEditor(process, PropertyEditor.TYPE_TIMER);
        slaPropertyEditor.setLabel("SLA");
        slaPropertyEditor.setAcceptedUnits(new Units[] { Units.Minutes, Units.Hours, Units.Days });
        slaPropertyEditor.addValueChangeListener(new ValueChangeListener() {
            public void propertyValueChanged(Object newValue) {
                TimeInterval.TimerValue timerValue = (TimeInterval.TimerValue) newValue;
                process.setAttribute(WorkAttributeConstant.SLA, timerValue.getInterval());
                process.setAttribute(WorkAttributeConstant.SLA_UNIT,
                        timerValue.getUnits().toString());
            }
        });
        slaPropertyEditor.render(composite);

        // synchronous checkbox
        syncPropertyEditor = new PropertyEditor(process, PropertyEditor.TYPE_CHECKBOX);
        syncPropertyEditor.setLabel("Service Process");
        syncPropertyEditor.addValueChangeListener(new ValueChangeListener() {
            public void propertyValueChanged(Object newValue) {
                Boolean value = (Boolean) newValue;
                process.setSynchronous(value.booleanValue());
            }
        });
        syncPropertyEditor.render(composite);

        // performance level dropdown
        performancePropertyEditor = new PropertyEditor(process, PropertyEditor.TYPE_COMBO);
        performancePropertyEditor.setLabel("Performance Level");
        performancePropertyEditor.setValueOptions(getPerformanceLevelOptions());
        performancePropertyEditor.setWidth(400);
        performancePropertyEditor.setReadOnly(true);
        performancePropertyEditor.addValueChangeListener(new ValueChangeListener() {
            public void propertyValueChanged(Object newValue) {
                String sel = (String) newValue;
                process.setPerformanceLevel(
                        Integer.parseInt(sel.substring(0, sel.indexOf('-') - 1).trim()));
            }
        });
        performancePropertyEditor.render(composite);

        // transition outcome radio
        transitionOutcomePropertyEditor = new PropertyEditor(process, PropertyEditor.TYPE_RADIO);
        transitionOutcomePropertyEditor.setLabel("Empty Transition Outcome");
        transitionOutcomePropertyEditor.setValueOptions(getTransitionOutcomeOptions());
        transitionOutcomePropertyEditor.addValueChangeListener(new ValueChangeListener() {
            public void propertyValueChanged(Object newValue) {
                process.setEmptyTransitionOutcome(((String) newValue).trim());
            }
        });
        transitionOutcomePropertyEditor.render(composite);

        // transition retry
        transitionRetryPropertyEditor = new PropertyEditor(process, PropertyEditor.TYPE_RADIO);
        transitionRetryPropertyEditor.setLabel("Transition Retry Default Limit");
        transitionRetryPropertyEditor.setValueOptions(getTransitionRetryOptions());
        transitionRetryPropertyEditor.addValueChangeListener(new ValueChangeListener() {
            public void propertyValueChanged(Object newValue) {
                String sel = (String) newValue;
                process.setDefaultTransitionRetryLimit(
                        Integer.parseInt(sel.substring(sel.indexOf('(') + 1, sel.indexOf(')'))));
            }
        });
        transitionRetryPropertyEditor.render(composite);

        // process config help link
        processConfigHelpLinkEditor = new PropertyEditor(process, PropertyEditor.TYPE_LINK);
        processConfigHelpLinkEditor.setLabel("Process Configuration Help");
        processConfigHelpLinkEditor.render(composite);

        // start page asset field
        startPagePropertyEditor = new WorkflowAssetEditor(process,
                WorkAttributeConstant.PROCESS_START_PAGE,
                Arrays.asList(new String[] { RuleSetVO.FACELET }));
        startPagePropertyEditor.setLabel("Custom Start Page");
        startPagePropertyEditor.addValueChangeListener(new ValueChangeListener() {
            public void propertyValueChanged(Object newValue) {
                process.setStartPage((String) newValue);
            }
        });
        startPagePropertyEditor.render(composite);

        // rendering engine
        // transition outcome radio
        customPageRenderingEditor = new PropertyEditor(process, PropertyEditor.TYPE_RADIO);
        customPageRenderingEditor.setLabel("Custom Page Rendering");
        customPageRenderingEditor.setValueOptions(getPageRenderingOptions());
        customPageRenderingEditor.addValueChangeListener(new ValueChangeListener() {
            public void propertyValueChanged(Object newValue) {
                process.setRenderingEngine(((String) newValue).trim());
            }
        });
        customPageRenderingEditor.render(composite);

        renderingInfoEditor = new PropertyEditor(process, PropertyEditor.TYPE_INFO);
        renderingInfoEditor.render(composite);

        // custom page config help link
        customPageHelpLinkEditor = new PropertyEditor(process, PropertyEditor.TYPE_LINK);
        customPageHelpLinkEditor.setLabel("Custom Page Help");
        customPageHelpLinkEditor.render(composite);
    }

    private List<String> perfLevelOptions;

    private List<String> getPerformanceLevelOptions() {
        if (perfLevelOptions == null) {
            perfLevelOptions = new ArrayList<String>();
            perfLevelOptions.add("0 - determined by global properties");
            perfLevelOptions.add("1 - No caching (one-message-per-transition for regular process)");
            perfLevelOptions.add(
                    "3 - Read cache variables/documents (multi-message-per-transition for regular process)");
            perfLevelOptions
                    .add("5 - Cache only for variable/documents (for service process only)");
            perfLevelOptions.add("9 - Cache only for all (for service process only)");
        }
        return perfLevelOptions;
    }

    private List<String> transOutcomeOptions;

    private List<String> getTransitionOutcomeOptions() {
        if (transOutcomeOptions == null) {
            transOutcomeOptions = new ArrayList<String>();
            transOutcomeOptions.add(ProcessVO.TRANSITION_ON_NULL + "     ");
            transOutcomeOptions.add(ProcessVO.TRANSITION_ON_DEFAULT);
        }
        return transOutcomeOptions;
    }

    private List<String> transRetryOptions;

    private List<String> getTransitionRetryOptions() {
        if (transRetryOptions == null) {
            transRetryOptions = new ArrayList<String>();
            transRetryOptions.add("Retry Not Allowed (0)     ");
            transRetryOptions.add("Retry Unlimited (-1)");
        }
        return transRetryOptions;
    }

    private List<String> pageRenderingOptions;

    private List<String> getPageRenderingOptions() {
        if (pageRenderingOptions == null) {
            pageRenderingOptions = new ArrayList<String>();
            pageRenderingOptions.add(WorkAttributeConstant.HTML5_RENDERING);
            pageRenderingOptions.add(WorkAttributeConstant.COMPATIBILITY_RENDERING);
        }
        return pageRenderingOptions;
    }

    public boolean select(Object toTest) {
        if (toTest == null || !(toTest instanceof WorkflowProcess))
            return false;

        process = (WorkflowProcess) toTest;
        if (process.hasInstanceInfo())
            return false;

        return true;
    }
}
