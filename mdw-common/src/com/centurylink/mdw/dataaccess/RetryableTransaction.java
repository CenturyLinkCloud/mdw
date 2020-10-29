package com.centurylink.mdw.dataaccess;

import com.centurylink.mdw.util.TransactionWrapper;

public interface RetryableTransaction {

    Object initTransactionRetry(TransactionWrapper transaction) throws DataAccessException;

    boolean canRetryTransaction(Throwable e);

    Object getTransactionRetrier() throws DataAccessException;

}
