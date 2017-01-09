/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.utils;

//import groovy.lang.Binding;
//import groovy.lang.GroovyShell;
//import groovy.lang.Script;

import java.util.List;

//import org.mozilla.javascript.Context;
//import org.mozilla.javascript.ContextAction;
//import org.mozilla.javascript.ContextFactory;
//import org.mozilla.javascript.EcmaError;
//import org.mozilla.javascript.EvaluatorException;
//import org.mozilla.javascript.Scriptable;


import org.w3c.dom.Document;

import com.centurylink.mdw.model.value.variable.VariableVO;
import com.qwest.mbeng.DomDocument;
import com.qwest.mbeng.MbengException;
import com.qwest.mbeng.MbengRuleSet;
import com.qwest.mbeng.MbengTableArray;

public class ScriptSyntaxValidator
{
  public static final String MAGICBOX = "MagicBox";
  public static final String JAVASCRIPT = "JavaScript";
  public static final String GROOVY = "Groovy";

  private String type;
  private String language;
  private List<VariableVO> variables;
  
  public ScriptSyntaxValidator(String type, String language, List<VariableVO> variables)
  {
    this.type = type;
    this.language = language;
    this.variables = variables;
  }
  
  public void verifySyntax(String script) throws MbengException
  {
    if (language == null || language.equals(MAGICBOX))
    {
      verifyMagicBoxSyntax(script);
    }
    else if (language.equals(JAVASCRIPT))
    {
      verifyJavaScriptSyntax(script);
    }
    else if (language.equals(GROOVY))
    {
      verifyGroovyScriptSyntax(script);
    }
  }
  
  private void verifyMagicBoxSyntax(String script) throws MbengException
  {
    MbengRuleSet ruleset = new MbengRuleSet("validate",
        type.equals("EXPRESSION") ? MbengRuleSet.RULESET_EXPR
            : type.equals("CONDITION") ? MbengRuleSet.RULESET_COND : MbengRuleSet.RULESET_RULE,
        false, false);
    String varName, varType;
    for (int i = 0; i < variables.size(); i++)
    {
      varName = variables.get(i).getVariableName();
      varType = variables.get(i).getVariableType();
      if (varType.equals(MbengTableArray.class.getName()))
        ruleset.defineTable(varName, varType, null, null);
      else if (varType.equals(Document.class.getName()))
        ruleset.defineDocument(varName, DomDocument.class.getName());
      else
        ruleset.defineVariable(varName, true);
    }
    ruleset.defineVariable(VariableVO.MASTER_REQUEST_ID, false);
    ruleset.parse(script);
  }

  private void verifyGroovyScriptSyntax(String scriptString)
  {
//    if (scriptString == null || scriptString.length() == 0)
//      throw new EvaluatorException("Empty Script Text");
//    try
//    {
//      GroovyShell shell = new GroovyShell(getClass().getClassLoader());
//      Script script = shell.parse(scriptString);
//
//      Binding binding = new Binding();
//      for (VariableVO variableVO : variables)
//      {
//        binding.setVariable(variableVO.getVariableName(), null);
//      }
//      script.setBinding(binding);
//      script.run();
//    }
//    catch (Exception ex)
//    {
//        String msg = ex.getMessage();
//        if (msg==null) msg = ex.getClass().getName();
//        ex.printStackTrace();
//        throw new EvaluatorException(msg);
//    }
  }

  private void verifyJavaScriptSyntax(final String script)
  {
//    if (script == null || script.length() == 0)
//      throw new EvaluatorException("Empty Script Text");
//
//    ContextFactory cf = new ContextFactory();
//    cf.call(new ContextAction()
//    {
//
//      public Object run(Context cx)
//      {
//        Scriptable scope = cx.initStandardObjects();
//
//        for (VariableVO variableVO : variables)
//        {
//          scope.put(variableVO.getVariableName(), scope, null);
//        }
//
//        try
//        {
//          cx.evaluateString(scope, script, "Line", 1, null);
//        }
//        catch (EcmaError ecmaerror)
//        {
//          throw new EvaluatorException(ecmaerror.getMessage()
//              + "\n Either define this variable locally or as process variable");
//        }
//        return null;
//      }
//    });
  }

}
