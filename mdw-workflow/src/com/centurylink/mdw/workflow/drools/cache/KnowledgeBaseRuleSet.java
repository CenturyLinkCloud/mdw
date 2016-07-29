/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.drools.cache;

import org.drools.KnowledgeBase;

import com.centurylink.mdw.model.value.attribute.RuleSetVO;

/**
 * Represents a compiled Drools KnowledgeBase (along with its underlying RuleSet).
 */
public class KnowledgeBaseRuleSet {
  
  private RuleSetVO ruleSet;
  public RuleSetVO getRuleSet() { return ruleSet; }
  
  private KnowledgeBase knowledgeBase;
  public KnowledgeBase getKnowledgeBase() { return knowledgeBase; }
  
  public KnowledgeBaseRuleSet(KnowledgeBase knowledgeBase, RuleSetVO ruleSet) {
      this.knowledgeBase = knowledgeBase;
      this.ruleSet = ruleSet;
  }

}
