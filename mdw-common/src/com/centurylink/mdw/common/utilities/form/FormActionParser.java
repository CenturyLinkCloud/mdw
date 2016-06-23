/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.utilities.form;

import java.util.ArrayList;
import java.util.List;

public class FormActionParser {

	private String functionName;
	private List<String> arguments;

	public FormActionParser(String action) throws Exception {
		arguments = new ArrayList<String>();
		if (action==null) throw new Exception("Null action");
		int k1 = action.indexOf('(');
		if (action.startsWith("hyperlink:")) {
			setFunctionName("hyperlink");
			arguments.add(action.substring("hyperlink:".length()));
		} else if (k1>0) {
			setFunctionName(action.substring(0,k1).trim());
			int k2 = action.lastIndexOf(')');
			if (k2<k1) throw new Exception("Invalid command syntax");
			String[] args = action.substring(k1+1,k2).split(", *");
			for (String a : args) {
				int n = a.length();
				if (n>=2) {
					if (a.charAt(0)=='\'' && a.charAt(n-1)=='\''
						|| a.charAt(0)=='"' && a.charAt(n-1)=='"')
						a = a.substring(1,n-1);
				}
				arguments.add(a);
			}
		} else {
			setFunctionName("perform_action");
			arguments.add(action);
		}
	}

	public void setFunctionName(String functionName) {
		this.functionName = functionName;
	}

	public String getFunctionName() {
		return functionName;
	}
	
	public String getArgument(int i) {
		if (i<arguments.size()) return arguments.get(i);
		else return null;
	}
	
	public String getArgumentEscaped(int i) {
		return arguments.get(i).replaceAll("&", "&amp;");
	}
	
	public int getArgumentCount() {
		return arguments.size();
	}
	
	public String getArgumentQuoted(int i) {
		String v = arguments.get(i).replaceAll("&", "&amp;");
		if (v.equals("true") || v.equals("false")) return v;
		else return "\"" + v + "\"";
	}

}
