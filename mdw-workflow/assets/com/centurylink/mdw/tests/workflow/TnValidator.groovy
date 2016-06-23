package com.centurylink.mdw.tests.workflow;

import groovy.lang.Closure;

public class TnValidator
{
  boolean validate(tn)
  {
    println 'validating tn (6): ' + tn;
    String[] parts = tn.split('-');
    return parts.length == 3 && parts[0].length() == 3 && parts[1].length() == 3 && parts[2].length() == 4;
  }

  Closure registerTn = { println("registering tn 1: " + it); }
}