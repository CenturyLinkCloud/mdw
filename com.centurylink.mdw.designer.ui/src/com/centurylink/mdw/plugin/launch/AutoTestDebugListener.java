package com.centurylink.mdw.plugin.launch;

import java.io.PrintStream;
import java.util.Date;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IProcess;

import com.centurylink.mdw.designer.testing.TestCase;
import com.centurylink.mdw.plugin.PluginMessages;

public class AutoTestDebugListener implements IDebugEventSetListener {

    private ILaunchConfiguration launchConfig;
    ILaunchConfiguration getLaunchConfig() { return launchConfig; }

    private TestCase testCase;
    private PrintStream log;

    AutoTestDebugListener(ILaunchConfiguration launchConfig, TestCase testCase, PrintStream log) {
        this.launchConfig = launchConfig;
        this.testCase = testCase;
        this.log = log;
    }

    public void handleDebugEvents(DebugEvent[] events) {
        for (DebugEvent event : events) {
            if (event.getSource() instanceof IProcess) {
                IProcess process = (IProcess) event.getSource();
                if (process.getLaunch().getLaunchConfiguration().equals(launchConfig) && process.isTerminated()) {
                    if (event.getKind() == DebugEvent.TERMINATE) {
                        testCase.setEndDate(new Date());
                        try {
                            if (process.getExitValue() == 0) {
                                testCase.setStatus(TestCase.STATUS_PASS);
                            }
                            else {
                                testCase.setStatus(TestCase.STATUS_FAIL);
                            }
                            if (log != System.out)
                                log.close();

                        }
                        catch (DebugException ex) {
                            PluginMessages.log(ex);
                            ex.printStackTrace(log);
                            testCase.setStatus(TestCase.STATUS_ERROR);
                            if (log != System.out)
                                log.close();
                        }
                    }
                }
            }
        }
    }

}
