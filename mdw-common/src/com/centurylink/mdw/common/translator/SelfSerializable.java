/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.translator;

import java.io.Serializable;

public interface SelfSerializable extends Serializable {

    void fromString(String str);
    
    public String toString();
}
