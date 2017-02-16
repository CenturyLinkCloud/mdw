/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.server;

import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.TaskModel;

public class FuseRuntimeWizardFragment extends ServiceMixRuntimeWizardFragment {
    @Override
    protected ServiceMixRuntime loadRuntime() {
        IRuntimeWorkingCopy runtimeWC = (IRuntimeWorkingCopy) getTaskModel()
                .getObject(TaskModel.TASK_RUNTIME);
        return (ServiceMixRuntime) runtimeWC.loadAdapter(FuseRuntime.class, null);
    }

}
