/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.testing;

import com.qwest.mbeng.DomDocument;
import com.qwest.mbeng.JsonDocument;
import com.qwest.mbeng.MbengDocument;
import com.qwest.mbeng.MbengException;
import com.qwest.mbeng.MbengNode;
import com.qwest.mbeng.MbengRuleSet;
import com.qwest.mbeng.MbengRuntime;
import com.qwest.mbeng.StreamLogger;
import com.qwest.mbeng.XmlPath;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TestDataFilter {

	// special place holder
	public static final String MasterRequestId = "MasterRequestId";
	public static final String RunNumber = "RunNumber";
	// special token
	public static final String AnyNumberToken = "#";
	public static final String AnyNumberTokenAndMeta = "#{#}";

	private String rawdata;
	private boolean onlyWithHash;
	private PrintStream log;

	private class FilterToken {
		int offset;
		int length;
		String token;
		boolean isPlaceHolder;
	}

	private List<FilterToken> filterTokens;

	public TestDataFilter(String rawdata, PrintStream log, boolean onlyWithHash) {
		this.rawdata = rawdata;
		this.log = log;
		this.onlyWithHash = onlyWithHash;
		collectFilterTokens();
	}

	public List<String> getUniquePlaceHolders() {
		List<String> holders = new ArrayList<String>();
		for (FilterToken token : filterTokens) {
			if (token.isPlaceHolder) {
            	if (!holders.contains(token.token)) holders.add(token.token);
			}
		}
		return holders;
	}

	private void collectFilterTokens() {
        int k, i, n;
        n = rawdata.length();
        filterTokens = new ArrayList<FilterToken>();
        for (i=0; i<n; i++) {
            char ch = rawdata.charAt(i);
            if (ch=='#' && i+3<n && rawdata.charAt(i+1)=='{') {
            	// handling place holders #{$place_holder} and expressions #{....}
            	ch = rawdata.charAt(i+2);
            	boolean isPlaceHolder = ch=='$';
            	k = i+3;
            	while (k<n) {
            		ch = rawdata.charAt(k);
            		if (ch=='}') break;
                    if (isPlaceHolder && ch!='_' && !Character.isLetterOrDigit(ch))
                    	isPlaceHolder = false;
                    if (ch=='\n' || ch=='\r') break;
                    k++;
            	}
            	if (k<n && ch=='}') {
                	FilterToken token = new FilterToken();
                	token.offset = i;
                	token.length = k-i+1;
                	token.token = isPlaceHolder?rawdata.substring(i+3,k):rawdata.substring(i+2,k);
                	token.isPlaceHolder = isPlaceHolder;
                	filterTokens.add(token);
                	i = k;
            	}
            } else if (!onlyWithHash && ch=='{' && i+2<n && rawdata.charAt(i+1)=='$') {
            	// backward compatibility handling {$place_holder}
                k = i+2;
                while (k<n) {
                    ch = rawdata.charAt(k);
                    if (ch=='}') break;
                    if (ch!='_' && !Character.isLetterOrDigit(ch)) break;
                    k++;
                }
                if (k<n && ch=='}') {
                	FilterToken token = new FilterToken();
                	token.offset = i;
                	token.length = k-i+1;
                	token.token = rawdata.substring(i+2,k);
                	token.isPlaceHolder = true;
                	filterTokens.add(token);
                	i = k;
                }
            } else if (ch=='%' && i+4<n && rawdata.charAt(i+1)=='%' &&
            		rawdata.charAt(i+2)=='M' && rawdata.charAt(i+3)=='%' && rawdata.charAt(i+4)=='%') {
            	// backward compatibility handling %%M%%
            	FilterToken token = new FilterToken();
            	token.offset = i;
            	token.length = 5;
            	token.token = MasterRequestId;
            	token.isPlaceHolder = true;
            	filterTokens.add(token);
            	i += 4;
            }
        }
    }

	public String applyFilters(Map<String,String> parameters, MbengDocument refdata) {
		if ((parameters==null || parameters.size()==0) && refdata==null) return rawdata;
		int m = filterTokens.size();
		if (m==0) return rawdata;
		StringBuffer sb = new StringBuffer();
		int k = 0;
		int n = rawdata.length();
		FilterToken token = filterTokens.get(0);
		for (int i=0; i<n; i++) {
			if (token==null || i<token.offset) sb.append(rawdata.charAt(i));
			else {
				String v;
				if (token.isPlaceHolder) {
					v = parameters==null?null:parameters.get(token.token);
				} else if (token.equals(AnyNumberToken)) {
					v = AnyNumberTokenAndMeta;	// process in a later phase
				} else {
					v = refdata==null?null:evaluateExpression(token.token, refdata);
				}
				if (v!=null) {
					sb.append(v);
				} else {
					sb.append(rawdata.substring(i,i+token.length));
				}
				i = i + token.length - 1;
				k++;
				token = k<m?filterTokens.get(k):null;
			}
		}
		return sb.toString();
	}

	public String applyAnyNumberFilters(String expected, String actual) {
		int k = expected.indexOf(AnyNumberTokenAndMeta);
		int n = actual.length();
		while (k>=0) {
			if (k>=n) break;
			StringBuffer nb = new StringBuffer();
			int m=k;
			while (m<n) {
				char ch = actual.charAt(m);
				if (!Character.isDigit(ch)) break;
				nb.append(ch);
				m++;
			}
			expected = expected.substring(0,k) + nb.toString()
				+ expected.substring(k+AnyNumberTokenAndMeta.length());
			k = expected.indexOf(AnyNumberTokenAndMeta);
		}
		return expected;
	}

	private String evaluateExpression(String exp, MbengDocument refdata) {
		try {
			if (exp.startsWith("/")) {	// assuming XPath expression
				XmlPath xpathobj = new XmlPath(exp);
				MbengNode node = xpathobj.findNode(refdata);
				return node==null?null:node.getValue();
			} else {
				MbengRuleSet ruleset = new MyRuleSet("filter_exp", MbengRuleSet.RULESET_EXPR);
				ruleset.parse(exp);
				MbengRuntime runtime = new MbengRuntime(ruleset, new StreamLogger(System.out));
				runtime.bind("$", refdata);
				return runtime.evaluate();
			}
		} catch (MbengException e) {
			log.println("Exception in evaluating expression in test data filter "
					+ exp + ": " + e.getMessage());
			return null;
		}
	}

    private class MyRuleSet extends MbengRuleSet {
        MyRuleSet(String name, char type) throws MbengException {
            super(name, type, true, false);
        }

        @Override
        protected boolean isSectionName(String name) {
            return true;
        }
    }

	public static MbengDocument parseRequest(String request) throws MbengException {
		MbengDocument reqdoc;
		if (request.startsWith("<")) {
			reqdoc = new DomDocument(request);
		} else if (request.startsWith("{")) {
			reqdoc = new JsonDocument(request);
		} else {
			throw new MbengException("0", "Request does not look like XML or JSON");
		}
		return reqdoc;
	}

	public static void loadPlaceHolderMap(File mapfile, Map<String,String> map, int row) throws IOException {
		if (!mapfile.exists()) return;
		InputStream is = null;
		BufferedReader reader = null;
		try {
			is = new FileInputStream(mapfile);
			reader = new BufferedReader(new InputStreamReader(is));
			String aLine = reader.readLine();
			if (aLine==null) return;
			String[] names = aLine.split(",");
			List<String> lines = new ArrayList<String>();
			while((aLine = reader.readLine()) != null) {
				if (aLine.length()>0) lines.add(aLine);
			}
			if (lines.size()==0) return;
			String[] values = lines.get(row%lines.size()).split(",");
			for (int j=0; j<names.length&&j<values.length; j++) {
				map.put(names[j], values[j]);
			}
		} finally {
		    if (reader != null)
		        reader.close();
			if (is != null)
			    is.close();
		}
	}

}
