/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.util;

import java.io.Serializable;
import java.util.Map;

public interface MessageHandler {

    void handleMessage(String message);

    void handleMessage(Map<?, ?> message);

    void handleMessage(byte[] message);

    void handleMessage(Serializable message);

}
