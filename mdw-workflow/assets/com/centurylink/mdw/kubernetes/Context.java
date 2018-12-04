package com.centurylink.mdw.kubernetes;

import java.io.File;

public class Context {

    public static final String K8S_NAMESPACE = "K8S_NAMESPACE";
    public static final String K8S_SERVICE_TOKEN = "K8S_SERVICE_TOKEN";
    public static final String API_HOST = "kubernetes.default.svc";
    public static final String INTERNAL_HOST = "kube-apiserver";
    public static final String LOGS_PATH = "/opt/mdw/logs";

    /**
     * TODO: better way to check?
     */
    public static boolean isKubernetes() {
        return System.getenv("KUBERNETES_PORT") != null;
    }

    public static String getApiHost() {
        return API_HOST;
    }

    public static String getNamespace() {
        return System.getenv(K8S_NAMESPACE);
    }

    /**
     * Includes no trailing slash
     */
    public static String getBaseUrl() {
        return "https://" + getApiHost() + "/api/v1/namespaces/" + getNamespace();
    }

    static String getServiceToken() {
        return System.getenv(K8S_SERVICE_TOKEN);
    }

    /**
     * TODO: configurable
     */
    static File getLogsDir() {
        return new File(LOGS_PATH);
    }
}
