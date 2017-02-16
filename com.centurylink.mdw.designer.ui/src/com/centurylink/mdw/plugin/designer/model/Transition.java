/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;
import com.centurylink.mdw.designer.display.Link;
import com.centurylink.mdw.model.data.event.EventType;
import com.centurylink.mdw.model.value.attribute.AttributeVO;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;
import com.centurylink.mdw.model.value.work.WorkTransitionInstanceVO;
import com.centurylink.mdw.model.value.work.WorkTransitionVO;
import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.common.constant.WorkTransitionAttributeConstant;

/**
 * Wraps a Designer workflow Link.
 */
public class Transition extends WorkflowElement implements AttributeHolder {
    private Link link;

    public Link getLink() {
        return link;
    }

    private WorkflowProcess process;

    public WorkflowProcess getProcess() {
        return process;
    }

    private List<WorkTransitionInstanceVO> instances;

    public List<WorkTransitionInstanceVO> getInstances() {
        return instances;
    }

    public void setInstances(List<WorkTransitionInstanceVO> instances) {
        this.instances = instances;
    }

    public boolean hasInstanceInfo() {
        return instances != null && instances.size() > 0;
    }

    /**
     * true if the activity is displayed within a process (still might not have
     * instance info if flow has not reached here yet)
     */
    public boolean isForProcessInstance() {
        return process != null && process.hasInstanceInfo();
    }

    public Entity getActionEntity() {
        return Entity.Transition;
    }

    public Transition(Link link, WorkflowProcess processVersion) {
        this.link = link;
        this.process = processVersion;
    }

    public List<AttributeVO> getAttributes() {
        return link.conn.getAttributes();
    }

    public void setAttribute(String name, String value) {
        link.conn.setAttribute(name, value);
    }

    public WorkflowProject getProject() {
        return process.getProject();
    }

    @Override
    public String getTitle() {
        return "Transition";
    }

    @Override
    public Long getId() {
        return link.conn.getWorkTransitionId();
    }

    @Override
    public String getIcon() {
        return "link.gif";
    }

    public boolean isReadOnly() {
        return process.isReadOnly();
    }

    public String getName() {
        return "Transition";
    }

    @Override
    public String getLabel() {
        String logicalId = getAttribute(WorkAttributeConstant.LOGICAL_ID);
        return getName() + (logicalId == null ? "" : " " + logicalId);
    }

    @Override
    public String getFullPathLabel() {
        return getPath() + (getProcess() == null ? "" : getProcess().getName() + "/") + getLabel();
    }

    public WorkflowPackage getPackage() {
        return process.getPackage();
    }

    public String getCompletionCode() {
        return link.conn.getCompletionCode();
    }

    public void setCompletionCode(String compCode) {
        link.conn.setCompletionCode(compCode);
    }

    public String getEventType() {
        return EventType.getEventTypes().get(new Integer(link.conn.getEventType()));
    }

    public void setEventType(String eventType) {
        link.setEventType(eventType);
    }

    public int getRetryCount() {
        String retryCountAttr = link.conn
                .getAttribute(WorkTransitionAttributeConstant.TRANSITION_RETRY_COUNT);
        if (retryCountAttr == null)
            return 0;

        return Integer.parseInt(retryCountAttr);
    }

    public void setRetryCount(int retryCount) {
        String retryCountAttr = null;
        if (retryCount != 0)
            retryCountAttr = String.valueOf(retryCount);

        link.conn.setAttribute(WorkTransitionAttributeConstant.TRANSITION_RETRY_COUNT,
                retryCountAttr);
    }

    public String getDelay() {
        return link.conn.getAttribute(WorkTransitionAttributeConstant.TRANSITION_DELAY);
    }

    public void setDelay(String delay) {
        link.conn.setAttribute(WorkTransitionAttributeConstant.TRANSITION_DELAY, delay);
    }

    public String getStyle() {
        return link.getType();
    }

    public void setStyle(String style) {
        link.setType(style);
    }

    public int getControlPoints() {
        return link.getNumberOfControlPoints();
    }

    public void setControlPoints(int controlPoints) {
        link.calcLinkPosition(controlPoints, Link.ARROW_STYLE_END);
    }

    public boolean sameFromAndTo() {
        return link.from == link.to;
    }

    public static List<String> getEventTypes() {
        List<String> eventTypes = new ArrayList<String>();
        for (String eventType : EventType.allEventTypeNames) {
            eventTypes.add(eventType);
        }
        return eventTypes;
    }

    public static List<String> getLinkStyles() {
        List<String> linkStyles = new ArrayList<String>();
        for (String linkStyle : Link.styles) {
            linkStyles.add(linkStyle);
        }
        return linkStyles;
    }

    public static List<Image> getLinkStyleImages() {
        List<Image> linkStyleImages = new ArrayList<Image>();
        for (String styleIcon : Link.styleIcons) {
            ImageDescriptor descriptor = MdwPlugin.getImageDescriptor("icons/" + styleIcon);
            linkStyleImages.add(descriptor.createImage());
        }
        return linkStyleImages;
    }

    public boolean equals(Object o) {
        if (!(o instanceof Transition))
            return false;

        Transition other = (Transition) o;
        if (getId() <= 0 && other.getId() <= 0) {
            WorkTransitionVO thisWtVo = getLink().conn;
            WorkTransitionVO otherWtVo = other.getLink().conn;
            if (thisWtVo.getFromWorkId() == null) {
                if (otherWtVo.getFromWorkId() != null)
                    return false;
            }
            else {
                if (!thisWtVo.getFromWorkId().equals(otherWtVo.getFromWorkId()))
                    return false;
            }
            if (thisWtVo.getToWorkId() == null) {
                if (otherWtVo.getToWorkId() != null)
                    return false;
            }
            else {
                if (!thisWtVo.getToWorkId().equals(otherWtVo.getToWorkId()))
                    return false;
            }
            if (thisWtVo.getCompletionCode() == null) {
                if (otherWtVo.getCompletionCode() != null)
                    return false;
            }
            else {
                if (!thisWtVo.getCompletionCode().equals(otherWtVo.getCompletionCode()))
                    return false;
            }
            if (thisWtVo.getEventType() == null) {
                if (otherWtVo.getEventType() != null)
                    return false;
            }
            else {
                if (!thisWtVo.getEventType().equals(otherWtVo.getEventType()))
                    return false;
            }
            return super.equals(other);
        }
        else {
            return super.equals(other);
        }
    }

    @Override
    public Long getProcessId() {
        return getProcess().getId();
    }

    @Override
    public boolean overrideAttributesApplied() {
        return getProcess().overrideAttributesApplied();
    }

    @Override
    public boolean isOverrideAttributeDirty(String prefix) {
        return getProcess().isAttributeOwnerDirty(prefix, OwnerType.WORK_TRANSITION, getId());
    }

    @Override
    public void setOverrideAttributeDirty(String prefix, boolean dirty) {
        getProcess().setAttributeOwnerDirty(prefix, OwnerType.WORK_TRANSITION, getId(), dirty);
    }

}
