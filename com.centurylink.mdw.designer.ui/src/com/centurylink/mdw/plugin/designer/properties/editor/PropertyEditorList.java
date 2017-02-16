/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties.editor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.plugin.designer.model.Activity;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.qwest.mbeng.FormatXml;
import com.qwest.mbeng.MbengDocumentClass;
import com.qwest.mbeng.MbengException;
import com.qwest.mbeng.MbengNode;

public class PropertyEditorList implements Iterable<PropertyEditor> {
    private WorkflowElement workflowElement;
    private MbengDocumentClass mbengDocument;
    private MbengNode currentNode;
    private List<PropertyEditor> propertyEditors = new ArrayList<PropertyEditor>();
    private String[] appliesTo;

    public String[] getAppliesTo() {
        return appliesTo;
    }

    public PropertyEditorList(Activity activity) throws PropertyEditorException {
        this(activity, activity.getActivityImpl().getAttrDescriptionXml());
    }

    public PropertyEditorList(WorkflowElement workflowElement, String attrXml)
            throws PropertyEditorException {
        this.workflowElement = workflowElement;
        FormatXml fmter = new FormatXml();
        mbengDocument = new MbengDocumentClass();
        try {
            fmter.load(mbengDocument, attrXml);
            String at = mbengDocument.getRootNode().getAttribute("APPLIES_TO");
            if (at != null)
                appliesTo = at.split(",");

            // instantiate the property editors
            for (currentNode = mbengDocument.getRootNode()
                    .getFirstChild(); currentNode != null; currentNode = currentNode
                            .getNextSibling()) {
                String type = currentNode.getName();
                PropertyEditor propEditor = null;

                if (type.equals(TableEditor.TYPE_TABLE)) {
                    propEditor = new TableEditor(workflowElement, currentNode);
                }
                else if (type.equals(PropertyEditor.TYPE_PICKLIST)
                        && currentNode.getAttribute("SOURCE") == null
                        && currentNode.getFirstChild() == null) {
                    currentNode.setName(TableEditor.TYPE_TABLE); // substitute
                                                                 // table with
                                                                 // one column
                    MbengNode column = mbengDocument.newNode("TEXT", null, "X", ' ');
                    column.setAttribute("LABEL", "Index");
                    currentNode.appendChild(column);
                    propEditor = new TableEditor(workflowElement, currentNode);
                    ((TableEditor) propEditor).setColumnDelimiter(' ');
                    ((TableEditor) propEditor).setRowDelimiter('#');
                }
                else if (type.equals(PropertyEditor.TYPE_COMBO)
                        && ("RuleSets".equals(currentNode.getAttribute("SOURCE"))
                                || "Form".equals(currentNode.getAttribute("SOURCE"))
                                || "Process".equals(currentNode.getAttribute("SOURCE"))
                                || "TaskTemplates".equals(currentNode.getAttribute("SOURCE")))) {
                    propEditor = new WorkflowAssetEditor(workflowElement, currentNode);
                    if (propEditor.getValue() != null
                            && propEditor.getValue().equals(currentNode.getAttribute("DEFAULT")))
                        propEditor.setValue(""); // don't set default value for
                                                 // rulesets
                }
                else if (type.equals(PropertyEditor.TYPE_COMBO)
                        && "ProcessVersion".equals(currentNode.getAttribute("SOURCE"))) {
                    propEditor = null; // this field is suppressed now
                                       // (populated through asset picker)
                }
                else {
                    propEditor = new PropertyEditor(workflowElement, currentNode);
                }

                if (propEditor != null && !propEditor.getType().equals(PropertyEditor.TYPE_LINK)
                        && !propEditor.getType().equals(PropertyEditor.TYPE_INFO)
                        && !propEditor.getType().equals(TableEditor.TYPE_TABLE)) {
                    final String attrName = currentNode.getAttribute("NAME");
                    String unitsAttrVal = currentNode.getAttribute("UNITS_ATTR");
                    final String unitsAttr = unitsAttrVal == null ? WorkAttributeConstant.SLA_UNIT
                            : unitsAttrVal;
                    final String attrSource = currentNode.getAttribute("SOURCE");
                    propEditor.addValueChangeListener(new ValueChangeListener() {
                        public void propertyValueChanged(Object newValue) {
                            WorkflowElement workflowElement = PropertyEditorList.this.workflowElement;
                            if (newValue instanceof TimeInterval.TimerValue) {
                                TimeInterval.TimerValue timerValue = (TimeInterval.TimerValue) newValue;
                                workflowElement.setAttribute(attrName,
                                        timerValue.getInterval().equals("0") ? null
                                                : timerValue.getInterval());
                                workflowElement.setAttribute(unitsAttr,
                                        timerValue.getUnits().toString());
                                if (!workflowElement.getProject().isMdw5()
                                        && workflowElement instanceof Activity
                                        && ((Activity) workflowElement).isEventWait())
                                    ((Activity) workflowElement).getNode().nodet
                                            .setSla(timerValue.getSeconds());
                            }
                            else if (newValue instanceof Boolean) {
                                workflowElement.setAttribute(attrName, newValue.toString());
                            }
                            else if ("TaskCategory".equals(attrSource) && newValue != null) {
                                String newVal = (String) newValue;
                                int sep = newVal.indexOf(" - ");
                                if (sep > 0)
                                    workflowElement.setAttribute(attrName,
                                            newVal.substring(0, sep));
                                else
                                    workflowElement.setAttribute(attrName, (String) newValue);
                            }
                            else {
                                workflowElement.setAttribute(attrName, (String) newValue);
                            }
                        }
                    });
                }

                if (propEditor != null)
                    propertyEditors.add(propEditor);
            }
        }
        catch (MbengException ex) {
            throw new PropertyEditorException(ex);
        }
    }

    public Iterator<PropertyEditor> iterator() {
        return new Iterator<PropertyEditor>() {
            int index = 0;

            public boolean hasNext() {
                return index < propertyEditors.size();
            }

            public PropertyEditor next() {
                if (index == propertyEditors.size())
                    throw new PropertyEditorException("Exhausted iterator.");

                return propertyEditors.get(index++);
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public List<String> getSections() {
        List<String> sections = new ArrayList<String>();
        for (PropertyEditor propEd : this) {
            if (propEd.getSection() != null && !sections.contains(propEd.getSection()))
                sections.add(propEd.getSection());
        }
        return sections;
    }
}
