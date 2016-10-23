/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.util;

import javax.transaction.Transaction;

import com.centurylink.mdw.dataaccess.DatabaseAccess;

public class TransactionWrapper {

	private boolean databaseConnectionAlreadyOpened;
	private boolean transactionAlreadyStarted;
	private boolean rollbackOnly;
	private Transaction transaction;
	private DatabaseAccess databaseAccess;
	
	public boolean isDatabaseConnectionAlreadyOpened() {
		return databaseConnectionAlreadyOpened;
	}
	public void setDatabaseConnectionAlreadyOpened(
			boolean databaseConnectionAlreadyOpened) {
		this.databaseConnectionAlreadyOpened = databaseConnectionAlreadyOpened;
	}
	public boolean isTransactionAlreadyStarted() {
		return transactionAlreadyStarted;
	}
	public void setTransactionAlreadyStarted(boolean transactionAlreadyStarted) {
		this.transactionAlreadyStarted = transactionAlreadyStarted;
	}
	public void setTransaction(Transaction transaction) {
		this.transaction = transaction;
	}
	public Transaction getTransaction() {
		return transaction;
	}
	public void setDatabaseAccess(DatabaseAccess databaseAccess) {
		this.databaseAccess = databaseAccess;
	}
	public DatabaseAccess getDatabaseAccess() {
		return databaseAccess;
	}
	public void setRollbackOnly(boolean rollbackOnly) {
		this.rollbackOnly = rollbackOnly;
	}
	public boolean isRollbackOnly() {
		return rollbackOnly;
	}
}
