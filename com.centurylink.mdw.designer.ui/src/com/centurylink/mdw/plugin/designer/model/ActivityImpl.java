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
package com.centurylink.mdw.plugin.designer.model;

import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.designer.utils.NodeMetaInfo;
import com.centurylink.mdw.model.value.activity.ActivityImplementorVO;
import com.centurylink.mdw.model.value.attribute.AttributeVO;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;

/**
 * Wraps a Designer workflow ActivityImplementorVO object.
 */
public class ActivityImpl extends WorkflowElement implements Comparable<ActivityImpl> {
    private ActivityImplementorVO activityImplVO;

    public ActivityImplementorVO getActivityImplVO() {
        return activityImplVO;
    }

    private WorkflowPackage packageVersion;

    public WorkflowPackage getPackage() {
        return packageVersion;
    }

    public void setPackage(WorkflowPackage pv) {
        this.packageVersion = pv;
        activityImplVO.setPackageName(pv.getName());
    }

    public boolean isInDefaultPackage() {
        return packageVersion == null || packageVersion.isDefaultPackage();
    }

    private boolean readOnly;

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public ActivityImpl(ActivityImplementorVO activityImplVO, WorkflowPackage packageVersion) {
        this.activityImplVO = activityImplVO;
        this.packageVersion = packageVersion;
        if (packageVersion != null) {
            this.activityImplVO.setPackageName(packageVersion.getName());
            setProject(packageVersion.getProject());
        }
    }

    public boolean hasInstanceInfo() {
        return false;
    }

    public Entity getActionEntity() {
        return Entity.ActivityImplementor;
    }

    public String getLabel() {
        if (activityImplVO == null)
            return null;
        return activityImplVO.getLabel();
    }

    public String getName() {
        return getLabel();
    }

    public void setLabel(String label) {
        activityImplVO.setLabel(label);
    }

    @Override
    public String getIcon() {
        return "act_impl.gif";
    }

    public String getImplClassName() {
        if (activityImplVO == null)
            return null;
        return activityImplVO.getImplementorClassName();
    }

    public void setImplClassName(String className) {
        activityImplVO.setImplementorClassName(className);
    }

    public String getBaseClassName() {
        if (activityImplVO == null)
            return null;
        return activityImplVO.getBaseClassName();
    }

    public void setBaseClassName(String baseClassName) {
        activityImplVO.setBaseClassName(baseClassName);
    }

    public String getIconName() {
        if (activityImplVO == null)
            return null;
        return activityImplVO.getIconName();
    }

    public void setIconName(String iconName) {
        activityImplVO.setIconName(iconName);
    }

    public String getAttrDescriptionXml() {
        String attrXml = null;
        if (activityImplVO != null)
            attrXml = activityImplVO.getAttributeDescription();
        if (attrXml == null)
            attrXml = "<PAGELET/>";
        return attrXml;
    }

    public void setAttrDescriptionXml(String attrDescriptionXml) {
        activityImplVO.setAttributeDescription(attrDescriptionXml);
    }

    public String getMdwVersion() {
        if (activityImplVO == null)
            return null;
        return activityImplVO.getMdwVersion();
    }

    public void setMdwVersion(String mdwVersion) {
        activityImplVO.setMdwVersion(mdwVersion);
    }

    public boolean isPseudoProcessActivity() {
        return getImplClassName().equals(NodeMetaInfo.PSEUDO_PROCESS_ACTIVITY);
    }

    @Override
    public boolean isUserAuthorized(String role) {
        if (isPseudoProcessActivity())
            return false;
        else
            return super.isUserAuthorized(role);
    }

    @Override
    public String getTitle() {
        return "Activity Implementor";
    }

    @Override
    public Long getId() {
        if (activityImplVO == null)
            return null;
        return activityImplVO.getImplementorId();
    }

    public static Class<?>[] getBaseClasses() {
        return ActivityImplementorVO.baseClasses;
    }

    public static String[] getOldBaseClasses() {
        return ActivityImplementorVO.oldBaseClasses;
    }

    public List<AttributeVO> getAttributes() {
        List<AttributeVO> attributes = new ArrayList<AttributeVO>();
        if (getLabel() != null)
            attributes.add(new AttributeVO("LABEL", getLabel()));
        if (getBaseClassName() != null)
            attributes.add(new AttributeVO("BASECLASS", getBaseClassName()));
        if (getIconName() != null)
            attributes.add(new AttributeVO("ICONNAME", getIconName()));
        if (getAttrDescriptionXml() != null)
            attributes.add(new AttributeVO("ATTRDESC", getAttrDescriptionXml()));
        if (getMdwVersion() != null)
            attributes.add(new AttributeVO("MDWVERSION", getMdwVersion()));

        return attributes;
    }

    private boolean dynamicJava;

    public boolean isDynamicJava() {
        return dynamicJava;
    }

    public void setDynamicJava(boolean dynamicJava) {
        this.dynamicJava = dynamicJava;
    }

    public int compareTo(ActivityImpl other) {
        if (this.getLabel() == null && other.getLabel() != null)
            return -1;
        else if (other.getLabel() == null && this.getLabel() != null)
            return 1;
        else if (this.getLabel() == null && other.getLabel() == null)
            return 0;
        return this.getLabel().compareToIgnoreCase(other.getLabel());
    }

    /**
     * don't change the format of this output since it is used for drag-and-drop
     * support
     */
    public String toString() {
        String packageLabel = getPackage() == null || getPackage().isDefaultPackage() ? ""
                : getPackage().getLabel();
        return "ActivityImpl~" + getProject().getName() + "^" + packageLabel + "^" + getId();
    }

}
