/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.display;

import java.util.List;

import com.centurylink.mdw.model.value.attribute.AttributeVO;

public interface Selectable {
    
    Long getId();
    
    String getName();
    void setName(String value);

    String getDescription();
    void setDescription(String value);

    // do not delete the followings - used by $+SLA for MDW 4
    int getSLA();
    void setSLA(int value);

    List<AttributeVO> getAttributes();
    String getAttribute(String name);
    void setAttribute(String name, String value);

}
