/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.value.attribute;

import java.io.Serializable;
import java.util.List;

public class AttributeVO implements Serializable, Comparable<AttributeVO> {

    private Long id;
    private String name;
    private String value;
    private String group;

    public AttributeVO(){
    }

    public AttributeVO(String name, String value) {
    	this.id = null;
    	this.name = name;
    	this.value = value;
    }

    public AttributeVO(Long id, String name, String value) {
        this.id = id;
        this.name = name;
        this.value = value;
    }

    public Long getAttributeId() {
        return id;
    }

    public void setAttributeId(Long id) {
        this.id = id;
    }

    public String getAttributeName() {
        return name;
    }

    public void setAttributeName(String name) {
        this.name = name;
    }

    public String getAttributeValue() {
        return value;
    }

    public void setAttributeValue(String value) {
        this.value = value;
    }

    public String getAttributeGroup() {
        return group;
    }

    public void setAttributeGroup(String group) {
        this.group = group;
    }

    public static String findAttribute(List<AttributeVO> attrs, String name) {
        if (attrs==null) return null;
        for (AttributeVO attr : attrs) {
            if (name.equals(attr.getAttributeName()))
                return attr.getAttributeValue();
        }
        return null;
    }

    /**
     * Set the value of a process attribute.
     * If the value is null, the attribute is removed.
     * If the attribute does not exist and the value is not null, the attribute
     * is created.
     * @param name attribute name
     * @param v value to be set. When it is null, the attribute is removed
     */
    public static void setAttribute(List<AttributeVO> attrs, String name, String v) {
        for (AttributeVO attr : attrs) {
            if (name.equals(attr.getAttributeName())) {
                if (v!=null) attr.setAttributeValue(v);
                else attrs.remove(attr);  // TODO this will throw a concurrent modification exception
                return;
            }
        }
        if (v!=null) {
            AttributeVO attr = new AttributeVO(null, name, v);
            // TODO: need to retire attribute type concept
            attrs.add(attr);
        }
    }

    public static void removeAttribute(List<AttributeVO> attrs, String name) {
        setAttribute(attrs, name, null);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AttributeVO))
            return false;
        AttributeVO other = (AttributeVO) o;

        if (getAttributeName() == null)
            return other.getAttributeName() == null;
        else
            return getAttributeName().equals(other.getAttributeName());
    }

    public int compareTo(AttributeVO other) {
        if (other == null || other.getAttributeName() == null)
            return 1;
        if (this.getAttributeName() == null)
            return -1;

        return this.getAttributeName().compareTo(other.getAttributeName());
    }

}
