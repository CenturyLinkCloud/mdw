/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.draw;

import javax.swing.ImageIcon;

import org.json.JSONObject;

import com.centurylink.mdw.model.workflow.ActivityImplementor;

public class Implementor extends ActivityImplementor {
    ImageIcon icon;
    private final String assetPath;

    public final ImageIcon getIcon() {
        return this.icon;
    }

    public final void setIcon(ImageIcon icon) {
        this.icon = icon;
    }

    public final String getAssetPath() {
        return this.assetPath;
    }

    public Implementor(String assetPath, JSONObject json) {
        super(json);
        this.assetPath = assetPath;
    }

    public Implementor(String implClass) {
        this(null, new JSONObject("{\"implementorClass\":\"" + implClass + "\"}"));
        this.setImplementorClassName(implClass);
        this.setIconName("shape:activity");
        this.setBaseClassName("com.centurylink.mdw.activity.types.GeneralActivity");
    }
}
