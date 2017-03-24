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
package com.centurylink.mdw.plugin.codegen.meta;

import com.centurylink.mdw.model.value.activity.ActivityImplementorVO;
import com.centurylink.mdw.plugin.designer.model.ActivityImpl;

public class Activity extends Code {
    // label
    private String label;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    // icon
    private String icon = "shape:activity";

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    // attrXml
    private String attrXml = "<PAGELET/>";

    public String getAttrXml() {
        return attrXml;
    }

    public void setAttrXml(String attrXml) {
        this.attrXml = attrXml;
    }

    // base class
    private String baseClass;

    public String getBaseClass() {
        return baseClass;
    }

    public void setBaseClass(String baseClass) {
        this.baseClass = baseClass;
    }

    /**
     * Dynamically adds the activity implementor to the designer toolbox.
     */
    public ActivityImpl createActivityImpl() {
        String implClass = getJavaPackage() + "." + getClassName();
        ActivityImplementorVO activityImplVO = new ActivityImplementorVO();
        activityImplVO.setImplementorClassName(implClass);
        activityImplVO.setLabel(label);
        activityImplVO.setIconName(icon);
        activityImplVO.setBaseClassName(getBaseClass());
        activityImplVO.setAttributeDescription(attrXml);

        return new ActivityImpl(activityImplVO, getPackage());
    }
}
