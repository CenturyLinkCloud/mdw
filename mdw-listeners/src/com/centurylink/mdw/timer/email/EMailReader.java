/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.timer.email;




// CUSTOM IMPORTS -----------------------------------------------------

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TimerTask;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;

import org.exjello.mail.ExchangeConstants;
import org.exjello.mail.ExchangeStore;

import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.exception.PropertyException;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.listener.email.MDWEmailListener;

// JAVA IMPORTS -------------------------------------------------------

/**
 * The start up monitor, that monitors the task state
 */
public class EMailReader extends TimerTask{

    // CONSTANTS ------------------------------------------------------
    protected static final String FOLDER_INBOX = "INBOX";
    protected static final String HOST = "host";
    protected static final String USER_NAME = "username";
    protected static final String PASSWORD = "password";
    protected static final String DELETE_EMAIL = "deleteEmail";
    protected static final String DEFAULT_HOST = "qtdenexmbm22.AD.QINTRA.COM";

	// CLASS VARIABLES ------------------------------------------------
    protected static StandardLogger logger = LoggerUtil.getStandardLogger();
	// INSTANCE VARIABLES ---------------------------------------------

	// CONSTRUCTORS ---------------------------------------------------
    /**
     * Default Constructor
     */
    public EMailReader(){
        super();
    }

	// PUBLIC AND PROTECTED METHODS -----------------------------------
	/**
	 * Method that gets invoked periodically by the container
     *
	 */
	public void run(){
       logger.info("methodEntry-->EMailReader.run()");
        try {
			Map<String,Properties> emailParams = getEmailParamProperties();
			for (String name : emailParams.keySet()) {
				Properties nameProps = emailParams.get(name);
				String username = (String)nameProps.get(USER_NAME);
				String password = (String)nameProps.get(PASSWORD);
				String host = (String) nameProps.get(HOST);
				String deleteEmail = (String) nameProps.get(DELETE_EMAIL);
				
				if (StringHelper.isEmpty(username)) {
					logger.severe(" Username is empty ");
					return;
				}
				
				if (StringHelper.isEmpty(password)) {
					logger.severe(" Password is empty ");
					return;
				}
				
				if (StringHelper.isEmpty(host)) {
					host = DEFAULT_HOST;
				}
				processInbox(host, username, password,deleteEmail);
			}
		} catch (Exception e) {
			logger.severeException(e.getMessage(), e);
		}
       logger.info("methodExit-->EMailReader.run()");
    }

    /**
	 * Method that processes Inbox
     * @param pType
     * @param pHost
     * @param pUserId
     * @param pPaswword
     * @param pPort
     *
	 */
	private void processInbox(String pHost, String pUserId, String pPassword, String deleteEmail){
       logger.info("methodEntry-->EMailReader.processInbox():"+pUserId);

        Folder inbox = null;
        Store store = null;
        try{
        	Properties props = System.getProperties();
			// Use UNFILTERED_PROPERTY if you want read and unreaded emails
			props.setProperty(ExchangeConstants.UNFILTERED_PROPERTY,String.valueOf(false));
			props.setProperty(ExchangeConstants.DELETE_PROPERTY,String.valueOf(deleteEmail));
			Session session = Session.getInstance(props, null);
			store = new ExchangeStore(session, null);
			store.connect(pHost,pUserId,pPassword);
			
			inbox = store.getFolder(FOLDER_INBOX);
			inbox.open(Folder.READ_WRITE);
			Message[] messages = inbox.getMessages();
        	
			if (messages.length > 0) {
				MDWEmailListener mdwEmailListener = new MDWEmailListener();
				mdwEmailListener.process(messages);
			} else {
				logger.info("There are no new messages to process ");
			}
        }catch(Throwable ex){
            logger.severeException(ex.getMessage(), ex);
        }finally{
            try{
                if(inbox != null)
                    inbox.close(true);
                 if(store != null)
                    store.close();
            }catch(MessagingException ex1){
            	logger.severeException(ex1.getMessage(), ex1);
            }
        }

       logger.info("methodExit-->processInbox():"+pUserId);
    }
    
    protected Map<String,Properties> getEmailParamProperties() throws PropertyException{
    	Map<String,Properties> emailProps = new HashMap<String, Properties>();
    	Properties emailListenerProperties = PropertyManager.getInstance().getProperties(PropertyNames.MDW_EMAIL_LISTENER);
    	for (String pn : emailListenerProperties.stringPropertyNames()) {
     		String[] pnParsed = pn.split("\\.");
     		if (pnParsed.length==5) {
     			String name = pnParsed[3];
     			String attrname = pnParsed[4];
     			Properties procspec = emailProps.get(name);
     			if (procspec==null) {
     				procspec = new Properties();
     				emailProps.put(name, procspec);
     			}
     			String value = emailListenerProperties.getProperty(pn);
     			procspec.put(attrname, value);
     		}
     	}
    	return emailProps;
    }


     /**
     * Method that process the new email message  the pMessage
     * @param pMessage
     */
    protected boolean processNewEMailMessage(Message pMessage, String pUserId) {
    	return false;
    }


    // PRIVATE METHODS ------------------------------------------------


	// ACCESSOR METHODS -----------------------------------------------

	// INNER CLASSES --------------------------------------------------

}
