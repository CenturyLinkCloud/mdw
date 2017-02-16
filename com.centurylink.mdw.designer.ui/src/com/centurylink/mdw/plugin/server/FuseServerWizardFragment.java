/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.server;

import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.TaskModel;

public class FuseServerWizardFragment extends ServiceMixServerWizardFragment {
    @Override
    protected ServiceMixServer loadServer() {
        IServerWorkingCopy serverWC = (IServerWorkingCopy) getTaskModel()
                .getObject(TaskModel.TASK_SERVER);
        return (ServiceMixServer) serverWC.loadAdapter(FuseServer.class, null);
    }
}
