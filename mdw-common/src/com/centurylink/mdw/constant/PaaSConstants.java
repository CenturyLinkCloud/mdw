/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.constant;

public class PaaSConstants {

    /**
     * Constants for PaaS, typically Cloud Foundry
     *
     */
    // Env variables
    public static final String PAAS_HOME = System.getenv("HOME");
    public static final String PAAS_MEMORY_LIMIT = System.getenv("MEMORY_LIMIT");
    public static final String PAAS_PORT = System.getenv("PORT");
    public static final String PAAS_PWD = System.getenv("PWD");
    public static final String PAAS_TMPDIR = System.getenv("TMPDIR");
    public static final String PAAS_USER = System.getenv("USER");
    public static final String PAAS_VCAP_APP_HOST = System.getenv("VCAP_APP_HOST");
    public static final String PAAS_VCAP_APPLICATION = System.getenv("VCAP_APPLICATION");
    public static final String PAAS_VCAP_APP_PORT = System.getenv("VCAP_APP_PORT");
    public static final String PAAS_VCAP_SERVICES = System.getenv("VCAP_SERVICES");
    // Application Instance specific variables
    public static final String PAAS_INSTANCE_ADDR = System.getenv("CF_INSTANCE_ADDR");
    public static final String PAAS_INSTANCE_INDEX = System.getenv("CF_INSTANCE_INDEX");
    public static final String PAAS_INSTANCE_IP = System.getenv("CF_INSTANCE_IP");
    public static final String PAAS_INSTANCE_PORT = System.getenv("CF_INSTANCE_PORT");
    public static final String PAAS_INSTANCE_PORTS = System.getenv("CF_INSTANCE_PORTS");

}
