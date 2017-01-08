/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.testing;

import java.util.ArrayList;

public class TestFileLine {

	static final char COMMENT_CHAR = '#';
	
	private int lineNumber;
	private ArrayList<String> words;
	
	TestFileLine(int line, ArrayList<String> wordList) {
		this.lineNumber = line;
		words = wordList;
	}
	
	TestFileLine(String firstWord) {
		this.lineNumber = 0;
		words = new ArrayList<String>();
		words.add(firstWord);
	}
	
	public String getCommand() {
		return words.get(0);
	}
	
	TestFileLine addWord(String word) {
		words.add(word);
		return this;
	}
	
	int getLineNumber() {
		return lineNumber;
	}
	
	int getWordCount() {
		return words.size();
	}
	
	public String getWord(int i) {
		return words.get(i);
	}

}
