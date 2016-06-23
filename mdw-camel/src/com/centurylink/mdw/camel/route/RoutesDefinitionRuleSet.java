/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.camel.route;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.RoutesDefinition;

import com.centurylink.mdw.model.value.attribute.RuleSetVO;

/**
 * Represents a compiled Drools KnowledgeBase (along with its underlying RuleSet).
 */
public class RoutesDefinitionRuleSet {
  
  private RuleSetVO ruleSet;
  public RuleSetVO getRuleSet() { return ruleSet; }
  
  private RoutesDefinition routesDefinition;
  public RoutesDefinition getRoutesDefinition() { return routesDefinition; }
  
  private List<String> routeIds = new ArrayList<String>();
  public List<String> getRouteIds() { return routeIds; }
  
  public RoutesDefinitionRuleSet(RoutesDefinition routes, RuleSetVO ruleSet) {
      this.routesDefinition = routes;
      this.ruleSet = ruleSet;
      
      for (RouteDefinition routeDef : routesDefinition.getRoutes()) {
          routeIds.add(routeDef.getId());
      }
  }

}