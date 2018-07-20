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
