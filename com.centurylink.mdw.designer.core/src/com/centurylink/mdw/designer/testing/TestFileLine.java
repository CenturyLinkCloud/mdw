/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
