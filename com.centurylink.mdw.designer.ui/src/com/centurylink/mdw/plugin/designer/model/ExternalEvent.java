/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.model;

import com.centurylink.mdw.model.value.event.ExternalEventVO;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

/**
 * Wraps an ExternalEventVO model object.
 */
public class ExternalEvent extends WorkflowElement implements Comparable<ExternalEvent> {
    private ExternalEventVO externalEventVO;

    public ExternalEventVO getExternalEventVO() {
        return externalEventVO;
    }

    private WorkflowPackage packageVersion;

    public WorkflowPackage getPackage() {
        return packageVersion;
    }

    public void setPackage(WorkflowPackage pv) {
        this.packageVersion = pv;
        externalEventVO.setPackageName(pv.getName());
    }

    public boolean isInDefaultPackage() {
        return packageVersion == null || packageVersion.isDefaultPackage();
    }

    public Entity getActionEntity() {
        return Entity.ExternalEvent;
    }

    private boolean readOnly;

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public ExternalEvent(ExternalEventVO externalEventVO, WorkflowPackage packageVersion) {
        this.externalEventVO = externalEventVO;
        this.packageVersion = packageVersion;
        this.externalEventVO.setPackageName(packageVersion.getName());
    }

    public ExternalEvent(ExternalEvent cloneFrom) {
        this.externalEventVO = cloneFrom.getExternalEventVO();
        this.packageVersion = cloneFrom.getPackage();
    }

    public ExternalEvent() {
    } // don't use this constructor

    public WorkflowProject getProject() {
        return packageVersion.getProject();
    }

    @Override
    public String getTitle() {
        return "External Event";
    }

    @Override
    public Long getId() {
        return externalEventVO.getId();
    }

    @Override
    public String getName() {
        return externalEventVO.getEventName();
    }

    public void setName(String name) {
        externalEventVO.setEventName(name);
    }

    @Override
    public String getIcon() {
        return "extevent.gif";
    }

    public String getMessagePattern() {
        return getName();
    }

    public void setMessagePattern(String messagePattern) {
        setName(messagePattern);
    }

    public String getEventHandler() {
        return externalEventVO.getEventHandler();
    }

    public void setEventHandler(String handler) {
        externalEventVO.setEventHandler(handler);
    }

    private static final String LISTENER_PACKAGE = "com.centurylink.mdw.listener";
    private static final String OLD_LISTENER_PACKAGE = "com.qwest.mdw.listener";

    private boolean dynamicJava;

    public boolean isDynamicJava() {
        return dynamicJava;
    }

    public void setDynamicJava(boolean dynamicJava) {
        this.dynamicJava = dynamicJava;
    }

    public String getEventHandlerClassName() {
        if (getEventHandler() == null)
            return null;
        String handlerClassName = null;

        if (getEventHandler().startsWith("START_PROCESS")) {
            if (!getProject().checkRequiredVersion(5, 5))
                handlerClassName = OLD_LISTENER_PACKAGE + ".ProcessStartEventHandler";
            else
                handlerClassName = LISTENER_PACKAGE + ".ProcessStartEventHandler";
        }
        else if (getEventHandler().startsWith("NOTIFY_PROCESS")) {
            if (!getProject().checkRequiredVersion(5, 5))
                handlerClassName = OLD_LISTENER_PACKAGE + ".NotifyWaitingActivityEventHandler";
            else
                handlerClassName = LISTENER_PACKAGE + ".NotifyWaitingActivityEventHandler";
        }
        else if (getEventHandler().indexOf('?') > 0)
            handlerClassName = getEventHandler().substring(0, getEventHandler().indexOf('?'));
        else
            handlerClassName = getEventHandler();

        return handlerClassName;
    }

    @Override
    public boolean hasInstanceInfo() {
        return false;
    }

    public boolean isArchived() {
        return getPackage() != null && getPackage().isArchived();
    }

    public int compareTo(ExternalEvent other) {
        return this.getExternalEventVO().getEventName()
                .compareToIgnoreCase(other.getExternalEventVO().getEventName());
    }

    /**
     * don't change the format of this output since it is use for drag-and-drop
     * support
     */
    public String toString() {
        String packageLabel = getPackage() == null || getPackage().isDefaultPackage() ? ""
                : getPackage().getLabel();
        return "ExternalEvent~" + getProject().getName() + "^" + packageLabel + "^" + getId();
    }

    // placeholder classes for WorkflowElementActionHandler
    public class CamelProcessLaunch extends ExternalEvent {
    }

    public class CamelEventNotify extends ExternalEvent {
    }

}
