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
package com.centurylink.mdw.plugin.launch;

import java.util.Date;
import java.util.Map;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.viewers.StructuredSelection;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.designer.DesignerDataAccess;
import com.centurylink.mdw.designer.testing.LogMessageMonitor;
import com.centurylink.mdw.designer.testing.TestCaseRun;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.model.AutomatedTestCase;

/**
 * Invokes the Groovy AutoTest launch config
 *
 */
public class AutoTestCaseRun extends TestCaseRun {

    private AutomatedTestCase autoTestCase;
    AutomatedTestCase getAutoTestCase() { return autoTestCase; }

    private boolean debug;

    private AutoTestLaunchShortcut launchShortcut;

    public AutoTestCaseRun(AutomatedTestCase autoTestCase, int run, String masterRequestId,
            DesignerDataAccess dao, LogMessageMonitor monitor, Map<String,ProcessVO> processCache,
            boolean debug) throws DataAccessException {
        super(autoTestCase.getTestCase(), run, masterRequestId, dao, monitor, processCache, false, true, false);
        this.autoTestCase = autoTestCase;
        this.debug = debug;
        this.launchShortcut = new AutoTestLaunchShortcut(this);
    }

    @Override
    public void run() {
        try {
            String mode = debug ? ILaunchManager.DEBUG_MODE : ILaunchManager.RUN_MODE;
            launchShortcut.launch(new StructuredSelection(autoTestCase.getFile()), mode);
        }
        catch (Exception ex) {
            PluginMessages.log(ex);
            getAutoTestCase().setStartTime(new Date()); // avoid NPE
            finishExecution(ex);
        }
    }

    @Override
    public void stop() {
        if (launchShortcut != null) {
            ILaunchConfiguration debugLaunchConfig = launchShortcut.getLaunchConfiguration();
            if (debugLaunchConfig != null) {
                try {
                    for (ILaunch launch : DebugPlugin.getDefault().getLaunchManager().getLaunches()) {
                        if (launch.getLaunchConfiguration().equals(debugLaunchConfig))
                            launch.terminate();
                    }
                    super.stop();
                }
                catch (DebugException ex) {
                    PluginMessages.log(ex);
                }
            }
        }
        super.stop();
    }

}
