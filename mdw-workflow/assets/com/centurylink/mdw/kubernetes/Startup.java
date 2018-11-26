package com.centurylink.mdw.kubernetes;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.startup.StartupException;
import com.centurylink.mdw.startup.StartupService;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RegisteredService(StartupService.class)
public class Startup implements StartupService {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private List<Logger> loggers;

    @Override
    public void onStartup() throws StartupException {

        try {
            // shutdown existing loggers (if any)
            onShutdown();

            // get the pod list and create loggers
            Invoker invoker = new Invoker();
            PodList podList = new PodList(new JSONObject(invoker.invoke("pods")));
            loggers = new ArrayList<>();
            for (Pod pod : podList.getPods()) {
                loggers.add(new Logger(pod));
            }
            for (Logger logger : loggers) {
                logger.startup();
            }
        }
        catch (IOException ex) {
            logger.severeException(ex.getMessage(), ex);
            onShutdown();
        }
    }

    @Override
    public void onShutdown() {
        if (loggers != null) {
            for (Logger logger : loggers) {
                logger.shutdown();
            }
        }
    }
}
