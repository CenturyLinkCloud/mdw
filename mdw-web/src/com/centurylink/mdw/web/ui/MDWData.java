/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.ui;

import java.util.ArrayList;

import com.qwest.mbeng.MbengNode;


public class MDWData 
{
    private ArrayList<String> names;
    private ArrayList<String> values;
    
    public MDWData(MbengNode node) {
    	MbengNode child;
    	names = new ArrayList<String>();
    	values = new ArrayList<String>();
    	for (child=node.getFirstChild(); child!=null; child=child.getNextSibling()) {
    		names.add(child.getName());
    		values.add(child.getValue());
    	}
    }
    
    public String getValue(String name) {
    	for (int i=0; i<names.size(); i++) {
    		if (names.get(i).equals(name)) return values.get(i);
    	}
    	return null;
    }
    
    public String getName(int i) {
    	return names.get(i);
    }
    
    public String getValue(int i) {
    	return values.get(i);
    }
    
    public int getSize() {
    	return names.size();
    }

}
