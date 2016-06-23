/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.utilities;

import java.sql.Connection;

import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;

public class TransactionUtil {
    
    private static TransactionUtil instance;
    private static ThreadLocal<Connection> currentConnection;

    public static TransactionUtil getInstance() {
    	if (instance==null) {
    		instance = new TransactionUtil();
    		currentConnection = new ThreadLocal<Connection>();
    	}
        return instance;
    }
    
    /**
     * Returns the current transaction
     */
    public Transaction getTransaction() {
        try {
            return getTransactionManager().getTransaction();
        }
        catch (Exception ex) {
        	StandardLogger logger = LoggerUtil.getStandardLogger();
            logger.severeException(ex.getMessage(), ex);
            return null;
        }
    }
    
    /**
     * Returns transaction manager
     */
    public TransactionManager getTransactionManager() {
    	TransactionManager transMgr = null;
    	try {
    		String jndiName = ApplicationContext.getNamingProvider().getTransactionManagerName();
    		Object txMgr = ApplicationContext.getNamingProvider().lookup(null, jndiName, TransactionManager.class);
    		transMgr = (TransactionManager)txMgr;
    	} catch (Exception ex) {
        	StandardLogger logger = LoggerUtil.getStandardLogger();
    		logger.severeException(ex.getMessage(), ex);
    	}
    	return transMgr;
    }
    
//    /**
//     * Create a user transaction
//     */
//    public UserTransaction createTransaction() {
//        try {
//        	String jndiName = ApplicationContext.getNamingProvider().getUserTransactionName();
//        	UserTransaction userTrans = (UserTransaction)ApplicationContext.getNamingProvider()
//        		.lookup(null, jndiName, UserTransaction.class);
//        	userTrans.begin();
//        	return userTrans;
//        } catch (Exception ex) {
//        	StandardLogger logger = LoggerUtil.getStandardLogger();
//            logger.severeException(ex.getMessage(), ex);
//            return null;
//        }
//    }
    
	public boolean isInTransaction() throws SystemException {
		TransactionManager transManager = getTransactionManager();
		return (transManager.getStatus()==Status.STATUS_ACTIVE);
	}
	
	public Connection getCurrentConnection() {
		return currentConnection.get();
	}
	
	public void setCurrentConnection(Connection connection) {
		currentConnection.set(connection);
	}
	
	public static void clearCurrentConnection() {
	    if (currentConnection != null) {
	      currentConnection.remove();
	    }
	}

}
