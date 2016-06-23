/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.ui;

import java.util.List;

public class MenuItem {

    private String id;
    public String getId() { return id; }

    private String label;
    public String getLabel() { return label; }

    public String action;
    public String getAction() { return action; }

    public MenuItem(String id, String label, String action) {
        this.id = id;
        this.label = label;
        this.action = action;
    }

    private String image;
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    private List<MenuItem> children;
    public List<MenuItem> getChildren() { return children; }
    public void setChildren(List<MenuItem> children) { this.children = children; }

    private Object data;
    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }

    private boolean submit;
    public boolean isSubmit() { return submit; }
    public void setSubmit(boolean submit) { this.submit = submit; }

    private boolean requireComment;
    public boolean isRequireComment() { return requireComment; }
    public void setRequireComment(boolean requireComment) { this.requireComment = requireComment; }

    public String toString() {
        return label + ": " + action;
    }
}
