/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.value.activity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.common.utilities.JsonUtil;
import com.centurylink.mdw.model.value.attribute.AttributeVO;

public class TextNoteVO implements Serializable, Jsonable {

    private String content;
    private String reference;
    private List<AttributeVO> attributes;

    public TextNoteVO() {

    }

	/**
	 * @return the attributes
	 */
	public List<AttributeVO> getAttributes() {
		return attributes;
	}

	/**
	 * @param attributes the attributes to set
	 */
	public void setAttributes(List<AttributeVO> attributes) {
		this.attributes = attributes;
	}

    /**
     * Returns the value of a process attribute.
     * @param attrname
     * @return the value of the attribute, or null if the attribute does not exist
     */
    public String getAttribute(String attrname) {
        return AttributeVO.findAttribute(attributes, attrname);
    }

    /**
     * Set the value of a process attribute.
     * If the value is null, the attribute is removed.
     * If the attribute does not exist and the value is not null, the attribute
     * is created.
     * @param attrname
     * @param value
     */
    public void setAttribute(String attrname, String value) {
        if (attributes==null) attributes = new ArrayList<AttributeVO>();
        AttributeVO.setAttribute(attributes, attrname, value);
    }

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getReference() {
		return reference;
	}

	public void setReference(String reference) {
		this.reference = reference;
	}

    public String getLogicalId() {
    	return getAttribute(WorkAttributeConstant.LOGICAL_ID);
    }

    public void setLogicalId(String id) {
    	setAttribute(WorkAttributeConstant.LOGICAL_ID, id);
    }

    public TextNoteVO(JSONObject json) throws JSONException {
        this.content = json.getString("content");
        if (json.has("attributes"))
            this.attributes = JsonUtil.getAttributes(json.getJSONObject("attributes"));
        setLogicalId(json.getString("id"));
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", getLogicalId());
        json.put("content", content);
        if (attributes != null && !attributes.isEmpty())
            json.put("attributes", JsonUtil.getAttributesJson(attributes));
        return json;
    }

    public String getJsonName() {
        return JsonUtil.padLogicalId(getLogicalId());
    }

}
