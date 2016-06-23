/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.script;

import groovy.lang.Binding;
import groovy.lang.MissingPropertyException;

import java.util.Map;

/**
 * Groovy executor that's compatible with old-style (pre-MDW 5) scripting
 * conventions.  Specifically, older scripts don't specify return values.
 */
@Deprecated
public class GroovyExecutorCompatible extends GroovyExecutor implements ScriptExecutor, ScriptEvaluator
{

  /**
   * For backward compatibility with pre-MDW 5 scripts (support for vReturnCode).
   */
  @Override
  public Object execute(String script, Map<String, Object> bindings) throws ExecutionException
  {
    Object retObj = super.execute(script, bindings);
    Object vReturnCode = null;
    
    Binding binding = getBinding();
    try
    {
      vReturnCode = binding.getVariable("vReturnCode");
    }
    catch (MissingPropertyException mpe)
    {
      // vReturnCode not set
    }
    
    if (vReturnCode != null)
      bindings.put("vReturnCode", vReturnCode);
    
    return retObj;
  }

}
