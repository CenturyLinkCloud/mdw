/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.task.strategy;

import java.util.ArrayList;
import java.util.List;

import org.drools.KnowledgeBase;
import org.drools.command.CommandFactory;
import org.drools.runtime.StatelessKnowledgeSession;

import com.centurylink.mdw.common.StrategyException;
import com.centurylink.mdw.constant.TaskAttributeConstant;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.task.TaskInstance;
import com.centurylink.mdw.model.user.User;
import com.centurylink.mdw.observer.ObserverException;
import com.centurylink.mdw.observer.task.AutoAssignStrategy;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.UserManager;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class RulesBasedAutoAssignStrategy extends RulesBasedStrategy implements AutoAssignStrategy {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();


    public User selectAssignee(TaskInstance taskInstanceVO) throws ObserverException{
      User user = new User();
      UserManager userManager = ServiceLocator.getUserManager();
      KnowledgeBase knowledgeBase = null;
      logger.info("Getting knowledgeBase");
      try
      {
        knowledgeBase = getKnowledgeBase();
      }
      catch (StrategyException e)
      {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

      StatelessKnowledgeSession knowledgeSession = knowledgeBase.newStatelessKnowledgeSession();

      List<Object> facts = new ArrayList<Object>();
      facts.add(getParameters());
      knowledgeSession.setGlobal("user", user);

      knowledgeSession.execute(CommandFactory.newInsertElements(facts));

      logger.info("Getting UserVO ::"+ user);

      try
      {
        user = userManager.getUser(user.getCuid());
        logger.info("Got UserVO ::"+ user);
      }
      catch (DataAccessException e)
      {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

      return user;

    }

    @Override
    protected String getKnowledgeBaseAttributeName() {
        return TaskAttributeConstant.AUTO_ASSIGN_RULES;
    }
}
