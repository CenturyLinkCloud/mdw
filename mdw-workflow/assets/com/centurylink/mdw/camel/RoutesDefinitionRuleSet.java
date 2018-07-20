/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.camel;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.RoutesDefinition;

import com.centurylink.mdw.model.asset.Asset;

/**
 * Represents a compiled Drools KnowledgeBase (along with its underlying RuleSet).
 */
public class RoutesDefinitionRuleSet {

  private Asset ruleSet;
  public Asset getRuleSet() { return ruleSet; }

  private RoutesDefinition routesDefinition;
  public RoutesDefinition getRoutesDefinition() { return routesDefinition; }

  private List<String> routeIds = new ArrayList<String>();
  public List<String> getRouteIds() { return routeIds; }

  public RoutesDefinitionRuleSet(RoutesDefinition routes, Asset ruleSet) {
      this.routesDefinition = routes;
      this.ruleSet = ruleSet;

      for (RouteDefinition routeDef : routesDefinition.getRoutes()) {
          routeIds.add(routeDef.getId());
      }
  }

}