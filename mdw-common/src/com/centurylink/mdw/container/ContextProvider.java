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
package com.centurylink.mdw.container;

import java.rmi.Remote;

import javax.naming.NamingException;

/**
 * Container context provider
 */
public interface ContextProvider {

    String TRANSACTION_MANAGER_SYSTEM_PROPERTY = "com.centurylink.mdw.transaction.manager";

    // standard JavaEE resource names
    String JAVA_TRANSACTION_MANAGER = "javax.transaction.TransactionManager";

    String qualifyJmsQueueName(String name);
    String qualifyJmsTopicName(String name);

    String getTransactionManagerName();
    int getServerPort() throws Exception;

    Object lookup(String hostPort, String name, Class<?> cls) throws NamingException;
    void bind(String name, Remote object) throws NamingException;
    void unbind(String name) throws NamingException;

}
