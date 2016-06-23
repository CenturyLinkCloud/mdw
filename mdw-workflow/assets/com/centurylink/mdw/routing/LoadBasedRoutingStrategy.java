/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.routing;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.service.AbstractRoutingStrategy;
import com.centurylink.mdw.common.service.RequestRoutingStrategy;
import com.centurylink.mdw.common.utilities.HttpHelper;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.model.listener.Listener;

/**
 * Routing strategy that determines the appropriate destination based on instances' loads.
 */
@RegisteredService(RequestRoutingStrategy.class)
public class LoadBasedRoutingStrategy extends AbstractRoutingStrategy {

    private static final Object lock = new Object();
    private final int priority = 999;
    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private static volatile Map<String,Integer> threadCountbyInstance = new HashMap<String,Integer>();
    private static Map<String,URL> instanceList = new HashMap<String,URL>();
    private static List<String> instanceSortedList = new ArrayList<String>();

    private static int thresholdForThreadCountRefresh = PropertyManager.getIntegerProperty(PropertyNames.MDW_THREADPOOL_MAX_THREADS, 10);
    private static volatile int requestCount = thresholdForThreadCountRefresh;
    private double UTILIZATION_FACTOR = 0.75;
    private static int instancesToUse = 1;
    private static Random rnd = new Random();
    private static volatile double[] percentageThresholds = null;


    public LoadBasedRoutingStrategy() {
        if (threadCountbyInstance.isEmpty()) {
            try {
                // Initialize thread count map and worker instances' URL map
                for (String instance : super.getWorkerInstances()) {
                    threadCountbyInstance.put(instance,  0);
                    instanceList.put(instance, new URL("http://" + instance + "/" + ApplicationContext.getServicesContextRoot() + "/Services/SystemInfo?type=threadDumpCount&format=text"));
                }
            }
            catch (MalformedURLException ex) {
                logger.severeException(ex.getMessage(), ex);
            }

            // Refine request distribution model based on number of worker instances
            int instanceCount = super.getWorkerInstances().size();
            if (instanceCount > 1) {
                instancesToUse = (int)(instanceCount * UTILIZATION_FACTOR);
                thresholdForThreadCountRefresh = thresholdForThreadCountRefresh * instancesToUse;
            }
        }
    }

    public int getPriority() {
        return priority;
    }

    public URL getDestination(Object request, Map<String,String> headers) {
        if (StringHelper.isEmpty(PropertyManager.getProperty(PropertyNames.MDW_ROUTING_REQUESTS_DEFAULT_STRATEGY)) || !this.getClass().getName().equalsIgnoreCase(PropertyManager.getProperty(PropertyNames.MDW_ROUTING_REQUESTS_DEFAULT_STRATEGY)))
            return null;

        String instance = null;
        double value;

        synchronized (lock) {
            if (requestCount >= thresholdForThreadCountRefresh) {
                requestCount = 0;
                getThreadCounts();
            }
            requestCount++;
            value = rnd.nextDouble();

            List<String> activeList = super.getActiveWorkerInstances();

            for (int i=instancesToUse-1; i >= 0; i--) {
                if (instance == null && value < percentageThresholds[i] && activeList.contains(instanceSortedList.get(i)))
                    instance = instanceSortedList.get(i);
            }

            // Instance could be null if selected instance is NOT active/responding
            // Pick new instance with lowest load and that is active
            if (instance == null) {
                if (!activeList.isEmpty())
                    for (String server : instanceSortedList) {
                        if (instance == null && activeList.contains(server))
                            instance = server;
                    }
            }
        }

        try {
            // If instance is still null, then there are no active servers to route to
            return instance == null ? null : buildURL(headers, instance);
        }
        catch (Exception ex) {
            logger.severeException("Cannot get routing destination for request path: " + headers.get(Listener.METAINFO_REQUEST_PATH), ex);
            return null;
        }
    }

    private void getThreadCounts() {

        // First get the thread counts from each instance - If response times out assume really busy
        for (String instance : instanceList.keySet()) {
            try {
                HttpHelper helper = new HttpHelper(instanceList.get(instance));
                helper.setConnectTimeout(2000);     // Timeout after 2 seconds
                helper.setReadTimeout(2000);
                String response = helper.get();

                if (StringHelper.isEmpty(response))
                    threadCountbyInstance.put(instance, Integer.MAX_VALUE);
                else
                    threadCountbyInstance.put(instance, Integer.parseInt(response));
            }
            catch (Exception ex) {
                threadCountbyInstance.put(instance, Integer.MAX_VALUE);
                logger.info("Unable to update threads count for host " + instance);
            }
        }

        // Now let's make a sorted list based on the thread counts
        instanceSortedList.clear();
        int i;
        for (String instance : threadCountbyInstance.keySet()) {
            if (instanceSortedList.isEmpty())
                instanceSortedList.add(instance);
            else {
                i = 0;
                while (i < instanceSortedList.size() && threadCountbyInstance.get(instance) > threadCountbyInstance.get(instanceSortedList.get(i)))
                    i++;

                instanceSortedList.add(i, instance);
            }
        }

        // Finally let's make a weighted/factor array to determine percentage of requests to be sent to each instance based on
        // number of instances to use (utilization factor) and proportional load of each instance against the most loaded instance
        percentageThresholds = new double[instancesToUse];
        double baseline = 1 / super.getWorkerInstances().size();
        percentageThresholds[instancesToUse-1] = baseline;
        percentageThresholds[0] = 1.0;

        for (int j=instancesToUse-2; j > 0; j--) {
            percentageThresholds[j] = percentageThresholds[j+1] + (baseline * (threadCountbyInstance.get(instanceSortedList.get(instancesToUse-1)) / threadCountbyInstance.get(instanceSortedList.get(j))));
        }
    }
}
