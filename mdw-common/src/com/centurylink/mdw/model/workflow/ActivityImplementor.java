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
package com.centurylink.mdw.model.workflow;

import javax.swing.ImageIcon;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.activity.types.GeneralActivity;
import com.centurylink.mdw.annotations.Activity;
import com.centurylink.mdw.model.Jsonable;

public class ActivityImplementor implements Comparable<ActivityImplementor>, Jsonable {

    public ActivityImplementor(String implClass, Activity annotation) {
        this(implClass, annotation.category().getName(), annotation.value(), annotation.icon(), annotation.pagelet());
    }

    public ActivityImplementor(String implClass, String category, String label, String iconAsset, String pagelet) {
        this.implementorClass = implClass;
        if (implClass.lastIndexOf('.') > 0)
            this.packageName = implClass.substring(0, implClass.lastIndexOf('.'));
        this.category = category;
        this.label = label;
        this.icon = iconAsset;
        this.pagelet = pagelet;
    }

    /**
     * generic implementor for unfound
     */
    public ActivityImplementor(String implClass) {
        this.implementorClass = implClass;
    }

    private Long id;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    private String implementorClass;
    public String getImplementorClass() { return implementorClass; }
    public void setImplementorClass(String implClass) { this.implementorClass = implClass; }
    @Deprecated
    public String getImplementorClassName() {
        return getImplementorClass();
    }

    private String packageName;
    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }

    private String category = GeneralActivity.class.getName();
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    @Deprecated
    public String getBaseClassName() {
        return getCategory();
    }

    private String label;
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    private String icon = "shape:activity";
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    @Deprecated
    public String getIconName() {
        return getIcon();
    }

    private ImageIcon imageIcon;
    public ImageIcon getImageIcon() { return imageIcon; }
    public void setImageIcon(ImageIcon imageIcon) { this.imageIcon = imageIcon; }

    private String pagelet;
    public String getPagelet() { return pagelet; }
    public void setPagelet(String pagelet) { this.pagelet = pagelet; }
    @Deprecated
    public String getAttributeDescription() {
        return getPagelet();
    }

    private boolean hidden;
    public boolean isHidden() { return hidden; }
    public void setHidden(boolean hidden) { this.hidden = hidden; }

    public String getSimpleName() {
        return implementorClass.substring(implementorClass.lastIndexOf('.') + 1);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ActivityImplementor)) return false;
        return id.equals(((ActivityImplementor)obj).id);
    }

    public int compareTo(ActivityImplementor other) {
        if (this.getLabel() == null)
          return -1;
        return this.getLabel().compareTo(other.getLabel());
    }

    public boolean isStart() {
        return getCategory() != null
          && getCategory().endsWith("StartActivity");
      }

    /**
     * Parse old-style (non-annotation) .impl assets.
     */
    public ActivityImplementor(JSONObject json) throws JSONException {
        this.implementorClass = json.getString("implementorClass");
        if (json.has("category"))
            this.category = json.getString("category");
        if (json.has("label"))
            this.label = json.getString("label");
        else
            this.label = this.implementorClass;
        if (json.has("icon"))
            this.icon = json.getString("icon");
        if (json.has("pagelet"))
            this.pagelet = json.getString("pagelet");
        if (json.has("hidden"))
            this.hidden = json.getBoolean("hidden");
    }

    /**
     * Serialize old-style (non-annotation) .impl assets.
     */
    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        json.put("implementorClass", implementorClass);
        if (category != null)
            json.put("category", category);
        if (label != null)
            json.put("label", label);
        if (icon != null)
            json.put("icon", icon);
        if (pagelet != null)
            json.put("pagelet", pagelet);
        if (hidden)
            json.put("hidden", true);
        return json;
    }

    public String getJsonName() {
        return implementorClass;
    }
}
