/**
 * Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.launch;

import java.util.Map;

import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.viewers.StructuredSelection;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.designer.DesignerDataAccess;
import com.centurylink.mdw.designer.testing.LogMessageMonitor;
import com.centurylink.mdw.designer.testing.TestCaseRun;
import com.centurylink.mdw.model.value.process.ProcessVO;
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
        String mode = debug ? ILaunchManager.DEBUG_MODE : ILaunchManager.RUN_MODE;
        launchShortcut.launch(new StructuredSelection(autoTestCase.getFile()), mode);
    }

    @Override
    public void stop() {
        // TODO halt via launch.terminate()
        super.stop();
    }

}
