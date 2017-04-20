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
package com.centurylink.mdw.drools;

import java.util.ArrayList;
import java.util.List;

//import org.drools.KnowledgeBase;
import org.kie.internal.KnowledgeBase;
import org.kie.internal.runtime.StatelessKnowledgeSession;
import org.drools.command.CommandFactory;
//import org.drools.runtime.StatelessKnowledgeSession;
import com.centurylink.mdw.annotations.RegisteredService;
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

@RegisteredService(AutoAssignStrategy.class)
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
