/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
