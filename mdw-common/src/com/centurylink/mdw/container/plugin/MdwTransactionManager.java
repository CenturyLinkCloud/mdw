/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.container.plugin;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

public class MdwTransactionManager implements TransactionManager {
	
	private static ThreadLocal<MdwTransactionManager> transManager;
	
	private MdwTransaction transaction;
	
	private MdwTransactionManager() {
		transaction = null;
	}

	@Override
	public void begin() throws NotSupportedException, SystemException {
		transaction = new MdwTransaction();
	}

	@Override
	public void commit() throws HeuristicMixedException,
			HeuristicRollbackException, IllegalStateException,
			RollbackException, SecurityException, SystemException {
		transaction.commit();
		transaction = null;
	}

	@Override
	public int getStatus() throws SystemException {
		return transaction==null?Status.STATUS_NO_TRANSACTION:transaction.getStatus();
	}

	@Override
	public Transaction getTransaction() throws SystemException {
		if (transaction==null) return null;
		if (transaction.status==Status.STATUS_COMMITTED) return null;
		return transaction;
	}
	
	public static void clearTransactionManager() {
	    if (transManager != null)
	      transManager.remove();
	}

	@Override
	public void resume(Transaction arg0) throws IllegalStateException,
			InvalidTransactionException, SystemException {
		// not used
	}

	@Override
	public void rollback() throws IllegalStateException, SecurityException,
			SystemException {
		transaction.rollback();
		transaction = null;
	}

	@Override
	public void setRollbackOnly() throws IllegalStateException, SystemException {
		transaction.setRollbackOnly();
	}

	@Override
	public void setTransactionTimeout(int arg0) throws SystemException {
		// TODO Auto-generated method stub
	}

	@Override
	public Transaction suspend() throws SystemException {
		return null;		// not implemented
	}
	
	public synchronized static TransactionManager getInstance() {
		if (transManager==null) transManager = new ThreadLocal<MdwTransactionManager>();
		MdwTransactionManager transmgr = transManager.get();
		if (transmgr == null) {
			transmgr = new MdwTransactionManager();
			transManager.set(transmgr);
		}
		return transmgr;
	}
	
	class MdwTransaction implements Transaction {

		private int status;
		
		MdwTransaction() {
			status = Status.STATUS_ACTIVE;
		}

		@Override
		public void commit() throws HeuristicMixedException,
				HeuristicRollbackException, RollbackException,
				SecurityException, SystemException {
			status = Status.STATUS_COMMITTED;
		}

		@Override
		public boolean delistResource(XAResource arg0, int arg1)
				throws IllegalStateException, SystemException {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean enlistResource(XAResource arg0)
				throws IllegalStateException, RollbackException,
				SystemException {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public int getStatus() throws SystemException {
			return status;
		}

		@Override
		public void registerSynchronization(Synchronization arg0)
				throws IllegalStateException, RollbackException,
				SystemException {
			// not used
		}

		@Override
		public void rollback() throws IllegalStateException, SystemException {
			status = Status.STATUS_ROLLEDBACK;
		}

		@Override
		public void setRollbackOnly() throws IllegalStateException,
				SystemException {
			status = Status.STATUS_MARKED_ROLLBACK;			
		}
		
	}

}
