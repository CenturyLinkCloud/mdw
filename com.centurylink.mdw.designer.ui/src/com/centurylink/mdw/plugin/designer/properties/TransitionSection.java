/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties;

import org.eclipse.swt.widgets.Composite;

import com.centurylink.mdw.plugin.designer.model.Transition;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.TimeInterval;
import com.centurylink.mdw.plugin.designer.properties.editor.ValueChangeListener;
import com.centurylink.mdw.designer.display.Link;

public class TransitionSection extends PropertySection {
    private Transition transition;

    public Transition getTransition() {
        return transition;
    }

    private PropertyEditor idPropertyEditor;
    private PropertyEditor resultCodePropertyEditor;
    private PropertyEditor eventTypePropertyEditor;
    private PropertyEditor retryCountPropertyEditor;
    private PropertyEditor delayPropertyEditor;
    private PropertyEditor stylePropertyEditor;
    private PropertyEditor controlPointsPropertyEditor;

    public void setSelection(WorkflowElement selection) {
        transition = (Transition) selection;

        idPropertyEditor.setElement(transition);
        idPropertyEditor.setValue(transition.getId());

        resultCodePropertyEditor.setElement(transition);
        resultCodePropertyEditor.setValue(transition.getCompletionCode());
        resultCodePropertyEditor.setEditable(!transition.isReadOnly());

        eventTypePropertyEditor.setElement(transition);
        eventTypePropertyEditor.setValue(transition.getEventType());
        eventTypePropertyEditor.setEditable(!transition.isReadOnly());

        retryCountPropertyEditor.setElement(transition);
        retryCountPropertyEditor.setValue(transition.getRetryCount());
        retryCountPropertyEditor.setEditable(!transition.isReadOnly());

        delayPropertyEditor.setElement(transition);
        if (transition.getDelay() != null && transition.getDelay().endsWith("s"))
            delayPropertyEditor.setValue(
                    transition.getDelay().substring(0, transition.getDelay().length() - 1),
                    TimeInterval.Units.Seconds);
        else if (transition.getDelay() != null && transition.getDelay().endsWith("h"))
            delayPropertyEditor.setValue(
                    transition.getDelay().substring(0, transition.getDelay().length() - 1),
                    TimeInterval.Units.Hours);
        else
            delayPropertyEditor.setValue(transition.getDelay(), TimeInterval.Units.Minutes);

        delayPropertyEditor.setEditable(!transition.isReadOnly());

        stylePropertyEditor.setElement(transition);
        stylePropertyEditor.setValue(transition.getStyle());
        stylePropertyEditor.setEditable(!transition.isReadOnly() && !transition.sameFromAndTo());

        controlPointsPropertyEditor.setElement(transition);
        controlPointsPropertyEditor.setValue(transition.getControlPoints());
        controlPointsPropertyEditor
                .setEditable(!transition.isReadOnly() && !transition.sameFromAndTo());
    }

    public void drawWidgets(Composite composite, WorkflowElement selection) {
        transition = (Transition) selection;

        // id text field
        idPropertyEditor = new PropertyEditor(transition, PropertyEditor.TYPE_TEXT);
        idPropertyEditor.setLabel("ID");
        idPropertyEditor.setWidth(150);
        idPropertyEditor.setReadOnly(true);
        idPropertyEditor.render(composite);

        // result code text field
        resultCodePropertyEditor = new PropertyEditor(transition, PropertyEditor.TYPE_TEXT);
        resultCodePropertyEditor.setLabel("Result");
        resultCodePropertyEditor.setWidth(190);
        resultCodePropertyEditor.addValueChangeListener(new ValueChangeListener() {
            public void propertyValueChanged(Object newValue) {
                transition.setCompletionCode((String) newValue);
            }
        });
        resultCodePropertyEditor.render(composite);

        // event type combo
        eventTypePropertyEditor = new PropertyEditor(transition, PropertyEditor.TYPE_COMBO);
        eventTypePropertyEditor.setLabel("Event Type");
        eventTypePropertyEditor.setWidth(175);
        eventTypePropertyEditor.setReadOnly(true);
        eventTypePropertyEditor.setValueOptions(Transition.getEventTypes());
        eventTypePropertyEditor.addValueChangeListener(new ValueChangeListener() {
            public void propertyValueChanged(Object newValue) {
                transition.setEventType((String) newValue);
            }
        });
        eventTypePropertyEditor.render(composite);

        // retry count text field
        retryCountPropertyEditor = new PropertyEditor(transition, PropertyEditor.TYPE_TEXT);
        retryCountPropertyEditor.setLabel("Retry Count");
        retryCountPropertyEditor.setWidth(150);
        retryCountPropertyEditor.addValueChangeListener(new ValueChangeListener() {
            public void propertyValueChanged(Object newValue) {
                String value = (String) newValue;
                transition.setRetryCount(value.length() == 0 ? 0 : Integer.parseInt(value));
            }
        });
        retryCountPropertyEditor.render(composite);

        // transition delay text field
        delayPropertyEditor = new PropertyEditor(transition, PropertyEditor.TYPE_TIMER);
        delayPropertyEditor.setLabel("Delay");
        delayPropertyEditor.addValueChangeListener(new ValueChangeListener() {
            public void propertyValueChanged(Object newValue) {
                TimeInterval.TimerValue timerValue = (TimeInterval.TimerValue) newValue;
                if (timerValue.getUnits().equals(TimeInterval.Units.Seconds))
                    transition.setDelay(timerValue.getInterval() + "s");
                else if (timerValue.getUnits().equals(TimeInterval.Units.Minutes))
                    transition.setDelay(
                            timerValue.getInterval().equals("0") ? null : timerValue.getInterval());
                else if (timerValue.getUnits().equals(TimeInterval.Units.Hours))
                    transition.setDelay(timerValue.getInterval() + "h");
            }
        });
        delayPropertyEditor.render(composite);

        // link style combo
        stylePropertyEditor = new PropertyEditor(transition, PropertyEditor.TYPE_IMAGE_COMBO);
        stylePropertyEditor.setLabel("Style");
        stylePropertyEditor.setWidth(72);
        stylePropertyEditor.setValueOptions(Transition.getLinkStyles());
        stylePropertyEditor.setImageOptions(Transition.getLinkStyleImages());
        stylePropertyEditor.addValueChangeListener(new ValueChangeListener() {
            public void propertyValueChanged(Object newValue) {
                String newStyle = (String) newValue;
                transition.setStyle(newStyle);
                // logic copied from LinkAttrDialog
                if (newStyle.equals(Link.CURVE))
                    controlPointsPropertyEditor.setMaxValue(4);
                else if (newStyle.equals(Link.STRAIGHT))
                    controlPointsPropertyEditor.setMaxValue(8);
                else // Link.ELBOW, Link.ELBOWH, Link.ELBOWV
                    controlPointsPropertyEditor.setMaxValue(8);
            }
        });
        stylePropertyEditor.render(composite);

        // control points spinner
        controlPointsPropertyEditor = new PropertyEditor(transition, PropertyEditor.TYPE_SPINNER);
        controlPointsPropertyEditor.setLabel("Control Points");
        controlPointsPropertyEditor.setWidth(50);
        controlPointsPropertyEditor.addValueChangeListener(new ValueChangeListener() {
            public void propertyValueChanged(Object newValue) {
                transition.setControlPoints(Integer.parseInt((String) newValue));
            }
        });
        controlPointsPropertyEditor.render(composite);
        controlPointsPropertyEditor.setMinValue(2);
        controlPointsPropertyEditor.setMaxValue(8);
    }
}