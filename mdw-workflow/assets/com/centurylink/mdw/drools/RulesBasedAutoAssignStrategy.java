package com.centurylink.mdw.drools;

import java.util.ArrayList;
import java.util.List;

import org.kie.api.KieBase;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.internal.command.CommandFactory;

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
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

@RegisteredService(AutoAssignStrategy.class)
public class RulesBasedAutoAssignStrategy extends RulesBasedStrategy implements AutoAssignStrategy {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();


    @SuppressWarnings("unchecked")
    public User selectAssignee(TaskInstance taskInstanceVO) throws ObserverException{
      User user = new User();
      KieBase knowledgeBase = null;
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
      if (knowledgeBase != null) {
          StatelessKieSession knowledgeSession = knowledgeBase.newStatelessKieSession();
          List<Object> facts = new ArrayList<>();
          facts.add(getParameters());
          knowledgeSession.setGlobal("user", user);

          knowledgeSession.execute(CommandFactory.newInsertElements(facts));
      }

      logger.info("Getting UserVO ::" + user);

      try {
          user = ServiceLocator.getUserServices().getUser(user.getCuid());
          logger.info("Got UserVO ::" + user);
      } catch (DataAccessException e) {
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
