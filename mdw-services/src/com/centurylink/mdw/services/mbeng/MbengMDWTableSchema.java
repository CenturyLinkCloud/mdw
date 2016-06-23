/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.mbeng;

import com.qwest.mbeng.MbengTableSchema;

public class MbengMDWTableSchema implements MbengTableSchema {

    public int getColumnCount() {
        return 1;
    }

    public String getColumnName(int arg0) {
        return "value";
    }

    public boolean isKey(int arg0) {
        return false;
    }

}
