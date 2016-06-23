/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.jsf.component;

import javax.faces.component.UIComponentBase;

public class HtmlTag extends UIComponentBase {

    public static final String COMPONENT_FAMILY = "javax.faces.Output";
    public static final String COMPONENT_TYPE = HtmlTag.class.getName();

    @Override
    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    public String getManifest() {
      return (String) getStateHelper().eval("manifest");
    }
    public void setManifest(String manifest) {
      getStateHelper().put("manifest", manifest);
    }
}
