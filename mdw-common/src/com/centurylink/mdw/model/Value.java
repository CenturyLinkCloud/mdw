/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Jsonable;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value="Value", description="MDW runtime value")
public class Value implements Jsonable {

    public enum Display {
        Required,
        Optional,
        ReadOnly,
        Hidden
    }

    private String name;
    @ApiModelProperty(value="May be an expression", required=true)
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    private String value;
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    private String label;
    @ApiModelProperty(value="Null means use name")
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    private String type;
    @ApiModelProperty(value="Format for java.util.Date is ")
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    private Display display;
    @ApiModelProperty(value="Null implies not displayed (different from hidden)")
    public Display getDisplay() { return display; }
    public void setDisplay(Display display) { this.display = display; }

    private int sequence;
    @ApiModelProperty(value="Display order")
    public int getSequence() { return sequence; }
    public void setSequence(int sequence) { this.sequence = sequence; }

    private String indexKey;
    @ApiModelProperty(value="For indexed values")
    public String getIndexKey() { return indexKey; }
    public void setIndexKey(String key) { this.indexKey = key; }

    public Value(String name) {
        this.name = name;
    }

    public Value(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public Value(String name, JSONObject json) throws JSONException {
        this.name = name;
        if (json.has("value"))
            this.value = json.getString("value");
        if (json.has("label"))
            this.label = json.getString("label");
        if (json.has("type"))
            this.type = json.getString("type");
        if (json.has("display"))
            this.display = Display.valueOf(json.getString("display"));
        if (json.has("sequence"))
            this.sequence = json.getInt("sequence");
        if (json.has("indexKey"))
            this.indexKey = json.getString("indexKey");
    }

    /**
     * expects to be a json object named 'name',
     * so does not include name
     */
    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        if (value != null)
            json.put("value", value);
        if (label != null)
            json.put("label", label);
        if (type != null)
            json.put("type", type);
        if (display != null)
            json.put("display", display.toString());
        if (sequence != 0)
            json.put("sequence", sequence);
        if (indexKey != null)
            json.put("indexKey", indexKey);
        return json;
    }

    public String getJsonName() {
        return "Value";
    }

    @ApiModelProperty(hidden=true)
    public boolean isExpression() {
        return name.startsWith("#{") || name.startsWith("${");
    }

    /**
     * Maps Designer display option to Display type.
     */
    @ApiModelProperty(hidden=true)
    public static Display getDisplay(String option) {
        if ("Optional".equals(option))
            return Display.Optional;
        else if ("Required".equals(option))
            return Display.Required;
        else if ("Read Only".equals(option))
            return Display.ReadOnly;
        else if ("Hidden".equals(option))
            return Display.Hidden;
        else
            return null;
    }

    /**
     * Maps VariableVO display mode to Display type.
     */
    public static Display getDisplay(int mode) {
        if (mode == 0)
            return Display.Required;
        else if (mode == 1)
            return Display.Optional;
        else if (mode == 2)
            return Display.ReadOnly;
        else if (mode == 3)
            return Display.Hidden;
        else
            return null;
    }
}
