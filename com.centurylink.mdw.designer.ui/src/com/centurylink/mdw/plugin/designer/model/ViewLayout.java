/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.model;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.resources.IFile;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.PluginUtil;
import com.centurylink.mdw.plugin.designer.DesignerProxy;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.task.UiColumn;
import com.centurylink.mdw.task.UiDetail;
import com.centurylink.mdw.task.UiField;
import com.centurylink.mdw.task.UiFilter;
import com.centurylink.mdw.task.UiList;
import com.centurylink.mdw.task.UiRow;
import com.centurylink.mdw.task.ViewDocument;
import com.centurylink.mdw.common.Compatibility;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;

public class ViewLayout extends WorkflowElement {
    private IFile file;

    public ViewLayout(IFile file) {
        this.file = file;
        setProject(WorkflowProjectManager.getInstance().getWorkflowProject(file.getProject()));
        try {
            // view doc template
            String url = MdwPlugin.getSettings().getWorkspaceSetupUrl();
            if (!url.endsWith("/"))
                url += "/";
            String docUrl = url + "templates/MDWTaskView.xml";
            viewDocTemplate = ViewDocument.Factory.parse(new URL(docUrl),
                    Compatibility.namespaceOptions());

            // task variable mappings
            DesignerProxy designerProxy = getProject() == null ? null
                    : getProject().getDesignerProxy();
            if (designerProxy == null || designerProxy.getPluginDataAccess() == null)
                taskVariableMappings = new HashMap<String, List<String>>();
            else
                taskVariableMappings = designerProxy.getPluginDataAccess()
                        .getTaskVariableMappings();
        }
        catch (Exception ex) {
            PluginMessages.uiError(ex, "Load Task View Template", getProject());
        }
    }

    public static final String VARIABLES_ATTRIBUTE_OPTION = "variables";

    private ViewDocument viewDocTemplate;

    protected ViewDocument getViewDocTemplate() {
        return viewDocTemplate;
    }

    private Map<String, List<String>> taskVariableMappings;

    public String[] getAttributeOptions(Attr attr) {
        Element columnElem = getContainingElement(attr, "column");
        if (columnElem != null && attr.getName().equals("attribute")) {
            UiList uiList = getRelevantUiList(attr);
            if (uiList != null) {
                Element listElem = getContainingElement(columnElem, "list");
                if (listElem != null) {
                    List<String> options = new ArrayList<String>();
                    List<String> existingAttrs = getExistingAttributeNames(listElem, "column");
                    for (UiColumn uiColumn : uiList.getColumnList()) {
                        if (!existingAttrs.contains(uiColumn.getAttribute()))
                            options.add(uiColumn.getAttribute()); // not already
                                                                  // used
                    }
                    return options.toArray(new String[0]);
                }
            }
        }
        Element fieldElem = getContainingElement(attr, "field");
        if (fieldElem != null && attr.getName().equals("attribute")) {
            UiFilter uiFilter = getRelevantUiFilter(attr);
            if (uiFilter != null) {
                Element filterElem = getContainingElement(fieldElem, "filter");
                if (filterElem != null) {
                    List<String> options = new ArrayList<String>();
                    List<String> existingAttrs = getExistingAttributeNames(filterElem, "field");
                    for (UiField uiField : uiFilter.getFieldList()) {
                        if (!existingAttrs.contains(uiField.getAttribute()))
                            options.add(uiField.getAttribute()); // not already
                                                                 // used
                    }
                    return options.toArray(new String[0]);
                }
            }
        }
        Element rowElem = getContainingElement(attr, "row");
        if (rowElem != null && attr.getName().equals("attribute")) {
            UiDetail uiDetail = getRelevantUiDetail(attr);
            if (uiDetail != null) {
                Element detailElem = getContainingElement(rowElem, "detail");
                if (detailElem != null) {
                    List<String> options = new ArrayList<String>();
                    List<String> existingAttrs = getExistingAttributeNames(detailElem, "row");
                    for (UiRow uiRow : uiDetail.getRowList()) {
                        if (!existingAttrs.contains(uiRow.getAttribute()))
                            options.add(uiRow.getAttribute()); // not already
                                                               // used
                    }
                    return options.toArray(new String[0]);
                }
            }
        }
        return null;
    }

    private List<String> getExistingAttributeNames(Element elem, String subElemName) {
        List<String> attrNames = new ArrayList<String>();
        NodeList subNodes = elem.getElementsByTagName(subElemName);
        for (int i = 0; i < subNodes.getLength(); i++) {
            Element subElem = (Element) subNodes.item(i);
            String attr = subElem.getAttribute("attribute");
            if (attr != null)
                attrNames.add(attr);
        }
        return attrNames;
    }

    private List<String> getExistingVarAttributeNames(Element elem, String subElemName) {
        List<String> varAttrs = new ArrayList<String>();
        for (String attr : getExistingAttributeNames(elem, subElemName)) {
            if (attr.startsWith("$"))
                varAttrs.add(attr);
        }
        return varAttrs;
    }

    private boolean isColumnAttribute(Attr attr) {
        return getContainingElement(attr, "column") != null && "attribute".equals(attr.getName());
    }

    private boolean isFieldAttribute(Attr attr) {
        return getContainingElement(attr, "field") != null && "attribute".equals(attr.getName());
    }

    private Element getContainingElement(Element subElem, String elemName) {
        Node parentNode = subElem.getParentNode();
        if (parentNode != null && parentNode.getNodeType() == Node.ELEMENT_NODE) {
            Element parentElem = (Element) parentNode;
            if (elemName.equals(parentElem.getNodeName()))
                return parentElem;
        }
        return null;
    }

    private Element getContainingElement(Attr attr, String elemName) {
        Element ownerElem = (Element) attr.getOwnerElement();
        if (ownerElem != null && elemName.equals(ownerElem.getNodeName()))
            return ownerElem;
        else
            return null;
    }

    /**
     * From the template document on the sharepoint site.
     */
    public UiList getRelevantUiList(Attr columnAttr) {
        Node parent = columnAttr.getOwnerElement().getParentNode();
        if (parent != null) {
            if ("list".equals(parent.getNodeName())) {
                Node idNode = parent.getAttributes().getNamedItem("id");
                if (idNode != null && idNode.getNodeType() == Node.ATTRIBUTE_NODE) {
                    Attr idAttr = (Attr) idNode;
                    for (UiList uiList : getViewDocTemplate().getView().getListList()) {
                        if (uiList.getId().equals(idAttr.getValue()))
                            return uiList;
                    }
                }
            }
        }
        return null;
    }

    /**
     * From the template document on the sharepoint site.
     */
    public UiFilter getRelevantUiFilter(Attr fieldAttr) {
        Node parent = fieldAttr.getOwnerElement().getParentNode();
        if (parent != null) {
            if ("filter".equals(parent.getNodeName())) {
                Node idNode = parent.getAttributes().getNamedItem("id");
                if (idNode != null && idNode.getNodeType() == Node.ATTRIBUTE_NODE) {
                    Attr idAttr = (Attr) idNode;
                    for (UiFilter uiFilter : getViewDocTemplate().getView().getFilterList()) {
                        if (uiFilter.getId().equals(idAttr.getValue()))
                            return uiFilter;
                    }
                }
            }
        }
        return null;
    }

    /**
     * From the template document on the sharepoint site.
     */
    public UiDetail getRelevantUiDetail(Attr rowAttr) {
        Node parent = rowAttr.getOwnerElement().getParentNode();
        if (parent != null) {
            if ("detail".equals(parent.getNodeName())) {
                Node idNode = parent.getAttributes().getNamedItem("id");
                if (idNode != null && idNode.getNodeType() == Node.ATTRIBUTE_NODE) {
                    Attr idAttr = (Attr) idNode;
                    for (UiDetail uiDetail : getViewDocTemplate().getView().getDetailList()) {
                        if (uiDetail.getId().equals(idAttr.getValue()))
                            return uiDetail;
                    }
                }
            }
        }
        return null;
    }

    public Map<String, List<String>> getVariables(Attr attr, String option) {
        if (isColumnAttribute(attr) && option.equals(VARIABLES_ATTRIBUTE_OPTION)) {
            Element listElem = getContainingElement(attr.getOwnerElement(), "list");
            if (listElem != null) {
                List<String> existingAttrs = getExistingVarAttributeNames(listElem, "column");
                if (existingAttrs.isEmpty()) {
                    return taskVariableMappings;
                }
                else {
                    return removeExistingVariables(existingAttrs, taskVariableMappings);
                }
            }
        }
        else if (isFieldAttribute(attr) && option.equals(VARIABLES_ATTRIBUTE_OPTION)) {
            Element filterElem = getContainingElement(attr.getOwnerElement(), "filter");
            if (filterElem != null) {
                List<String> existingAttrs = getExistingVarAttributeNames(filterElem, "field");
                if (existingAttrs.isEmpty()) {
                    return taskVariableMappings;
                }
                else {
                    return removeExistingVariables(existingAttrs, taskVariableMappings);
                }
            }
        }

        return null;
    }

    private Map<String, List<String>> removeExistingVariables(List<String> existingAttrs,
            Map<String, List<String>> varMap) {
        // remove variables that are already tied to attributes
        Map<String, List<String>> weeded = new TreeMap<String, List<String>>(
                PluginUtil.getStringComparator());
        for (String key : taskVariableMappings.keySet()) {
            for (String var : taskVariableMappings.get(key)) {
                if (!existingAttrs.contains("$" + var)) {
                    if (weeded.get(key) == null)
                        weeded.put(key, new ArrayList<String>());

                    weeded.get(key).add(var);
                }
            }
        }
        return weeded;
    }

    public Long getId() {
        return file.getLocalTimeStamp(); // TODO
    }

    @Override
    public String getTitle() {
        return "View Layout";
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public String getIcon() {
        return "layout.gif";
    }

    @Override
    public boolean isReadOnly() {
        return false; // actually controlled by WorkflowAsset (which is
                      // disconnected)
    }

    public boolean isLocal() {
        return true;
    }

    @Override
    public boolean hasInstanceInfo() {
        return false;
    }

    @Override
    public Entity getActionEntity() {
        return null;
    }

}
