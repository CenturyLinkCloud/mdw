/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.utilities;

import java.io.Serializable;
import java.util.Map;

public interface MessageHandler {

    void handleMessage(String message);

    void handleMessage(Map<?, ?> message);

    void handleMessage(byte[] message);

    void handleMessage(Serializable message);

}
