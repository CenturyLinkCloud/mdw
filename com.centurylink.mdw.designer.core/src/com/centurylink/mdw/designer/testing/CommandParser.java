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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;

public class CommandParser {
	
	private static final char EOF = Character.MAX_VALUE;

	private int parseLine;
	private int parseHead;
	private char parseBuffer[];
	private TestFile testfile;
	
	public CommandParser(TestFile file) {
		this.testfile = file;
	}
	
	private char getNextNonBlankChar() {
		while (parseHead<parseBuffer.length) {
			char ch = parseBuffer[parseHead++];
			if (ch=='\\') {
				if (parseHead+1<parseBuffer.length && parseBuffer[parseHead]=='\n') {
					// skip to next line
					parseLine++;
					parseHead++;
					ch = parseBuffer[parseHead++];
				} else if (parseHead+2<parseBuffer.length 
						&& parseBuffer[parseHead]=='\r' && parseBuffer[parseHead+1]=='\n') {
					// skip to next line
					parseLine++;
					parseHead += 2;
					ch = parseBuffer[parseHead++];
				}
			}
			if (ch!=' ' && ch!='\t' && ch!='\r') {
				if (ch=='\n') parseLine++;
				return ch;
			}
		}
		return EOF;
	}
	
	private char getNextChar() {
		char ch;
		if (parseHead>=parseBuffer.length) ch = EOF;
		else {
			ch = parseBuffer[parseHead++];
			if (ch=='\n') parseLine++;
		}
		return ch;
	}
	
	private void rollback(char ch) {
		parseHead--;
		if (ch=='\n') parseLine--;
	}
	
	private boolean nextIsTag(String tag) {
		int n = tag.length();
		if (parseHead+n<=parseBuffer.length) {
			for (int i=0; i<n; i++) {
				if (tag.charAt(i)!=parseBuffer[parseHead+i]) return false;
			}
			if (parseHead+n<parseBuffer.length) {
				char ch = parseBuffer[parseHead+n];
				if (ch!=' '&&ch!='\r'&& ch!='\n' && ch!='\t') return false;
			}
			parseHead += n;
			return true;
		}
		return false;
	}
	
	private String readNextWord(char firstChar) throws ParseException {
		StringBuffer sb = new StringBuffer();
		String word;
		if (firstChar=='"') {
			char ch = getNextChar();
			while (ch!='"' && ch!='\r' && ch!='\n' && ch!=EOF) {
				sb.append(ch);
				ch = getNextChar();
			}
			if (ch!='"') {
				throw new ParseException("double quote has no ending", parseLine);
			}
			word = sb.toString();
		} else if (firstChar=='<') {
			char ch = getNextChar();
			if (ch!='<') throw new ParseException("here arg must start with <<", parseLine);
			ch = getNextChar();
			while (ch!=' ' && ch!='\r' && ch!='\n' && ch!='\t' && ch!=EOF) {
				sb.append(ch);
				ch = getNextChar();
			}
			String tag = sb.append(">>").toString();
			while (ch!='\n' && ch!=EOF) {		// skip to the EOL
				if (ch!=' ' && ch!='\t' && ch!='\r')
					throw new ParseException("no characters allowed after here tag", parseLine);
				ch = getNextChar();	
			}
			if (ch==EOF) throw new ParseException("here arg end tag not found", parseLine);
			sb = new StringBuffer();
			while (!nextIsTag(tag)) {
				ch = getNextChar();
				while (ch!=EOF && ch!='\n') {
					if (ch!='\r') sb.append(ch);
					ch = getNextChar();
				}
				if (ch==EOF) break;
				sb.append(ch);
			}
			if (ch=='\n') sb.deleteCharAt(sb.length()-1);	// remove last new line
			if (ch==EOF) throw new ParseException("here arg end tag not found", parseLine);
			word = sb.toString();
		} else if (firstChar=='@') {
		    char ch = getNextChar();
            while (ch!=' ' && ch!='\r' && ch!='\n' && ch!=EOF) {
                sb.append(ch);
                ch = getNextChar();
            }
            if (ch=='\n') rollback(ch);
            String filename = sb.toString();
            try {
				word = new String(testfile.readFile(filename));
            } catch (FileNotFoundException e) {
    	        throw new ParseException("file not found: "+filename, parseLine);
    	    } catch (IOException e) {
    	        throw new ParseException("file IO exception: "+e.getMessage(), parseLine);
			}
		} else {
			char ch = firstChar;
			while (ch!=' ' && ch!='\r' && ch!='\n' && ch!=EOF) {
				sb.append(ch);
				ch = getNextChar();
			}
			if (ch=='\n') rollback(ch);
			word = sb.toString();
		}
		return word;
	}
	
	public ArrayList<TestFileLine> parse(char[] buffer) 
	    throws ParseException {
	    parseBuffer = buffer;
	    parseHead = 0;
	    parseLine = 1;
	    ArrayList<String> wordList;
	    ArrayList<TestFileLine> lines = new ArrayList<TestFileLine>();
	    char ch = getNextNonBlankChar();
	    while (ch!=EOF) {
	        if (ch=='\n') {		// blank line - ignore
	            ch = getNextNonBlankChar();
	        } else {
	            int line = parseLine;
	            wordList = new ArrayList<String>();
	            while (ch!=EOF && ch!='\n') {
	                String word = readNextWord(ch);
	                wordList.add(word);
	                ch = getNextNonBlankChar();
	            }
	            lines.add(new TestFileLine(line, wordList));
	        }	
	    }
	    return lines;

	}

}
