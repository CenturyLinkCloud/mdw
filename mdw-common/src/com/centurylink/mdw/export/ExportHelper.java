/*
 * Copyright (C) 2018 CenturyLink, Inc.
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
package com.centurylink.mdw.export;

import com.centurylink.mdw.constant.WorkAttributeConstant;
import com.centurylink.mdw.model.asset.Pagelet;
import com.centurylink.mdw.model.asset.Pagelet.Widget;
import com.centurylink.mdw.model.attribute.Attribute;
import com.centurylink.mdw.model.project.Project;
import com.centurylink.mdw.model.workflow.Activity;
import com.centurylink.mdw.model.workflow.ActivityImplementor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public abstract class ExportHelper {

    protected Set<String> excludedAttributes = new HashSet<>();
    protected Map<String,String> excludedAttributesForSpecificValues = new HashMap<>();
    protected Project project;


    public ExportHelper(Project project) {
        this.project = project;

        excludedAttributes.add(WorkAttributeConstant.LOGICAL_ID);
        excludedAttributes.add(WorkAttributeConstant.REFERENCE_ID);
        excludedAttributes.add(WorkAttributeConstant.WORK_DISPLAY_INFO);
        excludedAttributes.add(WorkAttributeConstant.REFERENCES);
        excludedAttributes.add(WorkAttributeConstant.DOCUMENTATION);
        excludedAttributes.add(WorkAttributeConstant.SIMULATION_STUB_MODE);
        excludedAttributes.add(WorkAttributeConstant.SIMULATION_RESPONSE);
        excludedAttributes.add(WorkAttributeConstant.DESCRIPTION);
        excludedAttributes.add("BAM@START_MSGDEF");
        excludedAttributes.add("BAM@FINISH_MSGDEF");

        excludedAttributesForSpecificValues.put("DoNotNotifyCaller", "false");
        excludedAttributesForSpecificValues.put("DO_LOGGING", "True");
    }

    public boolean isExcludeAttribute(String name, String value) {
        return (value == null || value.isEmpty() || excludedAttributes.contains(name)
                || value.equals(excludedAttributesForSpecificValues.get(name)));
    }

    /**
     * Empty except excluded attributes.
     */
    public boolean isEmpty(List<Attribute> attributes) {
        for (Attribute attribute : attributes) {
            if (!isExcludeAttribute(attribute.getName(), attribute.getValue()))
                return false;
        }
        return true;
    }

    // pagelets are cached to avoid reparsing
    private Map<String,Pagelet> activityPagelets = new HashMap<>();

    protected Widget getWidget(Activity activity, String attributeName) throws IOException {
        Pagelet pagelet = null;
        if (activityPagelets.containsKey(activity.getLogicalId())) {
            pagelet = activityPagelets.get(activity.getLogicalId());
        }
        else {
            ActivityImplementor implementor = getImplementor(activity);
            if (implementor != null) {
                String pageletContent = implementor.getPagelet();
                if (pageletContent != null) {
                    try {
                        if (!pageletContent.startsWith("{") && !pageletContent.contains("<")) {
                            // must be an asset spec
                            int slash = pageletContent.indexOf('/');
                            if (slash > 0) {
                                String path = pageletContent.substring(0, slash).replace('.', '/') + pageletContent.substring(slash);
                                File pageletFile = new File(project.getAssetRoot() + "/" + path);
                                pageletContent = new String(Files.readAllBytes(pageletFile.toPath()));
                            }
                        }
                        pagelet = new Pagelet(pageletContent);
                    } catch (Exception ex) {
                        if (ex instanceof IOException)
                            throw (IOException)ex;
                        else
                            throw new IOException(ex);
                    }
                }
            }
            activityPagelets.put(activity.getLogicalId(), pagelet);
        }
        if (pagelet != null) {
            for (Widget widget : pagelet.getWidgets()) {
                if (widget.getName().equals(attributeName)) {
                    return widget;
                }
            }
            return null;
        }
        return null;
    }

    protected ActivityImplementor getImplementor(Activity activity) throws IOException {
        return project.getActivityImplementors().get(activity.getImplementor());
    }

    protected String getAttributeLabel(Activity activity, Attribute attribute) throws IOException {
        String label = attribute.getName();
        Widget widget = getWidget(activity, attribute.getName());
        if (widget != null && "edit".equals(widget.getType())) {
            label = widget.getName().equals("Java") ? "Java" : activity.getAttribute("Language");
            if (label == null)
                label = activity.getAttribute("SCRIPT");
            if (label == null)
                label = activity.getAttribute("PreScriptLang");
            if (label == null)
                label = activity.getAttribute("PostScriptLang");
            if (label == null)
                label = "Value";
        }
        return label;
    }

    protected Table getTable(Activity activity, Attribute attribute) throws IOException {
        Widget widget = getWidget(activity, attribute.getName());
        List<String> cols;
        List<String[]> rows;
        if ("mapping".equals(widget.getType())) {
            cols = Arrays.asList(new String[]{"Variable", "Binding Expression"});
            rows = new ArrayList<>();
            Map<String,String> map = Attribute.parseMap(attribute.getValue());
            for (String key : map.keySet()) {
                rows.add(new String[]{key, map.get(key)});
            }
        }
        else {
            if (WorkAttributeConstant.MONITORS.equals(attribute.getName())) {
                cols = Arrays.asList(new String[]{"Enabled", "Name", "Implementation", "Options"});
            }
            else {
                cols = new ArrayList<>();
            }
            for (Widget tableWidget : widget.getWidgets())
                cols.add(tableWidget.getName());
            rows = Attribute.parseTable(attribute.getValue(), ',', ';', cols.size());
        }
        String[][] values = new String[cols.size()][rows.size()];
        for (int i = 0; i < cols.size(); i++) {
            for (int j = 0; j < rows.size(); j++) {
                values[i][j] = rows.get(j)[i];
            }
        }
        return new Table(cols.toArray(new String[0]), values);
    }

    protected boolean isTabular(Activity activity, Attribute attribute) throws IOException {
        Widget widget = getWidget(activity, attribute.getName());
        if (widget != null) {
            return "table".equals(widget.getType()) || "mapping".equals(widget.getType())
                    || WorkAttributeConstant.MONITORS.equals(attribute.getName());
        }
        return false;
    }

    protected boolean isCode(Activity activity, Attribute attribute) throws IOException {
        Widget widget = getWidget(activity, attribute.getName());
        if (widget != null) {
            return "edit".equals(widget.getType());
        }
        return false;
    }
}
