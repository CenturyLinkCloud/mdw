/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.monitor;

import com.centurylink.mdw.common.service.RegisteredService;
import com.centurylink.mdw.util.CallURL;

public interface ScheduledJob extends RegisteredService {

	void run(CallURL args);

}
