package com.centurylink.mdw.container;

import javax.naming.NamingException;

/**
 * Container context provider
 */
public interface ContextProvider {

    String TRANSACTION_MANAGER_SYSTEM_PROPERTY = "com.centurylink.mdw.transaction.manager";

    // standard JavaEE resource names
    String JAVA_TRANSACTION_MANAGER = "javax.transaction.TransactionManager";

    String getTransactionManagerName();
    int getServerPort() throws Exception;

    Object lookup(String hostPort, String name, Class<?> cls) throws NamingException;
}
